package ingatlanok.portal.oc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import tools.jackson.databind.JsonNode;

import ingatlanok.common.JsonSupport;
import ingatlanok.common.PropertyParser;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.PortalSupport;

public class OtthonCentrumRawParser {

  private static final Pattern ID_PATTERN = Pattern.compile("/ingatlanok/([A-Za-z0-9]+)");

  public List<RawPropertyListing> parseRawListings(Document document) {
    List<RawPropertyListing> items = new ArrayList<>();
    if (document == null) {
      return items;
    }

    Elements cards = document.select(
        "div.js-list-item[data-controller='seo'], div.real-estate-card,"
            + " div[data-controller='seo'], div.js-list-item");
    if (cards.isEmpty()) {
      cards = document.select("a[href*=/ingatlanok/]");
    }

    for (Element card : cards) {
      try {
        RawPropertyListing raw = parseCard(card);
        if (raw != null) {
          items.add(raw);
        }
      } catch (Exception ignored) {
        // Keep fault-isolated so one bad card does not abort the batch.
      }
    }

    return items;
  }

  private RawPropertyListing parseCard(Element card) {
    String href = extractHref(card);
    if (href == null || href.isBlank() || !href.matches(".*/ingatlanok/[A-Za-z0-9]+$")) {
      return null;
    }

    String fullLink = PortalSupport.resolveUrl(OtthonCentrumUrlBuilder.BASE_URL, href);
    String id = PropertyParser.extractFirstGroup(fullLink, ID_PATTERN, null);
    if (id == null || "lista".equalsIgnoreCase(id) || "terkep".equalsIgnoreCase(id)) {
      return null;
    }
    String cardText = card.text().replaceAll("\\s+", " ").trim();

    String seoAttr = extractSeoAttr(card);
    SeoMetadata seo = parseSeoMetadata(seoAttr);
    String title = resolveTitle(card, cardText, seo.locationCity(), id);
    String imageUrl = PortalSupport.extractImageWithFallback(card, OtthonCentrumUrlBuilder.BASE_URL);

    return RawPropertyListing.builder()
        .portalId("oc")
        .nativeId(id)
        .url(fullLink)
        .title(title)
        .priceHuf(seo.priceHuf())
        .priceText(cardText)
        .detailsText(cardText)
        .locationText(seo.locationCity())
        .imageUrl(imageUrl)
        .descriptionText(cardText + " " + seoAttr)
        .method(ExtractionMethod.HTML)
        .build();
  }

  private String extractHref(Element card) {
    String href = card.is("a") ? card.attr("href") : "";
    if (href.isBlank()) {
      Element a = card.selectFirst("a[href*=/ingatlanok/]");
      if (a != null) {
        href = a.attr("href");
      }
    }
    return href;
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
      return new SeoMetadata(null, null);
    }
    try {
      JsonNode seoObj = JsonSupport.parse(seoAttr);
      JsonNode items = seoObj.path("ecommerce").path("items");
      if (!items.isArray() || items.isEmpty()) {
        return new SeoMetadata(null, null);
      }
      JsonNode first = items.get(0);
      String city = first.has("location_city") ? first.get("location_city").asString() : null;
      Long price = first.has("price") && !first.get("price").isNull()
          ? first.get("price").asLong()
          : null;
      if (price != null && price <= 0) {
        price = null;
      }
      return new SeoMetadata(city, price);
    } catch (Exception ignored) {
      return new SeoMetadata(null, null);
    }
  }

  private String resolveTitle(Element card, String cardText, String locationCity, String id) {
    Element headingEl = card.selectFirst("div.collapse.description h6, .description h6, h6, h3");
    if (headingEl != null && !headingEl.text().isBlank()) {
      return headingEl.text().trim();
    }
    return cleanTitle(cardText, locationCity, id);
  }

  private record SeoMetadata(String locationCity, Long priceHuf) {}

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
