package ingatlanok.scraper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;
import ingatlanok.portal.PortalEngineRegistry;

public class ScraperRegistry {

  private static final ScraperRegistry INSTANCE = new ScraperRegistry();
  private final Map<String, IngatlanScraper> scrapers = new ConcurrentHashMap<>();

  private ScraperRegistry() {
    register(new DunaHouseScraper());
    register(new OtthonCentrumScraper());
    register(new JofogasScraper());
    register(new IngatlanComScraper());
    register(new IngatlanBazarScraper());
    register(new KoltozzbeScraper());
  }

  public static ScraperRegistry getInstance() {
    return INSTANCE;
  }

  public void register(IngatlanScraper scraper) {
    if (scraper != null && scraper.getPortalId() != null) {
      String key = scraper.getPortalId().toLowerCase(Locale.ROOT);
      scrapers.put(key, scraper);

      if (PortalEngineRegistry.getInstance().getEngine(key).isEmpty()) {
        PortalEngineRegistry.getInstance()
            .register(
                new PortalEngine() {
                  @Override
                  public PortalDescriptor getDescriptor() {
                    return new PortalDescriptor(
                        scraper.getPortalId(),
                        scraper.getName(),
                        "#3b82f6",
                        "",
                        true,
                        List.of("lakas", "haz", "telek"),
                        true);
                  }

                  @Override
                  public PortalScrapeResult scrape(SearchCriteria criteria) {
                    return scraper.scrape(criteria);
                  }
                });
      }
    }
  }

  public Optional<IngatlanScraper> getScraper(String portalId) {
    if (portalId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(scrapers.get(portalId.toLowerCase(Locale.ROOT)));
  }

  public List<IngatlanScraper> getAllScrapers() {
    return new ArrayList<>(scrapers.values());
  }

  public List<IngatlanScraper> getActiveScrapers(SearchCriteria criteria) {
    if (criteria == null) {
      return getAllScrapers();
    }
    List<IngatlanScraper> active = new ArrayList<>();
    for (IngatlanScraper scraper : scrapers.values()) {
      if (criteria.isPortalEnabled(scraper.getPortalId())) {
        active.add(scraper);
      }
    }
    return active;
  }
}
