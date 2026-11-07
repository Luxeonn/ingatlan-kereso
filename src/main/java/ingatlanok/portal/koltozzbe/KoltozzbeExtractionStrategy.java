package ingatlanok.portal.koltozzbe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.jsoup.nodes.Document;

import ingatlanok.common.AppLogger;
import ingatlanok.common.HttpFetcher;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawExtractionResult;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalSupport;

public class KoltozzbeExtractionStrategy implements ExtractionStrategy {

  private static final AppLogger LOGGER = AppLogger.getLogger(KoltozzbeExtractionStrategy.class);

  private final KoltozzbeUrlBuilder urlBuilder;
  private final KoltozzbeRawParser rawParser;
  private final Function<String, Document> documentFetcher;
  private final int maxPages;

  public KoltozzbeExtractionStrategy() {
    this(
        new KoltozzbeUrlBuilder(),
        new KoltozzbeRawParser(),
        HttpFetcher::fetchDocument,
        KoltozzbePortalEngine.DEFAULT_MAX_PAGES);
  }

  public KoltozzbeExtractionStrategy(
      KoltozzbeUrlBuilder urlBuilder,
      KoltozzbeRawParser rawParser,
      Function<String, Document> documentFetcher,
      int maxPages) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new KoltozzbeUrlBuilder();
    this.rawParser = rawParser != null ? rawParser : new KoltozzbeRawParser();
    this.documentFetcher = documentFetcher != null ? documentFetcher : HttpFetcher::fetchDocument;
    this.maxPages = maxPages > 0 ? maxPages : KoltozzbePortalEngine.DEFAULT_MAX_PAGES;
  }

  @Override
  public ExtractionMethod method() {
    return ExtractionMethod.HTML;
  }

  @Override
  public RawExtractionResult extract(SearchCriteria criteria) {
    String searchUrl = urlBuilder.buildSearchUrl(criteria);
    LOGGER.info("Költözzbe kinyerési stratégia indítása: {}", searchUrl);

    List<RawPropertyListing> allListings = new ArrayList<>();
    Set<String> processedUrls = new HashSet<>();
    Set<String> processedNativeIds = new HashSet<>();
    Set<String> visitedPageUrls = new HashSet<>();
    int pagesProcessed = 0;
    String stopReason = "MAX_PAGES_REACHED";

    for (int page = 1; page <= maxPages; page++) {
      String pageUrl = urlBuilder.buildPageUrl(searchUrl, page);
      if (pageUrl == null || !visitedPageUrls.add(pageUrl)) {
        stopReason = "DUPLICATE_PAGE_URL";
        break;
      }

      PageFetchOutcome fetchOutcome = fetchPageDocument(pageUrl, page);
      if (fetchOutcome.botProtected()) {
        if (allListings.isEmpty()) {
          return RawExtractionResult.botProtected(method(), searchUrl, "Bot-védelem detektálva");
        }
        stopReason = "INTERMEDIATE_PAGE_BOT_PROTECTED";
        break;
      }
      if (fetchOutcome.document() == null) {
        if (page == 1 && allListings.isEmpty()) {
          return PortalSupport.resolveFirstPageFetchFailure(
              method(), searchUrl, fetchOutcome.errorMessage());
        }
        stopReason = fetchOutcome.stopReason();
        break;
      }

      pagesProcessed++;
      String pageStopReason =
          collectPageListings(
              fetchOutcome.document(), page, processedUrls, processedNativeIds, allListings);
      if (pageStopReason != null) {
        stopReason = pageStopReason;
        break;
      }
    }

    LOGGER.info(
        "Költözzbe lapozás befejezve: feldolgozott oldalak={}, találatok={}, leállás oka={}",
        pagesProcessed,
        allListings.size(),
        stopReason);

    if (allListings.isEmpty()) {
      return RawExtractionResult.empty(method(), searchUrl);
    }
    String details = String.format("pages=%d, stopReason=%s", pagesProcessed, stopReason);
    return RawExtractionResult.success(allListings, method(), details);
  }

  private PageFetchOutcome fetchPageDocument(String pageUrl, int page) {
    try {
      Document doc = documentFetcher.apply(pageUrl);
      if (doc == null) {
        LOGGER.warn("Nem sikerült letölteni a Költözzbe {}. oldalt: {}", page, pageUrl);
        return new PageFetchOutcome(
            null, false, "INTERMEDIATE_PAGE_FETCH_FAILED", "Nem sikerült letölteni az oldalt");
      }
      if (PortalSupport.isCommonBotChallenge(doc)) {
        LOGGER.warn("Költözzbe: Bot-védelem detektálva");
        return new PageFetchOutcome(null, true, "BOT_PROTECTED", "Bot-védelem detektálva");
      }
      return new PageFetchOutcome(doc, false, null, null);
    } catch (Exception e) {
      LOGGER.error("Hiba a Költözzbe {}. oldal lekérésekor: {}", page, e.getMessage(), e);
      return new PageFetchOutcome(null, false, "INTERMEDIATE_PAGE_EXCEPTION", e.getMessage());
    }
  }

  private record PageFetchOutcome(
      Document document, boolean botProtected, String stopReason, String errorMessage) {}

  private String collectPageListings(
      Document doc,
      int page,
      Set<String> processedUrls,
      Set<String> processedNativeIds,
      List<RawPropertyListing> allListings) {
    List<RawPropertyListing> pageHtml = rawParser.parseHtmlListings(doc);
    int addedHtml = addOrMergeListings(pageHtml, processedUrls, processedNativeIds, allListings);

    List<RawPropertyListing> pageJsonLd = rawParser.parseJsonLdListings(doc);
    int addedJsonLd =
        addOrMergeListings(pageJsonLd, processedUrls, processedNativeIds, allListings);

    boolean hasContent = !pageHtml.isEmpty() || !pageJsonLd.isEmpty();
    if (!hasContent) {
      return page == 1 ? "FIRST_PAGE_EMPTY" : "EMPTY_PAGE";
    }
    if (addedHtml + addedJsonLd == 0) {
      return "REPEATED_PAGE_CONTENT";
    }
    if (!rawParser.hasNextPage(doc, page)) {
      return "NO_NEXT_PAGE";
    }
    return null;
  }

  private int addOrMergeListings(
      List<RawPropertyListing> candidates,
      Set<String> processedUrls,
      Set<String> processedNativeIds,
      List<RawPropertyListing> target) {
    if (candidates == null) {
      return 0;
    }
    int added = 0;
    for (RawPropertyListing raw : candidates) {
      if (raw == null) {
        continue;
      }
      if (PortalSupport.markProcessedListing(
          processedUrls, processedNativeIds, raw.url(), raw.nativeId())) {
        target.add(raw);
        added++;
      } else {
        mergeIntoExistingListing(target, raw);
      }
    }
    return added;
  }

  private void mergeIntoExistingListing(
      List<RawPropertyListing> target, RawPropertyListing incoming) {
    String incomingUrl = PortalSupport.canonicalListingUrl(incoming.url());
    String incomingId = incoming.nativeId() != null ? incoming.nativeId().trim() : "";
    for (int i = 0; i < target.size(); i++) {
      RawPropertyListing existing = target.get(i);
      if (isSameListing(existing, incomingUrl, incomingId)) {
        target.set(i, mergeComplementaryFields(existing, incoming));
        return;
      }
    }
  }

  private boolean isSameListing(
      RawPropertyListing existing, String incomingUrl, String incomingId) {
    if (existing == null) {
      return false;
    }
    String existingUrl = PortalSupport.canonicalListingUrl(existing.url());
    if (!incomingUrl.isEmpty() && incomingUrl.equals(existingUrl)) {
      return true;
    }
    String existingId = existing.nativeId() != null ? existing.nativeId().trim() : "";
    return !incomingId.isEmpty() && incomingId.equals(existingId);
  }

  private RawPropertyListing mergeComplementaryFields(
      RawPropertyListing existing, RawPropertyListing incoming) {
    return RawPropertyListing.builder()
        .portalId(firstNonBlank(existing.portalId(), incoming.portalId()))
        .nativeId(firstNonBlank(existing.nativeId(), incoming.nativeId()))
        .url(firstNonBlank(existing.url(), incoming.url()))
        .title(firstNonBlank(existing.title(), incoming.title()))
        .priceText(firstNonBlank(existing.priceText(), incoming.priceText()))
        .priceHuf(existing.priceHuf() != null ? existing.priceHuf() : incoming.priceHuf())
        .detailsText(firstNonBlank(existing.detailsText(), incoming.detailsText()))
        .areaSquareMeters(
            existing.areaSquareMeters() != null
                ? existing.areaSquareMeters()
                : incoming.areaSquareMeters())
        .roomCount(existing.roomCount() != null ? existing.roomCount() : incoming.roomCount())
        .locationText(firstNonBlank(existing.locationText(), incoming.locationText()))
        .imageUrl(firstNonBlank(existing.imageUrl(), incoming.imageUrl()))
        .descriptionText(firstNonBlank(existing.descriptionText(), incoming.descriptionText()))
        .method(existing.method() != null ? existing.method() : incoming.method())
        .extra(existing.extra() != null ? existing.extra() : incoming.extra())
        .build();
  }

  private String firstNonBlank(String primary, String fallback) {
    return (primary != null && !primary.isBlank()) ? primary : fallback;
  }
}
