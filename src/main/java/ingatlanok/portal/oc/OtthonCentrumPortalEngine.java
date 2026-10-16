package ingatlanok.portal.oc;

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
 * Otthon Centrum portál motor a moduláris Clean Architecture architektúrában.
 */
public class OtthonCentrumPortalEngine implements PortalEngine {

    private static final AppLogger LOGGER = AppLogger.getLogger(OtthonCentrumPortalEngine.class);
    private static final PortalDescriptor DESCRIPTOR = new PortalDescriptor(
            "oc",
            "Otthon Centrum",
            "#ea580c",
            "https://www.oc.hu",
            true,
            List.of("lakas", "haz", "nyaralo", "telek", "garazs", "iroda", "osszes"),
            true
    );

    private final OtthonCentrumUrlBuilder urlBuilder;
    private final OtthonCentrumParser parser;

    public OtthonCentrumPortalEngine() {
        this(new OtthonCentrumUrlBuilder(), new OtthonCentrumParser());
    }

    public OtthonCentrumPortalEngine(OtthonCentrumUrlBuilder urlBuilder, OtthonCentrumParser parser) {
        this.urlBuilder = urlBuilder != null ? urlBuilder : new OtthonCentrumUrlBuilder();
        this.parser = parser != null ? parser : new OtthonCentrumParser();
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
        if (criteria == null) {
            criteria = new SearchCriteria();
        }

        String searchUrl = urlBuilder.buildSearchUrl(criteria);
        LOGGER.info("Otthon Centrum keresés indítása: {}", searchUrl);

        try {
            Document document = documentFetcher.apply(searchUrl);
            if (document == null) {
                long elapsed = System.currentTimeMillis() - startTime;
                LOGGER.warn("Nem sikerült letölteni az Otthon Centrum keresési oldalt [{} ms]: {}", elapsed, searchUrl);
                return PortalScrapeResult.failure(DESCRIPTOR.id(), DESCRIPTOR.name(), "Nem sikerült letölteni az oldalt", elapsed);
            }

            String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            Set<String> processed = new HashSet<>();
            List<IngatlanItem> items = parser.parseItems(document, criteria, now, processed);

            long elapsed = System.currentTimeMillis() - startTime;
            if (items.isEmpty()) {
                LOGGER.info("oc.hu: Nincs közvetlen találat a(z) {} keresésre [{} ms]", criteria.getCity(), elapsed);
                return PortalScrapeResult.empty(DESCRIPTOR.id(), DESCRIPTOR.name(), searchUrl, elapsed);
            }

            LOGGER.info("oc.hu Találatok száma: {} db [{} ms]", items.size(), elapsed);
            return PortalScrapeResult.success(DESCRIPTOR.id(), DESCRIPTOR.name(), items, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.error("Hiba az Otthon Centrum keresésekor [{} ms]: {}", elapsed, e.getMessage(), e);
            return PortalScrapeResult.failure(DESCRIPTOR.id(), DESCRIPTOR.name(), e.getMessage(), elapsed);
        }
    }

    public OtthonCentrumUrlBuilder getUrlBuilder() {
        return urlBuilder;
    }

    public OtthonCentrumParser getParser() {
        return parser;
    }
}
