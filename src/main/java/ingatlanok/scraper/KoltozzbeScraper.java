package ingatlanok.scraper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ingatlanok.common.AppLogger;
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
 * Web scraper implementáció a koltozzbe.hu portálhoz.
 * Kifejezetten eladó családi házakra és lakásokra optimalizálva,
 * támogatva Debrecent és a környező agglomerációs településeket.
 */
public class KoltozzbeScraper extends BaseScraper implements IngatlanScraper {

    private static final AppLogger LOGGER = AppLogger.getLogger(KoltozzbeScraper.class);
    private static final String BASE_URL = "https://koltozzbe.hu";
    private static final Pattern ID_PATTERN = Pattern.compile("/h([0-9]+)");
    private static final PortalSource SOURCE = PortalSource.of("Költözzbe");

    @Override
    public String getPortalId() {
        return "koltozzbe";
    }

    @Override
    public String getName() {
        return "Költözzbe";
    }

    @Override
    public void init() {
    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 20;
    }

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

        String route = "/elado-" + typeSlug + "-" + citySlug;
        List<String> params = new ArrayList<>();

        if (criteria.getMinPriceMillion() != null) {
            params.add("p1=" + criteria.getMinPriceMillion());
        }
        if (criteria.getMaxPriceMillion() != null) {
            params.add("p2=" + criteria.getMaxPriceMillion());
        }
        if (criteria.getMinArea() != null) {
            params.add("a1=" + criteria.getMinArea());
        }
        if (criteria.getMaxArea() != null) {
            params.add("a2=" + criteria.getMaxArea());
        }

        if (params.isEmpty()) {
            return BASE_URL + route;
        }
        return BASE_URL + route + "?" + String.join("&", params);
    }

    @Override
    public PortalScrapeResult scrape(SearchCriteria criteria) {
        long startTime = System.currentTimeMillis();
        List<IngatlanItem> items = new ArrayList<>();
        if (criteria == null) {
            criteria = new SearchCriteria();
        }

        String searchUrl = buildSearchUrl(criteria);
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

                Document doc = getDocument(pageUrl);
                if (doc == null) {
                    LOGGER.warn("Nem sikerült letölteni a Költözzbe oldalt: {}", pageUrl);
                    continue;
                }

                Elements cards = doc.select(".listing-card, div[data-lid]");
                LOGGER.debug("Költözzbe [{} oldal] -> {} kártya található", page, cards.size());

                if (!cards.isEmpty()) {
                    for (Element card : cards) {
                        Element linkElem = card.selectFirst("a.listing-item-link, a[href*=/elado-]");
                        if (linkElem == null) {
                            continue;
                        }

                        String href = linkElem.attr("href");
                        if (href == null || href.isBlank()) {
                            continue;
                        }

                        String fullUrl = href.startsWith("http") ? href : BASE_URL + (href.startsWith("/") ? "" : "/") + href;
                        if (processedUrls.contains(fullUrl)) {
                            continue;
                        }
                        processedUrls.add(fullUrl);

                        String lid = card.attr("data-lid");
                        if (lid.isBlank()) {
                            lid = PropertyParser.extractFirstGroup(fullUrl, ID_PATTERN, UUID.randomUUID().toString().substring(0, 8));
                        }

                        String title = linkElem.attr("title");
                        if (title == null || title.isBlank()) {
                            Element priceHeader = card.selectFirst(".item-price");
                            title = (priceHeader != null) ? priceHeader.text().trim() : criteria.getCity() + " - Eladó ingatlan";
                        }

                        Element priceElem = card.selectFirst(".item-type.h2");
                        String rawPriceText = (priceElem != null) ? priceElem.text() : card.text();
                        PriceInfo priceInfo = PropertyParser.parsePrice(rawPriceText);

                        Element paramsElem = card.selectFirst(".listing-parameters");
                        String detailsText = (paramsElem != null) ? paramsElem.text() : card.text();
                        PropertyDetails details = PropertyParser.parseDetails(detailsText, priceInfo);

                        Element imgElem = card.selectFirst(".listing-thumbnail img, img");
                        String imageUrl = null;
                        if (imgElem != null) {
                            imageUrl = imgElem.hasAttr("data-src") ? imgElem.attr("data-src") : imgElem.attr("src");
                            if (imageUrl != null && imageUrl.startsWith("/")) {
                                imageUrl = BASE_URL + imageUrl;
                            }
                        }

                        boolean isNew = !IngatlanRepository.getInstance().containsUrl(fullUrl);
                        IngatlanItem item = new IngatlanItem(
                                lid,
                                SOURCE,
                                fullUrl,
                                title,
                                priceInfo,
                                criteria.getCity(),
                                details,
                                now,
                                isNew
                        );
                        item.setLocation(criteria.getCity());
                        item.setImageUrl(imageUrl);

                        if (!criteria.matches(item)) {
                            LOGGER.debug("Költözzbe tétel kiszűrve feltételek miatt: {} (Ár: {})", item.getUrl(), item.getPrice());
                            continue;
                        }
                        items.add(item);
                    }
                } else {
                    // Fallback to Schema.org JSON-LD extraction
                    parseJsonLd(doc, criteria, items, processedUrls, now);
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            long newCount = items.stream().filter(IngatlanItem::isNew).count();
            LOGGER.info("Költözzbe találatok feldolgozva: {} db (új: {}) [{} ms]", items.size(), newCount, elapsed);
            return PortalScrapeResult.success(getPortalId(), getName(), items, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.error("Hiba a Költözzbe keresésekor [{} ms]: {}", elapsed, e.getMessage(), e);
            return PortalScrapeResult.failure(getPortalId(), getName(), e.getMessage(), elapsed, 500, searchUrl);
        }
    }

    private void parseJsonLd(Document doc, SearchCriteria criteria, List<IngatlanItem> items, Set<String> processedUrls, String now) {
        Elements scripts = doc.select("script[type=application/ld+json]");
        for (Element script : scripts) {
            String jsonText = script.data();
            if (jsonText == null || !jsonText.contains("ListItem") || !jsonText.contains("Product")) {
                continue;
            }
            try {
                JsonElement root = JsonParser.parseString(jsonText);
                if (!root.isJsonObject()) continue;
                JsonObject obj = root.getAsJsonObject();
                if (!obj.has("itemListElement")) continue;

                JsonArray elements = obj.getAsJsonArray("itemListElement");
                for (JsonElement el : elements) {
                    if (!el.isJsonObject()) continue;
                    JsonObject itemObj = el.getAsJsonObject();
                    if (!itemObj.has("item")) continue;
                    JsonObject product = itemObj.getAsJsonObject("item");

                    String url = product.has("url") ? product.get("url").getAsString() : null;
                    if (url == null || processedUrls.contains(url)) continue;
                    processedUrls.add(url);

                    String name = product.has("name") ? product.get("name").getAsString() : criteria.getCity() + " - Eladó ingatlan";
                    String id = PropertyParser.extractFirstGroup(url, ID_PATTERN, UUID.randomUUID().toString().substring(0, 8));

                    long rawPrice = 0;
                    if (product.has("offers")) {
                        JsonObject offers = product.getAsJsonObject("offers");
                        if (offers.has("priceSpecification")) {
                            JsonObject ps = offers.getAsJsonObject("priceSpecification");
                            if (ps.has("price")) {
                                rawPrice = ps.get("price").getAsLong();
                            }
                        }
                    }

                    PriceInfo priceInfo = (rawPrice > 0)
                            ? PropertyParser.parsePrice(rawPrice + " Ft")
                            : PropertyParser.parsePrice(name);

                    PropertyDetails details = PropertyParser.parseDetails(name, priceInfo);

                    String imageUrl = null;
                    if (product.has("image") && product.get("image").isJsonArray()) {
                        JsonArray imgArr = product.getAsJsonArray("image");
                        if (!imgArr.isEmpty()) {
                            imageUrl = imgArr.get(0).getAsString();
                        }
                    }

                    boolean isNew = !IngatlanRepository.getInstance().containsUrl(url);
                    IngatlanItem item = new IngatlanItem(
                            id,
                            SOURCE,
                            url,
                            name,
                            priceInfo,
                            criteria.getCity(),
                            details,
                            now,
                            isNew
                    );
                    item.setLocation(criteria.getCity());
                    item.setImageUrl(imageUrl);

                    if (criteria.matches(item)) {
                        items.add(item);
                    }
                }
            } catch (Exception ex) {
                LOGGER.debug("JSON-LD parse warning: {}", ex.getMessage());
            }
        }
    }
}
