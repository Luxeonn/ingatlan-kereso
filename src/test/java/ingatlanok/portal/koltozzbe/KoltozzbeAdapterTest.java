package ingatlanok.portal.koltozzbe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
import ingatlanok.portal.PortalSupport;

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

  @Test
  @DisplayName("Koltozzbe placeholder képek (nopic, app-banner) szűrése")
  void testKoltozzbePlaceholderImageFiltering() {
    String html = """
        <html>
        <head>
          <script type="application/ld+json">
          {"@context":"https://schema.org","@type":"ItemList","itemListElement":[{
            "@type":"ListItem","item":{"@type":"Product",
            "url":"https://koltozzbe.hu/elado-lakas-miskolc/h2001",
            "name":"Miskolc eladó lakás",
            "image":["/y/img/nopic_new.png"],
            "offers":{"price":22000000}
          }}]}
          </script>
        </head>
        <body>
          <div class="listing-card" data-lid="2002">
            <a class="listing-item-link" href="/elado-lakas-miskolc/h2002" title="Lakas 2">
              <div class="listing-thumbnail">
                <img data-src="https://k2.koltozzbe.hu/img/nopic_200x150.png" class="lazy">
              </div>
            </a>
          </div>
          <div class="listing-card" data-lid="2003">
            <a class="listing-item-link" href="/elado-lakas-miskolc/h2003" title="Lakas 3">
              <div class="listing-thumbnail">
                <img data-src="https://k1-cdn.koltozzbe.hu/img2/1920x1080/86/real-photo.jpg?class=th" class="lazy">
              </div>
            </a>
          </div>
        </body>
        </html>
        """;
    Document doc = Jsoup.parse(html, KoltozzbeUrlBuilder.BASE_URL);

    KoltozzbeRawParser rawParser = new KoltozzbeRawParser();
    List<RawPropertyListing> jsonLd = rawParser.parseJsonLdListings(doc);
    assertEquals(1, jsonLd.size());
    assertNull(jsonLd.get(0).imageUrl(), "A JSON-LD nopic placeholder nem maradhat meg képként");

    List<RawPropertyListing> htmlListings = rawParser.parseHtmlListings(doc);
    assertEquals(2, htmlListings.size());
    assertNull(htmlListings.get(0).imageUrl(), "A HTML kártya nopic_200x150.png placeholder képként kiszűrésre kerül");
    assertEquals("https://k1-cdn.koltozzbe.hu/img2/1920x1080/86/real-photo.jpg?class=th",
        htmlListings.get(1).imageUrl(), "A valós hirdetésfotó megmarad");
  }

  @Test
  @DisplayName("PortalSupport placeholder szűrés egyéb általános mintákra")
  void testPortalSupportPlaceholderFiltering() {
    assertNull(PortalSupport.extractImageWithFallback(
        Jsoup.parse("<img src='/y/img/app_banner/dog.png'>").body(), "https://koltozzbe.hu"));
    assertNull(PortalSupport.extractImageWithFallback(
        Jsoup.parse("<img data-src='https://cdn.example.com/no-image.jpg'>").body(), "https://koltozzbe.hu"));
    assertEquals("https://cdn.example.com/real.jpg", PortalSupport.extractImageWithFallback(
        Jsoup.parse("<img data-src='https://cdn.example.com/real.jpg'>").body(), "https://koltozzbe.hu"));
  }
}
