package ingatlanok.portal.jofogas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.SearchCriteria;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;

class JofogasPortalEngineTest {

  @Test
  @DisplayName("Hivatalos Jófogás URL generálás: Hajdúszoboszló eladó ház szűrőkkel")
  void testBuildSearchUrlHajduszoboszloHaz() {
    JofogasUrlBuilder urlBuilder = new JofogasUrlBuilder();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Hajdúszoboszló");
    criteria.setPropertyType("haz");
    criteria.setMinPriceMillion(80);
    criteria.setMaxPriceMillion(100);
    criteria.setMinArea(90);
    criteria.setMaxArea(200);

    String url = urlBuilder.buildSearchUrl(criteria);

    assertEquals(
        "https://ingatlan.jofogas.hu/hajdu-bihar/hajduszoboszlo/haz?max_price=100000000&max_size=200&min_price=80000000&min_size=90&st=s",
        url);
  }

  @Test
  @DisplayName("Jófogás URL generálás Budapest esetén dedikált útvonalat használ")
  void testBuildSearchUrlBudapest() {
    JofogasUrlBuilder urlBuilder = new JofogasUrlBuilder();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Budapest");
    criteria.setPropertyType("lakas");
    criteria.setMinPriceMillion(null);
    criteria.setMaxPriceMillion(null);
    criteria.setMinArea(null);
    criteria.setMaxArea(null);

    String url = urlBuilder.buildSearchUrl(criteria);

    assertEquals("https://ingatlan.jofogas.hu/budapest/lakas?st=s", url);
  }

  @Test
  @DisplayName("Jófogás URL generálás alapértelmezett megye és város hozzárendeléssel")
  void testBuildSearchUrlDebrecenDefaults() {
    JofogasUrlBuilder urlBuilder = new JofogasUrlBuilder();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType("telek");
    criteria.setMinPriceMillion(null);
    criteria.setMaxPriceMillion(null);
    criteria.setMinArea(null);
    criteria.setMaxArea(null);

    String url = urlBuilder.buildSearchUrl(criteria);

    assertEquals("https://ingatlan.jofogas.hu/hajdu-bihar/debrecen/telek?st=s", url);
  }


  @Test
  @DisplayName("JofogasPortalEngine determinisztikus mock dokumentummal")
  void testEngineScrapeWithMockFetcher() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Hajdúszoboszló");
    criteria.setPropertyType("haz");
    criteria.setMinPriceMillion(80);
    criteria.setMaxPriceMillion(100);
    criteria.setMinArea(90);
    criteria.setMaxArea(200);

    String html =
        "<html><body>"
            + "<a href=\"https://ingatlan.jofogas.hu/hajdu_bihar/Hajduszoboszlo_haz_12345678.htm\">"
            + "  Hajdúszoboszló eladó családi ház 95 M Ft 150 m2 4 szoba"
            + "</a>"
            + "</body></html>";
    Document doc = Jsoup.parse(html, JofogasUrlBuilder.BASE_URL);

    JofogasPortalEngine engine = new JofogasPortalEngine();
    PortalScrapeResult result = engine.scrape(criteria, url -> doc);

    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    assertEquals("jofogas", result.getPortalId());
    assertEquals(1, result.getItems().size());
    assertEquals("12345678", result.getItems().get(0).getId());
    assertEquals(95_000_000L, result.getItems().get(0).getPriceInfo().getAmountHuf());
  }

  @Test
  @DisplayName("Null keresési feltételek esetén kivételt dob")
  void testBuildSearchUrlRejectsNullCriteria() {
    JofogasUrlBuilder urlBuilder = new JofogasUrlBuilder();
    assertThrows(IllegalArgumentException.class, () -> urlBuilder.buildSearchUrl(null));
  }

  @Test
  @DisplayName("Null vagy üres város esetén kivételt dob és nem állít be hamis várost")
  void testBuildSearchUrlRejectsBlankCity() {
    JofogasUrlBuilder urlBuilder = new JofogasUrlBuilder();
    SearchCriteria nullCityCriteria =
        new SearchCriteria() {
          @Override
          public String getCity() {
            return null;
          }
        };
    assertThrows(IllegalArgumentException.class, () -> urlBuilder.buildSearchUrl(nullCityCriteria));

    SearchCriteria blankCityCriteria =
        new SearchCriteria() {
          @Override
          public String getCity() {
            return "   ";
          }
        };
    assertThrows(IllegalArgumentException.class, () -> urlBuilder.buildSearchUrl(blankCityCriteria));
  }

  @Test
  @DisplayName("Engine scrape null criteria esetén hibát ad vissza összeomlás nélkül")
  void testEngineScrapeNullCriteriaReturnsFailure() {
    JofogasPortalEngine engine = new JofogasPortalEngine();
    PortalScrapeResult result = engine.scrape(null, url -> null);

    assertEquals(PortalScrapeStatus.FAILED, result.getStatus());
  }
}
