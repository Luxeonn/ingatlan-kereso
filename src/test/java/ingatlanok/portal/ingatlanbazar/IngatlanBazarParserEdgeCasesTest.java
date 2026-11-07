package ingatlanok.portal.ingatlanbazar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.core.normalization.ListingNormalizer;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.RawPropertyListing;

class IngatlanBazarParserEdgeCasesTest {

  private IngatlanBazarRawParser rawParser;
  private ListingNormalizer normalizer;
  private String edgeCaseJson;

  @BeforeEach
  void setUp() throws Exception {
    rawParser = new IngatlanBazarRawParser();
    normalizer = new ListingNormalizer();
    try (InputStream is = getClass().getResourceAsStream(
        "/fixtures/edgecases/ingatlanbazar/ingatlanbazar-edge-cases.json")) {
      assertNotNull(is);
      edgeCaseJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  @DisplayName("Ingatlanbazár Parser: hiányzó vagy null ár esetén az ár null marad és a hirdetés érvényes")
  void testIngatlanBazarParserMissingOrNullPriceLeavesPriceNullWithoutRejecting() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseJson);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "IB502".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    assertNull(raw.priceHuf());
    IngatlanItem item = normalizer.normalize(
        raw, new IngatlanBazarPortalEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item, "A null árú hirdetés nem kerülhet elutasításra");
    assertNull(item.getPriceInfo().getAmountHuf());
    assertEquals(70.0, item.getDetailsInfo().getAreaSquareMeters());
    assertEquals(3.0, item.getDetailsInfo().getRoomCount());
  }

  @Test
  @DisplayName("Ingatlanbazár Parser: 0 értékű ár esetén az ár null marad 0 Ft helyett")
  void testIngatlanBazarParserZeroPriceLeavesPriceNullInsteadOfZeroHuf() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseJson);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "IB503".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    assertNull(raw.priceHuf(), "A 0 ár nem maradhat numerikus 0L");
    IngatlanItem item = normalizer.normalize(
        raw, new IngatlanBazarPortalEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item);
    assertNull(item.getPriceInfo().getAmountHuf());
    assertEquals(80.0, item.getDetailsInfo().getAreaSquareMeters());
  }

  @Test
  @DisplayName("Ingatlanbazár Parser: hiányzó vagy 0 alapterület esetén a terület null marad")
  void testIngatlanBazarParserMissingOrZeroAreaLeavesAreaNullInsteadOfZeroSqm() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseJson);
    RawPropertyListing nullAreaRaw = rawListings.stream()
        .filter(r -> "IB504".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();
    assertNull(nullAreaRaw.areaSquareMeters());

    RawPropertyListing zeroAreaRaw = rawListings.stream()
        .filter(r -> "IB505".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();
    assertNull(zeroAreaRaw.areaSquareMeters(), "A 0 alapterület nem maradhat numerikus 0.0");

    IngatlanItem item = normalizer.normalize(
        zeroAreaRaw, new IngatlanBazarPortalEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");
    assertNotNull(item);
    assertNull(item.getDetailsInfo().getAreaSquareMeters());
  }

  @Test
  @DisplayName("Ingatlanbazár Parser: numerikus karakterláncokból (String) is kinyeri a számértékeket")
  void testIngatlanBazarParserNumericStringsParsesPriceAndAreaCorrectly() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseJson);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "IB506".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    assertEquals(78_000_000L, raw.priceHuf());
    assertEquals(90.5, raw.areaSquareMeters());
    assertEquals(4.0, raw.roomCount());
  }

  @Test
  @DisplayName("Ingatlanbazár Parser: hiányzó/üres seoUrl esetén a találat kihagyásra kerül")
  void testIngatlanBazarParserMissingSeoUrlIsSkippedWithoutException() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseJson);
    boolean hasEmptyUrl = rawListings.stream()
        .anyMatch(r -> "IB507".equals(r.nativeId()) || r.url().isEmpty());

    assertFalse(hasEmptyUrl, "Az üres URL-lel rendelkező találatot ki kell hagyni");
  }

  @Test
  @DisplayName("Ingatlanbazár Parser: különböző képformátumok (photo, picture, renderPictures) feloldása")
  void testIngatlanBazarParserVariousImageFormatsNormalizesCandidates() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseJson);
    RawPropertyListing hit1 = rawListings.stream().filter(r -> "IB501".equals(r.nativeId())).findFirst().orElseThrow();
    assertEquals("https://cdn.ib.hu/photos/1.jpg", hit1.imageUrl());

    RawPropertyListing hit6 = rawListings.stream().filter(r -> "IB506".equals(r.nativeId())).findFirst().orElseThrow();
    assertEquals("https://cdn.ib.hu/pic2.jpg", hit6.imageUrl());

    RawPropertyListing hit8 = rawListings.stream().filter(r -> "IB508".equals(r.nativeId())).findFirst().orElseThrow();
    assertTrue(hit8.imageUrl().contains("images2.ingatlanbazar.hu"));
    assertTrue(hit8.imageUrl().endsWith("_485x354.jpg"));
  }

  @Test
  @DisplayName("Ingatlanbazár Parser: hibás elemek a hits tömbben nem szakítják meg a feldolgozást")
  void testIngatlanBazarParserCorruptedArrayElementsDoesNotAbortRemainingHits() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseJson);
    assertTrue(rawListings.size() >= 5, "Minden érvényes rekordnak fel kell dolgozódnia");
  }

  @Test
  @DisplayName("Ingatlanbazár Parser: nem-JSON HTML hibaoldal esetén kontrolláltan üres listát ad")
  void testIngatlanBazarParserNonJsonHtmlErrorPageReturnsEmptyListWithoutThrowing() {
    String html502 = "<html><head><title>502 Bad Gateway</title></head><body>Server error</body></html>";
    List<RawPropertyListing> listings = rawParser.parseRawListings(html502);
    assertNotNull(listings);
    assertTrue(listings.isEmpty());
  }

  @Test
  @DisplayName("Ingatlanbazár Parser: hits tömb nélküli JSON esetén kontrolláltan üres listát ad")
  void testIngatlanBazarParserJsonWithoutHitsArrayReturnsEmptyListGracefully() {
    String jsonNoHits = "{\"status\": \"error\", \"message\": \"Rate limit exceeded\"}";
    List<RawPropertyListing> listings = rawParser.parseRawListings(jsonNoHits);
    assertNotNull(listings);
    assertTrue(listings.isEmpty());
  }
}
