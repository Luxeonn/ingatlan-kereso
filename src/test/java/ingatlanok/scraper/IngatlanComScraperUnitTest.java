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

class IngatlanComScraperUnitTest {

  @Test
  @DisplayName("IngatlanComScraper URL builder generates correct slugs")
  void testBuildSearchUrl() {
    IngatlanComScraper scraper = new IngatlanComScraper();

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType("haz");
    criteria.setMinPriceMillion(40);
    criteria.setMaxPriceMillion(90);

    String url = scraper.buildSearchUrl(criteria);
    assertTrue(url.contains("elado+haz+debrecen"));
    assertTrue(url.contains("+40-90-mFt"));

    SearchCriteria minOnly = new SearchCriteria();
    minOnly.setCity("Debrecen");
    minOnly.setPropertyType("lakas");
    minOnly.setMinPriceMillion(50);
    minOnly.setMaxPriceMillion(null);
    assertTrue(scraper.buildSearchUrl(minOnly).contains("+50-mFt-tol"));

    SearchCriteria maxOnly = new SearchCriteria();
    maxOnly.setCity("Debrecen");
    maxOnly.setPropertyType("lakas");
    maxOnly.setMinPriceMillion(null);
    maxOnly.setMaxPriceMillion(80);
    assertTrue(scraper.buildSearchUrl(maxOnly).contains("+80-mFt-ig"));
  }

  @Test
  @DisplayName("IngatlanComScraper parses HTML anchor cards when accessible")
  void testScrapeAccessibleHtmlCards() {
    String sampleHtml =
        """
        <!DOCTYPE html>
        <html>
        <body>
            <div class="listing-item">
                <a href="/ingatlan/33445566" class="listing-card">
                    <span class="price">75 M Ft</span>
                    <span class="address">Debrecen, Nagyerdő</span>
                    <span class="area">85 m²</span>
                    <span class="rooms">3 szoba</span>
                    <img src="https://ingatlan.com/pic1.jpg" alt="Lakás">
                </a>
            </div>
        </body>
        </html>
        """;

    IngatlanComScraper scraper =
        new IngatlanComScraper() {
          @Override
          public Document getDocument(String targetUrl) {
            return Jsoup.parse(sampleHtml);
          }
        };

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType("lakas");
    criteria.setMinPriceMillion(50);
    criteria.setMaxPriceMillion(100);
    criteria.setMinArea(50);
    criteria.setMaxArea(120);

    PortalScrapeResult result = scraper.scrape(criteria);
    assertNotNull(result);
    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    assertEquals(1, result.getItems().size());

    IngatlanItem item = result.getItems().get(0);
    assertEquals("33445566", item.getId());
    assertEquals("https://ingatlan.com/ingatlan/33445566", item.getUrl());
    assertEquals("Ingatlan.com", item.getPortal());
    assertEquals("Debrecen, Nagyerdő", item.getTitle());
    assertEquals("https://ingatlan.com/pic1.jpg", item.getImageUrl());
    assertNotNull(item.getPriceInfo());
    assertEquals(75.0, item.getPriceInfo().getAmountMillion());
    assertNotNull(item.getDetailsInfo());
    assertEquals(85.0, item.getDetailsInfo().getAreaSquareMeters());
    assertEquals(3.0, item.getDetailsInfo().getRoomCount());
  }

  @Test
  @DisplayName("IngatlanComScraper returns bot-protected fallback when cards cannot be fetched")
  void testScrapeBotProtectedFallback() {
    IngatlanComScraper scraper =
        new IngatlanComScraper() {
          @Override
          public Document getDocument(String targetUrl) {
            return null;
          }
        };

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");

    PortalScrapeResult result = scraper.scrape(criteria);
    assertNotNull(result);
    assertEquals(PortalScrapeStatus.BOT_PROTECTED, result.getStatus());
    assertEquals(403, result.getHttpStatusCode());
    assertNotNull(result.getFallbackUrl());
    assertTrue(result.getFallbackUrl().contains("ingatlan.com"));
  }

  @Test
  @DisplayName(
      "IngatlanComScraper detects Cloudflare Turnstile challenge HTML and returns bot-protected"
          + " fallback")
  void testScrapeCloudflareChallengeHtml() {
    String cloudflareHtml =
        """
        <!DOCTYPE html>
        <html lang="hu">
        <head>
            <title>Csak egy gyors ellenőrzés - ingatlan.com</title>
        </head>
        <body>
            <div class="main-content">
                <h1>Csak egy gyors ellenőrzés!</h1>
                <span id="challenge-error-text">Enable JavaScript and cookies</span>
                <svg id="robot-protection-illustration"></svg>
            </div>
        </body>
        </html>
        """;

    IngatlanComScraper scraper =
        new IngatlanComScraper() {
          @Override
          public Document getDocument(String targetUrl) {
            return Jsoup.parse(cloudflareHtml);
          }
        };

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType("lakas");

    PortalScrapeResult result = scraper.scrape(criteria);
    assertNotNull(result);
    assertEquals(PortalScrapeStatus.BOT_PROTECTED, result.getStatus());
    assertEquals(403, result.getHttpStatusCode());
    assertNotNull(result.getFallbackUrl());
    assertTrue(result.getFallbackUrl().contains("ingatlan.com"));
  }

  @Test
  @DisplayName(
      "IngatlanComScraper returns empty status when page is accessible but contains 0 cards")
  void testScrapeEmptyResultsWhenNoCardsAndNoBot() {
    String emptyHtml =
        """
        <!DOCTYPE html>
        <html>
        <head><title>Eladó lakások - Debrecen</title></head>
        <body>
            <div class="listings-container">
                <p>Nincs a keresési feltételeknek megfelelő találat.</p>
            </div>
        </body>
        </html>
        """;

    IngatlanComScraper scraper =
        new IngatlanComScraper() {
          @Override
          public Document getDocument(String targetUrl) {
            return Jsoup.parse(emptyHtml);
          }
        };

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");

    PortalScrapeResult result = scraper.scrape(criteria);
    assertNotNull(result);
    assertEquals(PortalScrapeStatus.BOT_PROTECTED, result.getStatus());
    assertNotNull(result.getFallbackUrl());
    assertTrue(result.getFallbackUrl().contains("ingatlan.com"));
  }
}
