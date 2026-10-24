package ingatlanok.portal.ingatlancom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import ingatlanok.common.AppLogger;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawExtractionResult;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.sidecar.SidecarCliExecutor;
import ingatlanok.portal.sidecar.SidecarResponse;

public class IngatlanComSidecarStrategy implements ExtractionStrategy {

  private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanComSidecarStrategy.class);

  private final IngatlanComUrlBuilder urlBuilder;

  public IngatlanComSidecarStrategy() {
    this(new IngatlanComUrlBuilder());
  }

  public IngatlanComSidecarStrategy(IngatlanComUrlBuilder urlBuilder) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new IngatlanComUrlBuilder();
  }

  @Override
  public ExtractionMethod method() {
    return ExtractionMethod.SIDECAR;
  }

  @Override
  public RawExtractionResult extract(SearchCriteria criteria) {
    String searchUrl = urlBuilder.buildSearchUrl(criteria);
    if (!SidecarCliExecutor.isAvailable()) {
      return RawExtractionResult.empty(method(), searchUrl);
    }

    LOGGER.info("Ingatlan.com: Sidecar modul detektálva, lekérdezés indítása...");
    try {
      Optional<SidecarResponse> sidecarResp =
          SidecarCliExecutor.execute("ingatlancom", criteria);
      if (sidecarResp.isPresent() && sidecarResp.get().isSuccess()) {
        SidecarResponse resp = sidecarResp.get();
        List<RawPropertyListing> rawListings = new ArrayList<>();
        Set<String> processedUrls = new HashSet<>();

        for (SidecarResponse.SidecarItem sidecarItem : resp.getItems()) {
          if (sidecarItem == null || sidecarItem.getUrl() == null) {
            continue;
          }
          if (!processedUrls.add(sidecarItem.getUrl())) {
            continue;
          }

          Long priceHuf = sidecarItem.getPriceMillion() != null
              ? Math.round(sidecarItem.getPriceMillion() * 1_000_000L)
              : null;

          RawPropertyListing raw = RawPropertyListing.builder()
              .portalId("ingatlancom")
              .nativeId(sidecarItem.getId())
              .url(sidecarItem.getUrl())
              .title(sidecarItem.getTitle())
              .priceHuf(priceHuf)
              .priceText(sidecarItem.getPriceRaw())
              .areaSquareMeters(sidecarItem.getAreaSquareMeters())
              .roomCount(sidecarItem.getRoomCount())
              .imageUrl(sidecarItem.getImageUrl())
              .descriptionText(sidecarItem.getTitle())
              .method(ExtractionMethod.SIDECAR)
              .build();

          rawListings.add(raw);
        }

        if (!rawListings.isEmpty()) {
          return RawExtractionResult.success(rawListings, method());
        }
      }
    } catch (Exception e) {
      LOGGER.error("Ingatlan.com: Hiba a sidecar végrehajtás során: {}", e.getMessage(), e);
    }

    return RawExtractionResult.empty(method(), searchUrl);
  }
}
