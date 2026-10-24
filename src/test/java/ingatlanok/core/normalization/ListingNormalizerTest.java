package ingatlanok.core.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PropertyType;
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
    assertEquals(PropertyType.LAKAS, item.getPropertyType());
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

  @ParameterizedTest
  @CsvSource({
      // Duna House
      "https://dh.hu/elado-ingatlan/lakas/debrecen/1, LAKAS",
      "https://dh.hu/elado-ingatlan/haz/debrecen/2, HAZ",
      "https://dh.hu/elado-ingatlan/telek/debrecen/3, TELEK",
      "https://dh.hu/elado-ingatlan/garazs/debrecen/4, GARAZS",
      "https://dh.hu/elado-ingatlan/ipari/debrecen/5, IPARI_KERESKEDELMI",
      // Otthon Centrum
      "https://www.oc.hu/ingatlanok/lista/jelleg:lakas;ertekesites:elado, LAKAS",
      "https://www.oc.hu/ingatlanok/lista/jelleg:haz;ertekesites:elado, HAZ",
      "https://www.oc.hu/ingatlanok/lista/jelleg:telek;ertekesites:elado, TELEK",
      "https://www.oc.hu/ingatlanok/lista/jelleg:garazs;ertekesites:elado, GARAZS",
      "https://www.oc.hu/ingatlanok/lista/jelleg:ipari;ertekesites:elado, IPARI_KERESKEDELMI",
      // Jófogás
      "https://ingatlan.jofogas.hu/hajdu_bihar/lakas_debrecen_1.htm, LAKAS",
      "https://ingatlan.jofogas.hu/hajdu_bihar/haz_debrecen_2.htm, HAZ",
      "https://ingatlan.jofogas.hu/hajdu_bihar/telek_debrecen_3.htm, TELEK",
      "https://ingatlan.jofogas.hu/hajdu_bihar/garazs_debrecen_4.htm, GARAZS",
      "https://ingatlan.jofogas.hu/hajdu_bihar/ipari-ingatlan_debrecen_5.htm, IPARI_KERESKEDELMI",
      // Költözzbe
      "https://koltozzbe.hu/elado-lakas-debrecen/1, LAKAS",
      "https://koltozzbe.hu/elado-haz-debrecen/2, HAZ",
      "https://koltozzbe.hu/elado-telek-debrecen/3, TELEK",
      "https://koltozzbe.hu/elado-garazs-debrecen/4, GARAZS",
      "https://koltozzbe.hu/elado-ipari/debrecen/5, IPARI_KERESKEDELMI",
      // Ingatlanbazár
      "https://www.ingatlanbazar.hu/ingatlan-Debrecen-Elado-Lakas-1, LAKAS",
      "https://www.ingatlanbazar.hu/ingatlan-Debrecen-Elado-Haz-2, HAZ",
      "https://www.ingatlanbazar.hu/ingatlan-Debrecen-Elado-Telek-3, TELEK",
      "https://www.ingatlanbazar.hu/ingatlan-Debrecen-Elado-Garazs-4, GARAZS",
      "https://www.ingatlanbazar.hu/ingatlan-Debrecen-Elado-Ipari-5, IPARI_KERESKEDELMI",
      // Ingatlan.com
      "https://ingatlan.com/lista/elado+lakas+debrecen, LAKAS",
      "https://ingatlan.com/lista/elado+haz+debrecen, HAZ",
      "https://ingatlan.com/lista/elado+telek+debrecen, TELEK",
      "https://ingatlan.com/lista/elado+garazs+debrecen, GARAZS",
      "https://ingatlan.com/lista/elado+ipari-ingatlan+debrecen, IPARI_KERESKEDELMI"
  })
  @DisplayName("ListingNormalizer resolves canonical PropertyType from URL slugs across all 6 portals")
  void testResolvePropertyTypeAllPortals(String url, PropertyType expected) {
    RawPropertyListing raw = RawPropertyListing.builder().url(url).build();
    assertEquals(expected, normalizer.resolvePropertyType(raw));
  }

  @ParameterizedTest
  @CsvSource({
      "Családi ház garázzsal és telekkel, HAZ",
      "Társasházi tégla lakás teremgarázzsal, LAKAS",
      "Építési telek kis bontandó faházzal, TELEK",
      "Önálló teremgarázs eladó a belvárosban, GARAZS",
      "Ipari telephely raktárral és irodával, IPARI_KERESKEDELMI",
      "Duplagarázs távirányítós kapuval, GARAZS",
      "Szántó és zártkert Debrecen mellett, TELEK",
      "Ikerház kertkapcsolattal, HAZ",
      "Sorház saját udvarral, HAZ",
      "Panellakás felújított konyhával, LAKAS",
      "Penthouse panorámás terasszal, LAKAS",
      "Garzonlakás egyetem közelében, LAKAS",
      "Felszíni beálló lakás vásárlási opcióval, GARAZS",
      "Ikerház kerttel és gépkocsibeállóval, HAZ",
      "Balatoni üdülő saját stranddal, HAZ"
  })
  @DisplayName("ListingNormalizer disambiguates primary property type from accessory features")
  void testResolvePropertyTypeDisambiguation(String title, PropertyType expected) {
    RawPropertyListing raw = RawPropertyListing.builder()
        .url("https://example.com/ad/123")
        .title(title)
        .build();
    assertEquals(expected, normalizer.resolvePropertyType(raw));
  }

  @Test
  @DisplayName("ListingNormalizer resolves PropertyType from Otthon Centrum item_category in descriptionText")
  void testResolvePropertyTypeFromOcCategory() {
    RawPropertyListing raw = RawPropertyListing.builder()
        .url("https://www.oc.hu/ingatlanok/reszletek/H123")
        .title("Debreceni ingatlan")
        .descriptionText("leírás... {\"item_category\":\"Családi ház\"}")
        .build();
    assertEquals(PropertyType.HAZ, normalizer.resolvePropertyType(raw));
  }

  @Test
  @DisplayName("ListingNormalizer resolves PropertyType from extra metadata")
  void testResolvePropertyTypeFromExtra() {
    RawPropertyListing raw = RawPropertyListing.builder()
        .url("https://example.com/1")
        .extra(Map.of("category", "Garázs"))
        .build();
    assertEquals(PropertyType.GARAZS, normalizer.resolvePropertyType(raw));
  }

  @Test
  @DisplayName("ListingNormalizer falls back safely to EGYEB_ISMERETLEN without speculative guessing")
  void testResolvePropertyTypeSafeFallback() {
    assertEquals(PropertyType.EGYEB_ISMERETLEN, normalizer.resolvePropertyType(null));

    RawPropertyListing ambiguous = RawPropertyListing.builder()
        .url("https://example.com/hirdetes/12345")
        .title("Kiváló befektetés Debrecen belvárosában!")
        .detailsText("70 m2 felújított")
        .build();
    assertEquals(PropertyType.EGYEB_ISMERETLEN, normalizer.resolvePropertyType(ambiguous));
  }
}
