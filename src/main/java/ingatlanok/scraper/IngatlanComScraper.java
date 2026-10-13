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
 * Scraper implementáció az Ingatlan.com portálhoz.
 * Támogatja a közvetlen keresési URL generálást és intelligens Cloudflare fallbacket.
 */
public class IngatlanComScraper extends BaseScraper implements IngatlanScraper {

    private static final String BASE_URL = "https://ingatlan.com";
    private static final Pattern ID_PATTERN = Pattern.compile("/ingatlan/(\\d+)");
    private static final PortalSource SOURCE = PortalSource.of("Ingatlan.com");

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
        // Init logic
    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 20;
    }

    /**
     * Előállítja az Ingatlan.com keresési URL-t a megadott feltételek alapján.
     */
    public String buildSearchUrl(SearchCriteria criteria) {
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
            case "osszes" -> "ingatlan";
            default -> "lakas";
        };

        String priceSlug = "";
        if (criteria.getMinPriceMillion() != null && criteria.getMaxPriceMillion() != null) {
            priceSlug = "+" + criteria.getMinPriceMillion() + "-" + criteria.getMaxPriceMillion() + "-mFt";
        } else if (criteria.getMinPriceMillion() != null) {
            priceSlug = "+" + criteria.getMinPriceMillion() + "-mFt-tol";
        } else if (criteria.getMaxPriceMillion() != null) {
            priceSlug = "+" + criteria.getMaxPriceMillion() + "-mFt-ig";
        }

        return BASE_URL + "/lista/elado+" + typeSlug + "+" + citySlug + priceSlug;
    }

    @Override
    public PortalScrapeResult scrape(SearchCriteria criteria) {
        long startTime = System.currentTimeMillis();
        List<IngatlanItem> items = new ArrayList<>();
        if (criteria == null) {
            criteria = new SearchCriteria();
        }

        String searchUrl = buildSearchUrl(criteria);
        System.out.println("Ingatlan.com keresés: " + searchUrl);

        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        Set<String> processedUrls = new HashSet<>();

        try {
            // Kísérlet a találati oldal letöltésére
            Document document = getDocument(searchUrl);
            if (document != null) {
                Elements cards = document.select("a[href*=/ingatlan/]");
                for (Element a : cards) {
                    String href = a.attr("href");
                    if (href == null || href.isBlank()) continue;

                    String fullLink = href.startsWith("http") ? href : BASE_URL + (href.startsWith("/") ? "" : "/") + href;
                    if (!fullLink.matches(".*/ingatlan/\\d+.*")) continue;
                    if (processedUrls.contains(fullLink)) continue;
                    processedUrls.add(fullLink);

                    String cardText = a.text().replaceAll("\\s+", " ").trim();
                    String id = PropertyParser.extractFirstGroup(fullLink, ID_PATTERN, UUID.randomUUID().toString().substring(0, 8));
                    PriceInfo priceInfo = PropertyParser.parsePrice(cardText);
                    PropertyDetails detailsInfo = PropertyParser.parseDetails(cardText, priceInfo);
                    String title = criteria.getCity() + " - " + extractTitle(cardText, criteria.getCity(), id);
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
                    item.setLocation(criteria.getCity());

                    // Szigorú utólagos szűrés ár és lokáció alapján
                    if (!criteria.matches(item)) {
                        continue;
                    }

                    items.add(item);
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            if (items.isEmpty()) {
                System.out.println("Ingatlan.com: Nem sikerült közvetlen hirdetéseket kinyerni (a portál bot-védelme aktív).");
                return PortalScrapeResult.botProtected(getPortalId(), getName(), searchUrl, elapsed);
            }

            System.out.println("ingatlan.com Találatok száma : " + items.size() + " (Új: " + items.stream().filter(IngatlanItem::isNew).count() + ")");
            return PortalScrapeResult.success(getPortalId(), getName(), items, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            System.err.println("Hiba az Ingatlan.com keresésekor: " + e.getMessage());
            return PortalScrapeResult.failure(getPortalId(), getName(), e.getMessage(), elapsed);
        }
    }

    private String extractTitle(String text, String defaultCity, String id) {
        if (text != null && text.length() > 5) {
            int max = Math.min(text.length(), 65);
            return text.substring(0, max).trim() + "...";
        }
        return defaultCity + " - Ingatlan (" + id + ")";
    }
}
