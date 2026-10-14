package ingatlanok.scraper;

import ingatlanok.common.AppLogger;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.koltozzbe.KoltozzbeParser;
import ingatlanok.portal.koltozzbe.KoltozzbeUrlBuilder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class KoltozzbeScraper extends BaseScraper implements IngatlanScraper {

    private static final AppLogger LOGGER = AppLogger.getLogger(KoltozzbeScraper.class);
    private final KoltozzbeUrlBuilder urlBuilder = new KoltozzbeUrlBuilder();
    private final KoltozzbeParser parser = new KoltozzbeParser();

    @Override
    public String getPortalId() {
        return "koltozzbe";
    }

    @Override
    public String getName() {
        return "Költözzbe";
    }

    @Override
    public void init() {
    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 20;
    }

    public String buildSearchUrl(SearchCriteria criteria) {
        return urlBuilder.buildSearchUrl(criteria);
    }

    @Override
    public PortalScrapeResult scrape(SearchCriteria criteria) {
        long startTime = System.currentTimeMillis();
        List<IngatlanItem> items = new ArrayList<>();
        if (criteria == null) {
            criteria = new SearchCriteria();
        }

        String searchUrl = buildSearchUrl(criteria);
        LOGGER.info("Költözzbe keresés indítása: {}", searchUrl);

        int maxPages = 2;
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        Set<String> processedUrls = new HashSet<>();

        try {
            for (int page = 1; page <= maxPages; page++) {
                String pageUrl = searchUrl;
                if (page > 1) {
                    pageUrl += (pageUrl.contains("?") ? "&" : "?") + "page=" + page;
                }

                Document doc = getDocument(pageUrl);
                if (doc == null) {
                    LOGGER.warn("Nem sikerült letölteni a Költözzbe oldalt: {}", pageUrl);
                    continue;
                }

                List<IngatlanItem> pageItems = parser.parseItems(doc, criteria, now, processedUrls);
                items.addAll(pageItems);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.info("Költözzbe találatok feldolgozva: {} db [{} ms]", items.size(), elapsed);
            return PortalScrapeResult.success(getPortalId(), getName(), items, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.error("Hiba a Költözzbe keresésekor [{} ms]: {}", elapsed, e.getMessage(), e);
            return PortalScrapeResult.failure(getPortalId(), getName(), e.getMessage(), elapsed, 500, searchUrl);
        }
    }
}
