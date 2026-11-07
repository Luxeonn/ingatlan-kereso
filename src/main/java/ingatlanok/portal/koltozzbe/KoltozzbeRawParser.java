package ingatlanok.portal.koltozzbe;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import tools.jackson.databind.JsonNode;

import ingatlanok.common.AppLogger;
import ingatlanok.common.JsonSupport;
import ingatlanok.common.PropertyParser;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.PortalSupport;

public class KoltozzbeRawParser {

  private static final AppLogger LOGGER = AppLogger.getLogger(KoltozzbeRawParser.class);
  private static final Pattern ID_PATTERN = Pattern.compile("(?i)/(?:elado-[^/]+/)?h?([0-9]+)");
  private static final Pattern PAGE_PARAM_PATTERN = Pattern.compile("[?&]page=(\\d+)");

  public List<RawPropertyListing> parseHtmlListings(Document doc) {
    List<RawPropertyListing> items = new ArrayList<>();
    if (doc == null) {
      return items;
    }

    Elements cards = doc.select(
        ".listing-card, .property-box, .property-card, .listing-item,"
            + " article[data-id], div[data-lid], div[data-id]");
    if (cards.isEmpty()) {
      cards = doc.select("a.listing-item-link, a[href*=/elado-]");
    }

    for (Element card : cards) {
      try {
        Element linkElem = card.is("a")
            ? card
            : card.selectFirst("a.listing-item-link, a[href*=/elado-], a[href*=/h], a[href*=/ingatlan]");
        if (linkElem == null) {
          continue;
        }

        String href = linkElem.attr("href");
        if (href == null || href.isBlank()) {
          continue;
        }

        String fullUrl = PortalSupport.resolveUrl(KoltozzbeUrlBuilder.BASE_URL, href);

        String lid = card.attr("data-lid");
        if (lid.isBlank()) {
          lid = card.attr("data-id");
        }
        if (lid.isBlank()) {
          lid = PropertyParser.extractFirstGroup(fullUrl, ID_PATTERN, null);
        }

        String title = extractTitle(card, linkElem);
        String priceText = extractPriceText(card);
        String detailsText = extractDetailsText(card);
        String imageUrl = PortalSupport.extractImageWithFallback(card, KoltozzbeUrlBuilder.BASE_URL);

        RawPropertyListing raw = RawPropertyListing.builder()
            .portalId("koltozzbe")
            .nativeId(lid)
            .url(fullUrl)
            .title(title)
            .priceText(priceText)
            .detailsText(detailsText)
            .imageUrl(imageUrl)
            .descriptionText(card.text())
            .method(ExtractionMethod.HTML)
            .build();

        items.add(raw);
      } catch (Exception ignored) {
        // Keep fault-isolated so one bad card does not abort the batch.
      }
    }

    return items;
  }

  public List<RawPropertyListing> parseJsonLdListings(Document doc) {
    List<RawPropertyListing> items = new ArrayList<>();
    if (doc == null) {
      return items;
    }

    Elements scripts = doc.select("script[type=application/ld+json]");
    for (Element script : scripts) {
      processJsonLdScript(script, items);
    }

    return items;
  }

  private void processJsonLdScript(Element script, List<RawPropertyListing> items) {
    String jsonText = script.data();
    if (jsonText == null
        || (!jsonText.contains("ListItem") && !jsonText.contains("itemListElement"))) {
      return;
    }
    try {
      JsonNode root = JsonSupport.parse(jsonText);
      if (!root.isObject() || !root.has("itemListElement")) {
        return;
      }
      for (JsonNode el : root.get("itemListElement")) {
        try {
          RawPropertyListing raw = parseJsonLdElement(el);
          if (raw != null) {
            items.add(raw);
          }
        } catch (Exception ignored) {
          // Keep fault-isolated so one bad element does not abort the batch.
        }
      }
    } catch (Exception ex) {
      LOGGER.debug("JSON-LD parse warning: {}", ex.getMessage());
    }
  }

  private RawPropertyListing parseJsonLdElement(JsonNode el) {
    if (!el.isObject()) {
      return null;
    }
    JsonNode product = el.has("item") ? el.get("item") : el;
    String rawUrl = product.has("url") ? product.get("url").asString() : null;
    String url = PortalSupport.resolveUrl(KoltozzbeUrlBuilder.BASE_URL, rawUrl);
    if (url.isEmpty()) {
      return null;
    }

    String name = product.has("name") ? product.get("name").asString() : "";
    String description = product.has("description") ? product.get("description").asString("") : "";
    String id = PropertyParser.extractFirstGroup(url, ID_PATTERN, null);

    long rawPrice = extractJsonLdPrice(product);
    String imageUrl = extractJsonLdImage(product);

    return RawPropertyListing.builder()
        .portalId("koltozzbe")
        .nativeId(id)
        .url(url)
        .title(name)
        .priceHuf(rawPrice > 0 ? rawPrice : null)
        .priceText(rawPrice > 0 ? null : name + " " + description)
        .detailsText(name + " " + description)
        .imageUrl(imageUrl)
        .descriptionText(description)
        .method(ExtractionMethod.JSON_LD)
        .build();
  }

  private String extractTitle(Element card, Element linkElem) {
    String title = linkElem.attr("title");
    if (title != null && !title.isBlank()) {
      return title.trim();
    }
    Element heading = card.selectFirst(".item-title, .listing-title, h2, h3, h4, .item-price");
    if (heading != null && !heading.text().isBlank()) {
      return heading.text().trim();
    }
    return "";
  }

  private String extractPriceText(Element card) {
    Elements dedicatedPrice = card.select(".item-price, .price, .listing-price");
    for (Element candidate : dedicatedPrice) {
      if (!candidate.text().isBlank()) {
        return candidate.text();
      }
    }
    Element typeCandidate = card.selectFirst(".item-type.h2");
    if (typeCandidate != null && !typeCandidate.text().isBlank()) {
      String txt = typeCandidate.text();
      if (txt.contains("Ft") || txt.matches(".*\\d.*")) {
        return txt;
      }
    }
    return card.text();
  }

  private String extractDetailsText(Element card) {
    Element paramsElem = card.selectFirst(".listing-parameters, .property-parameters, .item-parameters");
    if (paramsElem != null && !paramsElem.text().isBlank()) {
      return paramsElem.text();
    }
    return card.text();
  }

  private long extractJsonLdPrice(JsonNode product) {
    if (!product.has("offers")) {
      return 0;
    }
    JsonNode offers = product.get("offers");
    if (offers.isArray() && !offers.isEmpty()) {
      offers = offers.get(0);
    }
    if (offers.has("priceSpecification") && offers.get("priceSpecification").has("price")) {
      return offers.get("priceSpecification").get("price").asLong(0);
    }
    if (offers.has("price")) {
      return offers.get("price").asLong(0);
    }
    return 0;
  }

  private String extractJsonLdImage(JsonNode product) {
    if (!product.has("image")) {
      return null;
    }
    String resolved = null;
    JsonNode imageNode = product.get("image");
    if (imageNode.isArray() && !imageNode.isEmpty()) {
      resolved = PortalSupport.resolveUrl(KoltozzbeUrlBuilder.BASE_URL, imageNode.get(0).asString());
    } else if (imageNode.isObject() && imageNode.has("url")) {
      resolved = PortalSupport.resolveUrl(KoltozzbeUrlBuilder.BASE_URL, imageNode.get("url").asString());
    } else if (imageNode.isValueNode()) {
      resolved = PortalSupport.resolveUrl(KoltozzbeUrlBuilder.BASE_URL, imageNode.asString());
    }
    return (resolved != null && !PortalSupport.isPlaceholderImageUrl(resolved)) ? resolved : null;
  }

  public boolean hasNextPage(Document doc, int currentPage) {
    if (doc == null) {
      return false;
    }
    int nextPage = currentPage + 1;
    if (!doc.select("a[rel=next], .pagination .next:not(.disabled) a").isEmpty()) {
      return true;
    }

    Elements pageLinks = doc.select(".pagination a[href], a[href*=page=]");
    if (pageLinks.isEmpty()) {
      return doc.select(".pagination").isEmpty();
    }

    for (Element link : pageLinks) {
      String href = link.attr("href");
      Matcher matcher = PAGE_PARAM_PATTERN.matcher(href);
      if (matcher.find()) {
        try {
          int targetPage = Integer.parseInt(matcher.group(1));
          if (targetPage >= nextPage) {
            return true;
          }
        } catch (NumberFormatException ignored) {
          // Ignore malformed page numbers in pagination links.
        }
      }
    }
    return false;
  }
}
