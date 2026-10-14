package ingatlanok.scraper;

import ingatlanok.common.AppLogger;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarJsonParser;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarQueryBuilder;
import org.jsoup.nodes.Element;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class IngatlanBazarScraper extends BaseScraper implements IngatlanScraper {

    private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanBazarScraper.class);
    public static final String BASE_URL = IngatlanBazarQueryBuilder.BASE_URL;
    public static final String API_SEARCH_URL = IngatlanBazarQueryBuilder.API_SEARCH_URL;

    private final IngatlanBazarQueryBuilder queryBuilder = new IngatlanBazarQueryBuilder();
    private final IngatlanBazarJsonParser jsonParser = new IngatlanBazarJsonParser();

    @Override
    public String getPortalId() {
        return "ingatlanbazar";
    }

    @Override
    public String getName() {
        return "Ingatlanbazár";
    }

    @Override
    public void init() {
    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 25;
    }

    public String buildSearchUrl(SearchCriteria criteria) {
        return queryBuilder.buildSearchUrl(criteria);
    }

    public String buildApiSearchUrl(SearchCriteria criteria, int page) {
        return queryBuilder.buildApiSearchUrl(criteria, page);
    }

    @Override
    public PortalScrapeResult scrape(SearchCriteria criteria) {
        long startTime = System.currentTimeMillis();
        List<IngatlanItem> items = new ArrayList<>();
        if (criteria == null) {
            criteria = new SearchCriteria();
        }

        String fallbackUrl = buildSearchUrl(criteria);
        LOGGER.info("Ingatlanbazár keresés indítása: {}", fallbackUrl);

        int maxPages = 2;
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        Set<String> processedUrls = new HashSet<>();

        try {
            for (int page = 1; page <= maxPages; page++) {
                String apiUrl = buildApiSearchUrl(criteria, page);
                LOGGER.debug("Ingatlanbazár REST API lekérdezése (oldal: {}): {}", page, apiUrl);
                String json = getJson(apiUrl);

                if (json == null || json.isBlank()) {
                    if (page == 1) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        LOGGER.warn("Nem sikerült elérni az Ingatlanbazár REST API-t [{} ms]: {}", elapsed, apiUrl);
                        return PortalScrapeResult.failure(getPortalId(), getName(), "Nem sikerült elérni az Ingatlanbazár REST API-t", elapsed);
                    }
                    break;
                }

                List<IngatlanItem> pageItems = jsonParser.parseItemsFromJson(json, criteria, processedUrls, now);
                items.addAll(pageItems);

                if (pageItems.size() < 50) {
                    break;
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.info("Ingatlanbazár találatok száma: {} [{} ms]", items.size(), elapsed);

            if (items.isEmpty()) {
                return PortalScrapeResult.empty(getPortalId(), getName(), fallbackUrl, elapsed);
            }

            return PortalScrapeResult.success(getPortalId(), getName(), items, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.error("Hiba az Ingatlanbazár scraping során [{} ms]: {}", elapsed, e.getMessage(), e);
            return PortalScrapeResult.failure(getPortalId(), getName(), e.getMessage(), elapsed);
        }
    }

    public List<IngatlanItem> parseItemsFromJson(String json, SearchCriteria criteria, Set<String> processedUrls, String now) {
        return jsonParser.parseItemsFromJson(json, criteria, processedUrls, now);
    }
}
