package ingatlanok.portal.dh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SearchCriteria;

class DunaHousePaginationTest {

  private final DunaHouseUrlBuilder urlBuilder = new DunaHouseUrlBuilder();

  @Test
  @DisplayName("DH URL builder: page <= 1 esetén az alap URL marad, >= 2 esetén ?page=N vagy &page=N kerül rá")
  void testBuildPageUrlUsesQueryParameter() {
    String baseUrl = "https://dh.hu/elado-ingatlan/haz/hajdu-bihar-megye/debrecen/-/70-120-mFt/90-200-m2";
    assertEquals(baseUrl, urlBuilder.buildPageUrl(baseUrl, 1));
    assertEquals(baseUrl, urlBuilder.buildPageUrl(baseUrl, 0));
    assertEquals(baseUrl + "?page=2", urlBuilder.buildPageUrl(baseUrl, 2));
    assertEquals(baseUrl + "?page=3", urlBuilder.buildPageUrl(baseUrl, 3));

    String sortedUrl = baseUrl + "?sort=price&direction=asc";
    assertEquals(sortedUrl + "&page=2", urlBuilder.buildPageUrl(sortedUrl, 2));
    assertNull(urlBuilder.buildPageUrl(null, 2));
  }

  @Test
  @DisplayName("DH Engine: alapértelmezett lapozással 3 oldal (?page=2, ?page=3) összes hirdetését letölti")
  void testDefaultPaginationScrapesThreePagesViaPageQueryParam() {
    String baseUrl = "https://dh.hu/elado-ingatlan/haz/hajdu-bihar-megye/debrecen/-/70-120-mFt/90-200-m2";
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType(PropertyType.HAZ);
    criteria.setMinPriceMillion(70);
    criteria.setMaxPriceMillion(120);
    criteria.setMinArea(90);
    criteria.setMaxArea(200);

    List<String> requestedUrls = new ArrayList<>();
    DunaHousePortalEngine engine = new DunaHousePortalEngine();

    PortalScrapeResult result = engine.scrape(criteria, url -> {
      requestedUrls.add(url);
      if (baseUrl.equals(url)) {
        return buildDhPage(
            listingHtml("DH101", "85 M Ft", "110 m2"),
            listingHtml("DH102", "90 M Ft", "120 m2"));
      }
      if ((baseUrl + "?page=2").equals(url)) {
        return buildDhPage(
            listingHtml("DH201", "95 M Ft", "130 m2"),
            listingHtml("DH202", "100 M Ft", "140 m2"));
      }
      if ((baseUrl + "?page=3").equals(url)) {
        return buildDhPage(listingHtml("DH301", "110 M Ft", "150 m2"));
      }
      return buildDhPage();
    });

    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    assertEquals(5, result.getItems().size());
    assertTrue(requestedUrls.contains(baseUrl));
    assertTrue(requestedUrls.contains(baseUrl + "?page=2"));
    assertTrue(requestedUrls.contains(baseUrl + "?page=3"));
  }

  private static Document buildDhPage(String... cards) {
    String body = String.join("\n", cards);
    return Jsoup.parse("<html><body>" + body + "</body></html>", DunaHouseUrlBuilder.BASE_URL);
  }

  private static String listingHtml(String id, String price, String area) {
    return "<a href=\"/ingatlan/" + id + "\">"
        + "Debrecen, Eladó ház | " + price + " | " + area + " | 4 szoba"
        + "</a>";
  }
}
