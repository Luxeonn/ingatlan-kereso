package ingatlanok.portal.jofogas;

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

public class JofogasHtmlStrategy implements ExtractionStrategy {

  private static final AppLogger LOGGER = AppLogger.getLogger(JofogasHtmlStrategy.class);

  private final JofogasUrlBuilder urlBuilder;
  private final JofogasRawParser rawParser;
  private final Function<String, Document> documentFetcher;

  public JofogasHtmlStrategy() {
    this(new JofogasUrlBuilder(), new JofogasRawParser(), HttpFetcher::fetchDocument);
  }

  public JofogasHtmlStrategy(
      JofogasUrlBuilder urlBuilder,
      JofogasRawParser rawParser,
      Function<String, Document> documentFetcher) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new JofogasUrlBuilder();
    this.rawParser = rawParser != null ? rawParser : new JofogasRawParser();
    this.documentFetcher = documentFetcher != null ? documentFetcher : HttpFetcher::fetchDocument;
  }

  @Override
  public ExtractionMethod method() {
    return ExtractionMethod.HTML;
  }

  @Override
  public RawExtractionResult extract(SearchCriteria criteria) {
    if (criteria == null || criteria.getCity() == null || criteria.getCity().isBlank()) {
      return RawExtractionResult.failure(method(), "https://ingatlan.jofogas.hu", "Keresési feltétel érvénytelen", 400);
    }
    String searchUrl = urlBuilder.buildSearchUrl(criteria);
    LOGGER.info("Jófogás HTML stratégia indítása: {}", searchUrl);

    List<RawPropertyListing> allListings = new ArrayList<>();
    Set<String> processedUrls = new HashSet<>();
    int maxPages = 2;

    try {
      for (int i = 1; i <= maxPages; i++) {
        String pageUrl = (i == 1) ? searchUrl : searchUrl + "&o=" + i;
        Document document = documentFetcher.apply(pageUrl);
        if (document == null) {
          LOGGER.warn("Nem sikerült letölteni a Jófogás oldalt: {}", pageUrl);
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
      LOGGER.error("Hiba a Jófogás HTML stratégia futásakor: {}", e.getMessage(), e);
      return RawExtractionResult.failure(method(), searchUrl, e.getMessage(), 500);
    }
  }

  public JofogasUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public JofogasRawParser getRawParser() {
    return rawParser;
  }
}
