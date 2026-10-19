package ingatlanok.portal.oc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;

/** Otthon Centrum kártya és DOM feldolgozó komponens. */
public class OtthonCentrumParser {

  private static final Pattern ID_PATTERN = Pattern.compile("/ingatlanok/([A-Za-z0-9]+)");
  private static final PortalSource SOURCE = PortalSource.of("Otthon Centrum");

  public List<IngatlanItem> parseItems(
      Document document, SearchCriteria criteria, String executionTime, Set<String> processedUrls) {
    List<IngatlanItem> items = new ArrayList<>();
    if (document == null) {
      return items;
    }

    Elements cards =
        document.select(
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
        String fullLink =
            href.startsWith("http")
                ? href
                : OtthonCentrumUrlBuilder.BASE_URL + (href.startsWith("/") ? "" : "/") + href;
        if (processedUrls != null && processedUrls.contains(fullLink)) {
          continue;
        }
        if (processedUrls != null) {
          processedUrls.add(fullLink);
        }

        String cardText = card.text().replaceAll("\\s+", " ").trim();
        String id =
            PropertyParser.extractFirstGroup(
                fullLink, ID_PATTERN, UUID.randomUUID().toString().substring(0, 8));

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
            JsonObject seoObj = JsonParser.parseString(seoAttr).getAsJsonObject();
            if (seoObj.has("ecommerce")) {
              JsonObject ecom = seoObj.getAsJsonObject("ecommerce");
              if (ecom.has("items")) {
                JsonArray itemsArray = ecom.getAsJsonArray("items");
                if (!itemsArray.isEmpty()) {
                  JsonObject firstItem = itemsArray.get(0).getAsJsonObject();
                  if (firstItem.has("location_city")) {
                    locationCity = firstItem.get("location_city").getAsString();
                  }
                  if (firstItem.has("price") && !firstItem.get("price").isJsonNull()) {
                    jsonPriceHuf = firstItem.get("price").getAsLong();
                  }
                }
              }
            }
          } catch (Exception ignored) {
            // JSON-LD price metadata is optional; continue with the visible card text.
          }
        }

        PriceInfo priceInfo = PropertyParser.parsePrice(cardText);
        if (jsonPriceHuf != null && (priceInfo == null || priceInfo.getAmountHuf() == null)) {
          double million = Math.round((jsonPriceHuf / 1_000_000.0) * 100.0) / 100.0;
          priceInfo = new PriceInfo(jsonPriceHuf + " Ft", jsonPriceHuf, million);
        }

        PropertyDetails detailsInfo = PropertyParser.parseDetails(cardText, priceInfo);
        String actualCity =
            (locationCity != null && !locationCity.isBlank())
                ? locationCity
                : (criteria != null ? criteria.getCity() : "Debrecen");

        String title;
        Element headingEl =
            card.selectFirst("div.collapse.description h6, .description h6, h6, h3");
        if (headingEl != null && !headingEl.text().isBlank()) {
          title = headingEl.text().trim();
        } else {
          title = cleanTitle(cardText, actualCity, id);
        }

        IngatlanItem item =
            new IngatlanItem(
                id,
                SOURCE,
                fullLink,
                title,
                priceInfo,
                actualCity,
                detailsInfo,
                executionTime,
                false);
        item.setLocation(actualCity);

        String imageUrl = null;
        Element imgEl = card.selectFirst("img.lazyload[data-src], img[data-src]");
        if (imgEl != null && !imgEl.attr("data-src").isBlank()) {
          imageUrl = imgEl.attr("data-src");
        }
        if (imageUrl == null || imageUrl.isBlank()) {
          imageUrl = PropertyParser.extractImageUrl(card);
        }
        if ((imageUrl == null || imageUrl.isBlank()) && card.parent() != null) {
          imageUrl = PropertyParser.extractImageUrl(card.parent());
        }
        item.setImageUrl(imageUrl);

        if (criteria != null && !criteria.matches(item)) {
          continue;
        }

        items.add(item);
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
    return defaultCity + " - Eladó ingatlan (" + id + ")";
  }
}
