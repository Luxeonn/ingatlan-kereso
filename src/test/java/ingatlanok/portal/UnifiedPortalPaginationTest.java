package ingatlanok.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import ingatlanok.portal.ingatlanbazar.IngatlanBazarApiStrategy;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarPortalEngine;
import ingatlanok.portal.jofogas.JofogasHtmlStrategy;
import ingatlanok.portal.jofogas.JofogasPortalEngine;
import ingatlanok.portal.jofogas.JofogasUrlBuilder;
import ingatlanok.portal.koltozzbe.KoltozzbePortalEngine;
import ingatlanok.portal.oc.OtthonCentrumHtmlStrategy;
import ingatlanok.portal.oc.OtthonCentrumPortalEngine;
import ingatlanok.portal.oc.OtthonCentrumUrlBuilder;
import ingatlanok.testsupport.TestFixtures;

class UnifiedPortalPaginationTest {

  private static SearchCriteria debrecenHouseCriteria() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType(PropertyType.HAZ);
    criteria.setMinPriceMillion(70);
    criteria.setMaxPriceMillion(120);
    criteria.setMinArea(90);
    criteria.setMaxArea(200);
    return criteria;
  }

  @Test
  @DisplayName("Minden portálmotor alapértelmezett maxPages értéke 20")
  void allEnginesDefaultToTwentyMaxPages() {
    assertEquals(20, IngatlanBazarApiStrategy.DEFAULT_MAX_PAGES);
    assertEquals(20, OtthonCentrumHtmlStrategy.DEFAULT_MAX_PAGES);
    assertEquals(20, JofogasHtmlStrategy.DEFAULT_MAX_PAGES);
    assertEquals(20, KoltozzbePortalEngine.DEFAULT_MAX_PAGES);
  }

  @Test
  @DisplayName("PortalSupport.hasPaginationWithoutNextPage kezeli a ?param=N és &param=N formátumokat")
  void hasPaginationWithoutNextPageHandlesEdgeCases() {
    assertFalse(PortalSupport.hasPaginationWithoutNextPage(null, "page", 2));
    Document emptyDoc = Jsoup.parse("<html><body><div>Nincs lapozó</div></body></html>");
    assertFalse(PortalSupport.hasPaginationWithoutNextPage(emptyDoc, "page", 2));
    assertFalse(PortalSupport.hasPaginationWithoutNextPage(emptyDoc, " ", 2));

    Document withPage2 = Jsoup.parse(
        "<html><body><a href='/lista?page=2'>2</a><a href='/lista?foo=3&page=3'>3</a></body></html>");
    assertFalse(PortalSupport.hasPaginationWithoutNextPage(withPage2, "page", 2));
    assertFalse(PortalSupport.hasPaginationWithoutNextPage(withPage2, "page", 3));
    assertTrue(PortalSupport.hasPaginationWithoutNextPage(withPage2, "page", 4));

    Document jofogasDoc = Jsoup.parse(
        "<html><body><a href='/hajdu-bihar/haz?st=s&o=2&min_price=70'>2</a></body></html>");
    assertFalse(PortalSupport.hasPaginationWithoutNextPage(jofogasDoc, "o", 2));
    assertTrue(PortalSupport.hasPaginationWithoutNextPage(jofogasDoc, "o", 3));
  }

  @Test
  @DisplayName("Ingatlanbazár alapértelmezett lapozással 3+ oldalt is letölt és üres oldalon megáll")
  void ingatlanBazarDefaultPaginationFetchesBeyondTwoPages() {
    IngatlanBazarPortalEngine engine = TestFixtures.ingatlanBazarEngine();
    List<String> requestedUrls = new ArrayList<>();

    PortalScrapeResult result = engine.scrape(debrecenHouseCriteria(), url -> {
      requestedUrls.add(url);
      if (url.contains("&page=1")) {
        return ibJson("IB101", "/ingatlan/haz-101", 80_000_000L);
      }
      if (url.contains("&page=2")) {
        return ibJson("IB201", "/ingatlan/haz-201", 90_000_000L);
      }
      if (url.contains("&page=3")) {
        return ibJson("IB301", "/ingatlan/haz-301", 100_000_000L);
      }
      return "{\"hits\":[]}";
    });

    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    assertEquals(3, result.getItems().size());
    assertEquals(4, requestedUrls.size());
    assertTrue(result.getTechnicalDetails().contains("stopReason=EMPTY_PAGE"));
  }

  @Test
  @DisplayName("OC URL builder és alapértelmezett lapozás: 3 oldal letöltése és NO_MORE_PAGES leállás")
  void otthonCentrumDefaultPaginationAndEarlyStop() {
    OtthonCentrumUrlBuilder urlBuilder = new OtthonCentrumUrlBuilder();
    String baseUrl = urlBuilder.buildSearchUrl(debrecenHouseCriteria());
    assertNull(urlBuilder.buildPageUrl(null, 2));
    assertEquals(baseUrl, urlBuilder.buildPageUrl(baseUrl, 1));
    assertEquals(baseUrl + "?page=2", urlBuilder.buildPageUrl(baseUrl, 2));
    assertEquals(baseUrl + "?sort=asc&page=2", urlBuilder.buildPageUrl(baseUrl + "?sort=asc", 2));

    OtthonCentrumPortalEngine engine = TestFixtures.ocEngine();
    List<String> requestedUrls = new ArrayList<>();

    PortalScrapeResult result = engine.scrape(debrecenHouseCriteria(), url -> {
      requestedUrls.add(url);
      if (baseUrl.equals(url)) {
        return Jsoup.parse(
            ocHtml("H101", 80_000_000L, "<a href='/lista?page=2'>2</a>"), url);
      }
      if ((baseUrl + "?page=2").equals(url)) {
        return Jsoup.parse(
            ocHtml("H201", 90_000_000L, "<a href='/lista?page=3'>3</a>"), url);
      }
      if ((baseUrl + "?page=3").equals(url)) {
        return Jsoup.parse(
            ocHtml("H301", 100_000_000L, "<a href='/lista?page=2'>2</a>"), url);
      }
      throw new AssertionError("Nem szabadott volna lekérni: " + url);
    });

    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    assertEquals(3, result.getItems().size());
    assertEquals(3, requestedUrls.size());
    assertTrue(result.getTechnicalDetails().contains("stopReason=NO_MORE_PAGES"));
  }

  @Test
  @DisplayName("Jófogás URL builder és alapértelmezett lapozás: 3 oldal letöltése és NO_MORE_PAGES leállás")
  void jofogasDefaultPaginationAndEarlyStop() {
    JofogasUrlBuilder urlBuilder = new JofogasUrlBuilder();
    String baseUrl = urlBuilder.buildSearchUrl(debrecenHouseCriteria());
    assertNull(urlBuilder.buildPageUrl(null, 2));
    assertEquals(baseUrl, urlBuilder.buildPageUrl(baseUrl, 1));
    assertEquals(baseUrl + "&o=2", urlBuilder.buildPageUrl(baseUrl, 2));
    assertEquals("https://ingatlan.jofogas.hu/lista?o=2",
        urlBuilder.buildPageUrl("https://ingatlan.jofogas.hu/lista", 2));

    JofogasPortalEngine engine = TestFixtures.jofogasEngine();
    List<String> requestedUrls = new ArrayList<>();

    PortalScrapeResult result = engine.scrape(debrecenHouseCriteria(), url -> {
      requestedUrls.add(url);
      if (baseUrl.equals(url)) {
        return Jsoup.parse(
            jofogasHtml("101", 82, "<a href='/hajdu-bihar/debrecen/haz?st=s&o=2'>2</a>"), url);
      }
      if ((baseUrl + "&o=2").equals(url)) {
        return Jsoup.parse(
            jofogasHtml("201", 92, "<a href='/hajdu-bihar/debrecen/haz?st=s&o=3'>3</a>"), url);
      }
      if ((baseUrl + "&o=3").equals(url)) {
        return Jsoup.parse(
            jofogasHtml("301", 102, "<a href='/hajdu-bihar/debrecen/haz?st=s&o=2'>2</a>"), url);
      }
      throw new AssertionError("Nem szabadott volna lekérni: " + url);
    });

    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    assertEquals(3, result.getItems().size());
    assertEquals(3, requestedUrls.size());
    assertTrue(result.getTechnicalDetails().contains("stopReason=NO_MORE_PAGES"));
  }

  private static String ibJson(String id, String seoUrl, long priceHuf) {
    return "{\"hits\":[{\"propertyId\":\"" + id + "\",\"seoUrl\":\"" + seoUrl
        + "\",\"title\":\"Debrecen ház " + id + "\",\"priceHUF\":" + priceHuf
        + ",\"area\":120,\"rooms\":4,\"city\":\"Debrecen\"}]}";
  }

  private static String ocHtml(String id, long priceHuf, String paginationHtml) {
    String seoJson = "{\"ecommerce\":{\"items\":[{\"location_city\":\"Debrecen\",\"price\":"
        + priceHuf + "}]}}";
    return "<html><body>"
        + "<div class='js-list-item' data-controller='seo' data-seo-select-item-value='"
        + seoJson + "'><a href='/ingatlanok/" + id + "'><h6>Debrecen ház " + id
        + "</h6><span>" + (priceHuf / 1_000_000L) + " M Ft 120 m2 4 szoba</span></a></div>"
        + paginationHtml
        + "</body></html>";
  }

  private static String jofogasHtml(String id, int priceMillion, String paginationHtml) {
    return "<html><body>"
        + "<a href='https://ingatlan.jofogas.hu/hajdu-bihar/debrecen/Elado_haz_" + id + ".htm'>"
        + "<h3>Debrecen családi ház " + id + "</h3>"
        + "<span>" + priceMillion + " M Ft 120 m2 4 szoba Debrecen</span>"
        + "</a>"
        + paginationHtml
        + "</body></html>";
  }
}
