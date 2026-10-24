package ingatlanok.portal.ingatlanbazar;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;

import ingatlanok.common.AppLogger;
import ingatlanok.common.JsonSupport;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawPropertyListing;

public class IngatlanBazarRawParser {

  private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanBazarRawParser.class);

  public List<RawPropertyListing> parseRawListings(String json) {
    List<RawPropertyListing> result = new ArrayList<>();
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
        String propertyId = hit.has("propertyId") && !hit.get("propertyId").isNull()
            ? hit.get("propertyId").asText().trim()
            : null;

        String seoUrl = hit.has("seoUrl") && !hit.get("seoUrl").isNull()
            ? hit.get("seoUrl").asText().trim()
            : "";
        if (seoUrl.isBlank()) {
          continue;
        }

        String fullUrl = seoUrl.startsWith("http")
            ? seoUrl
            : IngatlanBazarQueryBuilder.BASE_URL + (seoUrl.startsWith("/") ? "" : "/") + seoUrl;

        String title = hit.has("title") && !hit.get("title").isNull()
            ? hit.get("title").asText().trim()
            : "Ingatlanbazár hirdetés";

        Long priceHuf = null;
        if (hit.has("priceHUF") && !hit.get("priceHUF").isNull()) {
          try {
            priceHuf = hit.get("priceHUF").asLong();
          } catch (Exception ignored) {
            // Keep price unavailable when malformed
          }
        }

        Double area = null;
        if (hit.has("area") && !hit.get("area").isNull()) {
          try {
            area = hit.get("area").asDouble();
          } catch (Exception ignored) {
            // Keep area unavailable when malformed
          }
        }

        Double rooms = null;
        if (hit.has("rooms") && !hit.get("rooms").isNull()) {
          try {
            rooms = hit.get("rooms").asDouble();
          } catch (Exception ignored) {
            // Keep rooms unavailable when malformed
          }
        }

        String settlement = hit.has("settlement") && !hit.get("settlement").isNull()
            ? hit.get("settlement").asText().trim()
            : "";
        String quarter = hit.has("quarter") && !hit.get("quarter").isNull()
            ? hit.get("quarter").asText().trim()
            : "";

        String location = settlement;
        if (!quarter.isBlank() && !settlement.equalsIgnoreCase(quarter)) {
          location = settlement.isBlank() ? quarter : settlement + ", " + quarter;
        }

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
        }

        String descriptionText = "";
        if (hit.has("condition") && !hit.get("condition").isNull()) {
          descriptionText = hit.get("condition").asText();
        }
        boolean isNew = (hit.has("isNewConstruction") && hit.get("isNewConstruction").asBoolean(false))
            || (hit.has("newConstruction") && hit.get("newConstruction").asBoolean(false));
        if (isNew) {
          descriptionText += " új építésű";
        }

        RawPropertyListing rawListing = RawPropertyListing.builder()
            .portalId("ingatlanbazar")
            .nativeId(propertyId)
            .url(fullUrl)
            .title(title)
            .priceHuf(priceHuf)
            .areaSquareMeters(area)
            .roomCount(rooms)
            .locationText(location)
            .imageUrl(imageUrl)
            .descriptionText(descriptionText.trim())
            .method(ExtractionMethod.API)
            .build();

        result.add(rawListing);
      }
    } catch (Exception e) {
      LOGGER.error("Hiba a nyers Ingatlanbazár JSON találatok feldolgozásakor: {}", e.getMessage(), e);
    }

    return result;
  }
}
