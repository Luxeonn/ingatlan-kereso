package ingatlanok.portal.ingatlanbazar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ingatlanok.common.AppLogger;
import ingatlanok.common.HttpFetcher;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;
import ingatlanok.portal.PortalSupport;

public class IngatlanBazarPortalEngine implements PortalEngine {

  private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanBazarPortalEngine.class);
  private static final PortalDescriptor DESCRIPTOR =
      new PortalDescriptor(
          "ingatlanbazar",
          "Ingatlanbazár",
          "#7c3aed",
          "https://www.ingatlanbazar.hu",
          true,
          List.of("lakas", "haz", "telek"),
          true);

  private final IngatlanBazarQueryBuilder queryBuilder;
  private final IngatlanBazarJsonParser jsonParser;

  public IngatlanBazarPortalEngine() {
    this(new IngatlanBazarQueryBuilder(), new IngatlanBazarJsonParser());
  }

  public IngatlanBazarPortalEngine(
      IngatlanBazarQueryBuilder queryBuilder, IngatlanBazarJsonParser jsonParser) {
    this.queryBuilder = queryBuilder != null ? queryBuilder : new IngatlanBazarQueryBuilder();
    this.jsonParser = jsonParser != null ? jsonParser : new IngatlanBazarJsonParser();
  }

  @Override
  public PortalDescriptor getDescriptor() {
    return DESCRIPTOR;
  }

  @Override
  public PortalScrapeResult scrape(SearchCriteria criteria) {
    return scrape(criteria, HttpFetcher::fetchJson);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria, java.util.function.Function<String, String> jsonFetcher) {
    long startTime = System.currentTimeMillis();
    List<IngatlanItem> items = new ArrayList<>();
    SearchCriteria safeCriteria = PortalSupport.normalizeCriteria(criteria);

    String fallbackUrl = queryBuilder.buildSearchUrl(safeCriteria);
    LOGGER.info("Ingatlanbazár keresés indítása: {}", fallbackUrl);

    int maxPages = 2;
    String now = PortalSupport.nowTimestamp();
    Set<String> processedUrls = new HashSet<>();

    try {
      for (int page = 1; page <= maxPages; page++) {
        String apiUrl = queryBuilder.buildApiSearchUrl(safeCriteria, page);
        LOGGER.debug("Ingatlanbazár REST API lekérdezése (oldal: {}): {}", page, apiUrl);
        String json = jsonFetcher.apply(apiUrl);

        if (json == null || json.isBlank()) {
          if (page == 1) {
            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.warn(
                "Nem sikerült elérni az Ingatlanbazár REST API-t [{} ms]: {}", elapsed, apiUrl);
            return PortalScrapeResult.failure(
                DESCRIPTOR.id(),
                DESCRIPTOR.name(),
                "Nem sikerült elérni az Ingatlanbazár REST API-t",
                elapsed,
                500,
                fallbackUrl);
          }
          break;
        }

        List<IngatlanItem> pageItems =
            jsonParser.parseItemsFromJson(json, safeCriteria, processedUrls, now);
        items.addAll(pageItems);

        if (pageItems.size() < 50) {
          break;
        }
      }

      long elapsed = System.currentTimeMillis() - startTime;
      LOGGER.info("Ingatlanbazár találatok száma: {} [{} ms]", items.size(), elapsed);

      if (items.isEmpty()) {
        return PortalScrapeResult.empty(DESCRIPTOR.id(), DESCRIPTOR.name(), fallbackUrl, elapsed);
      }

      return PortalScrapeResult.success(DESCRIPTOR.id(), DESCRIPTOR.name(), items, elapsed);
    } catch (Exception e) {
      long elapsed = System.currentTimeMillis() - startTime;
      LOGGER.error("Hiba az Ingatlanbazár scraping során [{} ms]: {}", elapsed, e.getMessage(), e);
      return PortalScrapeResult.failure(
          DESCRIPTOR.id(), DESCRIPTOR.name(), e.getMessage(), elapsed, 500, fallbackUrl);
    }
  }

  public IngatlanBazarQueryBuilder getQueryBuilder() {
    return queryBuilder;
  }

  public IngatlanBazarJsonParser getJsonParser() {
    return jsonParser;
  }
}
