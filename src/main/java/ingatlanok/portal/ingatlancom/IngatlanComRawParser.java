package ingatlanok.portal.ingatlancom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.PortalSupport;

public class IngatlanComRawParser {

  private static final Pattern ID_PATTERN =
      Pattern.compile("/(?:ingatlan/)?(\\d{5,12})(?:[/?#]|$)");
  private static final Pattern LABELED_AREA_PATTERN =
      Pattern.compile("(?iuU)Alapterület\\s*(\\d[\\d.,\\s]*)\\s*m(?:2|²)?");
  private static final Pattern LABELED_ROOMS_HALF_PATTERN =
      Pattern.compile("(?iuU)Szobák\\s*(\\d+)\\s*\\+\\s*(\\d+)");
  private static final Pattern LABELED_ROOMS_PATTERN =
      Pattern.compile("(?iuU)Szobák\\s*(\\d+(?:[.,]\\d+)?)");

  public List<RawPropertyListing> parseRawListings(Document document) {
    List<RawPropertyListing> items = new ArrayList<>();
    if (document == null) {
      return items;
    }

    Set<String> seenUrls = new HashSet<>();
    Elements cards =
        document.select(
            "a.listing-card, a[data-listing-id], div.listing-card, article.listing-card,"
                + " div.card[data-id], a[href*=/ingatlan/]");
    for (Element card : cards) {
      RawPropertyListing raw = parseListingCard(card);
      if (raw != null && seenUrls.add(raw.url())) {
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
    String id = resolveListingId(card, fullLink);
    if (id == null || id.isBlank()) {
      return null;
    }

    String cardText = card.text().replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    String priceText = resolvePriceText(card, cardText);
    String title = resolveCardTitle(card, cardText, id);
    String imageUrl = PortalSupport.extractImageWithFallback(card, IngatlanComUrlBuilder.BASE_URL);
    Double area = extractLabeledArea(cardText);
    Double rooms = extractLabeledRooms(cardText);

    return RawPropertyListing.builder()
        .portalId("ingatlancom")
        .nativeId(id)
        .url(fullLink)
        .title(title)
        .priceText(priceText)
        .areaSquareMeters(area)
        .roomCount(rooms)
        .detailsText(cardText)
        .imageUrl(imageUrl)
        .descriptionText(cardText)
        .method(ExtractionMethod.HTML)
        .build();
  }

  private String resolveListingId(Element card, String fullLink) {
    String urlId = PropertyParser.extractFirstGroup(fullLink, ID_PATTERN, null);
    if (urlId != null && !urlId.isBlank()) {
      return urlId.trim();
    }
    String dataListingId = card.attr("data-listing-id");
    if (dataListingId != null && !dataListingId.isBlank()) {
      return dataListingId.trim();
    }
    String dataId = card.attr("data-id");
    if (dataId != null && !dataId.isBlank()) {
      return dataId.trim();
    }
    return null;
  }

  private String resolveHref(Element card) {
    String href = card.is("a") ? card.attr("href") : "";
    if (href == null || href.isBlank()) {
      Element a = card.selectFirst("a[href]");
      if (a != null) {
        href = a.attr("href");
      }
    }
    return href;
  }

  private String resolvePriceText(Element card, String cardText) {
    Element priceEl =
        card.selectFirst(".fw-bold.fs-5, .price, .listing-card__price, .price-container");
    if (priceEl != null && !priceEl.text().isBlank()) {
      return priceEl.text().replace('\u00a0', ' ').trim();
    }
    return cardText;
  }

  private String resolveCardTitle(Element card, String cardText, String id) {
    Element titleEl =
        card.selectFirst(
            "span.d-block.fs-7.text-gray-900, .listing-card__title, .address, h2, h3");
    if (titleEl != null && !titleEl.text().isBlank()) {
      String baseTitle = titleEl.text().trim();
      Element labelEl = card.selectFirst("[data-slug=uj-epitesu], .listing-label");
      if (labelEl != null && labelEl.text().toLowerCase().contains("új építésű")) {
        return baseTitle + " [Új építésű]";
      }
      return baseTitle;
    }
    return extractTitle(cardText, id);
  }

  private Double extractLabeledArea(String cardText) {
    String areaStr = PropertyParser.extractFirstGroup(cardText, LABELED_AREA_PATTERN, null);
    if (areaStr != null) {
      try {
        return Double.parseDouble(areaStr.replace(" ", "").replace(",", "."));
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  private Double extractLabeledRooms(String cardText) {
    var halfMatcher = LABELED_ROOMS_HALF_PATTERN.matcher(cardText);
    if (halfMatcher.find()) {
      try {
        return Double.parseDouble(halfMatcher.group(1))
            + 0.5 * Double.parseDouble(halfMatcher.group(2));
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    String roomsStr = PropertyParser.extractFirstGroup(cardText, LABELED_ROOMS_PATTERN, null);
    if (roomsStr != null) {
      try {
        return Double.parseDouble(roomsStr.replace(",", "."));
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  public String extractTitle(String text, String id) {
    if (text != null && text.length() > 5) {
      int max = Math.min(text.length(), 65);
      return text.substring(0, max).trim() + "...";
    }
    return "Ingatlan.com hirdetés" + (id != null ? " (" + id + ")" : "");
  }
}
