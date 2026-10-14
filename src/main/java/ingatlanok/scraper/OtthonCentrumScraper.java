package ingatlanok.scraper;

import ingatlanok.common.AppLogger;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.oc.OtthonCentrumParser;
import ingatlanok.portal.oc.OtthonCentrumUrlBuilder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class OtthonCentrumScraper extends BaseScraper implements IngatlanScraper {

    private static final AppLogger LOGGER = AppLogger.getLogger(OtthonCentrumScraper.class);
    private final OtthonCentrumUrlBuilder urlBuilder = new OtthonCentrumUrlBuilder();
    private final OtthonCentrumParser parser = new OtthonCentrumParser();

    @Override
    public String getPortalId() {
        return "oc";
    }

    @Override
    public String getName() {
        return "Otthon Centrum";
    }

    @Override
    public void init() {
    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 12;
    }

    public String buildSearchUrl(SearchCriteria criteria) {
        return urlBuilder.buildSearchUrl(criteria);
    }

    @Override
    public PortalScrapeResult scrape(SearchCriteria criteria) {
        long startTime = System.currentTimeMillis();
        if (criteria == null) {
            criteria = new SearchCriteria();
        }

        String searchUrl = buildSearchUrl(criteria);
        LOGGER.info("Otthon Centrum keresés indítása: {}", searchUrl);

        try {
            Document document = getDocument(searchUrl);
            if (document == null) {
                long elapsed = System.currentTimeMillis() - startTime;
                LOGGER.warn("Nem sikerült letölteni az Otthon Centrum keresési oldalt [{} ms]: {}", elapsed, searchUrl);
                return PortalScrapeResult.failure(getPortalId(), getName(), "Nem sikerült letölteni az oldalt", elapsed);
            }

            String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            Set<String> processed = new HashSet<>();
            List<IngatlanItem> items = parser.parseItems(document, criteria, now, processed);

            long elapsed = System.currentTimeMillis() - startTime;
            if (items.isEmpty()) {
                LOGGER.info("oc.hu: Nincs közvetlen találat a(z) {} keresésre [{} ms]", criteria.getCity(), elapsed);
                return PortalScrapeResult.empty(getPortalId(), getName(), searchUrl, elapsed);
            }

            LOGGER.info("oc.hu Találatok száma: {} db [{} ms]", items.size(), elapsed);
            return PortalScrapeResult.success(getPortalId(), getName(), items, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.error("Hiba az Otthon Centrum keresésekor [{} ms]: {}", elapsed, e.getMessage(), e);
            return PortalScrapeResult.failure(getPortalId(), getName(), e.getMessage(), elapsed);
        }
    }
}
