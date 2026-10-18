package ingatlanok.scraper;

import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.portal.dh.DunaHousePortalEngine;
import org.jsoup.nodes.Element;

public class DunaHouseScraper extends BaseScraper implements IngatlanScraper {

    private final DunaHousePortalEngine engine;

    public DunaHouseScraper() {
        this.engine = (DunaHousePortalEngine) PortalEngineRegistry.getInstance()
                .getEngine("dh")
                .orElseGet(DunaHousePortalEngine::new);
    }

    public DunaHouseScraper(DunaHousePortalEngine engine) {
        this.engine = engine != null ? engine : new DunaHousePortalEngine();
    }

    @Override
    public String getPortalId() {
        return engine.getId();
    }

    @Override
    public String getName() {
        return engine.getName();
    }

    @Override
    public void init() {
    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 20;
    }

    public String buildSearchUrl(SearchCriteria criteria) {
        return engine.getUrlBuilder().buildSearchUrl(criteria);
    }

    @Override
    public PortalScrapeResult scrape(SearchCriteria criteria) {
        return engine.scrape(criteria, this::getDocument);
    }
}
