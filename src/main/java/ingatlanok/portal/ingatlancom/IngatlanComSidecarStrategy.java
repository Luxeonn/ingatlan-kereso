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

  public static final int DEFAULT_MAX_PAGES = 20;

  private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanComSidecarStrategy.class);
  private static final long CACHE_TTL_MS = 180_000L; // 3 minutes TTL to avoid Cloudflare 429 burst limit
  private static final Map<String, CachedResult> RESPONSE_CACHE = new ConcurrentHashMap<>();

  private final IngatlanComUrlBuilder urlBuilder;
  private final int maxPages;

  public IngatlanComSidecarStrategy() {
    this(new IngatlanComUrlBuilder(), DEFAULT_MAX_PAGES);
  }

  public IngatlanComSidecarStrategy(IngatlanComUrlBuilder urlBuilder) {
    this(urlBuilder, DEFAULT_MAX_PAGES);
  }

  public IngatlanComSidecarStrategy(IngatlanComUrlBuilder urlBuilder, int maxPages) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new IngatlanComUrlBuilder();
    this.maxPages = maxPages > 0 ? maxPages : DEFAULT_MAX_PAGES;
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
      return RawExtractionResult.success(
          cached.listings(), method(), cached.details(), cached.warningMessage());
    }

    if (!SidecarCliExecutor.isAvailable()) {
      return RawExtractionResult.empty(method(), searchUrl);
    }

    LOGGER.info("Ingatlan.com: Sidecar modul detektálva, lekérdezés indítása (maxPages={})...", maxPages);
    try {
      Optional<SidecarResponse> sidecarResp =
          SidecarCliExecutor.execute("ingatlancom", safeCriteria, maxPages);
      if (sidecarResp.isPresent()) {
        SidecarResponse resp = sidecarResp.get();
        if (resp.isSuccess()) {
          List<RawPropertyListing> rawListings = mapSidecarItems(resp.getItems(), safeCriteria);
          if (!rawListings.isEmpty()) {
            String details = buildPaginationDetails(resp);
            String warningMessage = buildPartialWarningMessage(resp);
            RESPONSE_CACHE.put(
                searchUrl,
                new CachedResult(List.copyOf(rawListings), details, warningMessage, now));
            LOGGER.info(
                "Ingatlan.com: Sidecar sikeresen kinyert {} db hirdetést ({})",
                rawListings.size(),
                details);
            return RawExtractionResult.success(rawListings, method(), details, warningMessage);
          }
        } else if (cached != null && !cached.listings().isEmpty()) {
          LOGGER.warn(
              "Ingatlan.com: Sidecar hiba ({}), korábbi gyorsítótárazott találatok ({} db) visszaadása",
              resp.getErrorMessage(),
              cached.listings().size());
          return RawExtractionResult.success(
              cached.listings(), method(), cached.details(), cached.warningMessage());
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

  private String buildPaginationDetails(SidecarResponse resp) {
    int pages = resp.getPagesProcessed() != null && resp.getPagesProcessed() > 0
        ? resp.getPagesProcessed()
        : 1;
    String stopReason = resp.getStopReason() != null && !resp.getStopReason().isBlank()
        ? resp.getStopReason()
        : "COMPLETED";
    return String.format("pages=%d, stopReason=%s", pages, stopReason);
  }

  private String buildPartialWarningMessage(SidecarResponse resp) {
    String stopReason = resp.getStopReason();
    if (!"INTERMEDIATE_PAGE_BOT_PROTECTED".equals(stopReason)
        && !"INTERMEDIATE_PAGE_RATE_LIMITED".equals(stopReason)) {
      return null;
    }
    int pages = resp.getPagesProcessed() != null && resp.getPagesProcessed() > 0
        ? resp.getPagesProcessed()
        : 1;
    return "Részleges találati lista (" + pages + " oldal): a Cloudflare WAF korlátozta a további oldalakat";
  }

  private record CachedResult(
      List<RawPropertyListing> listings,
      String details,
      String warningMessage,
      long timestampMs) {}
}
