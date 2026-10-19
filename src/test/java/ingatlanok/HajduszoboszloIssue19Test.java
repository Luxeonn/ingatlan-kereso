package ingatlanok;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;
import ingatlanok.scraper.DunaHouseScraper;
import ingatlanok.scraper.JofogasScraper;
import ingatlanok.scraper.OtthonCentrumScraper;

class HajduszoboszloIssue19Test {

  @Test
  void testExactIssue19UrlsAreCorrectlyGenerated() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Hajdúszoboszló");
    criteria.setPropertyType("haz");
    criteria.setMinPriceMillion(100);
    criteria.setMaxPriceMillion(120);

    DunaHouseScraper dh = new DunaHouseScraper();
    String dhUrl = dh.buildSearchUrl(criteria);
    assertEquals(
        "https://dh.hu/elado-ingatlan/haz/hajdu-bihar/hajduszoboszlo/100-120-mFt",
        dhUrl,
        "Duna House-nak kötelező a 'hajdu-bihar/hajduszoboszlo' megyei útvonalat használnia az"
            + " országos fallback elkerüléséhez");

    OtthonCentrumScraper oc = new OtthonCentrumScraper();
    String ocUrl = oc.buildSearchUrl(criteria);
    assertEquals(
        "https://www.oc.hu/elado/haz/hajduszoboszlo",
        ocUrl,
        "Otthon Centrumnak a működő /elado/haz/hajduszoboszlo SEO útvonalat kell használnia a hibás"
            + " q= paraméter helyett");

    JofogasScraper jf = new JofogasScraper();
    String jfUrl = jf.buildSearchUrl(criteria);
    assertTrue(jfUrl.contains("min_price=100000000"));
    assertTrue(jfUrl.contains("max_price=120000000"));
    assertTrue(jfUrl.contains("Hajd%C3%BAszoboszl%C3%B3+elado+haz"));
  }

  @Test
  void testAllReportedDefectsFromIssue19AreEliminated() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Hajdúszoboszló");
    criteria.setPropertyType("haz");
    criteria.setMinPriceMillion(100);
    criteria.setMaxPriceMillion(120);

    List<IngatlanItem> mixedCandidates = new ArrayList<>();

    mixedCandidates.add(
        createItem(
            "HZ070659",
            "Duna House",
            "https://dh.hu/ingatlan/HZ070659/elado-haz-heves-megye-kisnana-falu",
            "3264 Kisnána",
            8.9,
            "Kisnána"));

    mixedCandidates.add(
        createItem(
            "HZ069153",
            "Duna House",
            "https://dh.hu/ingatlan/HZ069153/elado-haz-pest-megye-cegled-oroszlan-utca",
            "2700 Cegléd",
            85.0,
            "Cegléd"));

    mixedCandidates.add(
        createItem(
            "HZ071728",
            "Duna House",
            "https://dh.hu/ingatlan/HZ071728/elado-haz-baranya-megye-pecs-patacs",
            "7634 Pécs",
            134.0,
            "Pécs"));

    mixedCandidates.add(
        createItem(
            "M339414",
            "Duna House",
            "https://dh.hu/ingatlan/M339414/elado-haz-hajdu-bihar-megye-hajduszoboszlo-4200-hajduszoboszlo,-kertvaros",
            "4200 Hajdúszoboszló",
            37.0,
            "Hajdúszoboszló"));

    mixedCandidates.add(
        createItem(
            "HZ068015",
            "Duna House",
            "https://dh.hu/ingatlan/HZ068015/elado-haz-hajdu-bihar-megye-hajduszoboszlo-kertvaros",
            "4200 Hajdúszoboszló",
            195.0,
            "Hajdúszoboszló"));

    mixedCandidates.add(
        createItem(
            "H525239",
            "Otthon Centrum",
            "https://www.oc.hu/ingatlanok/H525239",
            "Budapest XII. kerület",
            99.0,
            "Budapest XII. kerület"));

    IngatlanItem validDh =
        createItem(
            "PR020655",
            "Duna House",
            "https://dh.hu/ingatlan/PR020655-HZ-11437/elado-haz-hajdu-bihar-megye-hajduszoboszlo-stilusos,-modern-sorhazak-hajduszoboszlon",
            "4200 Hajdúszoboszló",
            105.0,
            "Hajdúszoboszló");
    mixedCandidates.add(validDh);

    IngatlanItem validOc =
        createItem(
            "U0050375",
            "Otthon Centrum",
            "https://www.oc.hu/ingatlanok/U0050375",
            "Hajdúszoboszló, Kertváros",
            104.9,
            "Hajdúszoboszló");
    mixedCandidates.add(validOc);

    IngatlanItem validJf =
        createItem(
            "5193013",
            "Jófogás Ingatlan",
            "https://ingatlan.jofogas.hu/hajdu_bihar/Elado_Haz__Hajduszoboszlo__5193013_161704151.htm",
            "Hajdúszoboszló",
            105.0,
            "Hajdúszoboszló");
    mixedCandidates.add(validJf);

    List<IngatlanItem> filtered = mixedCandidates.stream().filter(criteria::matches).toList();

    assertEquals(
        3,
        filtered.size(),
        "Kizárólag a 3 valós és feltételeknek megfelelő hirdetés maradhat fenn!");

    for (IngatlanItem item : filtered) {
      assertTrue(
          item.getPriceInfo().getAmountMillion() >= 100.0,
          "Ár nem lehet kisebb mint 100 M Ft: " + item.getPrice());
      assertTrue(
          item.getPriceInfo().getAmountMillion() <= 120.0,
          "Ár nem lehet nagyobb mint 120 M Ft: " + item.getPrice());
      assertTrue(
          ingatlanok.common.SlugUtils.toSlug(item.getLocation()).contains("hajduszoboszlo")
              || ingatlanok.common.SlugUtils.toSlug(item.getUrl()).contains("hajduszoboszlo"),
          "Lokációnak Hajdúszoboszlónak kell lennie: " + item.getLocation());
      assertFalse(item.getLocation().contains("Kisnána"));
      assertFalse(item.getLocation().contains("Cegléd"));
      assertFalse(item.getLocation().contains("Pécs"));
      assertFalse(item.getLocation().contains("Budapest"));
    }
  }

  private IngatlanItem createItem(
      String id, String portal, String url, String title, Double priceMillion, String location) {
    Long priceHuf = priceMillion != null ? Math.round(priceMillion * 1_000_000.0) : null;
    PriceInfo priceInfo = new PriceInfo(priceMillion + " M Ft", priceHuf, priceMillion);
    PropertyDetails detailsInfo = new PropertyDetails("100 m²", 100.0, 3.0, null);
    IngatlanItem item =
        new IngatlanItem(
            id,
            PortalSource.of(portal),
            url,
            title,
            priceInfo,
            location,
            detailsInfo,
            "2026-10-13 17:00:00",
            true);
    item.setLocation(location);
    return item;
  }

  @Test
  @org.junit.jupiter.api.Tag("live")
  @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(
      named = "RUN_LIVE_TESTS",
      matches = "true")
  void testLiveSearchHajduszoboszloIssue19() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Hajdúszoboszló");
    criteria.setPropertyType("haz");
    criteria.setMinPriceMillion(100);
    criteria.setMaxPriceMillion(120);

    ingatlanok.model.SearchResult result =
        ingatlanok.service.IngatlanSearchService.getInstance().executeSearch(criteria);
    assertNotNull(result);
    for (IngatlanItem item : result.getItems()) {
      if (item.getPriceInfo() != null && item.getPriceInfo().getAmountMillion() != null) {
        assertTrue(
            item.getPriceInfo().getAmountMillion() >= 99.9,
            "Találat ára nem lehet kisebb mint 100 M Ft: " + item.getPrice());
        assertTrue(
            item.getPriceInfo().getAmountMillion() <= 120.1,
            "Találat ára nem lehet nagyobb mint 120 M Ft: " + item.getPrice());
      }
      assertFalse(
          item.getLocation().contains("Kisnána"),
          "Nem tartalmazhat Kisnánát: " + item.getLocation());
      assertFalse(
          item.getLocation().contains("Cegléd"),
          "Nem tartalmazhat Ceglédet: " + item.getLocation());
      assertFalse(
          item.getLocation().contains("Pécs"), "Nem tartalmazhat Pécset: " + item.getLocation());
      assertFalse(
          item.getLocation().contains("Budapest"),
          "Nem tartalmazhat Budapestet: " + item.getLocation());
    }
  }
}
