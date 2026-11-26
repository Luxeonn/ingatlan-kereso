package ingatlanok.portal.jofogas;

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

class JofogasParserEdgeCasesTest {

  private JofogasRawParser rawParser;
  private ListingNormalizer normalizer;
  private Document edgeCaseDoc;

  @BeforeEach
  void setUp() throws Exception {
    rawParser = new JofogasRawParser();
    normalizer = new ListingNormalizer();
    try (InputStream is = getClass().getResourceAsStream("/fixtures/edgecases/jofogas/jofogas-edge-cases.html")) {
      assertNotNull(is, "jofogas-edge-cases.html fixture-nek leteznie kell");
      String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      edgeCaseDoc = Jsoup.parse(html, JofogasUrlBuilder.BASE_URL);
    }
  }

  @Test
  @DisplayName("Jófogás Parser: hiányzó ár esetén 'Ár a hirdetésben' marad és nem keletkezik kitalált érték")
  void testJofogasParserMissingPriceLeavesPriceInfoEmpty() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "12345679".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    IngatlanItem item = normalizer.normalize(
        raw, TestFixtures.jofogasEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item);
    assertNull(item.getPriceInfo().amountHuf());
    assertEquals("Ár a hirdetésben", item.getPriceInfo().rawText());
    assertEquals(65.0, item.getDetailsInfo().areaSquareMeters());
    assertEquals(3.0, item.getDetailsInfo().roomCount());
  }

  @Test
  @DisplayName("Jófogás Parser: hiányzó alapterület és szobaszám esetén null értékek maradnak")
  void testJofogasParserMissingAreaAndRoomsLeavesNullWithoutInventedValues() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "12345680".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    IngatlanItem item = normalizer.normalize(
        raw, TestFixtures.jofogasEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item);
    assertEquals(82_000_000L, item.getPriceInfo().amountHuf());
    assertNull(item.getDetailsInfo().areaSquareMeters());
    assertNull(item.getDetailsInfo().roomCount());
  }

  @Test
  @DisplayName("Jófogás Parser: teljes Ft ár nem-törő szóközökkel helyesen konvertálódik numerikus értékké")
  void testJofogasParserFullHufPriceWithNonBreakingSpacesParsesNumericAmount() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "12345681".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    IngatlanItem item = normalizer.normalize(
        raw, TestFixtures.jofogasEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item);
    assertEquals(45_500_000L, item.getPriceInfo().amountHuf());
    assertEquals(45.5, item.getPriceInfo().amountMillion());
    assertEquals(58.0, item.getDetailsInfo().areaSquareMeters());
    assertEquals(2.0, item.getDetailsInfo().roomCount());
  }

  @Test
  @DisplayName("Jófogás Parser: placeholder képek kiszűrésre kerülnek")
  void testJofogasParserPlaceholderImageFiltersOutPlaceholderMarker() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "12345682".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    assertNull(raw.imageUrl(), "A placeholder képet nullként kell kezelni");
  }

  @Test
  @DisplayName("Jófogás Parser: nem hirdetés típusú súgó/információs linkek nem generálnak hirdetést")
  void testJofogasParserNonListingInformationalLinksAreSkippedWithoutCrash() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    boolean hasInfoLink = rawListings.stream()
        .anyMatch(r -> r.url().contains("sugo_tajekoztato"));

    assertFalse(hasInfoLink, "Az információs súgólinket ki kell hagyni");
  }

  @Test
  @DisplayName("Jófogás Parser: hibás/túl rövid linkek nem állítják le a többi érvényes rekord kinyerését")
  void testJofogasParserCorruptedShortLinksDoesNotAbortRemainingRecords() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    assertTrue(rawListings.size() >= 4, "A legalább 4 érvényes kártyának fel kell dolgozódnia");
    assertTrue(rawListings.stream().anyMatch(r -> "12345678".equals(r.nativeId())));
  }
}
