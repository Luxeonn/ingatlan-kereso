package ingatlanok.portal.dh;

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

/** Duna House HTML dokumentum és hirdetéskártya feldolgozó komponens. */
public class DunaHouseParser {

  private static final Pattern ID_PATTERN = Pattern.compile("/ingatlan/([A-Za-z0-9\\-]+)");
  private static final PortalSource SOURCE = PortalSource.of("Duna House");

  public List<IngatlanItem> parseItems(
      Document document, SearchCriteria criteria, String executionTime, Set<String> processedUrls) {
    List<IngatlanItem> items = new ArrayList<>();
    if (document == null) {
      return items;
    }

    Elements anchorLinks = document.select("a[href*=/ingatlan/]");
    for (Element a : anchorLinks) {
      String href = a.attr("href");
      if (href == null || href.isBlank()) {
        continue;
      }

      String fullLink =
          href.startsWith("http")
              ? href
              : DunaHouseUrlBuilder.BASE_URL + (href.startsWith("/") ? "" : "/") + href;
      if (!fullLink.matches(".*/ingatlan/[A-Za-z0-9\\-]+/.*")
          && !fullLink.matches(".*/ingatlan/[A-Za-z0-9\\-]+$")) {
        continue;
      }
      if (processedUrls != null && processedUrls.contains(fullLink)) {
        continue;
      }
      if (processedUrls != null) {
        processedUrls.add(fullLink);
      }

      String cardText = a.text().replaceAll("\\s+", " ").trim();
      String id =
          PropertyParser.extractFirstGroup(
              fullLink, ID_PATTERN, UUID.randomUUID().toString().substring(0, 8));
      if ("elado".equalsIgnoreCase(id) || "kiado".equalsIgnoreCase(id)) {
        String[] segments = fullLink.split("/");
        if (segments.length > 0 && !segments[segments.length - 1].isBlank()) {
          id = segments[segments.length - 1];
        }
      }
      PriceInfo priceInfo = PropertyParser.parsePrice(cardText);
      PropertyDetails detailsInfo = PropertyParser.parseDetails(cardText, priceInfo);

      String extractedCity = PropertyParser.extractCityFromPostalAddress(cardText);
      if (extractedCity == null || extractedCity.isBlank()) {
        String urlCity = PropertyParser.extractCityFromDhUrl(fullLink);
        if (urlCity != null && !urlCity.isBlank()) {
          extractedCity = urlCity;
        }
      }
      String actualCity =
          (extractedCity != null && !extractedCity.isBlank())
              ? extractedCity
              : (criteria != null ? criteria.getCity() : "Debrecen");

      String title = cleanTitle(cardText, actualCity, id);

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

      String imageUrl = PropertyParser.extractImageUrl(a);
      if (imageUrl == null && a.parent() != null) {
        imageUrl = PropertyParser.extractImageUrl(a.parent());
      }
      item.setImageUrl(imageUrl);

      if (criteria != null && !criteria.matches(item)) {
        continue;
      }

      items.add(item);
    }

    return items;
  }

  public String cleanTitle(String text, String defaultCity, String id) {
    if (text != null && text.contains("|")) {
      String[] parts = text.split("\\|");
      if (parts.length > 0 && !parts[0].isBlank()) {
        return parts[0].replace("Új ", "").trim();
      }
    }
    return defaultCity + " - Eladó ingatlan (" + id + ")";
  }
}
