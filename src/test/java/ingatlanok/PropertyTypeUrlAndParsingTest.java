package ingatlanok;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.dh.DunaHouseUrlBuilder;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarQueryBuilder;
import ingatlanok.portal.ingatlancom.IngatlanComUrlBuilder;
import ingatlanok.portal.jofogas.JofogasUrlBuilder;
import ingatlanok.portal.oc.OtthonCentrumUrlBuilder;

class PropertyTypeUrlAndParsingTest {

  private final DunaHouseUrlBuilder dhScraper = new DunaHouseUrlBuilder();
  private final OtthonCentrumUrlBuilder ocScraper = new OtthonCentrumUrlBuilder();
  private final JofogasUrlBuilder jfScraper = new JofogasUrlBuilder();
  private final IngatlanComUrlBuilder icScraper = new IngatlanComUrlBuilder();
  private final IngatlanBazarQueryBuilder ibScraper = new IngatlanBazarQueryBuilder();

  @ParameterizedTest
  @ValueSource(strings = {"lakas", "haz", "telek"})
  void testAllPropertyTypesUrlGenerationDunaHouse(String type) {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Hajdúszoboszló");
    criteria.setPropertyType(type);
    criteria.setMinPriceMillion(80);
    criteria.setMaxPriceMillion(100);
    criteria.setMinArea(90);
    criteria.setMaxArea(200);

    String url = dhScraper.buildSearchUrl(criteria);
    assertNotNull(url);
    assertEquals(
        "https://dh.hu/elado-ingatlan/"
            + type
            + "/hajdu-bihar-megye/hajduszoboszlo/-/80-100-mFt/90-200-m2",
        url);
  }

  @ParameterizedTest
  @ValueSource(strings = {"lakas", "haz", "telek"})
  void testAllPropertyTypesUrlGenerationOtthonCentrum(String type) {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Hajdúszoboszló");
    criteria.setPropertyType(type);
    criteria.setMinPriceMillion(80);
    criteria.setMaxPriceMillion(100);
    criteria.setMinArea(90);
    criteria.setMaxArea(200);

    String url = ocScraper.buildSearchUrl(criteria);
    assertNotNull(url);
    assertEquals(
        "https://www.oc.hu/ingatlanok/lista/jelleg:"
            + type
            + ";ertekesites:elado;ar:80~100;elhelyezkedes:hajduszoboszlo;meret:90~200",
        url);
  }

  @ParameterizedTest
  @ValueSource(strings = {"lakas", "haz", "telek"})
  void testAllPropertyTypesUrlGenerationJofogas(String type) {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Hajdúszoboszló");
    criteria.setPropertyType(type);
    criteria.setMinPriceMillion(100);
    criteria.setMaxPriceMillion(120);

    String url = jfScraper.buildSearchUrl(criteria);
    assertNotNull(url);

    String expectedTypeWord =
        switch (type) {
          case "haz" -> "haz";
          case "telek" -> "telek";
          default -> "lakas";
        };
    assertTrue(
        url.startsWith("https://ingatlan.jofogas.hu/hajdu-bihar/hajduszoboszlo/" + expectedTypeWord),
        "Jófogás strukturált URL prefix hibás: " + url);
    assertTrue(
        url.contains("min_price=100000000"),
        "Jófogás URL-nek tartalmaznia kell a min árat: " + url);
    assertTrue(
        url.contains("max_price=120000000"),
        "Jófogás URL-nek tartalmaznia kell a max árat: " + url);
    assertTrue(
        url.contains("st=s"),
        "Jófogás URL-nek tartalmaznia kell az eladó szűrőt (st=s): " + url);
  }

  @ParameterizedTest
  @ValueSource(strings = {"lakas", "haz", "telek"})
  void testAllPropertyTypesUrlGenerationIngatlanCom(String type) {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Hajdúszoboszló");
    criteria.setPropertyType(type);
    criteria.setMinPriceMillion(80);
    criteria.setMaxPriceMillion(100);
    criteria.setMinArea(90);
    criteria.setMaxArea(200);

    String url = icScraper.buildSearchUrl(criteria);
    assertNotNull(url);
    assertEquals(
        "https://ingatlan.com/lista/elado+"
            + type
            + "+90-200-m2+80-100-mFt+hajduszoboszlo",
        url);
  }

  @Test
  void testIngatlanBazarOfficialUrlGenerationHajduszoboszlo() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Hajdúszoboszló");
    criteria.setPropertyType("haz");
    criteria.setMinPriceMillion(80);
    criteria.setMaxPriceMillion(100);
    criteria.setMinArea(90);
    criteria.setMaxArea(200);

    String url = ibScraper.buildSearchUrl(criteria);
    assertEquals(
        "https://www.ingatlanbazar.hu/ingatlan-Hajduszoboszlo-Elado-Haz-80-100-millio-Ft-90m2-tol-200m2-ig"
            + "?property_location=1462&price_min=80&price_max=100&property_floor_area=90-200&property__2=2",
        url);
  }

  @Test
  void testProcessingCardsForAllTypes() {

    String cardLakas = "4025 Debrecen Belváros | 2 szoba | 55 m² 45 000 000 Ft";
    PriceInfo priceLakas = PropertyParser.parsePrice(cardLakas);
    PropertyDetails detailsLakas = PropertyParser.parseDetails(cardLakas, priceLakas);
    assertEquals(45_000_000L, priceLakas.getAmountHuf());
    assertEquals(45.0, priceLakas.getAmountMillion());
    assertEquals(55.0, detailsLakas.getAreaSquareMeters());
    assertEquals(2.0, detailsLakas.getRoomCount());

    String cardHaz = "Prémium 4200 Hajdúszoboszló Kertváros | 4 szoba | 120 m² 105 000 000 Ft";
    PriceInfo priceHaz = PropertyParser.parsePrice(cardHaz);
    PropertyDetails detailsHaz = PropertyParser.parseDetails(cardHaz, priceHaz);
    assertEquals(105_000_000L, priceHaz.getAmountHuf());
    assertEquals(105.0, priceHaz.getAmountMillion());
    assertEquals(120.0, detailsHaz.getAreaSquareMeters());
    assertEquals(4.0, detailsHaz.getRoomCount());

    String cardGarazs = "Debrecen Egyetemváros eladó garázs 18 m² 6,5 M Ft";
    PriceInfo priceGarazs = PropertyParser.parsePrice(cardGarazs);
    PropertyDetails detailsGarazs = PropertyParser.parseDetails(cardGarazs, priceGarazs);
    assertEquals(6_500_000L, priceGarazs.getAmountHuf());
    assertEquals(6.5, priceGarazs.getAmountMillion());
    assertEquals(18.0, detailsGarazs.getAreaSquareMeters());

    String cardTelek = "Hajdúszoboszló belterületi építési telek 800 m² 22 000 000 Ft";
    PriceInfo priceTelek = PropertyParser.parsePrice(cardTelek);
    PropertyDetails detailsTelek = PropertyParser.parseDetails(cardTelek, priceTelek);
    assertEquals(22_000_000L, priceTelek.getAmountHuf());
    assertEquals(22.0, priceTelek.getAmountMillion());
    assertEquals(800.0, detailsTelek.getAreaSquareMeters());

    String cardNyaralo = "Balatonföldvár üdülőövezet 60 m² • 2 szoba 35,9M Ft";
    PriceInfo priceNyaralo = PropertyParser.parsePrice(cardNyaralo);
    PropertyDetails detailsNyaralo = PropertyParser.parseDetails(cardNyaralo, priceNyaralo);
    assertEquals(35_900_000L, priceNyaralo.getAmountHuf());
    assertEquals(35.9, priceNyaralo.getAmountMillion());
    assertEquals(60.0, detailsNyaralo.getAreaSquareMeters());
    assertEquals(2.0, detailsNyaralo.getRoomCount());

    String cardIroda = "Szeged Belvárosi Iroda 110 m² | 3 szoba 75 000 000 Ft";
    PriceInfo priceIroda = PropertyParser.parsePrice(cardIroda);
    PropertyDetails detailsIroda = PropertyParser.parseDetails(cardIroda, priceIroda);
    assertEquals(75_000_000L, priceIroda.getAmountHuf());
    assertEquals(75.0, priceIroda.getAmountMillion());
    assertEquals(110.0, detailsIroda.getAreaSquareMeters());
    assertEquals(3.0, detailsIroda.getRoomCount());
  }

  @Test
  void testBudapestSpecialCountyRouteHandling() {
    SearchCriteria criteriaBp = new SearchCriteria();
    criteriaBp.setCity("Budapest");
    criteriaBp.setPropertyType("lakas");

    String url = dhScraper.buildSearchUrl(criteriaBp);
    assertTrue(
        url.contains("/lakas/budapest/-/"),
        "Budapest esetén a DH URL-nek közvetlenül /budapest formátumúnak kell lennie: " + url);
  }
}
