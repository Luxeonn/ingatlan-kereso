package ingatlanok.scraper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scraper implementáció az Otthon Centrum (oc.hu) portálhoz.
 */
public class OtthonCentrumScraper extends BaseScraper implements IngatlanScraper {

    private static final String BASE_URL = "https://www.oc.hu";
    private static final Pattern ID_PATTERN = Pattern.compile("/ingatlanok/([A-Za-z0-9]+)");
    private static final PortalSource SOURCE = PortalSource.of("Otthon Centrum");

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
        // Init logic
    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 12;
    }

    /**
     * Előállítja az Otthon Centrum keresési URL-t a megadott feltételek alapján.
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
            case "nyaralo" -> "nyaralo";
            case "telek" -> "telek";
            case "garazs" -> "garazs";
            case "iroda" -> "uzleticelu";
            case "osszes" -> "lakas";
            default -> "lakas";
        };

        return BASE_URL + "/elado/" + typeSlug + "/" + citySlug;
    }

    @Override
    public PortalScrapeResult scrape(SearchCriteria criteria) {
        long startTime = System.currentTimeMillis();
        List<IngatlanItem> items = new ArrayList<>();
        if (criteria == null) {
            criteria = new SearchCriteria();
        }

        String searchUrl = buildSearchUrl(criteria);
        System.out.println("Otthon Centrum keresés: " + searchUrl);

        try {
            Document document = getDocument(searchUrl);
            if (document == null) {
                long elapsed = System.currentTimeMillis() - startTime;
                System.err.println("Nem sikerült letölteni az Otthon Centrum keresési oldalt: " + searchUrl);
                return PortalScrapeResult.failure(getPortalId(), getName(), "Nem sikerült letölteni az oldalt", elapsed);
            }

            // Ellenőrizzük, hogy van-e "Nincs találat" jelzés az oldalon
            boolean noResults = !document.select("div.option:contains(Nincs találat)").isEmpty()
                    || !document.select(".location-option-no-result:not(.d-none)").isEmpty();
            if (noResults) {
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.println("oc.hu: Nincs közvetlen találat a(z) " + criteria.getCity() + " keresésre.");
                return PortalScrapeResult.success(getPortalId(), getName(), items, elapsed);
            }

            String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            Set<String> processed = new HashSet<>();

            // Keressük a kártya elemeket
            Elements cards = document.select("div.real-estate-card, div[data-controller='seo']");
            if (cards.isEmpty()) {
                cards = document.select("a[href*=/ingatlanok/]");
            }

            for (Element card : cards) {
                String href = card.is("a") ? card.attr("href") : "";
                if (href.isBlank()) {
                    Element a = card.selectFirst("a[href*=/ingatlanok/]");
                    if (a != null) {
                        href = a.attr("href");
                    }
                }
                if (href == null || href.isBlank()) continue;

                if (href.matches(".*/ingatlanok/[A-Za-z0-9]+$")) {
                    String fullLink = href.startsWith("http") ? href : BASE_URL + (href.startsWith("/") ? "" : "/") + href;
                    if (processed.contains(fullLink)) continue;
                    processed.add(fullLink);

                    String cardText = card.text().replaceAll("\\s+", " ").trim();
                    String id = PropertyParser.extractFirstGroup(fullLink, ID_PATTERN, UUID.randomUUID().toString().substring(0, 8));

                    // Strukturált adatok kinyerése a beágyazott JSON-ból (data-seo-select-item-value)
                    String locationCity = null;
                    Long jsonPriceHuf = null;
                    String seoAttr = card.attr("data-seo-select-item-value");
                    if (seoAttr.isBlank()) {
                        Element seoHolder = card.selectFirst("[data-seo-select-item-value]");
                        if (seoHolder != null) {
                            seoAttr = seoHolder.attr("data-seo-select-item-value");
                        }
                    }

                    if (!seoAttr.isBlank()) {
                        try {
                            JsonObject seoObj = JsonParser.parseString(seoAttr).getAsJsonObject();
                            if (seoObj.has("ecommerce")) {
                                JsonObject ecom = seoObj.getAsJsonObject("ecommerce");
                                if (ecom.has("items")) {
                                    JsonArray itemsArray = ecom.getAsJsonArray("items");
                                    if (!itemsArray.isEmpty()) {
                                        JsonObject firstItem = itemsArray.get(0).getAsJsonObject();
                                        if (firstItem.has("location_city")) {
                                            locationCity = firstItem.get("location_city").getAsString();
                                        }
                                        if (firstItem.has("price") && !firstItem.get("price").isJsonNull()) {
                                            jsonPriceHuf = firstItem.get("price").getAsLong();
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    PriceInfo priceInfo = PropertyParser.parsePrice(cardText);
                    if (jsonPriceHuf != null && (priceInfo == null || priceInfo.getAmountHuf() == null)) {
                        double million = Math.round((jsonPriceHuf / 1_000_000.0) * 100.0) / 100.0;
                        priceInfo = new PriceInfo(jsonPriceHuf + " Ft", jsonPriceHuf, million);
                    }

                    PropertyDetails detailsInfo = PropertyParser.parseDetails(cardText, priceInfo);
                    String actualCity = (locationCity != null && !locationCity.isBlank()) ? locationCity : criteria.getCity();
                    String title = cleanTitle(cardText, actualCity, id);
                    boolean isNew = !IngatlanRepository.getInstance().containsUrl(fullLink);

                    IngatlanItem item = new IngatlanItem(
                            id,
                            SOURCE,
                            fullLink,
                            title,
                            priceInfo,
                            actualCity,
                            detailsInfo,
                            now,
                            isNew
                    );
                    item.setLocation(actualCity);

                    // Szigorú szűrés ár és lokáció alapján
                    if (!criteria.matches(item)) {
                        continue;
                    }

                    items.add(item);
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("oc.hu Találatok száma : " + items.size() + " (Új: " + items.stream().filter(IngatlanItem::isNew).count() + ")");
            return PortalScrapeResult.success(getPortalId(), getName(), items, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            System.err.println("Hiba az Otthon Centrum keresésekor: " + e.getMessage());
            return PortalScrapeResult.failure(getPortalId(), getName(), e.getMessage(), elapsed);
        }
    }

    private String cleanTitle(String text, String defaultCity, String id) {
        if (text != null && text.contains("szoba")) {
            int idx = text.indexOf("szoba");
            String after = text.substring(idx + 5).trim();
            if (after.length() > 5) {
                int end = Math.min(after.length(), 65);
                return after.substring(0, end).trim() + "...";
            }
        }
        return defaultCity + " - Eladó ingatlan (" + id + ")";
    }
}
