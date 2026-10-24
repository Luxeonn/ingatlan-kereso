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
  private final KoltozzbeParser paginationHelper;
  private final Function<String, Document> documentFetcher;
  private final int maxPages;

  public KoltozzbeExtractionStrategy() {
    this(new KoltozzbeUrlBuilder(), new KoltozzbeRawParser(), new KoltozzbeParser(),
        HttpFetcher::fetchDocument, KoltozzbePortalEngine.DEFAULT_MAX_PAGES);
  }

  public KoltozzbeExtractionStrategy(
      KoltozzbeUrlBuilder urlBuilder,
      KoltozzbeRawParser rawParser,
      KoltozzbeParser paginationHelper,
      Function<String, Document> documentFetcher,
      int maxPages) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new KoltozzbeUrlBuilder();
    this.rawParser = rawParser != null ? rawParser : new KoltozzbeRawParser();
    this.paginationHelper = paginationHelper != null ? paginationHelper : new KoltozzbeParser();
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

        List<RawPropertyListing> pageHtml = rawParser.parseHtmlListings(doc);
        for (RawPropertyListing raw : pageHtml) {
          if (raw != null && raw.url() != null && processedUrls.add(raw.url())) {
            allListings.add(raw);
          }
        }

        List<RawPropertyListing> pageJsonLd = rawParser.parseJsonLdListings(doc);
        for (RawPropertyListing raw : pageJsonLd) {
          if (raw != null && raw.url() != null && processedUrls.add(raw.url())) {
            allListings.add(raw);
          }
        }

        if ((pageHtml.isEmpty() && pageJsonLd.isEmpty()) || !paginationHelper.hasNextPage(doc, page)) {
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
}
