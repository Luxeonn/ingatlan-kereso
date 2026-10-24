package ingatlanok.core.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.PortalDescriptor;

class ListingNormalizerTest {

  private ListingNormalizer normalizer;
  private ListingValidator validator;
  private PortalDescriptor descriptor;

  @BeforeEach
  void setUp() {
    validator = new ListingValidator();
    normalizer = new ListingNormalizer(validator);
    descriptor = new PortalDescriptor(
        "dh",
        "Duna House",
        "#1d4ed8",
        "https://dh.hu",
        true,
        List.of("lakas", "haz"),
        true
    );
  }

  @Test
  @DisplayName("ListingValidator szűri az érvénytelen linkeket és határon kívüli adatokat")
  void testValidator() {
    assertFalse(validator.isValid(null, descriptor));
    assertFalse(validator.isValid(RawPropertyListing.builder().url("").build(), descriptor));
    assertFalse(validator.isValid(RawPropertyListing.builder().url("not a uri").build(), descriptor));

    // Külső eltérő domain
    assertFalse(validator.isValid(
        RawPropertyListing.builder().url("https://malicious.com/ad/123").build(), descriptor));

    // Irreális alapterület
    assertFalse(validator.isValid(
        RawPropertyListing.builder().url("https://dh.hu/1").areaSquareMeters(2.0).build(), descriptor));
    assertFalse(validator.isValid(
        RawPropertyListing.builder().url("https://dh.hu/1").areaSquareMeters(60000.0).build(), descriptor));

    // Irreális ár
    assertFalse(validator.isValid(
        RawPropertyListing.builder().url("https://dh.hu/1").priceHuf(-100L).build(), descriptor));
    assertFalse(validator.isValid(
        RawPropertyListing.builder().url("https://dh.hu/1").priceHuf(60_000_000_000L).build(), descriptor));

    // Érvényes
    assertTrue(validator.isValid(
        RawPropertyListing.builder()
            .url("https://dh.hu/ingatlan/123")
            .areaSquareMeters(65.0)
            .priceHuf(50_000_000L)
            .build(),
        descriptor));
  }

  @Test
  @DisplayName("ListingNormalizer helyesen normalizálja a nyers adatokat IngatlanItem-mé")
  void testNormalizerSuccessfulMapping() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");

    RawPropertyListing raw = RawPropertyListing.builder()
        .portalId("dh")
        .nativeId("DH-999")
        .url("/ingatlan/elado/lakas/debrecen/999")
        .title("Új építésű lakás Debrecenben")
        .priceHuf(52_000_000L)
        .areaSquareMeters(65.0)
        .roomCount(2.0)
        .locationText("Debrecen, Belváros")
        .imageUrl("/images/dh999.jpg")
        .descriptionText("Új építésű társasházi lakás")
        .method(ExtractionMethod.HTML)
        .build();

    IngatlanItem item = normalizer.normalize(raw, descriptor, criteria, "2026-10-24 12:00:00");

    assertNotNull(item);
    assertEquals("DH-999", item.getId());
    assertEquals("https://dh.hu/ingatlan/elado/lakas/debrecen/999", item.getUrl());
    assertEquals("https://dh.hu/images/dh999.jpg", item.getImageUrl());
    assertEquals(52_000_000L, item.getPriceInfo().getAmountHuf());
    assertEquals(52.0, item.getPriceInfo().getAmountMillion());
    assertEquals(65.0, item.getDetailsInfo().getAreaSquareMeters());
    assertEquals(2.0, item.getDetailsInfo().getRoomCount());
    assertEquals(Math.round(52_000_000.0 / 65.0), item.getDetailsInfo().getPricePerSquareMeter());
    assertTrue(item.isNewConstruction());
  }

  @Test
  @DisplayName("ListingNormalizer determinisztikus ID-t képez, ha nincs nativeId")
  void testNormalizerDeterministicIdFallback() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");

    RawPropertyListing raw = RawPropertyListing.builder()
        .portalId("dh")
        .url("https://dh.hu/ingatlan/elado/lakas/debrecen/abc")
        .priceText("50 M Ft")
        .detailsText("60 m2 2 szoba")
        .method(ExtractionMethod.HTML)
        .build();

    IngatlanItem item1 = normalizer.normalize(raw, descriptor, criteria, "2026-10-24 12:00:00");
    IngatlanItem item2 = normalizer.normalize(raw, descriptor, criteria, "2026-10-24 12:00:00");

    assertNotNull(item1);
    assertNotNull(item2);
    assertEquals(item1.getId(), item2.getId());
    assertFalse(item1.getId().isBlank());
  }

  @Test
  @DisplayName("ListingNormalizer null-t ad vissza ha a validátor elutasítja az elemet")
  void testNormalizerInvalidRejection() {
    RawPropertyListing invalid = RawPropertyListing.builder()
        .url("https://foreign-domain.com/ad/1")
        .build();
    assertNull(normalizer.normalize(invalid, descriptor, new SearchCriteria(), "now"));
  }
}
