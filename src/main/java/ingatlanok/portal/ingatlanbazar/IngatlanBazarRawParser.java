package ingatlanok.portal.ingatlanbazar;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;

import ingatlanok.common.AppLogger;
import ingatlanok.common.JsonSupport;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.PortalSupport;

public class IngatlanBazarRawParser {

  static final String THUMBNAIL_SUFFIX = "_485x354.jpg";
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

      for (JsonNode el : root.get("hits")) {
        if (el.isObject()) {
          try {
            RawPropertyListing rawListing = parseSingleHit(el);
            if (rawListing != null) {
              result.add(rawListing);
            }
          } catch (Exception ignored) {
            // Keep fault-isolated so one bad hit does not abort the batch.
          }
        }
      }
    } catch (Exception e) {
      LOGGER.error("Hiba a nyers Ingatlanbazár JSON találatok feldolgozásakor: {}", e.getMessage(), e);
    }

    return result;
  }

  private RawPropertyListing parseSingleHit(JsonNode hit) {
    String seoUrl = hit.has("seoUrl") && !hit.get("seoUrl").isNull()
        ? hit.get("seoUrl").asString().trim()
        : "";
    if (seoUrl.isBlank()) {
      return null;
    }

    String propertyId = hit.has("propertyId") && !hit.get("propertyId").isNull()
        ? hit.get("propertyId").asString().trim()
        : null;

    String rawUrl = seoUrl.startsWith("http")
        ? seoUrl
        : IngatlanBazarQueryBuilder.BASE_URL + (seoUrl.startsWith("/") ? "" : "/") + seoUrl;
    String fullUrl = PortalSupport.canonicalListingUrl(rawUrl);

    String title = hit.has("title") && !hit.get("title").isNull()
        ? hit.get("title").asString().trim()
        : "Ingatlanbazár hirdetés";

    return RawPropertyListing.builder()
        .portalId("ingatlanbazar")
        .nativeId(propertyId)
        .url(fullUrl)
        .title(title)
        .priceHuf(extractLong(hit, "priceHUF"))
        .areaSquareMeters(extractDouble(hit, "area"))
        .roomCount(extractDouble(hit, "rooms"))
        .locationText(resolveLocation(hit))
        .imageUrl(extractImageUrl(hit))
        .descriptionText(resolveDescription(hit))
        .method(ExtractionMethod.API)
        .build();
  }

  private Long extractLong(JsonNode hit, String field) {
    if (hit.has(field) && !hit.get(field).isNull()) {
      try {
        long val = hit.get(field).asLong();
        return val > 0 ? val : null;
      } catch (Exception ignored) {
        // Keep unavailable when malformed
      }
    }
    return null;
  }

  private Double extractDouble(JsonNode hit, String field) {
    if (hit.has(field) && !hit.get(field).isNull()) {
      try {
        double val = hit.get(field).asDouble();
        return val > 0 ? val : null;
      } catch (Exception ignored) {
        // Keep unavailable when malformed
      }
    }
    return null;
  }

  private String resolveLocation(JsonNode hit) {
    String settlement = hit.has("settlement") && !hit.get("settlement").isNull()
        ? hit.get("settlement").asString().trim()
        : "";
    String quarter = hit.has("quarter") && !hit.get("quarter").isNull()
        ? hit.get("quarter").asString().trim()
        : "";
    if (!quarter.isBlank() && !settlement.equalsIgnoreCase(quarter)) {
      return settlement.isBlank() ? quarter : settlement + ", " + quarter;
    }
    return settlement;
  }

  private String resolveDescription(JsonNode hit) {
    String descriptionText = "";
    if (hit.has("condition") && !hit.get("condition").isNull()) {
      descriptionText = hit.get("condition").asString();
    }
    boolean isNew = (hit.has("isNewConstruction") && hit.get("isNewConstruction").asBoolean(false))
        || (hit.has("newConstruction") && hit.get("newConstruction").asBoolean(false));
    if (isNew) {
      descriptionText += " új építésű";
    }
    return descriptionText.trim();
  }

  static String extractImageUrl(JsonNode hit) {
    if (hit == null || !hit.isObject()) {
      return null;
    }
    String candidate = findFirstImageCandidate(hit);
    if (candidate == null || candidate.isBlank()) {
      return null;
    }
    return normalizeCandidateUrl(candidate);
  }

  private static String findFirstImageCandidate(JsonNode hit) {
    String candidate = findInRenderPictures(hit);
    if (candidate != null) {
      return candidate;
    }
    candidate = findInPhoto(hit);
    if (candidate != null) {
      return candidate;
    }
    candidate = findInPicture(hit);
    if (candidate != null) {
      return candidate;
    }
    return findInImages(hit);
  }

  private static String findInRenderPictures(JsonNode hit) {
    if (hit.has("renderPictures") && hit.get("renderPictures").isArray()) {
      for (JsonNode pic : hit.get("renderPictures")) {
        if (pic.isValueNode() && !pic.asString("").isBlank()) {
          return pic.asString("").trim();
        }
      }
    }
    return null;
  }

  private static String findInPhoto(JsonNode hit) {
    if (hit.has("photo") && !hit.get("photo").isNull()) {
      JsonNode p = hit.get("photo");
      if (p.isValueNode()) {
        return p.asString("").trim();
      }
      if (p.isObject() && p.has("url")) {
        return p.get("url").asString("").trim();
      }
    }
    return null;
  }

  private static String findInPicture(JsonNode hit) {
    if (hit.has("picture") && !hit.get("picture").isNull()) {
      return hit.get("picture").asString("").trim();
    }
    return null;
  }

  private static String findInImages(JsonNode hit) {
    if (hit.has("images") && hit.get("images").isArray()) {
      for (JsonNode img : hit.get("images")) {
        if (img.isValueNode() && !img.asString("").isBlank()) {
          return img.asString("").trim();
        }
        if (img.isObject() && img.has("url") && !img.get("url").asString("").isBlank()) {
          return img.get("url").asString("").trim();
        }
      }
    }
    return null;
  }

  private static String normalizeCandidateUrl(String candidate) {
    String url = candidate;
    if (url.startsWith("//")) {
      url = "https:" + url;
    } else if (url.startsWith("/")) {
      url = IngatlanBazarQueryBuilder.BASE_URL + url;
    }

    if (url.contains("images2.ingatlanbazar.hu/static/property/images/")) {
      int lastSlash = url.lastIndexOf('/');
      String lastSegment = lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
      if (!lastSegment.contains(".")) {
        url = url + THUMBNAIL_SUFFIX;
      }
    }
    return url;
  }
}
