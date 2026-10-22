package ingatlanok.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.dh.DunaHouseParser;
import ingatlanok.portal.dh.DunaHouseUrlBuilder;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarJsonParser;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarQueryBuilder;
import ingatlanok.portal.ingatlancom.IngatlanComParser;
import ingatlanok.portal.ingatlancom.IngatlanComUrlBuilder;
import ingatlanok.portal.jofogas.JofogasParser;
import ingatlanok.portal.jofogas.JofogasUrlBuilder;
import ingatlanok.portal.koltozzbe.KoltozzbeParser;
import ingatlanok.portal.koltozzbe.KoltozzbeUrlBuilder;
import ingatlanok.portal.oc.OtthonCentrumParser;
import ingatlanok.portal.oc.OtthonCentrumUrlBuilder;

class PortalAdaptersModularTest {

  @Test
  @DisplayName("Duna House UrlBuilder és Parser egységteszt offline fixture adatokkal")
  void testDunaHouseModular() {
    DunaHouseUrlBuilder urlBuilder = new DunaHouseUrlBuilder();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType("lakas");
    criteria.setMinPriceMillion(40);
    criteria.setMaxPriceMillion(60);

    String url = urlBuilder.buildSearchUrl(criteria);
    assertTrue(url.contains("/elado-ingatlan/lakas/"));
    assertTrue(url.contains("40-60-mFt"));

    String html =
        "<html><body>"
            + "<a href=\"/ingatlan/elado/lakas/debrecen/12345\">"
            + "  <img src=\"/images/dh123.jpg\" />"
            + "  <div>Új építésű lakás | Debrecen Belváros 52 M Ft 65 m2 2 szoba</div>"
            + "</a>"
            + "</body></html>";
    Document doc = Jsoup.parse(html, DunaHouseUrlBuilder.BASE_URL);

    DunaHouseParser parser = new DunaHouseParser();
    List<IngatlanItem> items =
        parser.parseItems(doc, criteria, "2026-10-14 12:00:00", new HashSet<>());

    assertEquals(1, items.size());
    assertEquals("12345", items.get(0).getId());
    assertEquals(52_000_000L, items.get(0).getPriceInfo().getAmountHuf());
    assertEquals("https://dh.hu/images/dh123.jpg", items.get(0).getImageUrl());
    assertTrue(items.get(0).isNewConstruction());
  }

  @Test
  @DisplayName("Otthon Centrum UrlBuilder és Parser egységteszt offline fixture adatokkal")
  void testOtthonCentrumModular() {
    OtthonCentrumUrlBuilder urlBuilder = new OtthonCentrumUrlBuilder();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType("haz");

    String url = urlBuilder.buildSearchUrl(criteria);
    assertTrue(url.contains("/elado/haz/debrecen"));

    String html =
        "<html><body>"
            + "<div class=\"real-estate-card\">"
            + "  <a href=\"/ingatlanok/H45678\">"
            + "    <div class=\"collapse description\"><h6>Csodás családi ház</h6></div>"
            + "    <span>75 M Ft</span>"
            + "    <span>120 m2</span>"
            + "  </a>"
            + "</div>"
            + "</body></html>";
    Document doc = Jsoup.parse(html, OtthonCentrumUrlBuilder.BASE_URL);

    OtthonCentrumParser parser = new OtthonCentrumParser();
    List<IngatlanItem> items =
        parser.parseItems(doc, criteria, "2026-10-14 12:00:00", new HashSet<>());

    assertEquals(1, items.size());
    assertEquals("H45678", items.get(0).getId());
    assertEquals(75_000_000L, items.get(0).getPriceInfo().getAmountHuf());
  }

  @Test
  @DisplayName("Jófogás UrlBuilder és Parser egységteszt offline fixture adatokkal")
  void testJofogasModular() {
    JofogasUrlBuilder urlBuilder = new JofogasUrlBuilder();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType("lakas");
    criteria.setMinPriceMillion(30);

    String url = urlBuilder.buildSearchUrl(criteria);
    assertTrue(url.contains("/hajdu-bihar/debrecen/lakas"));
    assertTrue(url.contains("min_price=30000000"));
    assertTrue(url.contains("st=s"));

    String html =
        "<html><body>"
            + "<a href=\"https://ingatlan.jofogas.hu/hajdu_bihar/Debrecen_lakas_12345678.htm\">"
            + "  Debrecen Belvárosi tégla lakás 48 M Ft 55 m2 2 szoba"
            + "</a>"
            + "</body></html>";
    Document doc = Jsoup.parse(html, JofogasUrlBuilder.BASE_URL);

    JofogasParser parser = new JofogasParser();
    List<IngatlanItem> items =
        parser.parseItems(doc, criteria, "2026-10-14 12:00:00", new HashSet<>());

    assertEquals(1, items.size());
    assertEquals("12345678", items.get(0).getId());
  }

  @Test
  @DisplayName("Ingatlanbazár QueryBuilder és JsonParser egységteszt offline JSON adatokkal")
  void testIngatlanBazarModular() {
    IngatlanBazarQueryBuilder queryBuilder = new IngatlanBazarQueryBuilder();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setMinPriceMillion(40);
    criteria.setMaxPriceMillion(60);

    String apiUrl = queryBuilder.buildApiSearchUrl(criteria, 1);
    assertTrue(apiUrl.contains("/api/property-search"));
    assertTrue(apiUrl.contains("price_min=40"));
    assertTrue(apiUrl.contains("price_max=60"));

    String json =
        "{\n"
            + "  \"hits\": [\n"
            + "    {\n"
            + "      \"propertyId\": \"IB999\",\n"
            + "      \"seoUrl\": \"/ingatlan/debrecen/IB999\",\n"
            + "      \"title\": \"Felújított panellakás\",\n"
            + "      \"priceHUF\": 45000000,\n"
            + "      \"area\": 54.0,\n"
            + "      \"rooms\": 2.0,\n"
            + "      \"settlement\": \"Debrecen\"\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    IngatlanBazarJsonParser parser = new IngatlanBazarJsonParser();
    List<IngatlanItem> items =
        parser.parseItemsFromJson(json, criteria, new HashSet<>(), "2026-10-14 12:00:00");

    assertEquals(1, items.size());
    assertEquals("IB999", items.get(0).getId());
    assertEquals(45_000_000L, items.get(0).getPriceInfo().getAmountHuf());
    assertEquals(54.0, items.get(0).getDetailsInfo().getAreaSquareMeters());
  }

  @Test
  @DisplayName("Költözzbe UrlBuilder és Parser egységteszt offline fixture adatokkal")
  void testKoltozzbeModular() {
    KoltozzbeUrlBuilder urlBuilder = new KoltozzbeUrlBuilder();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType("haz");
    criteria.setMinPriceMillion(80);
    criteria.setMaxPriceMillion(100);
    criteria.setMinArea(90);
    criteria.setMaxArea(200);

    String url = urlBuilder.buildSearchUrl(criteria);
    assertTrue(url.contains("/elado-haz-debrecen"));
    assertTrue(url.contains("p1=80000000"));
    assertTrue(url.contains("p2=100000000"));
    assertTrue(url.contains("a1=90"));
    assertTrue(url.contains("a2=200"));

    String html =
        "<html><body><div class=\"listing-card\" data-lid=\"112233\">  <a"
            + " class=\"listing-item-link\" href=\"/elado-haz-debrecen/h112233\" title=\"Családi"
            + " ház Debrecen\"></a>  <div class=\"item-type h2\">82 M Ft</div>  <div"
            + " class=\"listing-parameters\">110 m2 • 4 szoba</div></div></body></html>";
    Document doc = Jsoup.parse(html, KoltozzbeUrlBuilder.BASE_URL);

    KoltozzbeParser parser = new KoltozzbeParser();
    List<IngatlanItem> items =
        parser.parseItems(doc, criteria, "2026-10-14 12:00:00", new HashSet<>());

    assertEquals(1, items.size());
    assertEquals("112233", items.get(0).getId());
    assertEquals(82_000_000L, items.get(0).getPriceInfo().getAmountHuf());
  }

  @Test
  @DisplayName("Ingatlan.com UrlBuilder és bot challenge detektálás")
  void testIngatlanComModular() {
    IngatlanComUrlBuilder urlBuilder = new IngatlanComUrlBuilder();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setMinPriceMillion(50);
    criteria.setMaxPriceMillion(75);

    String url = urlBuilder.buildSearchUrl(criteria);
    assertTrue(url.contains("/lista/elado+lakas+debrecen+50-75-mFt"));

    IngatlanComParser parser = new IngatlanComParser();
    Document normalDoc =
        Jsoup.parse("<html><head><title>Eladó lakások Debrecen</title></head><body></body></html>");
    assertFalse(parser.isBotChallenge(normalDoc));

    Document cloudflareDoc =
        Jsoup.parse(
            "<html><head><title>Just a moment... Cloudflare</title></head><body><div"
                + " id=\"challenge-error-text\">Error</div></body></html>");
    assertTrue(parser.isBotChallenge(cloudflareDoc));

    String cardsHtml =
        "<html><body>"
            + "<div class=\"listing-card\" data-id=\"999\">"
            + "  <a href=\"/ingatlan/999888\">"
            + "    <h2 class=\"listing-card__title\">Új építésű lakás Debrecen</h2>"
            + "    <div class=\"listing-card__price\">62 M Ft</div>"
            + "    <div>95 m² 3 szoba</div>"
            + "    <img data-src=\"https://ingatlan.com/img/999.jpg\" />"
            + "  </a>"
            + "</div>"
            + "</body></html>";
    List<IngatlanItem> parsedCards =
        parser.parseItems(
            Jsoup.parse(cardsHtml, IngatlanComUrlBuilder.BASE_URL),
            criteria,
            "2026-10-14 12:00:00",
            new HashSet<>());
    assertEquals(1, parsedCards.size());
    assertEquals("999888", parsedCards.get(0).getId());
    assertTrue(parsedCards.get(0).isNewConstruction());
    assertEquals("https://ingatlan.com/img/999.jpg", parsedCards.get(0).getImageUrl());
  }

  @Test
  @DisplayName(
      "Költözzbe JSON-LD és OtthonCentrum SEO JSON attribútum elemzés új építésű és képadatokkal")
  void testKoltozzbeJsonLdAndOcSeoParsing() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setMinPriceMillion(40);
    criteria.setMaxPriceMillion(120);
    criteria.setMinArea(40);
    criteria.setMaxArea(180);

    String jsonLdHtml =
        "<html><head><script type=\"application/ld+json\">"
            + "{\"@context\":\"https://schema.org\",\"@type\":\"ItemList\",\"itemListElement\":[{"
            + "\"@type\":\"ListItem\",\"item\":{\"@type\":\"Product\","
            + "\"url\":\"https://koltozzbe.hu/elado-lakas-debrecen/h554433\","
            + "\"name\":\"Debrecen új építésű 80 m² 3 szoba lakás\","
            + "\"image\":[\"https://koltozzbe.hu/img/554433.jpg\"],"
            + "\"offers\":{\"priceSpecification\":{\"price\":85000000}}"
            + "}}]}"
            + "</script></head><body></body></html>";
    KoltozzbeParser kbParser = new KoltozzbeParser();
    List<IngatlanItem> kbItems =
        kbParser.parseItems(
            Jsoup.parse(jsonLdHtml, KoltozzbeUrlBuilder.BASE_URL),
            criteria,
            "2026-10-18",
            new HashSet<>());
    assertEquals(1, kbItems.size());
    assertEquals("554433", kbItems.get(0).getId());
    assertTrue(kbItems.get(0).isNewConstruction());
    assertEquals("https://koltozzbe.hu/img/554433.jpg", kbItems.get(0).getImageUrl());

    String ocSeoHtml =
        "<html><body>"
            + "<div class=\"js-list-item\" data-controller=\"seo\" "
            + "data-seo-select-item-value='{\"ecommerce\":{\"items\":["
            + "{\"location_city\":\"Debrecen\",\"price\":78000000}]}}'>"
            + "  <a href=\"/ingatlanok/OC777\">85 m² 3 szoba Szerkezetkész új otthon</a>"
            + "  <img class=\"lazyload\" data-src=\"/uploads/oc777.jpg\" />"
            + "</div>"
            + "</body></html>";
    OtthonCentrumParser ocParser = new OtthonCentrumParser();
    List<IngatlanItem> ocItems =
        ocParser.parseItems(
            Jsoup.parse(ocSeoHtml, OtthonCentrumUrlBuilder.BASE_URL),
            criteria,
            "2026-10-18",
            new HashSet<>());
    assertEquals(1, ocItems.size());
    assertEquals("OC777", ocItems.get(0).getId());
    assertTrue(ocItems.get(0).isNewConstruction());
    assertEquals("https://www.oc.hu/uploads/oc777.jpg", ocItems.get(0).getImageUrl());
  }
}
