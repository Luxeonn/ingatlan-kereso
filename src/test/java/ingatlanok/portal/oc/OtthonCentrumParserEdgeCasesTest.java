package ingatlanok.portal.oc;

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

class OtthonCentrumParserEdgeCasesTest {

  private OtthonCentrumRawParser rawParser;
  private ListingNormalizer normalizer;
  private Document edgeCaseDoc;

  @BeforeEach
  void setUp() throws Exception {
    rawParser = new OtthonCentrumRawParser();
    normalizer = new ListingNormalizer();
    try (InputStream is = getClass().getResourceAsStream("/fixtures/edgecases/oc/oc-edge-cases.html")) {
      assertNotNull(is, "oc-edge-cases.html fixture-nek leteznie kell");
      String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      edgeCaseDoc = Jsoup.parse(html, OtthonCentrumUrlBuilder.BASE_URL);
    }
  }

  @Test
  @DisplayName("OC Parser: hiányzó ár a SEO JSON-ban és szövegben nem eredményez kitalált árat")
  void testOcParserMissingPriceInSeoAndTextKeepsPriceInfoEmpty() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "H1002".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    IngatlanItem item = normalizer.normalize(
        raw, new OtthonCentrumPortalEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item);
    assertNull(item.getPriceInfo().getAmountHuf());
    assertEquals("Ár a hirdetésben", item.getPriceInfo().getRawText());
    assertEquals(60.0, item.getDetailsInfo().getAreaSquareMeters());
    assertEquals(2.0, item.getDetailsInfo().getRoomCount());
  }

  @Test
  @DisplayName("OC Parser: hibás/nem numerikus ár a SEO JSON-ban esetén fallback a kártyaszöveg árára")
  void testOcParserFaultyNonNumericPriceInSeoFallsBackToCardTextPrice() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "H1003".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    IngatlanItem item = normalizer.normalize(
        raw, new OtthonCentrumPortalEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item);
    assertEquals(45_000_000L, item.getPriceInfo().getAmountHuf(), "A kártyaszövegből kinyert árat kell használnia");
    assertEquals(45.0, item.getPriceInfo().getAmountMillion());
    assertEquals(55.0, item.getDetailsInfo().getAreaSquareMeters());
    assertEquals(2.0, item.getDetailsInfo().getRoomCount());
  }

  @Test
  @DisplayName("OC Parser: 0 ár a SEO JSON-ban nem dobja el a hirdetést, hanem hiányzó árként kezeli")
  void testOcParserZeroPriceInSeoJsonDoesNotDiscardListingAndKeepsPriceNull() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "H1006".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    IngatlanItem item = normalizer.normalize(
        raw, new OtthonCentrumPortalEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item, "A 0 árú rekordot nem szabad elutasítani, ha egyébként érvényes");
    assertNull(item.getPriceInfo().getAmountHuf());
    assertEquals(50.0, item.getDetailsInfo().getAreaSquareMeters());
  }

  @Test
  @DisplayName("OC Parser: hiányzó alapterület és szoba esetén a mezők null értékek maradnak")
  void testOcParserMissingAreaAndRoomsLeavesNullWithoutInventedValues() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "H1004".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    IngatlanItem item = normalizer.normalize(
        raw, new OtthonCentrumPortalEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item);
    assertEquals(85_000_000L, item.getPriceInfo().getAmountHuf());
    assertNull(item.getDetailsInfo().getAreaSquareMeters());
    assertNull(item.getDetailsInfo().getRoomCount());
  }

  @Test
  @DisplayName("OC Parser: alternatív DOM struktúra (.real-estate-card) esetén is kinyeri a kártya adatait")
  void testOcParserAlternativeDomStructureExtractsCardParametersAndImages() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);
    RawPropertyListing raw = rawListings.stream()
        .filter(r -> "H1005".equals(r.nativeId()))
        .findFirst()
        .orElseThrow();

    assertEquals("https://www.oc.hu/uploads/oc5.jpg", raw.imageUrl());

    IngatlanItem item = normalizer.normalize(
        raw, new OtthonCentrumPortalEngine().getDescriptor(), new SearchCriteria(), "2026-11-07 10:00:00");

    assertNotNull(item);
    assertEquals(92_000_000L, item.getPriceInfo().getAmountHuf());
    assertEquals(110.0, item.getDetailsInfo().getAreaSquareMeters());
    assertEquals(4.0, item.getDetailsInfo().getRoomCount());
    assertEquals("https://www.oc.hu/uploads/oc5.jpg", item.getImageUrl());
  }

  @Test
  @DisplayName("OC Parser: navigációs link és hibás/üres kártyák nem szakítják meg a kötegelt feldolgozást")
  void testOcParserNavigationLinkAndEmptyCardsAreSkippedWithoutCrash() {
    List<RawPropertyListing> rawListings = rawParser.parseRawListings(edgeCaseDoc);

    boolean hasNavigation = rawListings.stream().anyMatch(r -> "lista".equalsIgnoreCase(r.nativeId()));
    assertFalse(hasNavigation, "A navigációs link nem szerepelhet találatként");
    assertTrue(rawListings.size() >= 5, "Az összes érvényes kártyának sikeresen fel kell dolgozódnia");
  }
}
