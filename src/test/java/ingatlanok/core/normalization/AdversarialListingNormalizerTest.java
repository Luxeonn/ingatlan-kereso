package ingatlanok.core.normalization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.PortalDescriptor;

/**
 * Adversarial stress and boundary test suite for ListingNormalizer and PropertyType.
 * Empirically validates robustness against linguistic edge cases, nulls, concurrency,
 * and contradictory inputs.
 */
class AdversarialListingNormalizerTest {

  private ListingNormalizer normalizer;
  private PortalDescriptor descriptor;

  @BeforeEach
  void setUp() {
    normalizer = new ListingNormalizer(new ListingValidator());
    descriptor = new PortalDescriptor(
        "dh",
        "Duna House",
        "#1d4ed8",
        "https://dh.hu",
        true,
        List.of("lakas", "haz", "telek", "garazs", "ipari"),
        true
    );
  }

  // --- 1. Boundary & Adversarial Inputs ---

  @Test
  @DisplayName("Adversarial: Completely null RawPropertyListing returns null safely without throwing")
  void testNullRawListing() {
    assertDoesNotThrow(() -> {
      IngatlanItem item = normalizer.normalize(null, descriptor, new SearchCriteria(), "now");
      assertNull(item);
      PropertyType type = normalizer.resolvePropertyType(null);
      assertEquals(PropertyType.EGYEB_ISMERETLEN, type);
    });
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\t\t", "\n\r", "   \t   \n  "})
  @DisplayName("Adversarial: Empty, null, and whitespace-only titles resolve safely without throwing")
  void testEmptyAndWhitespaceTitles(String adversarialTitle) {
    RawPropertyListing raw = RawPropertyListing.builder()
        .url("https://dh.hu/ingatlan/12345")
        .title(adversarialTitle)
        .build();

    assertDoesNotThrow(() -> {
      PropertyType type = normalizer.resolvePropertyType(raw);
      assertEquals(PropertyType.EGYEB_ISMERETLEN, type);
    });
  }

  @Test
  @DisplayName("Adversarial: Extra metadata map with null keys, null values, or blank values")
  void testAdversarialExtraMetadata() {
    Map<String, String> extra = new HashMap<>();
    extra.put("category", null);
    extra.put("propertyType", "   ");
    extra.put("item_category", "");
    extra.put("type", "non_existent_category_123");

    RawPropertyListing raw = RawPropertyListing.builder()
        .url("https://dh.hu/ingatlan/123")
        .title("Eladó ingatlan")
        .extra(extra)
        .build();

    assertDoesNotThrow(() -> {
      PropertyType type = normalizer.resolvePropertyType(raw);
      assertEquals(PropertyType.EGYEB_ISMERETLEN, type);
    });
  }

  @Test
  @DisplayName("Adversarial: ReDoS stress test with 100k repeated string does not freeze regex engine")
  void testReDosResilience() {
    String longGarbage = "nagyon ".repeat(15_000) + "szép eladó ház " + "kerttel ".repeat(5_000);
    RawPropertyListing raw = RawPropertyListing.builder()
        .url("https://dh.hu/ingatlan/redos")
        .title(longGarbage)
        .build();

    long startTime = System.currentTimeMillis();
    PropertyType type = normalizer.resolvePropertyType(raw);
    long duration = System.currentTimeMillis() - startTime;

    assertEquals(PropertyType.HAZ, type);
    assertTrue(duration < 2000, "Regex matching took too long (" + duration + "ms), possible ReDoS risk!");
  }

  // --- 2. Compound Hungarian Titles & Accessory Disambiguation ---

  @ParameterizedTest
  @CsvSource({
      "Családi ház garázzsal, HAZ",
      "Családi ház önálló garázzsal és kerti tóval, HAZ",
      "Ikerház kerttel és gépkocsibeállóval, HAZ",
      "Sorház terasszal és garázzsal, HAZ",
      "Lakás tárolóval és udvari beállóval, LAKAS",
      "Társasházi lakás teremgarázzsal, LAKAS",
      "Téglalakás tárolóval a pinceszinten, LAKAS",
      "Önálló garázs eladó a belvárosban, GARAZS",
      "Garázs villannyal és szerelőaknával, GARAZS",
      "Építési telek kis tároló faházzal, TELEK",
      "Ipari telephely műhellyel és irodával, IPARI_KERESKEDELMI"
  })
  @DisplayName("Adversarial: Primary property type correctly distinguished from accessory amenities")
  void testCompoundHungarianDisambiguation(String title, PropertyType expected) {
    RawPropertyListing raw = RawPropertyListing.builder()
        .url("https://example.com/ad/100")
        .title(title)
        .build();

    PropertyType actual = normalizer.resolvePropertyType(raw);
    assertEquals(expected, actual, "Failed to resolve primary property type for: " + title);
  }

  // --- 3. Obscure Hungarian Terminology ---

  @ParameterizedTest
  @CsvSource({
      "Felújítandó tanya melléképületekkel Ásotthalmon, HAZ",
      "Csendes parasztház búboskemencével, HAZ",
      "Balatoni nyaraló saját stéggel, HAZ",
      "Balatoni üdülő saját stranddal, HAZ",
      "Siófoki üdülőház nagy telken, HAZ",
      "Hétvégi ház hegyvidéki panorámával, HAZ",
      "Zártkert gazdasági épülettel a hegyoldalban, TELEK",
      "Külterületi szántó jó aranykorona értékkel, TELEK",
      "Belterületi építési telek összközműves, TELEK",
      "Fiatalos garzon alacsony rezsivel, LAKAS",
      "Modern garzonlakás egyetemvárosban, LAKAS",
      "Négyszobás panellakás szigetelt tömbben, LAKAS",
      "Emeleti téglalakás parkra néző kilátással, LAKAS",
      "Luxus tetőtéri lakás nagy tetőterasszal, LAKAS",
      "Duplex lakás belső kétszintes elrendezéssel, LAKAS",
      "Ipari csarnok kamionos megközelítéssel, IPARI_KERESKEDELMI",
      "Központi üzlethelyiség utcai bejárattal, IPARI_KERESKEDELMI",
      "Autószerelő műhely működési engedéllyel, IPARI_KERESKEDELMI"
  })
  @DisplayName("Adversarial: Obscure and traditional Hungarian real estate terminology")
  void testObscureHungarianTerminology(String title, PropertyType expected) {
    RawPropertyListing raw = RawPropertyListing.builder()
        .url("https://example.com/ad/200")
        .title(title)
        .build();

    PropertyType actual = normalizer.resolvePropertyType(raw);
    assertEquals(expected, actual, "Failed to resolve terminology for: " + title);
  }

  // --- 4. Accents, Non-Accents, and Case Insensitivity ---

  @ParameterizedTest
  @CsvSource({
      "csaladi haz nagy telekkel, HAZ",
      "CSALÁDI HÁZ GARÁZZSAL, HAZ",
      "CsAlAdI HaZ nAgY kErTtEl, HAZ",
      "tarsashazi lakas belvarosban, LAKAS",
      "TÁRSASHÁZI LAKÁS ERKÉLLYEL, LAKAS",
      "epitesi telek panoramas kilatassal, TELEK",
      "ÉPÍTÉSI TELEK VÍZZEL, TELEK",
      "onallo garazs zart udvarban, GARAZS",
      "ÖNÁLLÓ GARÁZS VILÁGÍTÁSSAL, GARAZS",
      "ipari telephely raktarral, IPARI_KERESKEDELMI",
      "IPARI CSARNOK GYÁRTERÜLETEN, IPARI_KERESKEDELMI"
  })
  @DisplayName("Adversarial: Diacritics, non-accented letters, and mixed casing")
  void testDiacriticsAndCasing(String title, PropertyType expected) {
    RawPropertyListing raw = RawPropertyListing.builder()
        .url("https://example.com/ad/300")
        .title(title)
        .build();

    PropertyType actual = normalizer.resolvePropertyType(raw);
    assertEquals(expected, actual, "Failed for casing/accent: " + title);
  }

  // --- 5. Contradictory & Ambiguous Fallbacks ---

  @ParameterizedTest
  @ValueSource(strings = {
      "Kiváló befektetési lehetőség a belvárosban",
      "Eladó ingatlan jó áron magánszemélytől",
      "Áron alul sürgősen eladó ingatlan",
      "Akciós ajánlat csak ezen a héten"
  })
  @DisplayName("Adversarial: Missing or ambiguous type signals safely fallback to EGYEB_ISMERETLEN")
  void testAmbiguousFallback(String title) {
    RawPropertyListing raw = RawPropertyListing.builder()
        .url("https://example.com/ad/400")
        .title(title)
        .build();

    PropertyType actual = normalizer.resolvePropertyType(raw);
    assertEquals(PropertyType.EGYEB_ISMERETLEN, actual, "Ambiguous title did not fallback cleanly: " + title);
  }

  // --- 6. Concurrency, Thread Safety & Determinism Stress Harness ---

  @Test
  @DisplayName("Stress: 50 concurrent worker threads executing 5,000 normalizations are 100% deterministic")
  void testConcurrentDeterministicStressHarness() throws InterruptedException, ExecutionException {
    int threadCount = 50;
    int iterationsPerThread = 100;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);

    List<RawPropertyListing> sampleListings = List.of(
        RawPropertyListing.builder()
            .portalId("dh")
            .url("https://dh.hu/elado-ingatlan/lakas/debrecen/101")
            .title("Családi ház garázzsal")
            .priceHuf(50_000_000L)
            .areaSquareMeters(100.0)
            .roomCount(3.0)
            .build(),
        RawPropertyListing.builder()
            .portalId("oc")
            .url("https://oc.hu/ingatlanok/lista/jelleg:telek")
            .title("Építési telek")
            .priceHuf(20_000_000L)
            .areaSquareMeters(800.0)
            .roomCount(0.0)
            .build(),
        RawPropertyListing.builder()
            .portalId("jofogas")
            .url("https://ingatlan.jofogas.hu/lakas_1.htm")
            .title("Panellakás felújítva")
            .priceHuf(35_000_000L)
            .areaSquareMeters(55.0)
            .roomCount(2.0)
            .build(),
        RawPropertyListing.builder()
            .portalId("dh")
            .url("https://dh.hu/ingatlan/ambiguous")
            .title("Befektetés a centrumban")
            .priceHuf(40_000_000L)
            .areaSquareMeters(60.0)
            .roomCount(2.0)
            .build()
    );

    List<Callable<List<PropertyType>>> tasks = new ArrayList<>();
    for (int t = 0; t < threadCount; t++) {
      tasks.add(() -> {
        List<PropertyType> resolved = new ArrayList<>();
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");
        for (int i = 0; i < iterationsPerThread; i++) {
          for (RawPropertyListing raw : sampleListings) {
            String portalHost = switch (raw.portalId()) {
              case "dh" -> "https://dh.hu";
              case "oc" -> "https://oc.hu";
              default -> "https://ingatlan.jofogas.hu";
            };
            PortalDescriptor itemDesc = new PortalDescriptor(
                raw.portalId(), raw.portalId(), "#000", portalHost, true, List.of(), true);
            IngatlanItem item = normalizer.normalize(raw, itemDesc, criteria, "2026-10-24 14:00:00");
            assertNotNull(item);
            resolved.add(item.getPropertyType());
          }
        }
        return resolved;
      });
    }

    List<Future<List<PropertyType>>> futures = executor.invokeAll(tasks);
    List<PropertyType> baseline = null;

    for (Future<List<PropertyType>> future : futures) {
      List<PropertyType> result = future.get();
      assertEquals(iterationsPerThread * sampleListings.size(), result.size());
      if (baseline == null) {
        baseline = Collections.unmodifiableList(result);
      } else {
        assertEquals(baseline, result, "Non-deterministic result detected under concurrent thread execution!");
      }
    }

    executor.shutdown();
  }

  // --- 7. PropertyType.fromString Boundary Stress ---

  @ParameterizedTest
  @CsvSource({
      "lakás, LAKAS",
      "LAKAS, LAKAS",
      "flat, LAKAS",
      "APARTMENT, LAKAS",
      "haz, HAZ",
      "HÁZ, HAZ",
      "house, HAZ",
      "telek, TELEK",
      "plot, TELEK",
      "LAND, TELEK",
      "garázs, GARAZS",
      "garage, GARAZS",
      "ipari, IPARI_KERESKEDELMI",
      "commercial, IPARI_KERESKEDELMI",
      "INDUSTRIAL, IPARI_KERESKEDELMI",
      "egyéb / ismeretlen, EGYEB_ISMERETLEN",
      "   , EGYEB_ISMERETLEN",
      "xyz_invalid, EGYEB_ISMERETLEN"
  })
  @DisplayName("PropertyType.fromString handles aliases, casing, whitespace, and unknown fallbacks")
  void testPropertyTypeFromString(String input, PropertyType expected) {
    assertEquals(expected, PropertyType.fromString(input));
  }
}
