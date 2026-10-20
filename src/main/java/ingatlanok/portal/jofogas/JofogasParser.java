package ingatlanok.portal.jofogas;

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

public class JofogasParser {

  private static final Pattern ID_PATTERN = Pattern.compile("_(\\d+)\\.htm");
  private static final PortalSource SOURCE = PortalSource.of("Jófogás Ingatlan");

  public List<IngatlanItem> parseItems(
      Document document, SearchCriteria criteria, String executionTime, Set<String> processedUrls) {
    List<IngatlanItem> items = new ArrayList<>();
    if (document == null) {
      return items;
    }

    Elements links = document.select("a[href*=/ingatlan/], a[href*=_]");
    for (Element a : links) {
      String href = a.attr("href");
      if (href == null || href.isBlank() || !href.endsWith(".htm")) {
        continue;
      }

      String fullLink =
          href.startsWith("http")
              ? href
              : JofogasUrlBuilder.BASE_URL + (href.startsWith("/") ? "" : "/") + href;
      if (processedUrls != null && processedUrls.contains(fullLink)) {
        continue;
      }
      if (processedUrls != null) {
        processedUrls.add(fullLink);
      }

      String cardText = a.text().replaceAll("\\s+", " ").trim();
      if (cardText.length() < 10) {
        continue;
      }

      String id =
          PropertyParser.extractFirstGroup(
              fullLink, ID_PATTERN, UUID.randomUUID().toString().substring(0, 8));
      PriceInfo priceInfo = PropertyParser.parsePrice(cardText);
      PropertyDetails detailsInfo = PropertyParser.parseDetails(cardText, priceInfo);
      String city = criteria != null ? criteria.getCity() : "Debrecen";
      String title = null;
      Element heading = a.selectFirst("h1, h2, h3, h4, h5");
      if (heading != null && !heading.text().isBlank()) {
        title = heading.text().trim();
      }
      if (title == null || title.isBlank()) {
        title = cleanTitle(cardText, city, id);
      }

      IngatlanItem item =
          new IngatlanItem(
              id, SOURCE, fullLink, title, priceInfo, city, detailsInfo, executionTime, false);
      item.setLocation(city);

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
    if (text != null) {
      String sanitized = text.replaceAll("<[^>]*>", "").trim();
      int mFtIndex = sanitized.indexOf("M Ft");
      if (mFtIndex > 0) {
        String prefix = sanitized.substring(0, mFtIndex).replaceAll("\\d+[,.]?\\d*\\s*$", "").trim();
        if (prefix.length() > 5) {
          return prefix;
        }
      }
    }
    return defaultCity + " - Jófogás ingatlan (" + id + ")";
  }
}
