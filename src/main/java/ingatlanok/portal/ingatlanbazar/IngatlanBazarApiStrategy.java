package ingatlanok.portal.ingatlanbazar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import ingatlanok.common.AppLogger;
import ingatlanok.common.HttpFetcher;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawExtractionResult;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.ExtractionStrategy;

public class IngatlanBazarApiStrategy implements ExtractionStrategy {

  private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanBazarApiStrategy.class);

  private final IngatlanBazarQueryBuilder queryBuilder;
  private final IngatlanBazarRawParser rawParser;
  private final Function<String, String> jsonFetcher;

  public IngatlanBazarApiStrategy() {
    this(new IngatlanBazarQueryBuilder(), new IngatlanBazarRawParser(), HttpFetcher::fetchJson);
  }

  public IngatlanBazarApiStrategy(
      IngatlanBazarQueryBuilder queryBuilder,
      IngatlanBazarRawParser rawParser,
      Function<String, String> jsonFetcher) {
    this.queryBuilder = queryBuilder != null ? queryBuilder : new IngatlanBazarQueryBuilder();
    this.rawParser = rawParser != null ? rawParser : new IngatlanBazarRawParser();
    this.jsonFetcher = jsonFetcher != null ? jsonFetcher : HttpFetcher::fetchJson;
  }

  @Override
  public ExtractionMethod method() {
    return ExtractionMethod.API;
  }

  @Override
  public RawExtractionResult extract(SearchCriteria criteria) {
    String webSearchUrl = queryBuilder.buildSearchUrl(criteria);
    LOGGER.info("Ingatlanbazár API lekérdezés indítása: {}", webSearchUrl);

    List<RawPropertyListing> allListings = new ArrayList<>();
    Set<String> processedUrls = new HashSet<>();
    int maxPages = 2;

    try {
      for (int page = 1; page <= maxPages; page++) {
        String apiUrl = queryBuilder.buildApiSearchUrl(criteria, page);
        String json = jsonFetcher.apply(apiUrl);

        if (json == null || json.isBlank()) {
          LOGGER.warn("Nem sikerült letölteni az Ingatlanbazár JSON választ: {}", apiUrl);
          if (page == 1) {
            return RawExtractionResult.failure(method(), webSearchUrl,
                "Nem sikerült elérni az Ingatlanbazár REST API-t", 500);
          }
          continue;
        }

        List<RawPropertyListing> pageListings = rawParser.parseRawListings(json);
        for (RawPropertyListing raw : pageListings) {
          if (raw != null && raw.url() != null && processedUrls.add(raw.url())) {
            allListings.add(raw);
          }
        }
      }

      if (allListings.isEmpty()) {
        return RawExtractionResult.empty(method(), webSearchUrl);
      }
      return RawExtractionResult.success(allListings, method());
    } catch (Exception e) {
      LOGGER.error("Hiba az Ingatlanbazár API stratégia futásakor: {}", e.getMessage(), e);
      return RawExtractionResult.failure(method(), webSearchUrl, e.getMessage(), 500);
    }
  }

  public IngatlanBazarQueryBuilder getQueryBuilder() {
    return queryBuilder;
  }

  public IngatlanBazarRawParser getRawParser() {
    return rawParser;
  }
}
