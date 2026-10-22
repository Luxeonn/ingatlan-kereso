package ingatlanok.portal.ingatlancom;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;

public class IngatlanComParser {

  private static final Pattern ID_PATTERN = Pattern.compile("/ingatlan/(\\d+)");
  private static final PortalSource SOURCE = PortalSource.of("Ingatlan.com");

  public boolean isBotChallenge(Document document) {
    if (document == null) {
      return false;
    }
    return (document.title() != null
            && (document.title().contains("ellenőrzés")
                || document.title().contains("Cloudflare")
                || document.title().contains("Just a moment")
                || document.title().contains("Attention Required")))
        || !document
            .select(
                "#challenge-error-text, #robot-protection-illustration,"
                    + " script[src*='challenge-platform']")
            .isEmpty();
  }

  public List<IngatlanItem> parseItems(
      Document document, SearchCriteria criteria, String executionTime, Set<String> processedUrls) {
    List<IngatlanItem> items = new ArrayList<>();
    if (document == null) {
      return items;
    }

    Elements cards =
        document.select(
            "div.listing-card, article.listing-card, div.card[data-id], a.listing-card,"
                + " a[href*=/ingatlan/]");
    for (Element card : cards) {
      String href = card.is("a") ? card.attr("href") : "";
      if (href == null || href.isBlank()) {
        Element a = card.selectFirst("a[href*=/ingatlan/]");
        if (a != null) {
          href = a.attr("href");
        }
      }
      if (href == null || href.isBlank()) {
        continue;
      }

      String fullLink =
          href.startsWith("http")
              ? href
              : IngatlanComUrlBuilder.BASE_URL + (href.startsWith("/") ? "" : "/") + href;
      if (!fullLink.matches(".*/ingatlan/\\d+.*")) {
        continue;
      }
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

      PriceInfo priceInfo = null;
      Element priceEl = card.selectFirst(".price, .listing-card__price, .price-container");
      if (priceEl != null && !priceEl.text().isBlank()) {
        priceInfo = PropertyParser.parsePrice(priceEl.text());
      }
      if (priceInfo == null || priceInfo.getAmountHuf() == null) {
        priceInfo = PropertyParser.parsePrice(cardText);
      }

      PropertyDetails detailsInfo = PropertyParser.parseDetails(cardText, priceInfo);
      String city = criteria != null ? criteria.getCity() : "Debrecen";

      String title;
      Element titleEl = card.selectFirst(".listing-card__title, .address, h2, h3");
      if (titleEl != null && !titleEl.text().isBlank()) {
        title = titleEl.text().trim();
      } else {
        title = city + " - " + extractTitle(cardText, city, id);
      }

      IngatlanItem item =
          new IngatlanItem(
              id, SOURCE, fullLink, title, priceInfo, city, detailsInfo, executionTime, false);
      item.setLocation(city);

      String imageUrl = PropertyParser.extractImageUrl(card);
      if ((imageUrl == null || imageUrl.isBlank()) && card.parent() != null) {
        imageUrl = PropertyParser.extractImageUrl(card.parent());
      }
      item.setImageUrl(imageUrl);
      item.setNewConstruction(PropertyParser.isNewConstructionText(cardText, title, fullLink));

      if (criteria != null && !criteria.matches(item)) {
        continue;
      }

      items.add(item);
    }

    return items;
  }

  public String extractTitle(String text, String defaultCity, String id) {
    if (text != null && text.length() > 5) {
      int max = Math.min(text.length(), 65);
      return text.substring(0, max).trim() + "...";
    }
    return defaultCity + " - Ingatlan (" + id + ")";
  }
}
