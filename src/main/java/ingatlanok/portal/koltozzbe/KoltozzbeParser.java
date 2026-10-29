package ingatlanok.portal.koltozzbe;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import tools.jackson.databind.JsonNode;

import ingatlanok.common.AppLogger;
import ingatlanok.common.JsonSupport;
import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalHtmlParser;
import ingatlanok.portal.PortalSupport;

public class KoltozzbeParser implements PortalHtmlParser {

  private static final AppLogger LOGGER = AppLogger.getLogger(KoltozzbeParser.class);
  private static final Pattern ID_PATTERN = Pattern.compile("/h([0-9]+)");
  private static final Pattern PAGE_PARAM_PATTERN = Pattern.compile("[?&]page=(\\d+)");
  private static final PortalSource SOURCE = PortalSource.of("Költözzbe");

  @Override
  public List<IngatlanItem> parseItems(
      Document doc, SearchCriteria criteria, String now, Set<String> processedUrls) {
    List<IngatlanItem> items = new ArrayList<>();
    if (doc == null) {
      return items;
    }

    Elements cards =
        doc.select(
            ".listing-card, .property-box, .property-card, .listing-item,"
                + " article[data-id], div[data-lid], div[data-id]");
    if (cards.isEmpty()) {
      cards = doc.select("a.listing-item-link, a[href*=/elado-]");
    }

    for (Element card : cards) {
      Element linkElem =
          card.is("a")
              ? card
              : card.selectFirst(
                  "a.listing-item-link, a[href*=/elado-], a[href*=/h], a[href*=/ingatlan]");
      if (linkElem == null) {
        continue;
      }

      String href = linkElem.attr("href");
      if (href == null || href.isBlank()) {
        continue;
      }

      String fullUrl = PortalSupport.resolveUrl(KoltozzbeUrlBuilder.BASE_URL, href);
      if (!PortalSupport.markProcessed(processedUrls, fullUrl)) {
        continue;
      }

      String lid = card.attr("data-lid");
      if (lid.isBlank()) {
        lid = card.attr("data-id");
      }
      if (lid.isBlank()) {
        lid =
            PropertyParser.extractFirstGroup(
                fullUrl, ID_PATTERN, UUID.randomUUID().toString().substring(0, 8));
      }

      String city = PortalSupport.resolveDefaultCity(criteria);
      String title = extractTitle(card, linkElem, city);

      PriceInfo priceInfo = extractPrice(card);
      PropertyDetails details = extractDetails(card, priceInfo);
      String imageUrl =
          PortalSupport.extractImageWithFallback(card, KoltozzbeUrlBuilder.BASE_URL);

      IngatlanItem item =
          IngatlanItem.builder()
              .id(lid)
              .source(SOURCE)
              .url(fullUrl)
              .title(title)
              .priceInfo(priceInfo)
              .city(city)
              .location(city)
              .detailsInfo(details)
              .discoveredAt(now)
              .imageUrl(imageUrl)
              .newConstruction(PropertyParser.isNewConstructionText(card.text(), title, fullUrl))
              .build();

      if (criteria != null && !criteria.matchesRelevance(item)) {
        continue;
      }
      items.add(item);
    }

    parseJsonLd(doc, criteria, items, processedUrls, now);

    return items;
  }

  @Override
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

  private String extractTitle(Element card, Element linkElem, String city) {
    String title = linkElem.attr("title");
    if (title != null && !title.isBlank()) {
      return title.trim();
    }
    Element heading = card.selectFirst(".item-title, .listing-title, h2, h3, h4, .item-price");
    if (heading != null && !heading.text().isBlank()) {
      return heading.text().trim();
    }
    return city + " - Eladó ingatlan";
  }

  private PriceInfo extractPrice(Element card) {
    Elements priceCandidates =
        card.select(".item-type.h2, .item-price, .price, .listing-price");
    for (Element candidate : priceCandidates) {
      PriceInfo parsed = PropertyParser.parsePrice(candidate.text());
      if (parsed != null && parsed.getAmountHuf() != null) {
        return parsed;
      }
    }
    return PropertyParser.parsePrice(card.text());
  }

  private PropertyDetails extractDetails(Element card, PriceInfo priceInfo) {
    Element paramsElem =
        card.selectFirst(".listing-parameters, .property-parameters, .item-parameters");
    if (paramsElem != null) {
      PropertyDetails parsed = PropertyParser.parseDetails(paramsElem.text(), priceInfo);
      if (parsed.getAreaSquareMeters() != null) {
        return parsed;
      }
    }
    return PropertyParser.parseDetails(card.text(), priceInfo);
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
      if (jsonText == null
          || (!jsonText.contains("ListItem") && !jsonText.contains("itemListElement"))) {
        continue;
      }
      try {
        JsonNode root = JsonSupport.parse(jsonText);
        if (!root.isObject() || !root.has("itemListElement")) {
          continue;
        }

        JsonNode elements = root.get("itemListElement");
        for (JsonNode el : elements) {
          if (!el.isObject()) {
            continue;
          }
          JsonNode product = el.has("item") ? el.get("item") : el;

          String rawUrl = product.has("url") ? product.get("url").asText() : null;
          String url = PortalSupport.resolveUrl(KoltozzbeUrlBuilder.BASE_URL, rawUrl);
          if (url.isEmpty() || !PortalSupport.markProcessed(processedUrls, url)) {
            continue;
          }

          String city = PortalSupport.resolveDefaultCity(criteria);
          String name =
              product.has("name") ? product.get("name").asText() : city + " - Eladó ingatlan";
          String description =
              product.has("description") ? product.get("description").asText("") : "";
          String id =
              PropertyParser.extractFirstGroup(
                  url, ID_PATTERN, UUID.randomUUID().toString().substring(0, 8));

          long rawPrice = extractJsonLdPrice(product);
          PriceInfo priceInfo =
              (rawPrice > 0)
                  ? PropertyParser.parsePrice(rawPrice + " Ft")
                  : PropertyParser.parsePrice(name + " " + description);

          PropertyDetails details = PropertyParser.parseDetails(name, priceInfo);
          if (details.getAreaSquareMeters() == null && !description.isBlank()) {
            details = PropertyParser.parseDetails(name + " " + description, priceInfo);
          }

          String imageUrl = extractJsonLdImage(product);

          IngatlanItem item =
              IngatlanItem.builder()
                  .id(id)
                  .source(SOURCE)
                  .url(url)
                  .title(name)
                  .priceInfo(priceInfo)
                  .city(city)
                  .location(city)
                  .detailsInfo(details)
                  .discoveredAt(now)
                  .imageUrl(imageUrl)
                  .newConstruction(PropertyParser.isNewConstructionText(name, description, url))
                  .build();

          if (criteria == null || criteria.matchesRelevance(item)) {
            items.add(item);
          }
        }
      } catch (Exception ex) {
        LOGGER.debug("JSON-LD parse warning: {}", ex.getMessage());
      }
    }
  }

  private long extractJsonLdPrice(JsonNode product) {
    if (!product.has("offers")) {
      return 0;
    }
    JsonNode offers = product.get("offers");
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
      resolved = PortalSupport.resolveUrl(KoltozzbeUrlBuilder.BASE_URL, imageNode.get(0).asText());
    } else if (imageNode.isValueNode()) {
      resolved = PortalSupport.resolveUrl(KoltozzbeUrlBuilder.BASE_URL, imageNode.asText());
    }
    return (resolved != null && !PortalSupport.isPlaceholderImageUrl(resolved)) ? resolved : null;
  }
}
