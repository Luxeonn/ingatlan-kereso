package ingatlanok.portal.oc;

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

public class OtthonCentrumHtmlStrategy implements ExtractionStrategy {

  private static final AppLogger LOGGER = AppLogger.getLogger(OtthonCentrumHtmlStrategy.class);

  private final OtthonCentrumUrlBuilder urlBuilder;
  private final OtthonCentrumRawParser rawParser;
  private final Function<String, Document> documentFetcher;

  public OtthonCentrumHtmlStrategy() {
    this(new OtthonCentrumUrlBuilder(), new OtthonCentrumRawParser(), HttpFetcher::fetchDocument);
  }

  public OtthonCentrumHtmlStrategy(
      OtthonCentrumUrlBuilder urlBuilder,
      OtthonCentrumRawParser rawParser,
      Function<String, Document> documentFetcher) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new OtthonCentrumUrlBuilder();
    this.rawParser = rawParser != null ? rawParser : new OtthonCentrumRawParser();
    this.documentFetcher = documentFetcher != null ? documentFetcher : HttpFetcher::fetchDocument;
  }

  @Override
  public ExtractionMethod method() {
    return ExtractionMethod.HTML;
  }

  @Override
  public RawExtractionResult extract(SearchCriteria criteria) {
    String searchUrl = urlBuilder.buildSearchUrl(criteria);
    LOGGER.info("Otthon Centrum HTML stratégia indítása: {}", searchUrl);

    List<RawPropertyListing> allListings = new ArrayList<>();
    Set<String> processedUrls = new HashSet<>();
    int maxPages = 2;

    try {
      for (int i = 1; i <= maxPages; i++) {
        String pageUrl = (i == 1) ? searchUrl : searchUrl + "?page=" + i;
        Document document = documentFetcher.apply(pageUrl);
        if (document == null) {
          LOGGER.warn("Nem sikerült letölteni az Otthon Centrum oldalt: {}", pageUrl);
          if (i == 1) {
            return RawExtractionResult.failure(method(), searchUrl, "Nem sikerült letölteni az oldalt", 500);
          }
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
      LOGGER.error("Hiba az Otthon Centrum HTML stratégia futásakor: {}", e.getMessage(), e);
      return RawExtractionResult.failure(method(), searchUrl, e.getMessage(), 500);
    }
  }

  public OtthonCentrumUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public OtthonCentrumRawParser getRawParser() {
    return rawParser;
  }
}
