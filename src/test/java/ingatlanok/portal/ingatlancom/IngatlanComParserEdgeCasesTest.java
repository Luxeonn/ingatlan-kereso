package ingatlanok.portal.ingatlancom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.core.normalization.ListingNormalizer;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.testsupport.TestFixtures;

class IngatlanComParserEdgeCasesTest {

  private IngatlanComRawParser rawParser;
  private ListingNormalizer normalizer;
  private Document edgeCaseDoc;

  @BeforeEach
  void setUp() throws Exception {
    rawParser = new IngatlanComRawParser();
    normalizer = new ListingNormalizer();
    try (InputStream is = getClass().getResourceAsStream(
        "/fixtures/edgecases/ingatlancom/ingatlancom-edge-cases.html")) {
      assertNotNull(is);
      String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      edgeCaseDoc = Jsoup.parse(html, IngatlanComUrlBuilder.BASE_URL);
    }
  }

  @Test
  @DisplayName("Ingatlan.com Parser: hiányzó ár esetén az ár null marad és nem generálódik kitalált érték")
  void testIngatlanComParserMissingPriceLeavesPriceInfoEmpty() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "35002".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    IngatlanItem item = normalizer.normalize(
        raw, TestFixtures.ingatlanComEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item);
    assertNull(item.getPriceInfo().amountHuf());
    assertEquals("Ár a hirdetésben", item.getPriceInfo().rawText());
    assertEquals(60.0, item.getDetailsInfo().areaSquareMeters());
    assertEquals(2.0, item.getDetailsInfo().roomCount());
  }

  @Test
  @DisplayName("Ingatlan.com Parser: hiányzó címkézett alapterület és szoba esetén a mezők null értékek maradnak")
  void testIngatlanComParserMissingLabeledAreaAndRoomsLeavesAreaAndRoomsNull() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "35003".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    IngatlanItem item = normalizer.normalize(
        raw, TestFixtures.ingatlanComEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item);
    assertEquals(55_000_000L, item.getPriceInfo().amountHuf());
    assertNull(item.getDetailsInfo().areaSquareMeters());
    assertNull(item.getDetailsInfo().roomCount());
  }

  @Test
  @DisplayName("Ingatlan.com Parser: több lehetséges ár a szövegben esetén a dedikált árelemet részesíti előnyben")
  void testIngatlanComParserMultiplePricesInTextPrefersDedicatedPriceContainer() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "35004".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    assertEquals("69 M Ft", raw.priceText());
    IngatlanItem item = normalizer.normalize(
        raw, TestFixtures.ingatlanComEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item);
    assertEquals(69_000_000L, item.getPriceInfo().amountHuf());
    assertEquals(69.0, item.getPriceInfo().amountMillion());
  }

  @Test
  @DisplayName("Ingatlan.com Parser: hirdetés-azonosító nélküli link nem kerül feldolgozásra")
  void testIngatlanComParserMissingListingIdSkipsNonListingLink() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    boolean hasNonListingLink = rawListings.stream()
        .anyMatch(r -> r.url().contains("/lista/elado+lakas"));

    assertFalse(hasNonListingLink, "A lista/kategória link nem képezhet hirdetést");
  }

  @Test
  @DisplayName("Ingatlan.com Parser: üres/hibás kártyaelem nem állítja le a többi érvényes rekord kinyerését")
  void testIngatlanComParserCorruptedCardElementDoesNotAbortRemainingRecords() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    assertTrue(rawListings.size() >= 4, "Minden érvényes kártyának fel kell dolgozódnia");
    assertTrue(rawListings.stream().anyMatch(r -> "35001".equals(r.nativeId())));
  }
}
