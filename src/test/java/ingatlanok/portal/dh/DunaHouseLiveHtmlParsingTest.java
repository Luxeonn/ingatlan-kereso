package ingatlanok.portal.dh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SearchCriteria;

class DunaHouseLiveHtmlParsingTest {

  private Document page2Doc;
  private SearchCriteria criteria;

  @BeforeEach
  void setUp() throws Exception {
    try (InputStream is = getClass().getResourceAsStream("/fixtures/dh/dh-search-page2.html")) {
      assertNotNull(is, "dh-search-page2.html fixture-nek leteznie kell");
      String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      page2Doc = Jsoup.parse(html, DunaHouseUrlBuilder.BASE_URL);
    }
    criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType(PropertyType.HAZ);
    criteria.setMinPriceMillion(70);
    criteria.setMaxPriceMillion(120);
    criteria.setMinArea(90);
    criteria.setMaxArea(200);
  }

  @Test
  @DisplayName("DH valós 2. oldali HTML: minden kártya fontos adatai (ár, terület, szoba, kép, város, URL) kinyerhetők")
  void testRealSearchPage2HtmlExtractsAllKeyFields() {
    DunaHousePortalEngine engine = new DunaHousePortalEngine();
    PortalScrapeResult result = engine.scrape(criteria, url -> page2Doc, 1);

    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    List<IngatlanItem> items = result.getItems();
    assertFalse(items.isEmpty(), "A mentett DH találati oldalnak tartalmaznia kell hirdetéseket");
    assertEquals(17, items.size(), "A DH 2. oldalon 17 egyedi hirdetésnek kell szerepelnie");

    for (IngatlanItem item : items) {
      String ctx = "ID=" + item.getId() + ", URL=" + item.getUrl() + ", title=" + item.getTitle();
      assertNotNull(item.getId(), "Hiányzó ID: " + ctx);
      assertFalse(item.getId().isBlank(), "Üres ID: " + ctx);
      assertNotNull(item.getUrl(), "Hiányzó URL: " + ctx);
      assertTrue(item.getUrl().startsWith("https://dh.hu/ingatlan/"), "Hibás URL: " + ctx);

      assertNotNull(item.getTitle(), "Hiányzó cím: " + ctx);
      assertFalse(item.getTitle().isBlank(), "Üres cím: " + ctx);
      assertFalse(item.getTitle().contains("Eladó ingatlan (" + item.getId() + ")"),
          "Fallback cím keletkezett valós hirdetés helyett: " + ctx);

      assertNotNull(item.getCity(), "Hiányzó település: " + ctx);
      assertTrue(item.getCity().contains("Debrecen"), "Település nem Debrecen (" + item.getCity() + "): " + ctx);
      assertEquals(PropertyType.HAZ, item.getPropertyType(), "Hibás ingatlantípus: " + ctx);

      assertNotNull(item.getPriceInfo(), "Hiányzó priceInfo: " + ctx);
      assertNotNull(item.getPriceInfo().getAmountHuf(), "Hiányzó ár (HUF): " + ctx);
      assertNotNull(item.getPriceInfo().getAmountMillion(), "Hiányzó ár (M Ft): " + ctx);
      double million = item.getPriceInfo().getAmountMillion();
      assertTrue(million >= 70.0 && million <= 120.0,
          "Ár kívül esik a 70-120 M Ft sávon (" + million + "): " + ctx);

      assertNotNull(item.getDetailsInfo(), "Hiányzó detailsInfo: " + ctx);
      assertNotNull(item.getDetailsInfo().getAreaSquareMeters(), "Hiányzó alapterület: " + ctx);
      assertTrue(item.getDetailsInfo().getAreaSquareMeters() >= 90.0
          && item.getDetailsInfo().getAreaSquareMeters() <= 200.0,
          "Alapterület kívül esik a 90-200 m2 sávon (" + item.getDetailsInfo().getAreaSquareMeters() + "): " + ctx);

      assertNotNull(item.getDetailsInfo().getRoomCount(), "Hiányzó szobaszám: " + ctx);
      assertTrue(item.getDetailsInfo().getRoomCount() >= 1.0, "Érvénytelen szobaszám: " + ctx);

      assertNotNull(item.getDetailsInfo().getPricePerSquareMeter(), "Hiányzó m2 ár: " + ctx);
      long expectedSqm = Math.round(item.getPriceInfo().getAmountHuf() / item.getDetailsInfo().getAreaSquareMeters());
      assertTrue(Math.abs(item.getDetailsInfo().getPricePerSquareMeter() - expectedSqm) <= 1,
          "Hibás m2 ár számítás (" + item.getDetailsInfo().getPricePerSquareMeter()
              + " vs " + expectedSqm + "): " + ctx);

      assertNotNull(item.getImageUrl(), "Hiányzó kép URL: " + ctx);
      assertTrue(item.getImageUrl().startsWith("https://"), "Kép URL nem https: " + item.getImageUrl() + " | " + ctx);
    }

    IngatlanItem first = items.stream().filter(i -> "HZ083898".equals(i.getId())).findFirst().orElseThrow();
    assertEquals(94_900_000L, first.getPriceInfo().getAmountHuf());
    assertEquals(103.0, first.getDetailsInfo().getAreaSquareMeters());
    assertEquals(3.0, first.getDetailsInfo().getRoomCount());

    IngatlanItem newProject = items.stream()
        .filter(i -> "PR058883-HZ-28925".equals(i.getId()))
        .findFirst()
        .orElseThrow();
    assertEquals(120_000_000L, newProject.getPriceInfo().getAmountHuf());
    assertEquals(111.0, newProject.getDetailsInfo().getAreaSquareMeters());
    assertEquals(4.0, newProject.getDetailsInfo().getRoomCount());
    assertTrue(newProject.isNewConstruction(), "Újépítésű projekt jelölőnek true-nak kell lennie");
  }
}
