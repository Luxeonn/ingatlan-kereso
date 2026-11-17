package ingatlanok.portal.oc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

public class OtthonCentrumRawParser implements HtmlListingParser {

  private static final Pattern ID_PATTERN =
      Pattern.compile("/(?:ingatlanok|uj-lakas)/([A-Za-z0-9]+)(?:\\?|$|/|#)");

  public String extractChunkFilterJson(Document document) {
    if (document == null) {
      return null;
    }
    Element container = document.selectFirst("[data-oc-filter]");
    if (container == null) {
      return null;
    }
    String json = container.attr("data-oc-filter");
    return json != null && !json.isBlank() ? json.trim() : null;
  }

  @Override
  public List<RawPropertyListing> parseRawListings(Document document) {
    List<RawPropertyListing> items = new ArrayList<>();
    if (document == null) {
      return items;
    }

    Elements cards = selectCardElements(document);
    Set<String> seenUrls = new HashSet<>();

    for (Element card : cards) {
      try {
        RawPropertyListing raw = parseCard(card);
        if (raw != null && seenUrls.add(raw.url())) {
          items.add(raw);
        }
      } catch (Exception ignored) {
        // Keep fault-isolated so one bad card does not abort the batch.
      }
    }

    return items;
  }

  private Elements selectCardElements(Document document) {
    Elements cards = document.select(
        "div.js-list-item[data-controller='seo'], div.real-estate-card,"
            + " div[data-controller='seo'], div.js-list-item");
    if (cards.isEmpty()) {
      cards = document.select("a[href*=/ingatlanok/], a[href*=/uj-lakas/]");
    }
    return cards;
  }

  private RawPropertyListing parseCard(Element card) {
    String href = extractHref(card);
    if (href == null || href.isBlank()) {
      return null;
    }

    String fullLink = PortalSupport.canonicalListingUrl(
        PortalSupport.resolveUrl(OtthonCentrumUrlBuilder.BASE_URL, href));
    if (!fullLink.matches(".*/(?:ingatlanok|uj-lakas)/[A-Za-z0-9]+$")) {
      return null;
    }
    String id = PropertyParser.extractFirstGroup(fullLink, ID_PATTERN, null);
    if (id == null || "lista".equalsIgnoreCase(id) || "terkep".equalsIgnoreCase(id)) {
      return null;
    }
    String cardText = card.text().replaceAll("\\s+", " ").trim();
    String footerText = extractFooterText(card);
    String structuredText = footerText != null ? footerText : cardText;

    String seoAttr = extractSeoAttr(card);
    SeoMetadata seo = parseSeoMetadata(seoAttr);
    String locationText = resolveLocationText(seo, card);
    String title = resolveTitle(card, cardText, seo.locationCity(), id);
    String imageUrl = PortalSupport.extractImageWithFallback(card, OtthonCentrumUrlBuilder.BASE_URL);

    return RawPropertyListing.builder()
        .portalId("oc")
        .nativeId(id)
        .url(fullLink)
        .title(title)
        .priceHuf(seo.priceHuf())
        .priceText(structuredText)
        .detailsText(structuredText)
        .locationText(locationText)
        .imageUrl(imageUrl)
        .descriptionText(cardText + " " + seoAttr)
        .method(ExtractionMethod.HTML)
        .build();
  }

  private String extractHref(Element card) {
    String href = card.is("a") ? card.attr("href") : "";
    if (href.isBlank()) {
      Element a = card.selectFirst("a[href*=/ingatlanok/], a[href*=/uj-lakas/]");
      if (a != null) {
        href = a.attr("href");
      }
    }
    return href;
  }

  private String extractFooterText(Element card) {
    Elements rows = card.select(".card-footer .info-row");
    if (rows.isEmpty()) {
      return null;
    }
    String joined = rows.text().replaceAll("\\s+", " ").trim();
    return joined.isBlank() ? null : joined;
  }

  private String resolveLocationText(SeoMetadata seo, Element card) {
    Elements leftRows = card.select(".card-footer .info-row .text-left");
    String city = firstNonBlank(seo.locationCity(), rowText(leftRows, 0));
    String fallbackDistrict = leftRows.size() >= 3 ? rowText(leftRows, 1) : null;
    String district = firstNonBlank(seo.locationDistrict(), fallbackDistrict);
    if (city == null) {
      return null;
    }
    if (district != null && !district.equalsIgnoreCase(city)) {
      return city + ", " + district;
    }
    return city;
  }

  private String rowText(Elements rows, int index) {
    if (rows == null || index < 0 || index >= rows.size()) {
      return null;
    }
    String text = rows.get(index).text().trim();
    return text.isBlank() ? null : text;
  }

  private String firstNonBlank(String primary, String secondary) {
    if (primary != null && !primary.isBlank()) {
      return primary.trim();
    }
    return secondary != null && !secondary.isBlank() ? secondary.trim() : null;
  }

  private String extractSeoAttr(Element card) {
    String seoAttr = card.attr("data-seo-select-item-value");
    if (seoAttr.isBlank()) {
      Element seoHolder = card.selectFirst("[data-seo-select-item-value]");
      if (seoHolder != null) {
        return seoHolder.attr("data-seo-select-item-value");
      }
    }
    return seoAttr;
  }

  private SeoMetadata parseSeoMetadata(String seoAttr) {
    if (seoAttr == null || seoAttr.isBlank()) {
      return new SeoMetadata(null, null, null);
    }
    try {
      JsonNode seoObj = JsonSupport.parse(seoAttr);
      JsonNode items = seoObj.path("ecommerce").path("items");
      if (!items.isArray() || items.isEmpty()) {
        return new SeoMetadata(null, null, null);
      }
      JsonNode first = items.get(0);
      String city = readTextField(first, "location_city");
      String district = readTextField(first, "location_district");
      Long price = first.has("price") && !first.get("price").isNull()
          ? first.get("price").asLong()
          : null;
      if (price != null && price <= 0) {
        price = null;
      }
      return new SeoMetadata(city, district, price);
    } catch (Exception ignored) {
      return new SeoMetadata(null, null, null);
    }
  }

  private String readTextField(JsonNode node, String fieldName) {
    if (!node.has(fieldName) || node.get(fieldName).isNull()) {
      return null;
    }
    String value = node.get(fieldName).asString().trim();
    return value.isBlank() ? null : value;
  }

  private String resolveTitle(Element card, String cardText, String locationCity, String id) {
    Element headingEl = card.selectFirst("div.collapse.description h6, .description h6, h6, h3");
    if (headingEl != null && !headingEl.text().isBlank()) {
      return headingEl.text().trim();
    }
    return cleanTitle(cardText, locationCity, id);
  }

  private record SeoMetadata(String locationCity, String locationDistrict, Long priceHuf) {}

  public String cleanTitle(String text, String defaultCity, String id) {
    if (text != null && text.contains("szoba")) {
      int idx = text.indexOf("szoba");
      String after = text.substring(idx + 5).trim();
      if (after.length() > 5) {
        int end = Math.min(after.length(), 65);
        return after.substring(0, end).trim() + "...";
      }
    }
    String city = defaultCity != null && !defaultCity.isBlank() ? defaultCity : "Eladó";
    return city + " - Eladó ingatlan" + (id != null ? " (" + id + ")" : "");
  }
}
