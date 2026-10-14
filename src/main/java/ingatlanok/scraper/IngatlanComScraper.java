package ingatlanok.scraper;

import ingatlanok.common.AppLogger;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.ingatlancom.IngatlanComParser;
import ingatlanok.portal.ingatlancom.IngatlanComUrlBuilder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class IngatlanComScraper extends BaseScraper implements IngatlanScraper {

    private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanComScraper.class);
    private static final String BASE_URL = IngatlanComUrlBuilder.BASE_URL;
    private static final PortalSource SOURCE = PortalSource.of("Ingatlan.com");

    private final IngatlanComUrlBuilder urlBuilder = new IngatlanComUrlBuilder();
    private final IngatlanComParser parser = new IngatlanComParser();

    @Override
    public String getPortalId() {
        return "ingatlancom";
    }

    @Override
    public String getName() {
        return "Ingatlan.com";
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
        LOGGER.info("Ingatlan.com keresés indítása: {}", searchUrl);

        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        Set<String> processedUrls = new HashSet<>();

        if (SidecarCliExecutor.isAvailable()) {
            LOGGER.info("Ingatlan.com: Sidecar modul detektálva, lekérdezés indítása...");
            try {
                Optional<SidecarResponse> sidecarResp = SidecarCliExecutor.execute(getPortalId(), criteria);
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
                        return PortalScrapeResult.success(getPortalId(), getName(), items, elapsed);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Ingatlan.com: Hiba a sidecar végrehajtás során: {}", e.getMessage(), e);
            }
        }

        try {
            Document document = getDocument(searchUrl);
            if (document == null) {
                long elapsed = System.currentTimeMillis() - startTime;
                LOGGER.warn("Ingatlan.com: Nem sikerült közvetlenül letölteni az oldalt (WAF tiltás vagy hálózati hiba) [{} ms] -> fallback: {}", elapsed, searchUrl);
                return PortalScrapeResult.botProtected(getPortalId(), getName(), searchUrl, elapsed);
            }

            if (parser.isBotChallenge(document)) {
                long elapsed = System.currentTimeMillis() - startTime;
                LOGGER.warn("Ingatlan.com: Cloudflare WAF bot-védelem detektálva [{} ms] -> fallback: {}", elapsed, searchUrl);
                return PortalScrapeResult.botProtected(getPortalId(), getName(), searchUrl, elapsed);
            }

            items = parser.parseItems(document, criteria, now, processedUrls);
            long elapsed = System.currentTimeMillis() - startTime;

            if (items.isEmpty()) {
                LOGGER.warn("Ingatlan.com: Nem sikerült közvetlen hirdetéseket kinyerni (bot-védelem aktív) [{} ms] -> fallback: {}", elapsed, searchUrl);
                return PortalScrapeResult.botProtected(getPortalId(), getName(), searchUrl, elapsed);
            }

            LOGGER.info("ingatlan.com Találatok száma: {} db [{} ms]", items.size(), elapsed);
            return PortalScrapeResult.success(getPortalId(), getName(), items, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.error("Hiba az Ingatlan.com keresésekor [{} ms]: {}", elapsed, e.getMessage(), e);
            return PortalScrapeResult.failure(getPortalId(), getName(), e.getMessage(), elapsed);
        }
    }
}
