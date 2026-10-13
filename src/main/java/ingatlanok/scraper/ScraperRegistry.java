package ingatlanok.scraper;

import ingatlanok.model.SearchCriteria;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
            scrapers.put(scraper.getPortalId().toLowerCase(Locale.ROOT), scraper);
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
