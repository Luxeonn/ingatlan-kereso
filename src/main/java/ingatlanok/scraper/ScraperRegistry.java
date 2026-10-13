package ingatlanok.scraper;

import ingatlanok.model.SearchCriteria;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dinamikus Scraper Registry (Provider Factory).
 * Felelős az ingatlan scraper modulok regisztrációjáért, életciklusáért és
 * a keresési feltételeknek megfelelő aktív scraperek felderítéséért (Open/Closed Principle).
 */
public class ScraperRegistry {

    private static final ScraperRegistry INSTANCE = new ScraperRegistry();
    private final Map<String, IngatlanScraper> scrapers = new ConcurrentHashMap<>();

    private ScraperRegistry() {
        // Alapértelmezett beépített scraperek regisztrációja
        register(new DunaHouseScraper());
        register(new OtthonCentrumScraper());
        register(new JofogasScraper());
        register(new IngatlanComScraper());
        register(new IngatlanBazarScraper());
    }

    public static ScraperRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Új portál scraper regisztrálása futási időben.
     */
    public void register(IngatlanScraper scraper) {
        if (scraper != null && scraper.getPortalId() != null) {
            scrapers.put(scraper.getPortalId().toLowerCase(Locale.ROOT), scraper);
        }
    }

    /**
     * Visszaadja a megadott azonosítójú scrapereket.
     */
    public Optional<IngatlanScraper> getScraper(String portalId) {
        if (portalId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(scrapers.get(portalId.toLowerCase(Locale.ROOT)));
    }

    /**
     * Visszaadja az összes regisztrált scrapereket megőrzött regisztrációs sorrendben.
     */
    public List<IngatlanScraper> getAllScrapers() {
        return new ArrayList<>(scrapers.values());
    }

    /**
     * Visszaadja az adott keresési feltételekben engedélyezett scrapereket.
     */
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
