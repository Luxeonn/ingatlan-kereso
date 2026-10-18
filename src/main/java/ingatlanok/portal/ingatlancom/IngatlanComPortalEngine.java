package ingatlanok.portal.ingatlancom;

import ingatlanok.common.AppLogger;
import ingatlanok.common.HttpFetcher;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;
import ingatlanok.scraper.PortalScrapeResult;
import ingatlanok.scraper.SidecarCliExecutor;
import ingatlanok.scraper.SidecarResponse;
import org.jsoup.nodes.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Ingatlan.com portál motor a moduláris Clean Architecture architektúrában.
 * Támogatja a hibrid Sidecar végrehajtást és a Cloudflare WAF bot-védelem graceful fallback kezelését.
 */
public class IngatlanComPortalEngine implements PortalEngine {

    private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanComPortalEngine.class);
    private static final PortalSource SOURCE = PortalSource.of("Ingatlan.com");
    private static final PortalDescriptor DESCRIPTOR = new PortalDescriptor(
            "ingatlancom",
            "Ingatlan.com",
            "#0284c7",
            "https://ingatlan.com",
            true,
            List.of("lakas", "haz", "telek"),
            false
    );

    private final IngatlanComUrlBuilder urlBuilder;
    private final IngatlanComParser parser;

    public IngatlanComPortalEngine() {
        this(new IngatlanComUrlBuilder(), new IngatlanComParser());
    }

    public IngatlanComPortalEngine(IngatlanComUrlBuilder urlBuilder, IngatlanComParser parser) {
        this.urlBuilder = urlBuilder != null ? urlBuilder : new IngatlanComUrlBuilder();
        this.parser = parser != null ? parser : new IngatlanComParser();
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
        LOGGER.info("Ingatlan.com keresés indítása: {}", searchUrl);

        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        Set<String> processedUrls = new HashSet<>();

        if (SidecarCliExecutor.isAvailable()) {
            LOGGER.info("Ingatlan.com: Sidecar modul detektálva, lekérdezés indítása...");
            try {
                Optional<SidecarResponse> sidecarResp = SidecarCliExecutor.execute(DESCRIPTOR.id(), criteria);
                if (sidecarResp.isPresent() && sidecarResp.get().isSuccess()) {
                    SidecarResponse resp = sidecarResp.get();
                    for (SidecarResponse.SidecarItem sidecarItem : resp.getItems()) {
                        if (sidecarItem.getUrl() == null || sidecarItem.getUrl().isBlank()) continue;
                        if (!processedUrls.add(sidecarItem.getUrl())) continue;

                        IngatlanItem item = sidecarItem.toIngatlanItem(SOURCE, criteria.getCity(), now, false);
                        if (criteria.matches(item)) {
                            items.add(item);
                        }
                    }

                    long elapsed = System.currentTimeMillis() - startTime;
                    if (!items.isEmpty()) {
                        LOGGER.info("ingatlan.com (Sidecar) Találatok száma: {} db [{} ms]", items.size(), elapsed);
                        return PortalScrapeResult.success(DESCRIPTOR.id(), DESCRIPTOR.name(), items, elapsed);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Ingatlan.com: Hiba a sidecar végrehajtás során: {}", e.getMessage(), e);
            }
        }

        try {
            Document document = documentFetcher.apply(searchUrl);
            if (document == null) {
                long elapsed = System.currentTimeMillis() - startTime;
                LOGGER.warn("Ingatlan.com: Nem sikerült közvetlenül letölteni az oldalt (WAF tiltás vagy hálózati hiba) [{} ms] -> fallback: {}", elapsed, searchUrl);
                return PortalScrapeResult.botProtected(DESCRIPTOR.id(), DESCRIPTOR.name(), searchUrl, elapsed);
            }

            if (parser.isBotChallenge(document)) {
                long elapsed = System.currentTimeMillis() - startTime;
                LOGGER.warn("Ingatlan.com: Cloudflare WAF bot-védelem detektálva [{} ms] -> fallback: {}", elapsed, searchUrl);
                return PortalScrapeResult.botProtected(DESCRIPTOR.id(), DESCRIPTOR.name(), searchUrl, elapsed);
            }

            items = parser.parseItems(document, criteria, now, processedUrls);
            long elapsed = System.currentTimeMillis() - startTime;

            if (items.isEmpty()) {
                LOGGER.warn("Ingatlan.com: Nem sikerült közvetlen hirdetéseket kinyerni (bot-védelem aktív) [{} ms] -> fallback: {}", elapsed, searchUrl);
                return PortalScrapeResult.botProtected(DESCRIPTOR.id(), DESCRIPTOR.name(), searchUrl, elapsed);
            }

            LOGGER.info("ingatlan.com Találatok száma: {} db [{} ms]", items.size(), elapsed);
            return PortalScrapeResult.success(DESCRIPTOR.id(), DESCRIPTOR.name(), items, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.error("Hiba az Ingatlan.com keresésekor [{} ms]: {}", elapsed, e.getMessage(), e);
            return PortalScrapeResult.failure(DESCRIPTOR.id(), DESCRIPTOR.name(), e.getMessage(), elapsed);
        }
    }

    public IngatlanComUrlBuilder getUrlBuilder() {
        return urlBuilder;
    }

    public IngatlanComParser getParser() {
        return parser;
    }
}
