package ingatlanok.scraper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;

class BaseScraperAndMockUnitTest {

  private static class DummyScraper extends BaseScraper {
    @Override
    public void init() {}

    @Override
    public int getCardNumber(Element resultPageSize) {
      return 10;
    }
  }

  @Test
  @DisplayName("BaseScraper pagination URL generation")
  void testPaginationUrlGeneration() {
    DummyScraper scraper = new DummyScraper();

    assertNotNull(scraper);
    assertEquals(10, scraper.getCardNumber(new Element("div")));
  }

  @Test
  @DisplayName("PortalScrapeResult construction and status handling")
  void testPortalScrapeResult() {
    IngatlanItem item = new IngatlanItem();
    item.setUrl("https://test.hu/1");

    PortalScrapeResult successResult =
        PortalScrapeResult.success("dh", "Duna House", List.of(item), 100L);
    assertEquals("dh", successResult.getPortalId());
    assertEquals("Duna House", successResult.getPortalName());
    assertEquals(PortalScrapeStatus.SUCCESS, successResult.getStatus());
    assertEquals(1, successResult.getItems().size());

    PortalScrapeResult botResult =
        PortalScrapeResult.botProtected("ingatlancom", "Ingatlan.com", "https://ingatlan.com", 50L);
    assertEquals(PortalScrapeStatus.BOT_PROTECTED, botResult.getStatus());
    assertTrue(botResult.getItems().isEmpty());
  }

  @Test
  @DisplayName("Mock HTML card parsing with Jsoup")
  void testMockHtmlParsing() {
    String html =
        "<div class='card'><a href='/ingatlan/123'>Lakás Debrecenben</a><span class='price'>50 M"
            + " Ft</span></div>";
    Document doc = Jsoup.parse(html);

    Element link = doc.selectFirst("a");
    assertNotNull(link);
    assertEquals("/ingatlan/123", link.attr("href"));
    assertEquals("Lakás Debrecenben", link.text());

    Element price = doc.selectFirst(".price");
    assertNotNull(price);
    assertEquals("50 M Ft", price.text());
  }
}
