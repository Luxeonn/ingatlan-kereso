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
      String href = card.is("a") ? card.attr("href") : "";
      if (href.isBlank()) {
        Element a = card.selectFirst("a[href*=/ingatlanok/]");
        if (a != null) {
          href = a.attr("href");
        }
      }
      if (href == null || href.isBlank()) {
        continue;
      }

      if (href.matches(".*/ingatlanok/[A-Za-z0-9]+$")) {
        String fullLink = PortalSupport.resolveUrl(OtthonCentrumUrlBuilder.BASE_URL, href);

        String cardText = card.text().replaceAll("\\s+", " ").trim();
        String id = PropertyParser.extractFirstGroup(fullLink, ID_PATTERN, null);

        String locationCity = null;
        Long jsonPriceHuf = null;
        String seoAttr = card.attr("data-seo-select-item-value");
        if (seoAttr.isBlank()) {
          Element seoHolder = card.selectFirst("[data-seo-select-item-value]");
          if (seoHolder != null) {
            seoAttr = seoHolder.attr("data-seo-select-item-value");
          }
        }

        if (!seoAttr.isBlank()) {
          try {
            JsonNode seoObj = JsonSupport.parse(seoAttr);
            if (seoObj.has("ecommerce")) {
              JsonNode ecom = seoObj.get("ecommerce");
              if (ecom.has("items")) {
                JsonNode itemsArray = ecom.get("items");
                if (!itemsArray.isEmpty()) {
                  JsonNode firstItem = itemsArray.get(0);
                  if (firstItem.has("location_city")) {
                    locationCity = firstItem.get("location_city").asText();
                  }
                  if (firstItem.has("price") && !firstItem.get("price").isNull()) {
                    jsonPriceHuf = firstItem.get("price").asLong();
                  }
                }
              }
            }
          } catch (Exception ignored) {
            // Optional metadata
          }
        }

        String title;
        Element headingEl = card.selectFirst("div.collapse.description h6, .description h6, h6, h3");
        if (headingEl != null && !headingEl.text().isBlank()) {
          title = headingEl.text().trim();
        } else {
          title = cleanTitle(cardText, locationCity, id);
        }

        String imageUrl = PortalSupport.extractImageWithFallback(card, OtthonCentrumUrlBuilder.BASE_URL);

        RawPropertyListing raw = RawPropertyListing.builder()
            .portalId("oc")
            .nativeId(id)
            .url(fullLink)
            .title(title)
            .priceHuf(jsonPriceHuf)
            .priceText(cardText)
            .detailsText(cardText)
            .locationText(locationCity)
            .imageUrl(imageUrl)
            .descriptionText(cardText + " " + seoAttr)
            .method(ExtractionMethod.HTML)
            .build();

        items.add(raw);
      }
    }

    return items;
  }

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
