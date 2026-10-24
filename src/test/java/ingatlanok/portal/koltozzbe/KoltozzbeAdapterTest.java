package ingatlanok.portal.koltozzbe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.AdapterPortalEngine;

class KoltozzbeAdapterTest {

  @Test
  @DisplayName("KoltozzbeRawParser kinyeri a JSON-LD és HTML nyers hirdetéseket")
  void testKoltozzbeRawParser() throws Exception {
    String html = new String(
        getClass().getResourceAsStream("/fixtures/koltozzbe/sample-koltozzbe.html").readAllBytes());
    Document doc = Jsoup.parse(html, KoltozzbeUrlBuilder.BASE_URL);

    KoltozzbeRawParser rawParser = new KoltozzbeRawParser();
    List<RawPropertyListing> jsonLdListings = rawParser.parseJsonLdListings(doc);
    List<RawPropertyListing> htmlListings = rawParser.parseHtmlListings(doc);

    assertEquals(1, jsonLdListings.size());
    assertEquals("1002", jsonLdListings.get(0).nativeId());
    assertEquals(95_000_000L, jsonLdListings.get(0).priceHuf());
    assertEquals(ExtractionMethod.JSON_LD, jsonLdListings.get(0).method());

    assertEquals(1, htmlListings.size());
    assertEquals("1001", htmlListings.get(0).nativeId());
    assertEquals(ExtractionMethod.HTML, htmlListings.get(0).method());
  }

  @Test
  @DisplayName("KoltozzbePortalAdapter + AdapterPortalEngine stratégia-lánc kinyerés")
  void testKoltozzbeAdapterStrategyChain() throws Exception {
    String html = new String(
        getClass().getResourceAsStream("/fixtures/koltozzbe/sample-koltozzbe.html").readAllBytes());
    Document doc = Jsoup.parse(html, KoltozzbeUrlBuilder.BASE_URL);

    KoltozzbeJsonLdStrategy jsonLdStrategy = new KoltozzbeJsonLdStrategy(
        new KoltozzbeUrlBuilder(),
        new KoltozzbeRawParser(),
        new KoltozzbeParser(),
        url -> doc
    );

    KoltozzbeHtmlStrategy htmlStrategy = new KoltozzbeHtmlStrategy(
        new KoltozzbeUrlBuilder(),
        new KoltozzbeRawParser(),
        new KoltozzbeParser(),
        url -> doc
    );

    KoltozzbePortalAdapter adapter = new KoltozzbePortalAdapter(
        new KoltozzbeUrlBuilder(),
        List.of(jsonLdStrategy, htmlStrategy)
    );

    AdapterPortalEngine engine = new AdapterPortalEngine(adapter);
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Hajdúszoboszló");
    criteria.setPropertyType("haz");

    PortalScrapeResult result = engine.scrape(criteria);

    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    assertEquals(1, result.getItems().size());
    assertEquals("1002", result.getItems().get(0).getId());
    assertEquals(95_000_000L, result.getItems().get(0).getPriceInfo().getAmountHuf());
  }
}
