package ingatlanok.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.dh.DunaHouseParser;
import ingatlanok.portal.dh.DunaHousePortalEngine;
import ingatlanok.portal.dh.DunaHouseUrlBuilder;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarJsonParser;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarPortalEngine;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarQueryBuilder;
import ingatlanok.portal.ingatlancom.IngatlanComParser;
import ingatlanok.portal.ingatlancom.IngatlanComPortalEngine;
import ingatlanok.portal.ingatlancom.IngatlanComUrlBuilder;
import ingatlanok.portal.jofogas.JofogasParser;
import ingatlanok.portal.jofogas.JofogasUrlBuilder;
import ingatlanok.portal.koltozzbe.KoltozzbeParser;
import ingatlanok.portal.koltozzbe.KoltozzbePortalEngine;
import ingatlanok.portal.koltozzbe.KoltozzbeUrlBuilder;
import ingatlanok.portal.oc.OtthonCentrumParser;
import ingatlanok.portal.oc.OtthonCentrumPortalEngine;
import ingatlanok.portal.oc.OtthonCentrumUrlBuilder;

class PortalAdaptersModularTest {

  @Test
  @DisplayName("Duna House UrlBuilder és Parser egységteszt offline fixture adatokkal (#205)")
  void testDunaHouseModular() {
    DunaHouseUrlBuilder urlBuilder = new DunaHouseUrlBuilder();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType("lakas");
    criteria.setMinPriceMillion(40);
    criteria.setMaxPriceMillion(60);
    criteria.setMinArea(50);
    criteria.setMaxArea(120);

    String url = urlBuilder.buildSearchUrl(criteria);
    assertEquals(
        "https://dh.hu/elado-ingatlan/lakas/hajdu-bihar-megye/debrecen/-/40-60-mFt/50-120-m2",
        url);
    assertEquals(url, urlBuilder.buildPageUrl(url, 1));
    assertEquals(url + "/oldal-2", urlBuilder.buildPageUrl(url, 2));

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
  @DisplayName("Otthon Centrum UrlBuilder és Parser egységteszt offline fixture adatokkal (#207)")
  void testOtthonCentrumModular() {
    OtthonCentrumUrlBuilder urlBuilder = new OtthonCentrumUrlBuilder();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType("haz");
    criteria.setMinPriceMillion(60);
    criteria.setMaxPriceMillion(100);
    criteria.setMinArea(90);
    criteria.setMaxArea(200);

    String url = urlBuilder.buildSearchUrl(criteria);
    assertEquals(
        "https://www.oc.hu/ingatlanok/lista/jelleg:haz;ertekesites:elado;ar:60~100;elhelyezkedes:debrecen;meret:90~200",
        url);

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
    criteria.setMinArea(40);
    criteria.setMaxArea(100);

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
  @DisplayName("Ingatlanbazár QueryBuilder és JsonParser egységteszt offline JSON adatokkal (#203)")
  void testIngatlanBazarModular() {
    IngatlanBazarQueryBuilder queryBuilder = new IngatlanBazarQueryBuilder();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Hajdúszoboszló");
    criteria.setPropertyType("haz");
    criteria.setMinPriceMillion(80);
    criteria.setMaxPriceMillion(100);
    criteria.setMinArea(90);
    criteria.setMaxArea(200);

    String webUrl = queryBuilder.buildSearchUrl(criteria);
    assertEquals(
        "https://www.ingatlanbazar.hu/ingatlan-Hajduszoboszlo-Elado-Haz-80-100-millio-Ft-90m2-tol-200m2-ig"
            + "?property_location=1462&price_min=80&price_max=100&property_floor_area=90-200&property__2=2",
        webUrl);

    String apiUrl = queryBuilder.buildApiSearchUrl(criteria, 1);
    assertTrue(apiUrl.contains("/api/property-search"));
    assertTrue(apiUrl.contains("property_location=1462"));
    assertTrue(apiUrl.contains("price_min=80"));
    assertTrue(apiUrl.contains("price_max=100"));
    assertTrue(apiUrl.contains("property_floor_area=90-200"));
    assertTrue(apiUrl.contains("property__2=2"));

    String json =
        "{\n"
            + "  \"hits\": [\n"
            + "    {\n"
            + "      \"propertyId\": \"IB999\",\n"
            + "      \"seoUrl\": \"/ingatlan/hajduszoboszlo/IB999\",\n"
            + "      \"title\": \"Felújított családi ház\",\n"
            + "      \"priceHUF\": 92000000,\n"
            + "      \"area\": 115.0,\n"
            + "      \"rooms\": 4.0,\n"
            + "      \"settlement\": \"Hajdúszoboszló\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"propertyId\": \"IB998\",\n"
            + "      \"seoUrl\": \"/ingatlan/hajduszoboszlo/IB998\",\n"
            + "      \"title\": \"Túl drága ház\",\n"
            + "      \"priceHUF\": 250000000,\n"
            + "      \"area\": 115.0,\n"
            + "      \"rooms\": 4.0,\n"
            + "      \"settlement\": \"Hajdúszoboszló\"\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    IngatlanBazarJsonParser parser = new IngatlanBazarJsonParser();
    List<IngatlanItem> items =
        parser.parseItemsFromJson(json, criteria, new HashSet<>(), "2026-10-14 12:00:00");

    assertEquals(1, items.size());
    assertEquals("IB999", items.get(0).getId());
    assertEquals(92_000_000L, items.get(0).getPriceInfo().getAmountHuf());
    assertEquals(115.0, items.get(0).getDetailsInfo().getAreaSquareMeters());
  }

  @Test
  @DisplayName("Költözzbe UrlBuilder, Parser és többoldalas lapozás egységteszt (#226)")
  void testKoltozzbeModularAndPagination() {
    KoltozzbeUrlBuilder urlBuilder = new KoltozzbeUrlBuilder();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Hajdúszoboszló");
    criteria.setPropertyType("haz");
    criteria.setMinPriceMillion(80);
    criteria.setMaxPriceMillion(120);
    criteria.setMinArea(90);
    criteria.setMaxArea(200);

    String url = urlBuilder.buildSearchUrl(criteria);
    assertEquals(
        "https://koltozzbe.hu/elado-haz-hajduszoboszlo?p1=80000000&p2=120000000&a1=90&a2=200",
        url);
    assertEquals(url + "&page=2", urlBuilder.buildPageUrl(url, 2));

    String page1Html =
        "<html><head><script type=\"application/ld+json\">"
            + "{\"@context\":\"https://schema.org\",\"@type\":\"ItemList\",\"itemListElement\":[{"
            + "\"@type\":\"ListItem\",\"item\":{\"@type\":\"Product\","
            + "\"url\":\"https://koltozzbe.hu/elado-haz-hajduszoboszlo/h1002\","
            + "\"name\":\"Hajdúszoboszló eladó ház 130 m² 4 szoba\","
            + "\"offers\":{\"priceSpecification\":{\"price\":95000000}}"
            + "}}]}"
            + "</script></head><body>"
            + "<div class=\"listing-card\" data-lid=\"1001\">"
            + "  <a class=\"listing-item-link\" href=\"/elado-haz-hajduszoboszlo/h1001\" title=\"Családi ház 1\"></a>"
            + "  <div class=\"item-type h2\">Eladó ház</div>"
            + "  <div class=\"item-price\">88 M Ft</div>"
            + "  <div class=\"listing-parameters\">110 m2 • 4 szoba</div>"
            + "</div>"
            + "<ul class=\"pagination\">"
            + "  <li><a href=\"?p1=80000000&p2=120000000&a1=90&a2=200&page=2\">2</a></li>"
            + "  <li><a href=\"?p1=80000000&p2=120000000&a1=90&a2=200&page=3\">3</a></li>"
            + "</ul>"
            + "</body></html>";

    String page2Html =
        "<html><body>"
            + "<div class=\"property-box\" data-id=\"1003\">"
            + "  <a href=\"/elado-haz-hajduszoboszlo/h1003\" title=\"Prémium ház 3\"></a>"
            + "  <div class=\"price\">105 M Ft</div>"
            + "  <div class=\"property-parameters\">145 m2 • 5 szoba</div>"
            + "</div>"
            + "<ul class=\"pagination\">"
            + "  <li><a href=\"?page=1\">1</a></li>"
            + "  <li><a href=\"?page=3\">3</a></li>"
            + "</ul>"
            + "</body></html>";

    String page3Html =
        "<html><body>"
            + "<div class=\"listing-card\" data-lid=\"1004\">"
            + "  <a class=\"listing-item-link\" href=\"/elado-haz-hajduszoboszlo/h1004\""
            + " title=\"Kertvárosi ház 4\"></a>"
            + "  <div class=\"item-type h2\">112 M Ft</div>"
            + "  <div class=\"listing-parameters\">160 m2 • 4 szoba</div>"
            + "</div>"
            + "<ul class=\"pagination\">"
            + "  <li><a href=\"?page=1\">1</a></li>"
            + "  <li><a href=\"?page=2\">2</a></li>"
            + "</ul>"
            + "</body></html>";

    List<String> requestedUrls = new ArrayList<>();
    KoltozzbePortalEngine engine = new KoltozzbePortalEngine();
    PortalScrapeResult result =
        engine.scrape(
            criteria,
            reqUrl -> {
              requestedUrls.add(reqUrl);
              if (reqUrl.contains("page=2")) {
                return Jsoup.parse(page2Html, KoltozzbeUrlBuilder.BASE_URL);
              }
              if (reqUrl.contains("page=3")) {
                return Jsoup.parse(page3Html, KoltozzbeUrlBuilder.BASE_URL);
              }
              return Jsoup.parse(page1Html, KoltozzbeUrlBuilder.BASE_URL);
            });

    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    assertEquals(3, requestedUrls.size(), "Mind a 3 oldalt le kell töltenie, majd megállnia");
    assertEquals(
        4,
        result.getItems().size(),
        "Kártyák és JSON-LD egyaránt feldolgozásra kerülnek");
  }

  @Test
  @DisplayName("Ingatlan.com UrlBuilder, méretszűrő sorrend és bot challenge detektálás (#209)")
  void testIngatlanComModular() {
    IngatlanComUrlBuilder urlBuilder = new IngatlanComUrlBuilder();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType("lakas");
    criteria.setMinPriceMillion(50);
    criteria.setMaxPriceMillion(75);
    criteria.setMinArea(60);
    criteria.setMaxArea(120);

    String url = urlBuilder.buildSearchUrl(criteria);
    assertEquals("https://ingatlan.com/lista/elado+lakas+60-120-m2+50-75-mFt+debrecen", url);

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

  @Test
  @DisplayName("PortalSupport, részleges szűrőhatárok és hibatűrő fallback URL tesztek")
  void testPortalSupportAndBoundaryFilters() {
    Set<String> processed = new HashSet<>();
    assertFalse(PortalSupport.markProcessed(processed, ""));
    assertTrue(PortalSupport.markProcessed(processed, "https://dh.hu/ingatlan/1"));
    assertFalse(PortalSupport.markProcessed(processed, "https://dh.hu/ingatlan/1"));
    assertTrue(PortalSupport.markProcessed(null, "https://dh.hu/ingatlan/2"));

    assertEquals("Hajduszoboszlo", PortalSupport.resolveAsciiCapitalizedCity("Hajdúszoboszló"));
    assertEquals("Debrecen", PortalSupport.resolveAsciiCapitalizedCity("   "));
    assertEquals("https://dh.hu/a/b", PortalSupport.resolveUrl("https://dh.hu/", "/a/b"));
    assertEquals("https://cdn.test/img.jpg", PortalSupport.resolveUrl("https://dh.hu", "//cdn.test/img.jpg"));
    assertNull(PortalSupport.extractImageWithFallback(null, "https://dh.hu"));

    SearchCriteria minOnly = new SearchCriteria();
    minOnly.setCity("Hajdúszoboszló");
    minOnly.setPropertyType("telek");
    minOnly.setMinPriceMillion(20);
    minOnly.setMaxPriceMillion(null);
    minOnly.setMinArea(500);
    minOnly.setMaxArea(null);

    assertEquals(
        "https://dh.hu/elado-ingatlan/telek/hajdu-bihar-megye/hajduszoboszlo/-/20-mFt-tol/500--m2",
        new DunaHouseUrlBuilder().buildSearchUrl(minOnly));
    assertEquals(
        "https://www.oc.hu/ingatlanok/lista/jelleg:telek;ertekesites:elado;ar:20~;elhelyezkedes:hajduszoboszlo;meret:500~",
        new OtthonCentrumUrlBuilder().buildSearchUrl(minOnly));
    assertEquals(
        "https://ingatlan.com/lista/elado+telek+500-m2-tol+20-mFt-tol+hajduszoboszlo",
        new IngatlanComUrlBuilder().buildSearchUrl(minOnly));
    assertEquals(
        "https://www.ingatlanbazar.hu/ingatlan-Hajduszoboszlo-Elado-Telek-20-millio-Ft-tol-500m2-tol"
            + "?property_location=1462&price_min=20&property_floor_area=500-&property__2=5",
        new IngatlanBazarQueryBuilder().buildSearchUrl(minOnly));

    SearchCriteria maxOnly = new SearchCriteria();
    maxOnly.setCity("Hajdúszoboszló");
    maxOnly.setPropertyType("lakas");
    maxOnly.setMinPriceMillion(null);
    maxOnly.setMaxPriceMillion(60);
    maxOnly.setMinArea(null);
    maxOnly.setMaxArea(80);

    assertEquals(
        "https://dh.hu/elado-ingatlan/lakas/hajdu-bihar-megye/hajduszoboszlo/-/60-mFt-ig/-80-m2",
        new DunaHouseUrlBuilder().buildSearchUrl(maxOnly));
    assertEquals(
        "https://www.oc.hu/ingatlanok/lista/jelleg:lakas;ertekesites:elado;ar:~60;elhelyezkedes:hajduszoboszlo;meret:~80",
        new OtthonCentrumUrlBuilder().buildSearchUrl(maxOnly));
    assertEquals(
        "https://ingatlan.com/lista/elado+lakas+80-m2-ig+60-mFt-ig+hajduszoboszlo",
        new IngatlanComUrlBuilder().buildSearchUrl(maxOnly));
    assertEquals(
        "https://www.ingatlanbazar.hu/ingatlan-Hajduszoboszlo-Elado-Lakas-60-millio-Ft-ig-80m2-ig"
            + "?property_location=1462&price_max=60&property_floor_area=-80&property__2=3",
        new IngatlanBazarQueryBuilder().buildSearchUrl(maxOnly));

    PortalScrapeResult dhEmpty = new DunaHousePortalEngine().scrape(minOnly, u -> null);
    assertEquals(PortalScrapeStatus.EMPTY, dhEmpty.getStatus());
    assertTrue(dhEmpty.getFallbackUrl().contains("hajdu-bihar-megye/hajduszoboszlo"));

    PortalScrapeResult ocFail = new OtthonCentrumPortalEngine().scrape(minOnly, u -> null);
    assertEquals(PortalScrapeStatus.FAILED, ocFail.getStatus());
    assertTrue(ocFail.getFallbackUrl().contains("/ingatlanok/lista/"));

    PortalScrapeResult ibFail = new IngatlanBazarPortalEngine().scrape(minOnly, u -> null);
    assertEquals(PortalScrapeStatus.FAILED, ibFail.getStatus());
    assertTrue(ibFail.getFallbackUrl().contains("property_location=1462"));

    PortalScrapeResult icBot = new IngatlanComPortalEngine().scrape(minOnly, u -> null);
    assertEquals(PortalScrapeStatus.BOT_PROTECTED, icBot.getStatus());
    assertEquals(
        "https://ingatlan.com/lista/elado+telek+500-m2-tol+20-mFt-tol+hajduszoboszlo",
        icBot.getFallbackUrl());
  }

  @Test
  @DisplayName("Fázis 0: Offline fixture fájlok integritásának és parsereinek ellenőrzése")
  void testOfflineFixturesIntegrity() throws Exception {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType("lakas");
    criteria.setMinPriceMillion(null);
    criteria.setMaxPriceMillion(null);
    criteria.setMinArea(null);
    criteria.setMaxArea(null);

    // Duna House fixture
    String dhHtml = new String(
        getClass().getResourceAsStream("/fixtures/dh/sample-dh.html").readAllBytes());
    Document dhDoc = Jsoup.parse(dhHtml, DunaHouseUrlBuilder.BASE_URL);
    List<IngatlanItem> dhItems = new DunaHouseParser().parseItems(
        dhDoc, criteria, "2026-10-14 12:00:00", new HashSet<>());
    assertEquals(2, dhItems.size());
    assertEquals("12345", dhItems.get(0).getId());

    // Otthon Centrum fixture
    SearchCriteria ocCriteria = new SearchCriteria();
    ocCriteria.setCity("Debrecen");
    ocCriteria.setPropertyType("haz");
    ocCriteria.setMinPriceMillion(null);
    ocCriteria.setMaxPriceMillion(null);
    ocCriteria.setMinArea(null);
    ocCriteria.setMaxArea(null);
    String ocHtml = new String(
        getClass().getResourceAsStream("/fixtures/oc/sample-oc.html").readAllBytes());
    Document ocDoc = Jsoup.parse(ocHtml, OtthonCentrumUrlBuilder.BASE_URL);
    List<IngatlanItem> ocItems = new OtthonCentrumParser().parseItems(
        ocDoc, ocCriteria, "2026-10-14 12:00:00", new HashSet<>());
    assertEquals(2, ocItems.size());
    assertEquals("H45678", ocItems.get(0).getId());

    // Jófogás fixture
    String jfHtml = new String(
        getClass().getResourceAsStream("/fixtures/jofogas/sample-jofogas.html").readAllBytes());
    Document jfDoc = Jsoup.parse(jfHtml, JofogasUrlBuilder.BASE_URL);
    List<IngatlanItem> jfItems = new JofogasParser().parseItems(
        jfDoc, criteria, "2026-10-14 12:00:00", new HashSet<>());
    assertEquals(2, jfItems.size());
    assertEquals("12345678", jfItems.get(0).getId());

    // Ingatlanbazár fixture
    SearchCriteria ibCriteria = new SearchCriteria();
    ibCriteria.setCity("Hajdúszoboszló");
    ibCriteria.setPropertyType("haz");
    ibCriteria.setMinPriceMillion(null);
    ibCriteria.setMaxPriceMillion(null);
    ibCriteria.setMinArea(null);
    ibCriteria.setMaxArea(null);
    String ibJson = new String(
        getClass().getResourceAsStream(
            "/fixtures/ingatlanbazar/sample-ingatlanbazar.json").readAllBytes());
    List<IngatlanItem> ibItems = new IngatlanBazarJsonParser().parseItemsFromJson(
        ibJson, ibCriteria, new HashSet<>(), "2026-10-14 12:00:00");
    assertEquals(2, ibItems.size());
    assertEquals("IB999", ibItems.get(0).getId());

    // Költözzbe fixture
    String kbHtml = new String(
        getClass().getResourceAsStream("/fixtures/koltozzbe/sample-koltozzbe.html").readAllBytes());
    Document kbDoc = Jsoup.parse(kbHtml, KoltozzbeUrlBuilder.BASE_URL);
    List<IngatlanItem> kbItems = new KoltozzbeParser().parseItems(
        kbDoc, ibCriteria, "2026-10-14 12:00:00", new HashSet<>());
    assertEquals(2, kbItems.size());

    // Ingatlan.com fixture
    String icHtml = new String(
        getClass().getResourceAsStream("/fixtures/ingatlancom/sample-ingatlancom.html").readAllBytes());
    Document icDoc = Jsoup.parse(icHtml, IngatlanComUrlBuilder.BASE_URL);
    List<IngatlanItem> icItems = new IngatlanComParser().parseItems(
        icDoc, criteria, "2026-10-14 12:00:00", new HashSet<>());
    assertEquals(1, icItems.size());
    assertEquals("12345678", icItems.get(0).getId());
  }
}
