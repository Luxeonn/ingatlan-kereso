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
  static final String THUMBNAIL_SUFFIX = "_485x354.jpg";

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

        String imageUrl = extractImageUrl(hit);

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

  static String extractImageUrl(JsonNode hit) {
    if (hit == null || !hit.isObject()) {
      return null;
    }

    String candidate = null;
    if (hit.has("renderPictures") && hit.get("renderPictures").isArray()) {
      JsonNode renderPics = hit.get("renderPictures");
      for (JsonNode pic : renderPics) {
        if (pic.isValueNode() && !pic.asText().isBlank()) {
          candidate = pic.asText().trim();
          break;
        }
      }
    }

    if (candidate == null && hit.has("photo") && !hit.get("photo").isNull()) {
      JsonNode p = hit.get("photo");
      if (p.isValueNode()) {
        candidate = p.asText().trim();
      } else if (p.isObject() && p.has("url")) {
        candidate = p.get("url").asText().trim();
      }
    }

    if (candidate == null && hit.has("picture") && !hit.get("picture").isNull()) {
      candidate = hit.get("picture").asText().trim();
    }

    if (candidate == null && hit.has("images") && hit.get("images").isArray()) {
      JsonNode imgs = hit.get("images");
      for (JsonNode img : imgs) {
        if (img.isValueNode() && !img.asText().isBlank()) {
          candidate = img.asText().trim();
          break;
        } else if (img.isObject() && img.has("url") && !img.get("url").asText().isBlank()) {
          candidate = img.get("url").asText().trim();
          break;
        }
      }
    }

    if (candidate == null || candidate.isBlank()) {
      return null;
    }

    if (candidate.startsWith("//")) {
      candidate = "https:" + candidate;
    } else if (candidate.startsWith("/")) {
      candidate = IngatlanBazarQueryBuilder.BASE_URL + candidate;
    }

    if (candidate.contains("images2.ingatlanbazar.hu/static/property/images/")) {
      int lastSlash = candidate.lastIndexOf('/');
      String lastSegment = lastSlash >= 0 ? candidate.substring(lastSlash + 1) : candidate;
      if (!lastSegment.contains(".")) {
        candidate = candidate + THUMBNAIL_SUFFIX;
      }
    }

    return candidate;
  }
}
