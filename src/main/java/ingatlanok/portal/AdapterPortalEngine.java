package ingatlanok.portal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ingatlanok.common.AppLogger;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.RawExtractionResult;
import ingatlanok.model.raw.RawPropertyListing;

public class AdapterPortalEngine implements PortalEngine {

  private static final AppLogger LOGGER = AppLogger.getLogger(AdapterPortalEngine.class);

  private final PortalAdapter adapter;
  private final RawListingNormalizer normalizer;

  public AdapterPortalEngine(PortalAdapter adapter) {
    this(adapter, RawListingNormalizer.getDefault());
  }

  public AdapterPortalEngine(PortalAdapter adapter, RawListingNormalizer normalizer) {
    if (adapter == null) {
      throw new IllegalArgumentException("PortalAdapter cannot be null");
    }
    this.adapter = adapter;
    this.normalizer = normalizer != null ? normalizer : RawListingNormalizer.getDefault();
  }

  @Override
  public PortalDescriptor getDescriptor() {
    return adapter.getDescriptor();
  }

  @Override
  public PortalScrapeResult scrape(SearchCriteria criteria) {
    long startTime = System.currentTimeMillis();
    PortalDescriptor descriptor = adapter.getDescriptor();
    SearchCriteria safeCriteria = PortalSupport.normalizeCriteria(criteria);

    String defaultSearchUrl = adapter.getUrlBuilder() != null
        ? adapter.getUrlBuilder().buildSearchUrl(safeCriteria)
        : descriptor.homepageUrl();

    List<ExtractionStrategy> strategies = adapter.getStrategies();
    LOGGER.info("[{}] Portál lekérdezés: {}", descriptor.name(), defaultSearchUrl);
    if (strategies == null || strategies.isEmpty()) {
      long elapsed = System.currentTimeMillis() - startTime;
      return PortalScrapeResult.empty(descriptor.id(), descriptor.name(), defaultSearchUrl, elapsed);
    }

    RawExtractionResult lastBotProtected = null;
    RawExtractionResult lastFailure = null;

    for (ExtractionStrategy strategy : strategies) {
      try {
        RawExtractionResult result = strategy.extract(safeCriteria);
        if (result == null) {
          continue;
        }
        if (result.status() == PortalScrapeStatus.SUCCESS && !result.listings().isEmpty()) {
          List<IngatlanItem> items = normalizeListings(result, descriptor, safeCriteria);
          long elapsed = System.currentTimeMillis() - startTime;
          return buildSuccessResult(descriptor, result, items, defaultSearchUrl, elapsed);
        }
        if (result.status() == PortalScrapeStatus.BOT_PROTECTED) {
          lastBotProtected = result;
          LOGGER.warn("Bot protection triggerelve a(z) {} stratégia futásakor: {}",
              strategy.method(), result.message());
        } else if (result.status() == PortalScrapeStatus.FAILED) {
          lastFailure = result;
          LOGGER.warn("Hiba a(z) {} stratégia futásakor: {}", strategy.method(), result.message());
        }
      } catch (Exception e) {
        LOGGER.error("Kivétel a(z) {} stratégia végrehajtásakor: {}", strategy.method(), e.getMessage(), e);
      }
    }

    long elapsed = System.currentTimeMillis() - startTime;
    return buildFallbackResult(descriptor, lastBotProtected, lastFailure, defaultSearchUrl, elapsed);
  }

  private List<IngatlanItem> normalizeListings(
      RawExtractionResult result,
      PortalDescriptor descriptor,
      SearchCriteria safeCriteria) {
    List<IngatlanItem> normalizedItems = new ArrayList<>();
    Set<String> processedUrls = new HashSet<>();
    Set<String> processedNativeIds = new HashSet<>();
    String now = PortalSupport.nowTimestamp();
    int rejected = 0;
    int duplicates = 0;

    for (RawPropertyListing raw : result.listings()) {
      if (raw == null) {
        continue;
      }
      IngatlanItem item = normalizer.normalize(raw, descriptor, safeCriteria, now);
      if (item == null) {
        rejected++;
        LOGGER.debug("[{}] Elutasított nyers hirdetés: {}", descriptor.name(), raw.url());
      } else if (PortalSupport.markProcessedListing(
          processedUrls, processedNativeIds, item.getUrl(), raw.nativeId())) {
        normalizedItems.add(item);
      } else {
        duplicates++;
      }
    }
    LOGGER.info(
        "[{}] Adattisztítás: {} nyers -> {} érvényes (elutasítva: {}, duplikált URL: {})",
        descriptor.name(),
        result.listings().size(),
        normalizedItems.size(),
        rejected,
        duplicates);
    return normalizedItems;
  }

  private PortalScrapeResult buildSuccessResult(
      PortalDescriptor descriptor,
      RawExtractionResult result,
      List<IngatlanItem> items,
      String defaultSearchUrl,
      long elapsed) {
    PortalScrapeResult scrapeResult = PortalScrapeResult.success(
        descriptor.id(),
        descriptor.name(),
        items,
        elapsed
    );
    scrapeResult.setFallbackUrl(
        result.fallbackUrl() != null ? result.fallbackUrl() : defaultSearchUrl);
    if (result.technicalDetails() != null && !result.technicalDetails().isBlank()) {
      String methodPrefix = result.usedMethod() != null
          ? "Strategy: " + result.usedMethod().name() + " | "
          : "";
      scrapeResult.setTechnicalDetails(methodPrefix + result.technicalDetails());
    } else if (result.usedMethod() != null) {
      scrapeResult.setTechnicalDetails("Strategy: " + result.usedMethod().name());
    }
    return scrapeResult;
  }

  private PortalScrapeResult buildFallbackResult(
      PortalDescriptor descriptor,
      RawExtractionResult lastBotProtected,
      RawExtractionResult lastFailure,
      String defaultSearchUrl,
      long elapsed) {
    if (lastBotProtected != null) {
      String fallback = lastBotProtected.fallbackUrl() != null
          ? lastBotProtected.fallbackUrl()
          : defaultSearchUrl;
      return PortalScrapeResult.botProtected(
          descriptor.id(),
          descriptor.name(),
          fallback,
          elapsed
      );
    }
    if (lastFailure != null) {
      String fallback = lastFailure.fallbackUrl() != null
          ? lastFailure.fallbackUrl()
          : defaultSearchUrl;
      return PortalScrapeResult.failure(
          descriptor.id(),
          descriptor.name(),
          lastFailure.message() != null ? lastFailure.message() : "Extraction failed",
          elapsed,
          lastFailure.httpStatusCode() != null ? lastFailure.httpStatusCode() : 500,
          fallback
      );
    }
    return PortalScrapeResult.empty(descriptor.id(), descriptor.name(), defaultSearchUrl, elapsed);
  }

  public PortalAdapter getAdapter() {
    return adapter;
  }

  public RawListingNormalizer getNormalizer() {
    return normalizer;
  }
}
