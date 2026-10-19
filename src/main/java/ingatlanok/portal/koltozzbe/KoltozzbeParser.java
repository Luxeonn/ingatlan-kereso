package ingatlanok.portal.koltozzbe;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import ingatlanok.common.AppLogger;
import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;

public class KoltozzbeParser {

  private static final AppLogger LOGGER = AppLogger.getLogger(KoltozzbeParser.class);
  private static final Pattern ID_PATTERN = Pattern.compile("/h([0-9]+)");
  private static final PortalSource SOURCE = PortalSource.of("Költözzbe");

  public List<IngatlanItem> parseItems(
      Document doc, SearchCriteria criteria, String now, Set<String> processedUrls) {
    List<IngatlanItem> items = new ArrayList<>();
    if (doc == null) {
      return items;
    }

    Elements cards = doc.select(".listing-card, div[data-lid]");
    if (!cards.isEmpty()) {
      for (Element card : cards) {
        Element linkElem = card.selectFirst("a.listing-item-link, a[href*=/elado-]");
        if (linkElem == null) {
          continue;
        }

        String href = linkElem.attr("href");
        if (href == null || href.isBlank()) {
          continue;
        }

        String fullUrl =
            href.startsWith("http")
                ? href
                : KoltozzbeUrlBuilder.BASE_URL + (href.startsWith("/") ? "" : "/") + href;
        if (processedUrls != null && processedUrls.contains(fullUrl)) {
          continue;
        }
        if (processedUrls != null) {
          processedUrls.add(fullUrl);
        }

        String lid = card.attr("data-lid");
        if (lid.isBlank()) {
          lid =
              PropertyParser.extractFirstGroup(
                  fullUrl, ID_PATTERN, UUID.randomUUID().toString().substring(0, 8));
        }

        String city = criteria != null ? criteria.getCity() : "Debrecen";
        String title = linkElem.attr("title");
        if (title == null || title.isBlank()) {
          Element priceHeader = card.selectFirst(".item-price");
          title = (priceHeader != null) ? priceHeader.text().trim() : city + " - Eladó ingatlan";
        }

        Element priceElem = card.selectFirst(".item-type.h2");
        String rawPriceText = (priceElem != null) ? priceElem.text() : card.text();
        PriceInfo priceInfo = PropertyParser.parsePrice(rawPriceText);

        Element paramsElem = card.selectFirst(".listing-parameters");
        String detailsText = (paramsElem != null) ? paramsElem.text() : card.text();
        PropertyDetails details = PropertyParser.parseDetails(detailsText, priceInfo);

        Element imgElem = card.selectFirst(".listing-thumbnail img, img");
        String imageUrl = null;
        if (imgElem != null) {
          imageUrl = imgElem.hasAttr("data-src") ? imgElem.attr("data-src") : imgElem.attr("src");
          if (imageUrl != null && imageUrl.startsWith("/")) {
            imageUrl = KoltozzbeUrlBuilder.BASE_URL + imageUrl;
          }
        }

        IngatlanItem item =
            new IngatlanItem(lid, SOURCE, fullUrl, title, priceInfo, city, details, now, false);
        item.setLocation(city);
        item.setImageUrl(imageUrl);

        if (criteria != null && !criteria.matches(item)) {
          continue;
        }
        items.add(item);
      }
    } else {
      parseJsonLd(doc, criteria, items, processedUrls, now);
    }

    return items;
  }

  public void parseJsonLd(
      Document doc,
      SearchCriteria criteria,
      List<IngatlanItem> items,
      Set<String> processedUrls,
      String now) {
    Elements scripts = doc.select("script[type=application/ld+json]");
    for (Element script : scripts) {
      String jsonText = script.data();
      if (jsonText == null || !jsonText.contains("ListItem") || !jsonText.contains("Product")) {
        continue;
      }
      try {
        JsonElement root = JsonParser.parseString(jsonText);
        if (!root.isJsonObject()) {
          continue;
        }
        JsonObject obj = root.getAsJsonObject();
        if (!obj.has("itemListElement")) {
          continue;
        }

        JsonArray elements = obj.getAsJsonArray("itemListElement");
        for (JsonElement el : elements) {
          if (!el.isJsonObject()) {
            continue;
          }
          JsonObject itemObj = el.getAsJsonObject();
          if (!itemObj.has("item")) {
            continue;
          }
          JsonObject product = itemObj.getAsJsonObject("item");

          String url = product.has("url") ? product.get("url").getAsString() : null;
          if (url == null || (processedUrls != null && processedUrls.contains(url))) {
            continue;
          }
          if (processedUrls != null) {
            processedUrls.add(url);
          }

          String city = criteria != null ? criteria.getCity() : "Debrecen";
          String name =
              product.has("name") ? product.get("name").getAsString() : city + " - Eladó ingatlan";
          String id =
              PropertyParser.extractFirstGroup(
                  url, ID_PATTERN, UUID.randomUUID().toString().substring(0, 8));

          long rawPrice = 0;
          if (product.has("offers")) {
            JsonObject offers = product.getAsJsonObject("offers");
            if (offers.has("priceSpecification")) {
              JsonObject ps = offers.getAsJsonObject("priceSpecification");
              if (ps.has("price")) {
                rawPrice = ps.get("price").getAsLong();
              }
            }
          }

          PriceInfo priceInfo =
              (rawPrice > 0)
                  ? PropertyParser.parsePrice(rawPrice + " Ft")
                  : PropertyParser.parsePrice(name);

          PropertyDetails details = PropertyParser.parseDetails(name, priceInfo);

          String imageUrl = null;
          if (product.has("image") && product.get("image").isJsonArray()) {
            JsonArray imgArr = product.getAsJsonArray("image");
            if (!imgArr.isEmpty()) {
              imageUrl = imgArr.get(0).getAsString();
            }
          }

          IngatlanItem item =
              new IngatlanItem(id, SOURCE, url, name, priceInfo, city, details, now, false);
          item.setLocation(city);
          item.setImageUrl(imageUrl);

          if (criteria == null || criteria.matches(item)) {
            items.add(item);
          }
        }
      } catch (Exception ex) {
        LOGGER.debug("JSON-LD parse warning: {}", ex.getMessage());
      }
    }
  }
}
