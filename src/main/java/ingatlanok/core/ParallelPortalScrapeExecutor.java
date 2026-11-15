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
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalEngine;

public class ParallelPortalScrapeExecutor {

  private static final AppLogger LOGGER = AppLogger.getLogger(ParallelPortalScrapeExecutor.class);
  private static final long DEFAULT_TIMEOUT_SECONDS = 15;
  private static final long DEFAULT_SIDECAR_TIMEOUT_SECONDS = 45;

  private final long timeoutSeconds;
  private final long sidecarTimeoutSeconds;

  public ParallelPortalScrapeExecutor() {
    this(DEFAULT_TIMEOUT_SECONDS, DEFAULT_SIDECAR_TIMEOUT_SECONDS);
  }

  public ParallelPortalScrapeExecutor(long timeoutSeconds) {
    long effective = timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
    this.timeoutSeconds = effective;
    this.sidecarTimeoutSeconds = effective;
  }

  public ParallelPortalScrapeExecutor(long timeoutSeconds, long sidecarTimeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
    this.sidecarTimeoutSeconds =
        sidecarTimeoutSeconds > 0 ? sidecarTimeoutSeconds : DEFAULT_SIDECAR_TIMEOUT_SECONDS;
  }

  long resolveTimeoutSeconds(String portalId) {
    return "ingatlancom".equalsIgnoreCase(portalId) ? sidecarTimeoutSeconds : timeoutSeconds;
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
      java.util.Map<String, String> mdc = org.slf4j.MDC.getCopyOfContextMap();
      for (PortalEngine engine : activeEngines) {
        futures.put(
            engine,
            executor.submit(
                () -> {
                  if (mdc != null) {
                    org.slf4j.MDC.setContextMap(mdc);
                  }
                  return engine.scrape(cityCriteria);
                }));
      }

      for (Map.Entry<PortalEngine, Future<PortalScrapeResult>> entry : futures.entrySet()) {
        PortalEngine engine = entry.getKey();
        Future<PortalScrapeResult> future = entry.getValue();
        long engineTimeoutSeconds = resolveTimeoutSeconds(engine.getId());
        try {
          PortalScrapeResult scrapeResult = future.get(engineTimeoutSeconds, TimeUnit.SECONDS);
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
              engineTimeoutSeconds,
              engine.getName(),
              currentCity);
          PortalScrapeResult timeoutResult =
              PortalScrapeResult.failure(
                  engine.getId(),
                  engine.getName(),
                  "Időtúllépés (" + engineTimeoutSeconds + " mp)",
                  engineTimeoutSeconds * 1000);
          scrapeResults.add(timeoutResult);
        } catch (Exception e) {
          LOGGER.error(
              "Hiba a(z) {} keresésekor ({}): {}",
              e,
              engine.getName(),
              currentCity,
              e.getMessage());
          PortalScrapeResult failResult =
              PortalScrapeResult.failure(engine.getId(), engine.getName(), e.getMessage(), 0);
          scrapeResults.add(failResult);
        }
      }
    }

    return new CityScrapeOutput(cityFoundItems, scrapeResults, portalCounts);
  }
}
