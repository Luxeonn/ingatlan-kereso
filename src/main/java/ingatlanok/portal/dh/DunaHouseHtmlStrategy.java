package ingatlanok.portal.dh;

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

public class DunaHouseHtmlStrategy implements ExtractionStrategy {

  private static final AppLogger LOGGER = AppLogger.getLogger(DunaHouseHtmlStrategy.class);

  private final DunaHouseUrlBuilder urlBuilder;
  private final DunaHouseRawParser rawParser;
  private final Function<String, Document> documentFetcher;

  public DunaHouseHtmlStrategy() {
    this(new DunaHouseUrlBuilder(), new DunaHouseRawParser(), HttpFetcher::fetchDocument);
  }

  public DunaHouseHtmlStrategy(
      DunaHouseUrlBuilder urlBuilder,
      DunaHouseRawParser rawParser,
      Function<String, Document> documentFetcher) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new DunaHouseUrlBuilder();
    this.rawParser = rawParser != null ? rawParser : new DunaHouseRawParser();
    this.documentFetcher = documentFetcher != null ? documentFetcher : HttpFetcher::fetchDocument;
  }

  @Override
  public ExtractionMethod method() {
    return ExtractionMethod.HTML;
  }

  @Override
  public RawExtractionResult extract(SearchCriteria criteria) {
    String searchUrl = urlBuilder.buildSearchUrl(criteria);
    LOGGER.info("Duna House HTML stratégia indítása: {}", searchUrl);

    List<RawPropertyListing> allListings = new ArrayList<>();
    Set<String> processedUrls = new HashSet<>();
    int maxPages = 2;

    try {
      for (int i = 1; i <= maxPages; i++) {
        String pageUrl = urlBuilder.buildPageUrl(searchUrl, i);
        Document document = documentFetcher.apply(pageUrl);
        if (document == null) {
          LOGGER.warn("Nem sikerült letölteni a Duna House oldalt: {}", pageUrl);
          continue;
        }

        List<RawPropertyListing> pageListings = rawParser.parseRawListings(document);
        for (RawPropertyListing raw : pageListings) {
          if (raw != null && raw.url() != null && processedUrls.add(raw.url())) {
            allListings.add(raw);
          }
        }
      }

      if (allListings.isEmpty()) {
        return RawExtractionResult.empty(method(), searchUrl);
      }
      return RawExtractionResult.success(allListings, method());
    } catch (Exception e) {
      LOGGER.error("Hiba a Duna House HTML stratégia futásakor: {}", e.getMessage(), e);
      return RawExtractionResult.failure(method(), searchUrl, e.getMessage(), 500);
    }
  }

  public DunaHouseUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public DunaHouseRawParser getRawParser() {
    return rawParser;
  }
}
