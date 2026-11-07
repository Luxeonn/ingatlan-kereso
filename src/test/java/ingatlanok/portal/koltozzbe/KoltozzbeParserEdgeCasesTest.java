package ingatlanok.portal.koltozzbe;

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

class KoltozzbeParserEdgeCasesTest {

  private KoltozzbeRawParser rawParser;
  private ListingNormalizer normalizer;
  private Document htmlDoc;
  private Document jsonLdDoc;

  @BeforeEach
  void setUp() throws Exception {
    rawParser = new KoltozzbeRawParser();
    normalizer = new ListingNormalizer();
    try (InputStream is = getClass().getResourceAsStream(
        "/fixtures/edgecases/koltozzbe/koltozzbe-edge-cases.html")) {
      assertNotNull(is);
      String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      htmlDoc = Jsoup.parse(html, KoltozzbeUrlBuilder.BASE_URL);
    }
    try (InputStream is = getClass().getResourceAsStream(
        "/fixtures/edgecases/koltozzbe/koltozzbe-jsonld-edge-cases.html")) {
      assertNotNull(is);
      String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      jsonLdDoc = Jsoup.parse(html, KoltozzbeUrlBuilder.BASE_URL);
    }
  }

  @Test
  @DisplayName("Költözzbe HTML: hiányzó árelem esetén az ár null marad és nem generálódik kitalált ár")
  void testKoltozzbeHtmlParserMissingPriceLeavesPriceInfoEmpty() {
    List<RawPropertyListing> rawListings = rawParser.parseHtmlListings(htmlDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "3002".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    IngatlanItem item = normalizer.normalize(
        raw, new KoltozzbePortalEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item);
    assertNull(item.getPriceInfo().getAmountHuf());
    assertEquals("Ár a hirdetésben", item.getPriceInfo().getRawText());
    assertEquals(100.0, item.getDetailsInfo().getAreaSquareMeters());
    assertEquals(4.0, item.getDetailsInfo().getRoomCount());
  }

  @Test
  @DisplayName("Költözzbe HTML: hiányzó paraméterek esetén az alapterület és szobaszám null marad")
  void testKoltozzbeHtmlParserMissingParametersLeavesAreaAndRoomsNull() {
    List<RawPropertyListing> rawListings = rawParser.parseHtmlListings(htmlDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "3003".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    IngatlanItem item = normalizer.normalize(
        raw, new KoltozzbePortalEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item);
    assertEquals(85_000_000L, item.getPriceInfo().getAmountHuf());
    assertNull(item.getDetailsInfo().getAreaSquareMeters());
    assertNull(item.getDetailsInfo().getRoomCount());
  }

  @Test
  @DisplayName("Költözzbe HTML: alternatív DOM osztályok (.property-box, .price, .property-parameters) feldolgozása")
  void testKoltozzbeHtmlParserAlternativeDomClassesExtractsParameters() {
    List<RawPropertyListing> rawListings = rawParser.parseHtmlListings(htmlDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "3004".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    IngatlanItem item = normalizer.normalize(
        raw, new KoltozzbePortalEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item);
    assertEquals(110_000_000L, item.getPriceInfo().getAmountHuf());
    assertEquals(130.0, item.getDetailsInfo().getAreaSquareMeters());
    assertEquals(5.0, item.getDetailsInfo().getRoomCount());
  }

  @Test
  @DisplayName("Költözzbe HTML: placeholder kép (no-pic) szűrésre kerül")
  void testKoltozzbeHtmlParserPlaceholderImageFiltersOutPlaceholderMarker() {
    List<RawPropertyListing> rawListings = rawParser.parseHtmlListings(htmlDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "3005".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    assertNull(raw.imageUrl(), "A placeholder kép szűrésre kell kerüljön");
  }

  @Test
  @DisplayName("Költözzbe HTML: hibás/link nélküli kártyaelem nem állítja le a többi kártya kinyerését")
  void testKoltozzbeHtmlParserCorruptedCardWithoutAnchorDoesNotAbortRemaining() {
    List<RawPropertyListing> rawListings = rawParser.parseHtmlListings(htmlDoc);
    assertTrue(rawListings.size() >= 5, "Minden érvényes kártyának sikeresen fel kell dolgozódnia");
    assertFalse(rawListings.stream().anyMatch(r -> "9999".equals(r.nativeId())));
  }

  @Test
  @DisplayName("Költözzbe JSON-LD: offers tömb formátumból is kinyeri az árat")
  void testKoltozzbeJsonLdParserOffersArrayExtractsPriceCorrectly() {
    List<RawPropertyListing> rawListings = rawParser.parseJsonLdListings(jsonLdDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "4002".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    assertEquals(54_000_000L, raw.priceHuf());
  }

  @Test
  @DisplayName("Költözzbe JSON-LD: ImageObject típusú képobjektumból kinyeri a közvetlen URL-t")
  void testKoltozzbeJsonLdParserImageObjectExtractsUrlCorrectly() {
    List<RawPropertyListing> rawListings = rawParser.parseJsonLdListings(jsonLdDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "4003".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    assertEquals("https://koltozzbe.hu/img/kb4003.jpg", raw.imageUrl());
  }

  @Test
  @DisplayName("Költözzbe JSON-LD: szintaktikailag hibás JSON script nem akasztja meg a következő script feldolgozását")
  void testKoltozzbeJsonLdParserMalformedJsonScriptDoesNotAbortSubsequentScripts() {
    List<RawPropertyListing> rawListings = rawParser.parseJsonLdListings(jsonLdDoc);
    assertTrue(rawListings.size() >= 3, "A hibás script utáni érvényes JSON-LD blokkoknak fel kell dolgozódniuk");
  }

  @Test
  @DisplayName("Költözzbe JSON-LD: 0 értékű ár esetén az ár null marad és nem dobja el a hirdetést")
  void testKoltozzbeJsonLdParserZeroPriceInOffersLeavesPriceNullWithoutRejecting() {
    List<RawPropertyListing> rawListings = rawParser.parseJsonLdListings(jsonLdDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "4004".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    assertNull(raw.priceHuf(), "A 0 ár nem maradhat 0L a nyers modellben");
    IngatlanItem item = normalizer.normalize(
        raw, new KoltozzbePortalEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");
    assertNotNull(item, "A 0 árú hirdetést nem szabad eldobni");
    assertNull(item.getPriceInfo().getAmountHuf());
  }

  @Test
  @DisplayName("Költözzbe stratégia: HTML és JSON-LD találatok egyesítése deduplikálja az átfedő URL-eket")
  void testKoltozzbeExtractionStrategyHtmlAndJsonLdPriorityDeduplicatesUrlsCorrectly() {
    KoltozzbeExtractionStrategy strategy = new KoltozzbeExtractionStrategy(
        new KoltozzbeUrlBuilder(), rawParser, url -> jsonLdDoc, 1);

    var result = strategy.extract(new SearchCriteria());
    assertNotNull(result);
    assertEquals(ingatlanok.model.PortalScrapeStatus.SUCCESS, result.status());

    long count4001 = result.listings().stream()
        .filter(r -> r.url().contains("/h4001"))
        .count();
    assertEquals(1, count4001, "Az azonos URL-ű hirdetés pontosan egyszer szerepelhet a listában");
  }
}
