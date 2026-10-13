package ingatlanok.scraper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ingatlanok.common.SlugUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;
import org.jsoup.nodes.Element;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Scraper implementáció az Ingatlanbazár (ingatlanbazar.hu) portálhoz.
 * A portál Angular SPA architektúrájához illeszkedve a hivatalos és nyílt belső REST JSON API-t
 * (GET /api/property-search) használja az adatok pontos, numerikus és megbízható lekérdezéséhez.
 */
public class IngatlanBazarScraper extends BaseScraper implements IngatlanScraper {

    public static final String BASE_URL = "https://www.ingatlanbazar.hu";
    public static final String API_SEARCH_URL = "https://www.ingatlanbazar.hu/api/property-search";
    private static final PortalSource SOURCE = PortalSource.of("Ingatlanbazár");

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
        // Init logic
    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 25;
    }

    /**
     * Előállítja a böngészőben közvetlenül megnyitható keresési és fallback URL-t a megadott kritériumok alapján.
     */
    public String buildSearchUrl(SearchCriteria criteria) {
        if (criteria == null) {
            criteria = new SearchCriteria();
        }
        String city = criteria.getCity();
        if (city == null || city.isBlank()) {
            city = "Debrecen";
        }
        String displayCity = Character.toUpperCase(city.charAt(0)) + city.substring(1);

        String rawType = criteria.getPropertyType() != null ? criteria.getPropertyType().toLowerCase().trim() : "lakas";
        String typeSlug = switch (rawType) {
            case "haz" -> "Haz";
            case "garazs" -> "Garazs";
            case "telek" -> "Telek";
            case "nyaralo" -> "Nyaralo";
            case "iroda" -> "Iroda";
            case "osszes" -> "Lakas-es-Haz";
            default -> "Lakas";
        };

        StringBuilder query = new StringBuilder("?");
        Optional<Integer> locId = IngatlanBazarLocationResolver.resolveLocationId(city);
        locId.ifPresent(id -> query.append("property_location=").append(id).append("&"));

        if (criteria.getMinPriceMillion() != null) {
            query.append("price_min=").append(criteria.getMinPriceMillion()).append("&");
        }
        if (criteria.getMaxPriceMillion() != null) {
            query.append("price_max=").append(criteria.getMaxPriceMillion()).append("&");
        }

        if ("haz".equals(rawType)) {
            query.append("property__2=2&");
        } else if ("osszes".equals(rawType)) {
            query.append("property__2=3_2&");
        } else {
            query.append("property__2=3&");
        }

        String q = query.toString();
        if (q.endsWith("&") || q.endsWith("?")) {
            q = q.substring(0, q.length() - 1);
        }

        return BASE_URL + "/ingatlan-" + displayCity + "-Elado-" + typeSlug + q;
    }

    /**
     * Előállítja az Ingatlanbazár belső REST keresési végpontjának URL-jét.
     */
    public String buildApiSearchUrl(SearchCriteria criteria, int page) {
        if (criteria == null) {
            criteria = new SearchCriteria();
        }
        StringBuilder sb = new StringBuilder(API_SEARCH_URL);
        sb.append("?property__1=1"); // Eladó

        String city = criteria.getCity();
        Optional<Integer> locId = IngatlanBazarLocationResolver.resolveLocationId(city);
        locId.ifPresent(id -> sb.append("&property_location=").append(id));

        String rawType = criteria.getPropertyType() != null ? criteria.getPropertyType().toLowerCase().trim() : "lakas";
        if ("haz".equals(rawType)) {
            sb.append("&property__2=2");
        } else if ("osszes".equals(rawType)) {
            sb.append("&property__2=3_2");
        } else if ("garazs".equals(rawType)) {
            sb.append("&property__2=4");
        } else if ("telek".equals(rawType)) {
            sb.append("&property__2=5");
        } else {
            sb.append("&property__2=3");
        }

        if (criteria.getMinPriceMillion() != null) {
            sb.append("&price_min=").append(criteria.getMinPriceMillion());
        }
        if (criteria.getMaxPriceMillion() != null) {
            sb.append("&price_max=").append(criteria.getMaxPriceMillion());
        }

        sb.append("&limit=50&page=").append(page);
        return sb.toString();
    }

    @Override
    public PortalScrapeResult scrape(SearchCriteria criteria) {
        long startTime = System.currentTimeMillis();
        List<IngatlanItem> items = new ArrayList<>();
        if (criteria == null) {
            criteria = new SearchCriteria();
        }

        String fallbackUrl = buildSearchUrl(criteria);
        System.out.println("Ingatlanbazár keresés: " + fallbackUrl);

        int maxPages = 2;
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        Set<String> processedUrls = new HashSet<>();

        try {
            for (int page = 1; page <= maxPages; page++) {
                String apiUrl = buildApiSearchUrl(criteria, page);
                String json = getJson(apiUrl);

                if (json == null || json.isBlank()) {
                    if (page == 1) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        return PortalScrapeResult.failure(getPortalId(), getName(), "Nem sikerült elérni az Ingatlanbazár REST API-t", elapsed);
                    }
                    break;
                }

                List<IngatlanItem> pageItems = parseItemsFromJson(json, criteria, processedUrls, now);
                items.addAll(pageItems);

                // Ha az oldalon kevesebb találat volt mint a limit (50), nincs több oldal
                if (pageItems.size() < 50) {
                    break;
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("ingatlanbazar.hu Találatok száma: " + items.size());

            if (items.isEmpty()) {
                return PortalScrapeResult.empty(getPortalId(), getName(), fallbackUrl, elapsed);
            }

            return PortalScrapeResult.success(getPortalId(), getName(), items, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            System.err.println("Hiba az Ingatlanbazár scraping során: " + e.getMessage());
            return PortalScrapeResult.failure(getPortalId(), getName(), e.getMessage(), elapsed);
        }
    }

    /**
     * Feldolgozza a REST API JSON válaszát IngatlanItem listává.
     */
    public List<IngatlanItem> parseItemsFromJson(String json, SearchCriteria criteria, Set<String> processedUrls, String now) {
        List<IngatlanItem> result = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return result;
        }

        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("hits") || !root.get("hits").isJsonArray()) {
                return result;
            }

            JsonArray hits = root.getAsJsonArray("hits");
            for (JsonElement el : hits) {
                if (!el.isJsonObject()) {
                    continue;
                }

                JsonObject hit = el.getAsJsonObject();
                String propertyId = hit.has("propertyId") && !hit.get("propertyId").isJsonNull()
                        ? hit.get("propertyId").getAsString() : UUID.randomUUID().toString();

                String seoUrl = hit.has("seoUrl") && !hit.get("seoUrl").isJsonNull()
                        ? hit.get("seoUrl").getAsString() : "";
                if (seoUrl.isBlank()) {
                    continue;
                }

                String fullUrl = seoUrl.startsWith("http") ? seoUrl : BASE_URL + (seoUrl.startsWith("/") ? "" : "/") + seoUrl;
                if (!processedUrls.add(fullUrl)) {
                    continue;
                }

                String title = hit.has("title") && !hit.get("title").isJsonNull()
                        ? hit.get("title").getAsString().trim() : "Ingatlanbazár hirdetés";

                Long priceHuf = null;
                if (hit.has("priceHUF") && !hit.get("priceHUF").isJsonNull()) {
                    try {
                        priceHuf = hit.get("priceHUF").getAsLong();
                    } catch (Exception ignored) {
                    }
                }

                PriceInfo priceInfo;
                if (priceHuf != null && priceHuf > 0) {
                    NumberFormat nf = NumberFormat.getInstance(new Locale("hu", "HU"));
                    String formatted = nf.format(priceHuf) + " Ft";
                    double million = Math.round((priceHuf / 1_000_000.0) * 100.0) / 100.0;
                    priceInfo = new PriceInfo(formatted, priceHuf, million);
                } else {
                    priceInfo = new PriceInfo("Ár a hirdetésben", null, null);
                }

                Double area = null;
                if (hit.has("area") && !hit.get("area").isJsonNull()) {
                    try {
                        area = hit.get("area").getAsDouble();
                    } catch (Exception ignored) {
                    }
                }

                Double rooms = null;
                if (hit.has("rooms") && !hit.get("rooms").isJsonNull()) {
                    try {
                        rooms = hit.get("rooms").getAsDouble();
                    } catch (Exception ignored) {
                    }
                }

                Long sqmPrice = null;
                if (hit.has("sqmPriceHuf") && !hit.get("sqmPriceHuf").isJsonNull()) {
                    try {
                        sqmPrice = hit.get("sqmPriceHuf").getAsLong();
                    } catch (Exception ignored) {
                    }
                } else if (priceHuf != null && area != null && area > 0) {
                    sqmPrice = Math.round(priceHuf / area);
                }

                StringBuilder detailsSb = new StringBuilder();
                if (area != null && area > 0) {
                    detailsSb.append(Math.round(area)).append(" m²");
                }
                if (rooms != null && rooms > 0) {
                    if (!detailsSb.isEmpty()) {
                        detailsSb.append(" • ");
                    }
                    detailsSb.append(Math.round(rooms)).append(" szoba");
                }

                PropertyDetails detailsInfo = new PropertyDetails(detailsSb.toString(), area, rooms, sqmPrice);

                String settlement = hit.has("settlement") && !hit.get("settlement").isJsonNull()
                        ? hit.get("settlement").getAsString().trim() : criteria.getCity();
                String quarter = hit.has("quarter") && !hit.get("quarter").isJsonNull()
                        ? hit.get("quarter").getAsString().trim() : "";

                String location = settlement;
                if (!quarter.isBlank() && !settlement.equalsIgnoreCase(quarter)) {
                    location = settlement + ", " + quarter;
                }

                String discoveredAt = hit.has("createdAt") && !hit.get("createdAt").isJsonNull()
                        ? hit.get("createdAt").getAsString().trim() : now;

                IngatlanItem item = new IngatlanItem(
                        propertyId,
                        SOURCE,
                        fullUrl,
                        title,
                        priceInfo,
                        settlement,
                        detailsInfo,
                        discoveredAt,
                        true
                );
                item.setLocation(location);

                result.add(item);
            }
        } catch (Exception e) {
            System.err.println("Hiba a JSON találatok feldolgozásakor: " + e.getMessage());
        }

        return result;
    }
}
