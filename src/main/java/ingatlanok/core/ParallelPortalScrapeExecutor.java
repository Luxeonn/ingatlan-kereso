package ingatlanok.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ingatlanok.common.AppLogger;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalEngine;
import ingatlanok.scraper.PortalScrapeResult;

/**
 * Párhuzamos portál-adatkinyerést végző motor Java 21 Virtual Threads alapon, szigorú
 * időkorlátokkal és 0-Crash hibatűréssel.
 */
public class ParallelPortalScrapeExecutor {

  private static final AppLogger LOGGER = AppLogger.getLogger(ParallelPortalScrapeExecutor.class);
  private final long timeoutSeconds;

  public ParallelPortalScrapeExecutor() {
    this(15);
  }

  public ParallelPortalScrapeExecutor(long timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 15;
  }

  public record CityScrapeOutput(
      List<IngatlanItem> items,
      List<PortalScrapeResult> results,
      Map<String, Integer> portalCounts) {}

  public CityScrapeOutput execute(
      List<PortalEngine> activeEngines, SearchCriteria cityCriteria, String currentCity) {
    List<IngatlanItem> cityFoundItems = new ArrayList<>();
    List<PortalScrapeResult> scrapeResults = new ArrayList<>();
    Map<String, Integer> portalCounts = new LinkedHashMap<>();

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Map<PortalEngine, Future<PortalScrapeResult>> futures = new LinkedHashMap<>();
      for (PortalEngine engine : activeEngines) {
        futures.put(engine, executor.submit(() -> engine.scrape(cityCriteria)));
      }

      for (Map.Entry<PortalEngine, Future<PortalScrapeResult>> entry : futures.entrySet()) {
        PortalEngine engine = entry.getKey();
        Future<PortalScrapeResult> future = entry.getValue();
        try {
          PortalScrapeResult scrapeResult = future.get(timeoutSeconds, TimeUnit.SECONDS);
          scrapeResults.add(scrapeResult);
          List<IngatlanItem> items = scrapeResult.getItems();
          if (items != null) {
            cityFoundItems.addAll(items);
            portalCounts.put(
                engine.getName(), portalCounts.getOrDefault(engine.getName(), 0) + items.size());
          }
          LOGGER.info(
              "  ├─ [{}] {} db hirdetés letöltve (Válaszidő: {} ms, Státusz: {})",
              engine.getName(),
              items != null ? items.size() : 0,
              scrapeResult.getExecutionTimeMs(),
              scrapeResult.getStatus());
        } catch (TimeoutException te) {
          future.cancel(true);
          LOGGER.warn(
              "Időtúllépés ({} mp) a(z) {} keresésekor ({})",
              timeoutSeconds,
              engine.getName(),
              currentCity);
          PortalScrapeResult timeoutResult =
              PortalScrapeResult.failure(
                  engine.getId(),
                  engine.getName(),
                  "Időtúllépés (" + timeoutSeconds + " mp)",
                  timeoutSeconds * 1000);
          scrapeResults.add(timeoutResult);
        } catch (Exception e) {
          LOGGER.error(
              "Hiba a(z) {} keresésekor ({}): {}",
              engine.getName(),
              currentCity,
              e.getMessage(),
              e);
          PortalScrapeResult failResult =
              PortalScrapeResult.failure(engine.getId(), engine.getName(), e.getMessage(), 0);
          scrapeResults.add(failResult);
        }
      }
    }

    return new CityScrapeOutput(cityFoundItems, scrapeResults, portalCounts);
  }
}
