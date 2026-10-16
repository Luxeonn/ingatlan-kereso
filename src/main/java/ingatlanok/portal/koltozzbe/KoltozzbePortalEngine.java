package ingatlanok.portal.koltozzbe;

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
 * Költözzbe.hu portál motor a moduláris Clean Architecture architektúrában.
 */
public class KoltozzbePortalEngine implements PortalEngine {

    private static final AppLogger LOGGER = AppLogger.getLogger(KoltozzbePortalEngine.class);
    private static final PortalDescriptor DESCRIPTOR = new PortalDescriptor(
            "koltozzbe",
            "Költözzbe",
            "#0284c7",
            "https://koltozzbe.hu",
            true,
            List.of("lakas", "haz", "garazs", "telek", "nyaralo", "iroda", "osszes"),
            true
    );

    private final KoltozzbeUrlBuilder urlBuilder;
    private final KoltozzbeParser parser;

    public KoltozzbePortalEngine() {
        this(new KoltozzbeUrlBuilder(), new KoltozzbeParser());
    }

    public KoltozzbePortalEngine(KoltozzbeUrlBuilder urlBuilder, KoltozzbeParser parser) {
        this.urlBuilder = urlBuilder != null ? urlBuilder : new KoltozzbeUrlBuilder();
        this.parser = parser != null ? parser : new KoltozzbeParser();
    }

    @Override
    public PortalDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public PortalScrapeResult scrape(SearchCriteria criteria) {
        return scrape(criteria, HttpFetcher::fetchDocument);
    }

    public PortalScrapeResult scrape(SearchCriteria criteria, java.util.function.Function<String, Document> documentFetcher) {
        long startTime = System.currentTimeMillis();
        List<IngatlanItem> items = new ArrayList<>();
        if (criteria == null) {
            criteria = new SearchCriteria();
        }

        String searchUrl = urlBuilder.buildSearchUrl(criteria);
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

                Document doc = documentFetcher.apply(pageUrl);
                if (doc == null) {
                    LOGGER.warn("Nem sikerült letölteni a Költözzbe oldalt: {}", pageUrl);
                    continue;
                }

                List<IngatlanItem> pageItems = parser.parseItems(doc, criteria, now, processedUrls);
                items.addAll(pageItems);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.info("Költözzbe találatok feldolgozva: {} db [{} ms]", items.size(), elapsed);
            return PortalScrapeResult.success(DESCRIPTOR.id(), DESCRIPTOR.name(), items, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.error("Hiba a Költözzbe keresésekor [{} ms]: {}", elapsed, e.getMessage(), e);
            return PortalScrapeResult.failure(DESCRIPTOR.id(), DESCRIPTOR.name(), e.getMessage(), elapsed, 500, searchUrl);
        }
    }

    public KoltozzbeUrlBuilder getUrlBuilder() {
        return urlBuilder;
    }

    public KoltozzbeParser getParser() {
        return parser;
    }
}
