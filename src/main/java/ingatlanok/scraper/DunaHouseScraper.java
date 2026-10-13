package ingatlanok.scraper;

import ingatlanok.common.PropertyParser;
import ingatlanok.common.SlugUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.IngatlanRepository;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Scraper implementáció a Duna House (dh.hu) portálhoz.
 */
public class DunaHouseScraper extends BaseScraper implements IngatlanScraper {

    private static final String BASE_URL = "https://dh.hu";
    private static final Pattern ID_PATTERN = Pattern.compile("/ingatlan/([A-Za-z0-9]+)");
    private static final PortalSource SOURCE = PortalSource.of("Duna House");

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
        // Init logic
    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 20;
    }

    @Override
    public PortalScrapeResult scrape(SearchCriteria criteria) {
        long startTime = System.currentTimeMillis();
        List<IngatlanItem> items = new ArrayList<>();
        if (criteria == null) {
            criteria = new SearchCriteria();
        }

        String citySlug = SlugUtils.toSlug(criteria.getCity());
        if (citySlug.isEmpty()) {
            citySlug = "debrecen";
        }

        String rawType = criteria.getPropertyType() != null ? criteria.getPropertyType().toLowerCase().trim() : "lakas";
        String typeSlug = switch (rawType) {
            case "haz" -> "haz";
            case "garazs" -> "garazs";
            case "telek" -> "telek";
            case "nyaralo" -> "nyaralo";
            case "iroda" -> "iroda";
            case "osszes" -> "lakas-haz";
            default -> "lakas";
        };

        String priceSlug = "";
        if (criteria.getMinPriceMillion() != null && criteria.getMaxPriceMillion() != null) {
            priceSlug = "/" + criteria.getMinPriceMillion() + "-" + criteria.getMaxPriceMillion() + "-mFt";
        } else if (criteria.getMinPriceMillion() != null) {
            priceSlug = "/" + criteria.getMinPriceMillion() + "-mFt-tol";
        } else if (criteria.getMaxPriceMillion() != null) {
            priceSlug = "/" + criteria.getMaxPriceMillion() + "-mFt-ig";
        }

        String searchRoute = "/elado-ingatlan/" + typeSlug + "/" + (citySlug.equals("debrecen") ? "hajdu-bihar/debrecen" : citySlug) + priceSlug;
        String searchUrl = BASE_URL + searchRoute;

        System.out.println("Duna House keresés: " + searchUrl);

        int maxPages = 2;
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        Set<String> processedUrls = new HashSet<>();

        try {
            for (int i = 1; i <= maxPages; i++) {
                String pageUrl = (i == 1) ? searchUrl : searchUrl + "/oldal-" + i;
                Document document = getDocument(pageUrl);
                if (document == null) {
                    System.err.println("Nem sikerült letölteni a Duna House oldalt: " + pageUrl);
                    continue;
                }

                Elements anchorLinks = document.select("a[href*=/ingatlan/]");
                for (Element a : anchorLinks) {
                    String href = a.attr("href");
                    if (href == null || href.isBlank()) continue;

                    String fullLink = href.startsWith("http") ? href : BASE_URL + (href.startsWith("/") ? "" : "/") + href;
                    if (!fullLink.matches(".*/ingatlan/[A-Za-z0-9]+/.*") && !fullLink.matches(".*/ingatlan/[A-Za-z0-9]+$")) {
                        continue;
                    }
                    if (processedUrls.contains(fullLink)) {
                        continue;
                    }
                    processedUrls.add(fullLink);

                    String cardText = a.text().replaceAll("\\s+", " ").trim();
                    String id = PropertyParser.extractFirstGroup(fullLink, ID_PATTERN, UUID.randomUUID().toString().substring(0, 8));
                    PriceInfo priceInfo = PropertyParser.parsePrice(cardText);
                    PropertyDetails detailsInfo = PropertyParser.parseDetails(cardText, priceInfo);
                    String title = cleanTitle(cardText, criteria.getCity(), id);
                    boolean isNew = !IngatlanRepository.getInstance().containsUrl(fullLink);

                    IngatlanItem item = new IngatlanItem(
                            id,
                            SOURCE,
                            fullLink,
                            title,
                            priceInfo,
                            criteria.getCity(),
                            detailsInfo,
                            now,
                            isNew
                    );
                    items.add(item);
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("dh.hu Találatok száma : " + items.size() + " (Új: " + items.stream().filter(IngatlanItem::isNew).count() + ")");
            return PortalScrapeResult.success(getPortalId(), getName(), items, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            System.err.println("Hiba a Duna House keresésekor: " + e.getMessage());
            return PortalScrapeResult.failure(getPortalId(), getName(), e.getMessage(), elapsed);
        }
    }

    private String cleanTitle(String text, String defaultCity, String id) {
        if (text != null && text.contains("|")) {
            String[] parts = text.split("\\|");
            if (parts.length > 0 && !parts[0].isBlank()) {
                return parts[0].replace("Új ", "").trim();
            }
        }
        return defaultCity + " - Eladó ingatlan (" + id + ")";
    }
}
