package ingatlanok.scraper;

import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.portal.oc.OtthonCentrumPortalEngine;
import org.jsoup.nodes.Element;

public class OtthonCentrumScraper extends BaseScraper implements IngatlanScraper {

    private final OtthonCentrumPortalEngine engine;

    public OtthonCentrumScraper() {
        this.engine = (OtthonCentrumPortalEngine) PortalEngineRegistry.getInstance()
                .getEngine("oc")
                .orElseGet(OtthonCentrumPortalEngine::new);
    }

    public OtthonCentrumScraper(OtthonCentrumPortalEngine engine) {
        this.engine = engine != null ? engine : new OtthonCentrumPortalEngine();
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
        return 12;
    }

    public String buildSearchUrl(SearchCriteria criteria) {
        return engine.getUrlBuilder().buildSearchUrl(criteria);
    }

    @Override
    public PortalScrapeResult scrape(SearchCriteria criteria) {
        return engine.scrape(criteria, this::getDocument);
    }
}
