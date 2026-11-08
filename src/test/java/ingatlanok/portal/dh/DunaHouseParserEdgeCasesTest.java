package ingatlanok.portal.dh;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class DunaHouseParserEdgeCasesTest {

  private DunaHouseRawParser rawParser;
  private ListingNormalizer normalizer;
  private Document edgeCaseDoc;

  @BeforeEach
  void setUp() throws Exception {
    rawParser = new DunaHouseRawParser();
    normalizer = new ListingNormalizer();
    try (InputStream is = getClass().getResourceAsStream("/fixtures/edgecases/dh/dh-edge-cases.html")) {
      assertNotNull(is, "dh-edge-cases.html fixture-nek leteznie kell");
      String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      edgeCaseDoc = Jsoup.parse(html, DunaHouseUrlBuilder.BASE_URL);
    }
  }

  @Test
  @DisplayName("DH Parser: hiányzó ár esetén 'Ár a hirdetésben' marad és nem keletkezik kitalált érték")
  void testDhParserMissingPriceKeepsPriceInfoEmptyWithoutInventedValues() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    RawPropertyListing missingPriceRaw = rawListings.stream()
        .filter(r -> "DH1002".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    IngatlanItem item = normalizer.normalize(
        missingPriceRaw, new DunaHousePortalEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item);
    assertNotNull(item.getPriceInfo());
    assertEquals("Ár a hirdetésben", item.getPriceInfo().getRawText());
    assertNull(item.getPriceInfo().getAmountHuf(), "Hiányzó árból nem keletkezhet numerikus HUF érték");
    assertNull(item.getPriceInfo().getAmountMillion(), "Hiányzó árból nem keletkezhet millió érték");
    assertEquals(70.0, item.getDetailsInfo().getAreaSquareMeters());
    assertEquals(3.0, item.getDetailsInfo().getRoomCount());
    assertNull(item.getDetailsInfo().getPricePerSquareMeter(), "Ár nélkül m2 ár nem számítható");
  }

  @Test
  @DisplayName("DH Parser: hiányzó alapterület és szobaszám esetén null értékek maradnak")
  void testDhParserMissingAreaAndRoomsLeavesAreaAndRoomsNull() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    RawPropertyListing missingDetailsRaw = rawListings.stream()
        .filter(r -> "DH1003".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    IngatlanItem item = normalizer.normalize(
        missingDetailsRaw, new DunaHousePortalEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item);
    assertEquals(75_000_000L, item.getPriceInfo().getAmountHuf());
    assertNull(item.getDetailsInfo().getAreaSquareMeters(), "Hiányzó terület nem lehet becsült érték");
    assertNull(item.getDetailsInfo().getRoomCount(), "Hiányzó szobaszám nem lehet kitalált érték");
    assertNull(item.getDetailsInfo().getPricePerSquareMeter(), "Terület nélkül m2 ár nem számítható");
  }

  @Test
  @DisplayName("DH Parser: placeholder kép (no-image.png) szűrésre kerül és null marad")
  void testDhParserPlaceholderImageFiltersOutPlaceholderMarker() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    RawPropertyListing placeholderRaw = rawListings.stream()
        .filter(r -> "DH1004".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    assertNull(placeholderRaw.imageUrl(), "A placeholder képjelölő nem adhat vissza érvényes képet");

    IngatlanItem item = normalizer.normalize(
        placeholderRaw, new DunaHousePortalEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");
    assertNotNull(item);
    assertNull(item.getImageUrl());
  }

  @Test
  @DisplayName("DH Parser: protokoll-relatív képhivatkozás (//cdn...) https séma prefixet kap")
  void testDhParserProtocolRelativeImageUrlNormalizesToHttps() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    RawPropertyListing relativeRaw = rawListings.stream()
        .filter(r -> "DH1005".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    assertEquals("https://cdn.dh.hu/images/photo5.jpg", relativeRaw.imageUrl());

    IngatlanItem item = normalizer.normalize(
        relativeRaw, new DunaHousePortalEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");
    assertNotNull(item);
    assertEquals("https://cdn.dh.hu/images/photo5.jpg", item.getImageUrl());
  }

  @Test
  @DisplayName("DH Parser: több száz milliós árformátum (M Ft) helyesen konvertálódik forintra és millióra")
  void testDhParserMultiHundredMillionPriceParsesCorrectly() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    RawPropertyListing multiMillionRaw = rawListings.stream()
        .filter(r -> "DH1006".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    IngatlanItem item = normalizer.normalize(
        multiMillionRaw, new DunaHousePortalEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item);
    assertEquals(350_500_000L, item.getPriceInfo().getAmountHuf());
    assertEquals(350.5, item.getPriceInfo().getAmountMillion());
    assertEquals(350.0, item.getDetailsInfo().getAreaSquareMeters());
    assertEquals(6.0, item.getDetailsInfo().getRoomCount());
  }

  @Test
  @DisplayName("DH Parser: kategória/navigációs link (/ingatlan/elado) nem kerül hirdetésként feldolgozásra")
  void testDhParserNavigationCategoryLinkIsSkippedWithoutCreatingListing() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    boolean hasCategoryRecord = rawListings.stream()
        .anyMatch(r -> "elado".equalsIgnoreCase(r.nativeId()) || r.url().endsWith("/ingatlan/elado"));

    assertTrue(!hasCategoryRecord, "A navigációs kategórialink nem jelenhet meg hirdetésrekordként");
  }

  @Test
  @DisplayName("DH Parser: hibás/üres rekordok nem állítják le a dokumentum többi érvényes kártyájának kinyerését")
  void testDhParserCorruptedRecordsAlongsideValidListingsDoesNotAbortBatch() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);

    assertTrue(rawListings.size() >= 5, "Az érvényes rekordoknak meg kell jelenniük a hibásak mellett");
    assertTrue(rawListings.stream().anyMatch(r -> "DH1001".equals(r.nativeId())));
    assertTrue(rawListings.stream().anyMatch(r -> "DH1006".equals(r.nativeId())));
  }
}
