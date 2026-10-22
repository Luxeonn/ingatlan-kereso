package ingatlanok.portal.ingatlanbazar;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

import ingatlanok.common.AppLogger;
import ingatlanok.common.JsonSupport;
import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;

public class IngatlanBazarJsonParser {

  private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanBazarJsonParser.class);
  private static final PortalSource SOURCE = PortalSource.of("Ingatlanbazár");

  public List<IngatlanItem> parseItemsFromJson(
      String json, SearchCriteria criteria, Set<String> processedUrls, String now) {
    List<IngatlanItem> result = new ArrayList<>();
    if (json == null || json.isBlank()) {
      return result;
    }

    try {
      JsonNode root = JsonSupport.parse(json);
      if (!root.has("hits") || !root.get("hits").isArray()) {
        LOGGER.warn("Ingatlanbazár API válasz nem tartalmaz 'hits' tömböt");
        return result;
      }

      JsonNode hits = root.get("hits");
      for (JsonNode el : hits) {
        if (!el.isObject()) {
          continue;
        }

        JsonNode hit = el;
        String propertyId =
            hit.has("propertyId") && !hit.get("propertyId").isNull()
                ? hit.get("propertyId").asText()
                : UUID.randomUUID().toString();

        String seoUrl =
            hit.has("seoUrl") && !hit.get("seoUrl").isNull()
                ? hit.get("seoUrl").asText()
                : "";
        if (seoUrl.isBlank()) {
          continue;
        }

        String fullUrl =
            seoUrl.startsWith("http")
                ? seoUrl
                : IngatlanBazarQueryBuilder.BASE_URL + (seoUrl.startsWith("/") ? "" : "/") + seoUrl;
        if (processedUrls != null && !processedUrls.add(fullUrl)) {
          continue;
        }

        String title =
            hit.has("title") && !hit.get("title").isNull()
                ? hit.get("title").asText().trim()
                : "Ingatlanbazár hirdetés";

        Long priceHuf = null;
        if (hit.has("priceHUF") && !hit.get("priceHUF").isNull()) {
          try {
            priceHuf = hit.get("priceHUF").asLong();
          } catch (Exception ignored) {
            // Keep price unavailable when this optional API field is malformed.
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
        if (hit.has("area") && !hit.get("area").isNull()) {
          try {
            area = hit.get("area").asDouble();
          } catch (Exception ignored) {
            // Keep area unavailable when this optional API field is malformed.
          }
        }

        Double rooms = null;
        if (hit.has("rooms") && !hit.get("rooms").isNull()) {
          try {
            rooms = hit.get("rooms").asDouble();
          } catch (Exception ignored) {
            // Keep room count unavailable when this optional API field is malformed.
          }
        }

        Long sqmPrice = null;
        if (hit.has("sqmPriceHuf") && !hit.get("sqmPriceHuf").isNull()) {
          try {
            sqmPrice = hit.get("sqmPriceHuf").asLong();
          } catch (Exception ignored) {
            // Keep square-meter price unavailable when this optional API field is malformed.
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

        PropertyDetails detailsInfo =
            new PropertyDetails(detailsSb.toString(), area, rooms, sqmPrice);

        String defaultCity = criteria != null ? criteria.getCity() : "Debrecen";
        String settlement =
            hit.has("settlement") && !hit.get("settlement").isNull()
                ? hit.get("settlement").asText().trim()
                : defaultCity;
        String quarter =
            hit.has("quarter") && !hit.get("quarter").isNull()
                ? hit.get("quarter").asText().trim()
                : "";

        String location = settlement;
        if (!quarter.isBlank() && !settlement.equalsIgnoreCase(quarter)) {
          location = settlement + ", " + quarter;
        }

        String discoveredAt =
            hit.has("createdAt") && !hit.get("createdAt").isNull()
                ? hit.get("createdAt").asText().trim()
                : now;

        IngatlanItem item =
            new IngatlanItem(
                propertyId,
                SOURCE,
                fullUrl,
                title,
                priceInfo,
                settlement,
                detailsInfo,
                discoveredAt,
                true);
        item.setLocation(location);

        String imageUrl = null;
        if (hit.has("photo") && !hit.get("photo").isNull()) {
          JsonNode p = hit.get("photo");
          if (p.isValueNode()) {
            imageUrl = p.asText();
          } else if (p.isObject() && p.has("url")) {
            imageUrl = p.get("url").asText();
          }
        } else if (hit.has("picture") && !hit.get("picture").isNull()) {
          imageUrl = hit.get("picture").asText();
        } else if (hit.has("images") && hit.get("images").isArray()) {
          JsonNode imgs = hit.get("images");
          if (!imgs.isEmpty()) {
            JsonNode first = imgs.get(0);
            if (first.isValueNode()) {
              imageUrl = first.asText();
            } else if (first.isObject() && first.has("url")) {
              imageUrl = first.get("url").asText();
            }
          }
        }
        if (imageUrl != null && !imageUrl.isBlank()) {
          if (imageUrl.startsWith("//")) {
            imageUrl = "https:" + imageUrl;
          } else if (imageUrl.startsWith("/")) {
            imageUrl = IngatlanBazarQueryBuilder.BASE_URL + imageUrl;
          }
          item.setImageUrl(imageUrl);
        }

        boolean isNewBuild =
            (hit.has("isNewConstruction") && hit.get("isNewConstruction").asBoolean(false))
                || (hit.has("newConstruction") && hit.get("newConstruction").asBoolean(false))
                || (hit.has("condition")
                    && PropertyParser.isNewConstructionText(hit.get("condition").asText("")))
                || PropertyParser.isNewConstructionText(title, fullUrl);
        item.setNewConstruction(isNewBuild);

        if (criteria != null && !criteria.matchesRelevance(item)) {
          continue;
        }

        result.add(item);
      }
    } catch (Exception e) {
      LOGGER.error("Hiba a JSON találatok feldolgozásakor: {}", e.getMessage(), e);
    }

    return result;
  }
}
