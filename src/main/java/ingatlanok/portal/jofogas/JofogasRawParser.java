package ingatlanok.portal.jofogas;

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

public class JofogasRawParser {

  private static final Pattern ID_PATTERN = Pattern.compile("_(\\d+)\\.htm");

  public List<RawPropertyListing> parseRawListings(Document document) {
    List<RawPropertyListing> items = new ArrayList<>();
    if (document == null) {
      return items;
    }

    Elements links = document.select("a[href*=/ingatlan/], a[href*=_]");
    for (Element a : links) {
      String href = a.attr("href");
      if (href == null || href.isBlank() || !href.endsWith(".htm")) {
        continue;
      }

      String fullLink = PortalSupport.resolveUrl(JofogasUrlBuilder.BASE_URL, href);
      String cardText = a.text().replaceAll("\\s+", " ").trim();
      if (cardText.length() < 10) {
        continue;
      }

      String id = PropertyParser.extractFirstGroup(fullLink, ID_PATTERN, null);

      String title = null;
      Element heading = a.selectFirst("h1, h2, h3, h4, h5");
      if (heading != null && !heading.text().isBlank()) {
        title = heading.text().trim();
      }
      if (title == null || title.isBlank()) {
        title = cleanTitle(cardText, id);
      }

      String imageUrl = PortalSupport.extractImageWithFallback(a, JofogasUrlBuilder.BASE_URL);

      RawPropertyListing raw = RawPropertyListing.builder()
          .portalId("jofogas")
          .nativeId(id)
          .url(fullLink)
          .title(title)
          .priceText(cardText)
          .detailsText(cardText)
          .imageUrl(imageUrl)
          .descriptionText(cardText)
          .method(ExtractionMethod.HTML)
          .build();

      items.add(raw);
    }

    return items;
  }

  public String cleanTitle(String text, String id) {
    if (text != null) {
      String t = text.replaceAll("\\d+\\s*(?:M\\s*Ft|millió|Ft)", "").trim();
      t = t.replaceAll("\\d+\\s*m2", "").trim();
      t = t.replaceAll("\\d+\\s*szoba", "").trim();
      if (t.length() > 5) {
        return t.length() > 60 ? t.substring(0, 60).trim() + "..." : t;
      }
    }
    return "Jófogás ingatlan" + (id != null ? " (" + id + ")" : "");
  }
}
