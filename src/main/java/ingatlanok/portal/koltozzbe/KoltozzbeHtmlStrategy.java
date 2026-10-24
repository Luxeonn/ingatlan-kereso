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

public class KoltozzbeHtmlStrategy implements ExtractionStrategy {

  private static final AppLogger LOGGER = AppLogger.getLogger(KoltozzbeHtmlStrategy.class);

  private final KoltozzbeUrlBuilder urlBuilder;
  private final KoltozzbeRawParser rawParser;
  private final KoltozzbeParser paginationHelper;
  private final Function<String, Document> documentFetcher;

  public KoltozzbeHtmlStrategy() {
    this(new KoltozzbeUrlBuilder(), new KoltozzbeRawParser(), new KoltozzbeParser(), HttpFetcher::fetchDocument);
  }

  public KoltozzbeHtmlStrategy(
      KoltozzbeUrlBuilder urlBuilder,
      KoltozzbeRawParser rawParser,
      KoltozzbeParser paginationHelper,
      Function<String, Document> documentFetcher) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new KoltozzbeUrlBuilder();
    this.rawParser = rawParser != null ? rawParser : new KoltozzbeRawParser();
    this.paginationHelper = paginationHelper != null ? paginationHelper : new KoltozzbeParser();
    this.documentFetcher = documentFetcher != null ? documentFetcher : HttpFetcher::fetchDocument;
  }

  @Override
  public ExtractionMethod method() {
    return ExtractionMethod.HTML;
  }

  @Override
  public RawExtractionResult extract(SearchCriteria criteria) {
    String searchUrl = urlBuilder.buildSearchUrl(criteria);
    LOGGER.info("Költözzbe HTML stratégia indítása: {}", searchUrl);

    List<RawPropertyListing> allListings = new ArrayList<>();
    Set<String> processedUrls = new HashSet<>();
    int maxPages = 3;

    try {
      for (int page = 1; page <= maxPages; page++) {
        String pageUrl = urlBuilder.buildPageUrl(searchUrl, page);
        Document doc = documentFetcher.apply(pageUrl);
        if (doc == null) {
          LOGGER.warn("Nem sikerült letölteni a Költözzbe oldalt (HTML): {}", pageUrl);
          break;
        }

        List<RawPropertyListing> pageListings = rawParser.parseHtmlListings(doc);
        for (RawPropertyListing raw : pageListings) {
          if (raw != null && raw.url() != null && processedUrls.add(raw.url())) {
            allListings.add(raw);
          }
        }

        if (!paginationHelper.hasNextPage(doc, page)) {
          break;
        }
      }

      if (allListings.isEmpty()) {
        return RawExtractionResult.empty(method(), searchUrl);
      }
      return RawExtractionResult.success(allListings, method());
    } catch (Exception e) {
      LOGGER.error("Hiba a Költözzbe HTML stratégia futásakor: {}", e.getMessage(), e);
      return RawExtractionResult.failure(method(), searchUrl, e.getMessage(), 500);
    }
  }
}
