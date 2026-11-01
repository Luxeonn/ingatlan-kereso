package ingatlanok.portal.ingatlancom;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.PortalSupport;

public class IngatlanComRawParser {

  private static final Pattern ID_PATTERN = Pattern.compile("/ingatlan/(\\d+)");

  public List<RawPropertyListing> parseRawListings(Document document) {
    List<RawPropertyListing> items = new ArrayList<>();
    if (document == null) {
      return items;
    }

    Elements cards = document.select(
        "div.listing-card, article.listing-card, div.card[data-id], a.listing-card,"
            + " a[href*=/ingatlan/]");
    for (Element card : cards) {
      RawPropertyListing raw = parseListingCard(card);
      if (raw != null) {
        items.add(raw);
      }
    }

    return items;
  }

  private RawPropertyListing parseListingCard(Element card) {
    String href = resolveHref(card);
    if (href == null || href.isBlank()) {
      return null;
    }

    String fullLink = PortalSupport.resolveUrl(IngatlanComUrlBuilder.BASE_URL, href);
    if (!fullLink.matches(".*/ingatlan/\\d+.*")) {
      return null;
    }

    String cardText = card.text().replaceAll("\\s+", " ").trim();
    String id = PropertyParser.extractFirstGroup(fullLink, ID_PATTERN, null);
    String priceText = resolvePriceText(card, cardText);
    String title = resolveCardTitle(card, cardText, id);
    String imageUrl = PortalSupport.extractImageWithFallback(card, IngatlanComUrlBuilder.BASE_URL);

    return RawPropertyListing.builder()
        .portalId("ingatlancom")
        .nativeId(id)
        .url(fullLink)
        .title(title)
        .priceText(priceText)
        .detailsText(cardText)
        .imageUrl(imageUrl)
        .descriptionText(cardText)
        .method(ExtractionMethod.HTML)
        .build();
  }

  private String resolveHref(Element card) {
    String href = card.is("a") ? card.attr("href") : "";
    if (href == null || href.isBlank()) {
      Element a = card.selectFirst("a[href*=/ingatlan/]");
      if (a != null) {
        href = a.attr("href");
      }
    }
    return href;
  }

  private String resolvePriceText(Element card, String cardText) {
    Element priceEl = card.selectFirst(".price, .listing-card__price, .price-container");
    if (priceEl != null && !priceEl.text().isBlank()) {
      return priceEl.text().trim();
    }
    return cardText;
  }

  private String resolveCardTitle(Element card, String cardText, String id) {
    Element titleEl = card.selectFirst(".listing-card__title, .address, h2, h3");
    if (titleEl != null && !titleEl.text().isBlank()) {
      return titleEl.text().trim();
    }
    return extractTitle(cardText, id);
  }

  public String extractTitle(String text, String id) {
    if (text != null && text.length() > 5) {
      int max = Math.min(text.length(), 65);
      return text.substring(0, max).trim() + "...";
    }
    return "Ingatlan.com hirdetés" + (id != null ? " (" + id + ")" : "");
  }
}
