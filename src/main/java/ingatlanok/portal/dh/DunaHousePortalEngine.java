package ingatlanok.portal.dh;

import ingatlanok.common.AppLogger;
import ingatlanok.common.HttpFetcher;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;
import ingatlanok.scraper.PortalScrapeResult;
import org.jsoup.nodes.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Duna House portál motor a moduláris Clean Architecture architektúrában.
 */
public class DunaHousePortalEngine implements PortalEngine {

    private static final AppLogger LOGGER = AppLogger.getLogger(DunaHousePortalEngine.class);
    private static final PortalDescriptor DESCRIPTOR = new PortalDescriptor(
            "dh",
            "Duna House",
            "#1d4ed8",
            "https://dh.hu",
            true,
            List.of("lakas", "haz", "garazs", "telek", "nyaralo", "iroda", "osszes"),
            true
    );

    private final DunaHouseUrlBuilder urlBuilder;
    private final DunaHouseParser parser;

    public DunaHousePortalEngine() {
        this(new DunaHouseUrlBuilder(), new DunaHouseParser());
    }

    public DunaHousePortalEngine(DunaHouseUrlBuilder urlBuilder, DunaHouseParser parser) {
        this.urlBuilder = urlBuilder != null ? urlBuilder : new DunaHouseUrlBuilder();
        this.parser = parser != null ? parser : new DunaHouseParser();
    }

    @Override
    public PortalDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public PortalScrapeResult scrape(SearchCriteria criteria) {
        long startTime = System.currentTimeMillis();
        List<IngatlanItem> items = new ArrayList<>();
        if (criteria == null) {
            criteria = new SearchCriteria();
        }

        String searchUrl = urlBuilder.buildSearchUrl(criteria);
        LOGGER.info("Duna House keresés indítása: {}", searchUrl);

        int maxPages = 2;
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        Set<String> processedUrls = new HashSet<>();

        try {
            for (int i = 1; i <= maxPages; i++) {
                String pageUrl = (i == 1) ? searchUrl : searchUrl + "/oldal-" + i;
                Document document = HttpFetcher.fetchDocument(pageUrl);
                if (document == null) {
                    LOGGER.warn("Nem sikerült letölteni a Duna House oldalt: {}", pageUrl);
                    continue;
                }

                List<IngatlanItem> pageItems = parser.parseItems(document, criteria, now, processedUrls);
                items.addAll(pageItems);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.info("Duna House találatok feldolgozva: {} db [{} ms]", items.size(), elapsed);
            return PortalScrapeResult.success(DESCRIPTOR.id(), DESCRIPTOR.name(), items, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.error("Hiba a Duna House keresésekor [{} ms]: {}", elapsed, e.getMessage(), e);
            return PortalScrapeResult.failure(DESCRIPTOR.id(), DESCRIPTOR.name(), e.getMessage(), elapsed);
        }
    }

    public DunaHouseUrlBuilder getUrlBuilder() {
        return urlBuilder;
    }

    public DunaHouseParser getParser() {
        return parser;
    }
}
