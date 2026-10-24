package ingatlanok.portal.dh;

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

public class DunaHouseRawParser {

  private static final Pattern ID_PATTERN = Pattern.compile("/ingatlan/([A-Za-z0-9\\-]+)");

  public List<RawPropertyListing> parseRawListings(Document document) {
    List<RawPropertyListing> items = new ArrayList<>();
    if (document == null) {
      return items;
    }

    Elements anchorLinks = document.select("a[href*=/ingatlan/]");
    for (Element a : anchorLinks) {
      String href = a.attr("href");
      if (href == null || href.isBlank()) {
        continue;
      }

      String fullLink = PortalSupport.resolveUrl(DunaHouseUrlBuilder.BASE_URL, href);
      if (!fullLink.matches(".*/ingatlan/[A-Za-z0-9\\-]+/.*")
          && !fullLink.matches(".*/ingatlan/[A-Za-z0-9\\-]+$")) {
        continue;
      }

      String cardText = a.text().replaceAll("\\s+", " ").trim();
      String id = PropertyParser.extractFirstGroup(fullLink, ID_PATTERN, null);
      if ("elado".equalsIgnoreCase(id) || "kiado".equalsIgnoreCase(id)) {
        String[] segments = fullLink.split("/");
        if (segments.length > 0 && !segments[segments.length - 1].isBlank()) {
          id = segments[segments.length - 1];
        }
      }

      String extractedCity = PropertyParser.extractCityFromPostalAddress(cardText);
      if (extractedCity == null || extractedCity.isBlank()) {
        String urlCity = PropertyParser.extractCityFromDhUrl(fullLink);
        if (urlCity != null && !urlCity.isBlank()) {
          extractedCity = urlCity;
        }
      }

      String title = cleanTitle(cardText, extractedCity, id);
      String imageUrl = PortalSupport.extractImageWithFallback(a, DunaHouseUrlBuilder.BASE_URL);

      RawPropertyListing raw = RawPropertyListing.builder()
          .portalId("dh")
          .nativeId(id)
          .url(fullLink)
          .title(title)
          .priceText(cardText)
          .detailsText(cardText)
          .locationText(extractedCity)
          .imageUrl(imageUrl)
          .descriptionText(cardText)
          .method(ExtractionMethod.HTML)
          .build();

      items.add(raw);
    }

    return items;
  }

  public String cleanTitle(String text, String defaultCity, String id) {
    if (text != null && text.contains("|")) {
      String[] parts = text.split("\\|");
      if (parts.length > 0 && !parts[0].isBlank()) {
        return parts[0].replaceFirst("(?i)^Új\\s+(?!építés)", "").trim();
      }
    }
    String city = defaultCity != null && !defaultCity.isBlank() ? defaultCity : "Eladó";
    return city + " - Eladó ingatlan" + (id != null ? " (" + id + ")" : "");
  }
}
