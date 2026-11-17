package ingatlanok.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.core.normalization.ListingNormalizer;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PropertyType;
import ingatlanok.persistence.InMemoryListingStorage;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.testsupport.TestFixtures;

class IngatlanComHtmlImportServiceTest {

  private static final String SAMPLE_HTML =
      """
      <html>
        <head><title>Eladó hajdúszoboszlói házak - ingatlan.com</title></head>
        <body>
          <h1>Eladó hajdúszoboszlói családi házak</h1>
          <a href="/34001001" class="listing-card" data-listing-id="34001001">
            <span class="d-block fs-7 text-gray-900">Hajdúszoboszló, Kossuth utca 12.</span>
            <span class="fw-bold fs-5">89 M Ft</span>
            <span>Alapterület 125 m² Szobák 4</span>
          </a>
          <a href="/34001002" class="listing-card" data-listing-id="34001002">
            <span class="d-block fs-7 text-gray-900">Hajdúszoboszló, József Attila utca 5.</span>
            <span class="fw-bold fs-5">112 M Ft</span>
            <span>Alapterület 150 m² Szobák 4+1</span>
          </a>
        </body>
      </html>
      """;

  private InMemoryListingStorage storage;
  private IngatlanComHtmlImportService importService;

  @BeforeEach
  void setUp() {
    storage = new InMemoryListingStorage();
    ListingNormalizer normalizer = TestFixtures.listingNormalizer();
    PortalEngineRegistry registry =
        new PortalEngineRegistry(List.of(TestFixtures.ingatlanComEngine(normalizer)));
    DataQualityEngine qualityEngine = new DataQualityEngine();
    importService =
        new IngatlanComHtmlImportService(
            registry,
            normalizer,
            TestFixtures.deduplicationEngine(storage),
            qualityEngine,
            storage,
            new SearchPersistenceSynchronizer(registry, storage));
  }

  @Test
  @DisplayName("HTML import kinyeri a hirdetéseket, felismeri a várost/típust és elmenti a tárolóba")
  void testImportHtmlParsesAndPersistsListings() {
    var summary =
        importService.importHtml(
            SAMPLE_HTML,
            "https://ingatlan.com/lista/elado+haz+95-200-m2+60-130-mFt+hajduszoboszlo?page=2",
            null,
            null);

    assertEquals("Hajdúszoboszló", summary.city());
    assertEquals(2, summary.parsedCount());
    assertEquals(2, summary.savedCount());
    assertEquals(2, summary.newlyAddedCount());
    assertEquals(0, summary.duplicateCount());
    assertEquals(2, storage.getItemsByCity("Hajdúszoboszló").size());

    IngatlanItem first = summary.items().getFirst();
    assertEquals(PropertyType.HAZ, first.getPropertyType());
    assertEquals("https://ingatlan.com/34001001", first.getUrl());
    assertNotNull(first.getQualityScore());

    var secondRun =
        importService.importHtml(
            SAMPLE_HTML,
            "https://ingatlan.com/lista/elado+haz+hajduszoboszlo?page=2",
            "Hajdúszoboszló",
            PropertyType.HAZ);
    assertEquals(2, secondRun.savedCount());
    assertEquals(0, secondRun.newlyAddedCount());
    assertEquals(2, secondRun.duplicateCount());
    assertFalse(secondRun.items().isEmpty());
  }

  @Test
  @DisplayName("HTML import kártya nélküli oldal esetén üres összefoglalóval tér vissza hiba nélkül")
  void testImportHtmlWithoutCardsReturnsEmptySummary() {
    var summary =
        importService.importHtml(
            "<html><body><p>Challenge page</p></body></html>",
            "https://ingatlan.com/lista/elado+haz+debrecen?page=2",
            "Debrecen",
            PropertyType.HAZ);

    assertEquals(0, summary.parsedCount());
    assertEquals(0, summary.savedCount());
    assertTrue(summary.items().isEmpty());
  }

  @Test
  @DisplayName("HTML import visszautasítja az üres, túl nagy vagy nem ingatlan.com forrás URL-t")
  void testImportHtmlValidationGuards() {
    assertThrows(
        IllegalArgumentException.class,
        () -> importService.importHtml("   ", null, "Debrecen", PropertyType.HAZ));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            importService.importHtml(
                "<html></html>", "https://evil.example/phish", "Debrecen", PropertyType.HAZ));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            importService.importHtml(
                "<html></html>", "javascript:alert(1)", "Debrecen", PropertyType.HAZ));

    String oversized = "a".repeat(IngatlanComHtmlImportService.MAX_HTML_BYTES + 1);
    assertThrows(
        IllegalArgumentException.class,
        () -> importService.importHtml(oversized, null, "Debrecen", PropertyType.HAZ));
  }
}
