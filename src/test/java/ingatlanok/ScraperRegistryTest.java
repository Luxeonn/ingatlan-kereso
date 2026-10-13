package ingatlanok;

import ingatlanok.model.SearchCriteria;
import ingatlanok.scraper.IngatlanScraper;
import ingatlanok.scraper.ScraperRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ScraperRegistryTest {

    @Test
    void testDefaultRegisteredScrapers() {
        ScraperRegistry registry = ScraperRegistry.getInstance();
        List<IngatlanScraper> all = registry.getAllScrapers();

        assertNotNull(all);
        assertTrue(all.size() >= 5, "Legalább 5 beépített scrapernek regisztrálva kell lennie");

        Optional<IngatlanScraper> dh = registry.getScraper("dh");
        Optional<IngatlanScraper> oc = registry.getScraper("oc");
        Optional<IngatlanScraper> jf = registry.getScraper("jofogas");
        Optional<IngatlanScraper> ic = registry.getScraper("ingatlancom");
        Optional<IngatlanScraper> ib = registry.getScraper("ingatlanbazar");

        assertTrue(dh.isPresent(), "dh scraper must be present");
        assertTrue(oc.isPresent(), "oc scraper must be present");
        assertTrue(jf.isPresent(), "jofogas scraper must be present");
        assertTrue(ic.isPresent(), "ingatlancom scraper must be present");
        assertTrue(ib.isPresent(), "ingatlanbazar scraper must be present");

        assertEquals("Duna House", dh.get().getName());
        assertEquals("Otthon Centrum", oc.get().getName());
    }

    @Test
    void testGetActiveScrapersForCriteria() {
        ScraperRegistry registry = ScraperRegistry.getInstance();
        SearchCriteria criteria = new SearchCriteria();

        // Alapértelmezett beállítások mellett a dh, oc, jofogas, ingatlancom aktív
        List<IngatlanScraper> active = registry.getActiveScrapers(criteria);
        assertTrue(active.stream().anyMatch(s -> s.getPortalId().equals("dh")));
        assertTrue(active.stream().anyMatch(s -> s.getPortalId().equals("oc")));

        // Kikapcsoljuk a duna house-t
        criteria.setPortalEnabled("dh", false);
        List<IngatlanScraper> filtered = registry.getActiveScrapers(criteria);
        assertFalse(filtered.stream().anyMatch(s -> s.getPortalId().equals("dh")), "Kikapcsolt dh portálnak nem szabad szerepelnie");
        assertTrue(filtered.stream().anyMatch(s -> s.getPortalId().equals("oc")), "Bekapcsolt oc portálnak szerepelnie kell");
    }
}
