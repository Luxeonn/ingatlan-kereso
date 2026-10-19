package ingatlanok.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;

class KoltozzbeScraperTest {

  @Test
  @DisplayName(
      "KoltozzbeScraper generates correct search URLs for Debrecen houses and agglomeration")
  void testBuildSearchUrl() {
    KoltozzbeScraper scraper = new KoltozzbeScraper();

    SearchCriteria criteriaDebrecen = new SearchCriteria();
    criteriaDebrecen.setCity("Debrecen");
    criteriaDebrecen.setPropertyType("haz");
    criteriaDebrecen.setMinPriceMillion(60);
    criteriaDebrecen.setMaxPriceMillion(110);
    criteriaDebrecen.setMinArea(80);
    criteriaDebrecen.setMaxArea(180);

    String debrecenUrl = scraper.buildSearchUrl(criteriaDebrecen);
    assertTrue(debrecenUrl.contains("https://koltozzbe.hu/elado-haz-debrecen"));
    assertTrue(debrecenUrl.contains("p1=60"));
    assertTrue(debrecenUrl.contains("p2=110"));
    assertTrue(debrecenUrl.contains("a1=80"));
    assertTrue(debrecenUrl.contains("a2=180"));

    SearchCriteria criteriaAgglom = new SearchCriteria();
    criteriaAgglom.setCity("Bocskaikert");
    criteriaAgglom.setPropertyType("haz");
    criteriaAgglom.setMinPriceMillion(null);
    criteriaAgglom.setMaxPriceMillion(null);
    criteriaAgglom.setMinArea(null);
    criteriaAgglom.setMaxArea(null);

    String agglomUrl = scraper.buildSearchUrl(criteriaAgglom);
    assertEquals("https://koltozzbe.hu/elado-haz-bocskaikert", agglomUrl);
  }

  @Test
  @DisplayName("KoltozzbeScraper extracts listing items from HTML cards")
  void testParseHtmlCards() {
    String sampleHtml =
        """
<!DOCTYPE html>
<html>
<body>
    <div class="listing-card" data-lid="10959230">
        <a href="/elado-csaladi_haz-debrecen/h10959230" class="listing-item-link" title="Eladó családi ház Debrecenben 125 M Ft">
            <div class="listing-thumbnail">
                <img src="https://k1-cdn.koltozzbe.hu/img2/test.jpg" alt="Ház">
            </div>
            <div class="listing-link">
                <header class="listing-header">
                    <div class="item-type h2">125 M Ft <span class="item-meanprice">1 225 490 Ft/m²</span></div>
                    <div class="item-price">Debrecen - Eladó családi ház</div>
                </header>
                <div class="listing-parameters">
                    <div class="listing-rooms">3 szoba</div>
                    <div class="listing-size">102 m²</div>
                    <div class="listing-plot">telekméret: 498 m²</div>
                </div>
            </div>
        </a>
    </div>
</body>
</html>
""";

    KoltozzbeScraper testScraper =
        new KoltozzbeScraper() {
          @Override
          public Document getDocument(String targetUrl) {
            return Jsoup.parse(sampleHtml);
          }
        };

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType("haz");
    criteria.setMinPriceMillion(100);
    criteria.setMaxPriceMillion(150);
    criteria.setMinArea(90);
    criteria.setMaxArea(120);

    PortalScrapeResult result = testScraper.scrape(criteria);
    assertNotNull(result);
    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    assertEquals(1, result.getItems().size());

    IngatlanItem item = result.getItems().get(0);
    assertEquals("10959230", item.getId());
    assertEquals("https://koltozzbe.hu/elado-csaladi_haz-debrecen/h10959230", item.getUrl());
    assertEquals("Költözzbe", item.getPortal());
    assertNotNull(item.getPriceInfo());
    assertEquals(125.0, item.getPriceInfo().getAmountMillion());
    assertNotNull(item.getDetailsInfo());
    assertEquals(102.0, item.getDetailsInfo().getAreaSquareMeters());
    assertEquals(3.0, item.getDetailsInfo().getRoomCount());
    assertEquals("https://k1-cdn.koltozzbe.hu/img2/test.jpg", item.getImageUrl());
  }

  @Test
  @DisplayName("KoltozzbeScraper fallback to JSON-LD structured data")
  void testJsonLdFallback() {
    String jsonLdHtml =
        """
        <!DOCTYPE html>
        <html>
        <head>
            <script type="application/ld+json">
            {
                "@context": "https://schema.org",
                "@type": "ItemList",
                "itemListElement": [
                    {
                        "@type": "ListItem",
                        "position": 1,
                        "item": {
                            "@type": "Product",
                            "name": "Eladó családi ház, Debrecenben 85 M Ft, 4 szobás",
                            "image": ["https://k1-cdn.koltozzbe.hu/img2/jsonld.jpg"],
                            "offers": {
                                "@type": "Offer",
                                "priceSpecification": {
                                    "@type": "PriceSpecification",
                                    "price": 85000000,
                                    "priceCurrency": "HUF"
                                }
                            },
                            "url": "https://koltozzbe.hu/elado-csaladi_haz-debrecen/h998877"
                        }
                    }
                ]
            }
            </script>
        </head>
        <body></body>
        </html>
        """;

    KoltozzbeScraper testScraper =
        new KoltozzbeScraper() {
          @Override
          public Document getDocument(String targetUrl) {
            return Jsoup.parse(jsonLdHtml);
          }
        };

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType("haz");
    criteria.setMinPriceMillion(50);
    criteria.setMaxPriceMillion(100);

    PortalScrapeResult result = testScraper.scrape(criteria);
    assertNotNull(result);
    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    assertEquals(1, result.getItems().size());

    IngatlanItem item = result.getItems().get(0);
    assertEquals("998877", item.getId());
    assertEquals("https://koltozzbe.hu/elado-csaladi_haz-debrecen/h998877", item.getUrl());
    assertNotNull(item.getPriceInfo());
    assertEquals(85.0, item.getPriceInfo().getAmountMillion());
    assertEquals("https://k1-cdn.koltozzbe.hu/img2/jsonld.jpg", item.getImageUrl());
  }

  @Test
  @DisplayName("KoltozzbeScraper handles network errors gracefully without throwing")
  void testHandlesNetworkErrorGracefully() {
    KoltozzbeScraper testScraper =
        new KoltozzbeScraper() {
          @Override
          public Document getDocument(String targetUrl) {
            throw new RuntimeException("Simulated connection timeout");
          }
        };

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");

    PortalScrapeResult result = testScraper.scrape(criteria);
    assertNotNull(result);
    assertEquals(PortalScrapeStatus.FAILED, result.getStatus());
    assertEquals(500, result.getHttpStatusCode());
    assertTrue(result.getTechnicalDetails().contains("Simulated connection timeout"));
  }
}
