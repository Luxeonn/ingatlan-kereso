package ingatlanok.portal.ingatlanbazar;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ingatlanok.common.AppLogger;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;

import java.text.NumberFormat;
import java.util.*;

/**
 * Ingatlanbazár JSON REST API válasz feldolgozó komponens.
 */
public class IngatlanBazarJsonParser {

    private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanBazarJsonParser.class);
    private static final PortalSource SOURCE = PortalSource.of("Ingatlanbazár");

    public List<IngatlanItem> parseItemsFromJson(String json, SearchCriteria criteria, Set<String> processedUrls, String now) {
        List<IngatlanItem> result = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return result;
        }

        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("hits") || !root.get("hits").isJsonArray()) {
                LOGGER.warn("Ingatlanbazár API válasz nem tartalmaz 'hits' tömböt");
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

                String fullUrl = seoUrl.startsWith("http") ? seoUrl : IngatlanBazarQueryBuilder.BASE_URL + (seoUrl.startsWith("/") ? "" : "/") + seoUrl;
                if (processedUrls != null && !processedUrls.add(fullUrl)) {
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

                String defaultCity = criteria != null ? criteria.getCity() : "Debrecen";
                String settlement = hit.has("settlement") && !hit.get("settlement").isJsonNull()
                        ? hit.get("settlement").getAsString().trim() : defaultCity;
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

                String imageUrl = null;
                if (hit.has("photo") && !hit.get("photo").isJsonNull()) {
                    JsonElement p = hit.get("photo");
                    if (p.isJsonPrimitive()) {
                        imageUrl = p.getAsString();
                    } else if (p.isJsonObject() && p.getAsJsonObject().has("url")) {
                        imageUrl = p.getAsJsonObject().get("url").getAsString();
                    }
                } else if (hit.has("picture") && !hit.get("picture").isJsonNull()) {
                    imageUrl = hit.get("picture").getAsString();
                } else if (hit.has("images") && hit.get("images").isJsonArray()) {
                    JsonArray imgs = hit.getAsJsonArray("images");
                    if (!imgs.isEmpty()) {
                        JsonElement first = imgs.get(0);
                        if (first.isJsonPrimitive()) {
                            imageUrl = first.getAsString();
                        } else if (first.isJsonObject() && first.getAsJsonObject().has("url")) {
                            imageUrl = first.getAsJsonObject().get("url").getAsString();
                        }
                    }
                }
                if (imageUrl != null && !imageUrl.isBlank()) {
                    if (imageUrl.startsWith("//")) {
                        imageUrl = "https:" + imageUrl;
                    }
                    item.setImageUrl(imageUrl);
                }

                result.add(item);
            }
        } catch (Exception e) {
            LOGGER.error("Hiba a JSON találatok feldolgozásakor: {}", e.getMessage(), e);
        }

        return result;
    }
}
