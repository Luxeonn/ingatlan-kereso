package ingatlanok.scraper;

import ingatlanok.common.AppLogger;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.dh.DunaHouseParser;
import ingatlanok.portal.dh.DunaHouseUrlBuilder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DunaHouseScraper extends BaseScraper implements IngatlanScraper {

    private static final AppLogger LOGGER = AppLogger.getLogger(DunaHouseScraper.class);
    private final DunaHouseUrlBuilder urlBuilder = new DunaHouseUrlBuilder();
    private final DunaHouseParser parser = new DunaHouseParser();

    @Override
    public String getPortalId() {
        return "dh";
    }

    @Override
    public String getName() {
        return "Duna House";
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
        LOGGER.info("Duna House keresés indítása: {}", searchUrl);

        int maxPages = 2;
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        Set<String> processedUrls = new HashSet<>();

        try {
            for (int i = 1; i <= maxPages; i++) {
                String pageUrl = (i == 1) ? searchUrl : searchUrl + "/oldal-" + i;
                Document document = getDocument(pageUrl);
                if (document == null) {
                    LOGGER.warn("Nem sikerült letölteni a Duna House oldalt: {}", pageUrl);
                    continue;
                }

                List<IngatlanItem> pageItems = parser.parseItems(document, criteria, now, processedUrls);
                items.addAll(pageItems);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.info("Duna House találatok feldolgozva: {} db [{} ms]", items.size(), elapsed);
            return PortalScrapeResult.success(getPortalId(), getName(), items, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.error("Hiba a Duna House keresésekor [{} ms]: {}", elapsed, e.getMessage(), e);
            return PortalScrapeResult.failure(getPortalId(), getName(), e.getMessage(), elapsed);
        }
    }
}
