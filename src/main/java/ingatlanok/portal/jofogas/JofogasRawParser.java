package ingatlanok.portal.jofogas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import tools.jackson.databind.JsonNode;

import ingatlanok.common.JsonSupport;
import ingatlanok.common.PropertyParser;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.HtmlListingParser;
import ingatlanok.portal.PortalSupport;

public class JofogasRawParser implements HtmlListingParser {

  private static final Pattern ID_PATTERN = Pattern.compile("_(\\d+)\\.htm");

  private record NextDataAd(
      String subject, Long priceHuf, String detailsText, String locationText) {}

  @Override
  public List<RawPropertyListing> parseRawListings(Document document) {
    List<RawPropertyListing> items = new ArrayList<>();
    if (document == null) {
      return items;
    }

    Map<String, NextDataAd> nextDataAds = extractNextDataAds(document);
    Elements links = document.select("a[href*=/ingatlan/], a[href*=_]");
    for (Element a : links) {
      try {
        RawPropertyListing raw = parseSingleLink(a, nextDataAds);
        if (raw != null) {
          items.add(raw);
        }
      } catch (Exception ignored) {
        // Keep fault-isolated so one bad link does not abort the batch.
      }
    }

    return items;
  }

  private RawPropertyListing parseSingleLink(Element a, Map<String, NextDataAd> nextDataAds) {
    String href = a.attr("href");
    if (href == null || href.isBlank()) {
      return null;
    }

    String fullLink = PortalSupport.canonicalListingUrl(
        PortalSupport.resolveUrl(JofogasUrlBuilder.BASE_URL, href));
    if (!fullLink.endsWith(".htm")) {
      return null;
    }
    String id = PropertyParser.extractFirstGroup(fullLink, ID_PATTERN, null);
    if (id == null || id.isBlank()) {
      return null;
    }

    String cardText = a.text().replaceAll("\\s+", " ").trim();
    if (cardText.length() < 10) {
      return null;
    }

    NextDataAd metadata = nextDataAds.get(id);
    String title = resolveHeadingTitle(a, cardText, id, metadata);
    String detailsText = resolveDetailsText(a, cardText, metadata);
    Long priceHuf = metadata != null ? metadata.priceHuf() : null;
    String locationText = metadata != null ? metadata.locationText() : null;
    String imageUrl = PortalSupport.extractImageWithFallback(a, JofogasUrlBuilder.BASE_URL);

    return RawPropertyListing.builder()
        .portalId("jofogas")
        .nativeId(id)
        .url(fullLink)
        .title(title)
        .priceHuf(priceHuf)
        .priceText(cardText)
        .detailsText(detailsText)
        .locationText(locationText)
        .imageUrl(imageUrl)
        .descriptionText(cardText)
        .method(ExtractionMethod.HTML)
        .build();
  }

  private String resolveHeadingTitle(
      Element a, String cardText, String id, NextDataAd metadata) {
    if (metadata != null && metadata.subject() != null && !metadata.subject().isBlank()) {
      return metadata.subject();
    }
    Element heading = a.selectFirst("h1, h2, h3, h4, h5");
    if (heading != null && !heading.text().isBlank()) {
      return heading.text().trim();
    }
    return cleanTitle(cardText, id);
  }

  private String resolveDetailsText(Element a, String cardText, NextDataAd metadata) {
    if (metadata != null && metadata.detailsText() != null && !metadata.detailsText().isBlank()) {
      return metadata.detailsText();
    }
    Element cloned = a.clone();
    cloned.select("h1, h2, h3, h4, h5, .subject, .title, .item-title").remove();
    String bodyText = cloned.text().replaceAll("\\s+", " ").trim();
    if (!bodyText.isBlank() && hasAreaOrRoomToken(bodyText)) {
      return bodyText;
    }
    return cardText;
  }

  private boolean hasAreaOrRoomToken(String text) {
    String lower = text.toLowerCase();
    return lower.contains("m²") || lower.contains("m2") || lower.contains("nm")
        || lower.contains("szoba");
  }

  private Map<String, NextDataAd> extractNextDataAds(Document document) {
    Map<String, NextDataAd> result = new HashMap<>();
    Element script = document.selectFirst("script#__NEXT_DATA__");
    if (script == null) {
      return result;
    }
    String json = script.data();
    if (json == null || json.isBlank()) {
      return result;
    }
    try {
      JsonNode root = JsonSupport.parse(json);
      JsonNode ads = root.path("props").path("pageProps").path("adList").path("ads");
      if (!ads.isArray()) {
        return result;
      }
      for (JsonNode ad : ads) {
        collectNextDataAd(ad, result);
      }
    } catch (Exception ignored) {
      // Fall back cleanly to DOM-only extraction when __NEXT_DATA__ is malformed.
    }
    return result;
  }

  private void collectNextDataAd(JsonNode ad, Map<String, NextDataAd> result) {
    String listId = ad.has("list_id") && !ad.get("list_id").isNull()
        ? ad.get("list_id").asString().trim()
        : null;
    if (listId == null || listId.isBlank()) {
      return;
    }
    String subject = ad.has("subject") && !ad.get("subject").isNull()
        ? ad.get("subject").asString().trim()
        : null;
    JsonNode priceNode = ad.path("price").path("value");
    Long priceHuf = priceNode.isNumber() && priceNode.asLong() > 0 ? priceNode.asLong() : null;

    String sizeLabel = extractParamLabel(ad, "size");
    String roomsLabel = extractParamLabel(ad, "rooms");
    String details = combineNonBlank(roomsLabel, sizeLabel, " • ");

    String city = extractParamLabel(ad, "city");
    String area = extractParamLabel(ad, "jfg_area");
    String locationText = buildLocationText(city, area);

    result.put(listId, new NextDataAd(subject, priceHuf, details, locationText));
  }

  private String extractParamLabel(JsonNode ad, String key) {
    JsonNode params = ad.path("parameters");
    if (!params.isArray()) {
      return null;
    }
    for (JsonNode param : params) {
      if (key.equals(param.path("key").asString())) {
        JsonNode values = param.path("values");
        if (values.isArray() && !values.isEmpty()) {
          String label = values.get(0).path("label").asString("").trim();
          return label.isBlank() ? null : label;
        }
      }
    }
    return null;
  }

  private String buildLocationText(String city, String area) {
    if (city == null || city.isBlank()) {
      return area;
    }
    if (area == null || area.isBlank() || area.equalsIgnoreCase(city)) {
      return city;
    }
    return city + ", " + area;
  }

  private String combineNonBlank(String first, String second, String separator) {
    if (first != null && !first.isBlank() && second != null && !second.isBlank()) {
      return first + separator + second;
    }
    return first != null && !first.isBlank() ? first : second;
  }

  public String cleanTitle(String text, String id) {
    if (text != null) {
      String t = text.replaceAll("\\d+\\s*(?:M\\s*Ft|millió|Ft)", "").trim();
      t = t.replaceAll("\\d+\\s*m2", "").trim();
      t = t.replaceAll("\\d+\\s*szoba", "").trim();
      if (t.length() > 5) {
        return t.length() > 60 ? t.substring(0, 60).trim() + "..." : t;
      }
    }
    return "Jófogás ingatlan" + (id != null ? " (" + id + ")" : "");
  }
}
