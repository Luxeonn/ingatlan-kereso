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

public class KoltozzbeExtractionStrategy implements ExtractionStrategy {

  private static final AppLogger LOGGER = AppLogger.getLogger(KoltozzbeExtractionStrategy.class);

  private final KoltozzbeUrlBuilder urlBuilder;
  private final KoltozzbeRawParser rawParser;
  private final Function<String, Document> documentFetcher;
  private final int maxPages;

  public KoltozzbeExtractionStrategy() {
    this(new KoltozzbeUrlBuilder(), new KoltozzbeRawParser(),
        HttpFetcher::fetchDocument, KoltozzbePortalEngine.DEFAULT_MAX_PAGES);
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

    try {
      for (int page = 1; page <= maxPages; page++) {
        String pageUrl = urlBuilder.buildPageUrl(searchUrl, page);
        Document doc = documentFetcher.apply(pageUrl);
        if (doc == null) {
          LOGGER.warn("Nem sikerült letölteni a Költözzbe oldalt: {}", pageUrl);
          if (page == 1) {
            return RawExtractionResult.failure(method(), searchUrl, "Nem sikerült letölteni az oldalt", 500);
          }
          break;
        }

        if (!collectPageListings(doc, page, processedUrls, allListings)) {
          break;
        }
      }

      if (allListings.isEmpty()) {
        return RawExtractionResult.empty(method(), searchUrl);
      }
      return RawExtractionResult.success(allListings, method());
    } catch (Exception e) {
      LOGGER.error("Hiba a Költözzbe stratégia futásakor: {}", e.getMessage(), e);
      return RawExtractionResult.failure(method(), searchUrl, e.getMessage(), 500);
    }
  }

  private boolean collectPageListings(
      Document doc,
      int page,
      Set<String> processedUrls,
      List<RawPropertyListing> allListings) {
    List<RawPropertyListing> pageHtml = rawParser.parseHtmlListings(doc);
    addUniqueListings(pageHtml, processedUrls, allListings);

    List<RawPropertyListing> pageJsonLd = rawParser.parseJsonLdListings(doc);
    addUniqueListings(pageJsonLd, processedUrls, allListings);

    boolean hasContent = !pageHtml.isEmpty() || !pageJsonLd.isEmpty();
    return hasContent && rawParser.hasNextPage(doc, page);
  }

  private void addUniqueListings(
      List<RawPropertyListing> candidates,
      Set<String> processedUrls,
      List<RawPropertyListing> target) {
    if (candidates == null) {
      return;
    }
    for (RawPropertyListing raw : candidates) {
      if (raw != null && raw.url() != null && processedUrls.add(raw.url())) {
        target.add(raw);
      }
    }
  }
}
