package ingatlanok.portal.ingatlancom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ingatlanok.common.AppLogger;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawExtractionResult;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalSupport;
import ingatlanok.portal.sidecar.SidecarCliExecutor;
import ingatlanok.portal.sidecar.SidecarResponse;

public class IngatlanComSidecarStrategy implements ExtractionStrategy {

  private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanComSidecarStrategy.class);
  private static final long CACHE_TTL_MS = 180_000L; // 3 minutes TTL to avoid Cloudflare 429 burst limit
  private static final Map<String, CachedResult> RESPONSE_CACHE = new ConcurrentHashMap<>();

  private final IngatlanComUrlBuilder urlBuilder;

  public IngatlanComSidecarStrategy() {
    this(new IngatlanComUrlBuilder());
  }

  public IngatlanComSidecarStrategy(IngatlanComUrlBuilder urlBuilder) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new IngatlanComUrlBuilder();
  }

  public static void clearCache() {
    RESPONSE_CACHE.clear();
  }

  @Override
  public ExtractionMethod method() {
    return ExtractionMethod.SIDECAR;
  }

  @Override
  public RawExtractionResult extract(SearchCriteria criteria) {
    SearchCriteria safeCriteria = PortalSupport.normalizeCriteria(criteria);
    String searchUrl = urlBuilder.buildSearchUrl(safeCriteria);

    CachedResult cached = RESPONSE_CACHE.get(searchUrl);
    long now = System.currentTimeMillis();
    if (cached != null && (now - cached.timestampMs()) < CACHE_TTL_MS) {
      LOGGER.info(
          "Ingatlan.com: Sidecar gyorsítótár találat ({} db hirdetés) -> {}",
          cached.listings().size(),
          searchUrl);
      return RawExtractionResult.success(cached.listings(), method());
    }

    if (!SidecarCliExecutor.isAvailable()) {
      return RawExtractionResult.empty(method(), searchUrl);
    }

    LOGGER.info("Ingatlan.com: Sidecar modul detektálva, lekérdezés indítása...");
    try {
      Optional<SidecarResponse> sidecarResp =
          SidecarCliExecutor.execute("ingatlancom", safeCriteria);
      if (sidecarResp.isPresent()) {
        SidecarResponse resp = sidecarResp.get();
        if (resp.isSuccess()) {
          List<RawPropertyListing> rawListings = mapSidecarItems(resp.getItems(), safeCriteria);
          if (!rawListings.isEmpty()) {
            RESPONSE_CACHE.put(searchUrl, new CachedResult(List.copyOf(rawListings), now));
            LOGGER.info(
                "Ingatlan.com: Sidecar sikeresen kinyert {} db hirdetést", rawListings.size());
            return RawExtractionResult.success(rawListings, method());
          }
        } else if (cached != null && !cached.listings().isEmpty()) {
          LOGGER.warn(
              "Ingatlan.com: Sidecar hiba ({}), korábbi gyorsítótárazott találatok ({} db) visszaadása",
              resp.getErrorMessage(),
              cached.listings().size());
          return RawExtractionResult.success(cached.listings(), method());
        } else {
          String err =
              resp.getErrorMessage() != null ? resp.getErrorMessage() : "Cloudflare WAF / 429";
          return RawExtractionResult.botProtected(method(), searchUrl, err);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Ingatlan.com: Hiba a sidecar végrehajtás során: {}", e.getMessage(), e);
    }

    return RawExtractionResult.empty(method(), searchUrl);
  }

  private List<RawPropertyListing> mapSidecarItems(
      List<SidecarResponse.SidecarItem> items, SearchCriteria criteria) {
    List<RawPropertyListing> rawListings = new ArrayList<>();
    Set<String> processedUrls = new HashSet<>();
    String defaultCity = PortalSupport.resolveDefaultCity(criteria);
    String propertyType =
        criteria.getPropertyType() != null ? criteria.getPropertyType().getSlug() : "lakas";

    for (SidecarResponse.SidecarItem sidecarItem : items) {
      if (sidecarItem == null || sidecarItem.getUrl() == null) {
        continue;
      }
      if (!processedUrls.add(sidecarItem.getUrl())) {
        continue;
      }

      Long priceHuf =
          sidecarItem.getPriceMillion() != null
              ? Math.round(sidecarItem.getPriceMillion() * 1_000_000L)
              : null;

      RawPropertyListing raw =
          RawPropertyListing.builder()
              .portalId("ingatlancom")
              .nativeId(sidecarItem.getId())
              .url(sidecarItem.getUrl())
              .title(sidecarItem.getTitle())
              .priceHuf(priceHuf)
              .priceText(sidecarItem.getPriceRaw())
              .locationText(defaultCity)
              .areaSquareMeters(sidecarItem.getAreaSquareMeters())
              .roomCount(sidecarItem.getRoomCount())
              .imageUrl(sidecarItem.getImageUrl())
              .detailsText(sidecarItem.getTitle())
              .descriptionText(sidecarItem.getTitle())
              .extra(Map.of("propertyType", propertyType))
              .method(ExtractionMethod.SIDECAR)
              .build();

      rawListings.add(raw);
    }
    return rawListings;
  }

  private record CachedResult(List<RawPropertyListing> listings, long timestampMs) {}
}
