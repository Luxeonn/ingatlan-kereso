package ingatlanok.scraper;

import ingatlanok.common.AppLogger;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.jofogas.JofogasParser;
import ingatlanok.portal.jofogas.JofogasUrlBuilder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class JofogasScraper extends BaseScraper implements IngatlanScraper {

    private static final AppLogger LOGGER = AppLogger.getLogger(JofogasScraper.class);
    private final JofogasUrlBuilder urlBuilder = new JofogasUrlBuilder();
    private final JofogasParser parser = new JofogasParser();

    @Override
    public String getPortalId() {
        return "jofogas";
    }

    @Override
    public String getName() {
        return "Jófogás Ingatlan";
    }

    @Override
    public void init() {
    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 25;
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
        LOGGER.info("Jófogás Ingatlan keresés indítása: {}", searchUrl);

        try {
            Document document = getDocument(searchUrl);
            if (document == null) {
                long elapsed = System.currentTimeMillis() - startTime;
                LOGGER.warn("Nem sikerült letölteni a Jófogás keresési oldalt [{} ms]: {}", elapsed, searchUrl);
                return PortalScrapeResult.failure(getPortalId(), getName(), "Nem sikerült letölteni az oldalt", elapsed);
            }

            String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            Set<String> processed = new HashSet<>();
            List<IngatlanItem> items = parser.parseItems(document, criteria, now, processed);

            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.info("jofogas.hu Találatok száma: {} db [{} ms]", items.size(), elapsed);
            return PortalScrapeResult.success(getPortalId(), getName(), items, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.error("Hiba a Jófogás keresésekor [{} ms]: {}", elapsed, e.getMessage(), e);
            return PortalScrapeResult.failure(getPortalId(), getName(), e.getMessage(), elapsed);
        }
    }
}
