package ingatlanok.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.dh.DunaHousePortalEngine;
import ingatlanok.portal.dh.DunaHouseUrlBuilder;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarPortalEngine;
import ingatlanok.portal.ingatlancom.IngatlanComPortalEngine;
import ingatlanok.portal.ingatlancom.IngatlanComUrlBuilder;
import ingatlanok.portal.jofogas.JofogasPortalEngine;
import ingatlanok.portal.jofogas.JofogasUrlBuilder;
import ingatlanok.portal.koltozzbe.KoltozzbePortalEngine;
import ingatlanok.portal.koltozzbe.KoltozzbeUrlBuilder;
import ingatlanok.portal.oc.OtthonCentrumPortalEngine;
import ingatlanok.portal.oc.OtthonCentrumUrlBuilder;

/**
 * Deterministic, offline Golden Dataset regression test suite for real estate scrapers (#370).
 * Asserts portal-level, record-level, and field-level parity through production adapter pipelines.
 */
@DisplayName("Portal Golden Dataset Regression Test Suite (#370)")
class PortalGoldenDatasetRegressionTest {

  private static final String GOLDEN_BASE_PATH = "/fixtures/golden/portals/";
  private static final List<String> SUPPORTED_PORTALS = List.of(
      "dh", "oc", "jofogas", "ingatlanbazar", "koltozzbe", "ingatlancom");

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("Portal Golden Dataset completeness check across all supported portals")
  void testSupportedPortalsCompleteness() {
    for (String portalId : SUPPORTED_PORTALS) {
      assertNotNull(
          getClass().getResourceAsStream(GOLDEN_BASE_PATH + portalId + "/metadata.json"),
          "metadata.json must exist for portal: " + portalId);
      assertNotNull(
          getClass().getResourceAsStream(GOLDEN_BASE_PATH + portalId + "/expected.json"),
          "expected.json must exist for portal: " + portalId);

      String sourceFileName = "ingatlanbazar".equals(portalId) ? "source.json" : "source.html";
      assertNotNull(
          getClass().getResourceAsStream(GOLDEN_BASE_PATH + portalId + "/" + sourceFileName),
          sourceFileName + " must exist for portal: " + portalId);
    }
  }

  @Test
  @DisplayName("Duna House (dh) Golden Dataset offline scraper regression test")
  void testDunaHouseGoldenDataset() throws IOException {
    PortalFixture fixture = loadFixture("dh", "source.html");
    Document doc = Jsoup.parse(fixture.sourceContent(), DunaHouseUrlBuilder.BASE_URL);

    DunaHousePortalEngine engine = new DunaHousePortalEngine();
    PortalScrapeResult result = engine.scrape(fixture.criteria(), url -> doc);

    assertScrapeResultMatches(fixture, result);
  }

  @Test
  @DisplayName("Otthon Centrum (oc) Golden Dataset offline scraper regression test")
  void testOtthonCentrumGoldenDataset() throws IOException {
    PortalFixture fixture = loadFixture("oc", "source.html");
    Document doc = Jsoup.parse(fixture.sourceContent(), OtthonCentrumUrlBuilder.BASE_URL);

    OtthonCentrumPortalEngine engine = new OtthonCentrumPortalEngine();
    PortalScrapeResult result = engine.scrape(fixture.criteria(), url -> doc);

    assertScrapeResultMatches(fixture, result);
  }

  @Test
  @DisplayName("Jófogás Ingatlan (jofogas) Golden Dataset offline scraper regression test")
  void testJofogasGoldenDataset() throws IOException {
    PortalFixture fixture = loadFixture("jofogas", "source.html");
    Document doc = Jsoup.parse(fixture.sourceContent(), JofogasUrlBuilder.BASE_URL);

    JofogasPortalEngine engine = new JofogasPortalEngine();
    PortalScrapeResult result = engine.scrape(fixture.criteria(), url -> doc);

    assertScrapeResultMatches(fixture, result);
  }

  @Test
  @DisplayName("Ingatlanbazár (ingatlanbazar) Golden Dataset offline scraper regression test")
  void testIngatlanBazarGoldenDataset() throws IOException {
    PortalFixture fixture = loadFixture("ingatlanbazar", "source.json");

    IngatlanBazarPortalEngine engine = new IngatlanBazarPortalEngine();
    PortalScrapeResult result = engine.scrape(fixture.criteria(), url -> fixture.sourceContent());

    assertScrapeResultMatches(fixture, result);
  }

  @Test
  @DisplayName("Költözzbe (koltozzbe) Golden Dataset offline scraper regression test")
  void testKoltozzbeGoldenDataset() throws IOException {
    PortalFixture fixture = loadFixture("koltozzbe", "source.html");
    Document doc = Jsoup.parse(fixture.sourceContent(), KoltozzbeUrlBuilder.BASE_URL);

    KoltozzbePortalEngine engine = new KoltozzbePortalEngine();
    PortalScrapeResult result = engine.scrape(fixture.criteria(), url -> doc);

    assertScrapeResultMatches(fixture, result);
  }

  @Test
  @DisplayName("Ingatlan.com (ingatlancom) Golden Dataset offline scraper regression test")
  void testIngatlanComGoldenDataset() throws IOException {
    PortalFixture fixture = loadFixture("ingatlancom", "source.html");
    Document doc = Jsoup.parse(fixture.sourceContent(), IngatlanComUrlBuilder.BASE_URL);

    IngatlanComPortalEngine engine = new IngatlanComPortalEngine();
    PortalScrapeResult result = engine.scrape(fixture.criteria(), url -> doc);

    assertScrapeResultMatches(fixture, result);
  }

  @Test
  @DisplayName("Diagnostic reporting surfaces portal-, record-, and field-level discrepancies (#370)")
  void testDiagnosticMismatchReporting() {
    ExpectedListing expected = new ExpectedListing(
        "DIAG-1",
        "https://dh.hu/ingatlan/1",
        "Elvárt cím",
        "LAKAS",
        "Debrecen",
        50_000_000L,
        50.0,
        60.0,
        2.0,
        "https://dh.hu/img.jpg",
        true);

    IngatlanItem actual = IngatlanItem.builder()
        .id("DIAG-1")
        .url("https://dh.hu/ingatlan/different")
        .title("Eltérő cím")
        .city("Nyíregyháza")
        .propertyType(PropertyType.HAZ)
        .priceInfo(PriceInfo.builder().amountHuf(45_000_000L).amountMillion(45.0).build())
        .detailsInfo(PropertyDetails.builder().areaSquareMeters(55.0).roomCount(3.0).build())
        .imageUrl("https://dh.hu/other.jpg")
        .newConstruction(false)
        .build();

    List<String> diffs = new ArrayList<>();
    verifyListingFields("dh", expected, actual, diffs);

    assertEquals(10, diffs.size(), "All 10 differing fields must be captured");
    assertTrue(diffs.stream().anyMatch(d -> d.contains("[dh]") && d.contains("[Mező: priceHuf]")),
        "Must diagnose priceHuf mismatch");
    assertTrue(
        diffs.stream().anyMatch(d -> d.contains("[Rekord: DIAG-1]") && d.contains("[Mező: areaSquareMeters]")),
        "Must diagnose areaSquareMeters mismatch");
    assertTrue(diffs.stream().anyMatch(d -> d.contains("[Mező: propertyType]")),
        "Must diagnose propertyType mismatch");
    assertTrue(diffs.stream().anyMatch(d -> d.contains("[Mező: newConstruction]")),
        "Must diagnose newConstruction mismatch");
  }

  private void assertScrapeResultMatches(PortalFixture fixture, PortalScrapeResult result) {
    assertNotNull(result, "Scrape result must not be null for " + fixture.portalId());
    assertEquals(
        PortalScrapeStatus.SUCCESS,
        result.getStatus(),
        "Scrape status must be SUCCESS for " + fixture.portalId());

    List<IngatlanItem> actualItems = result.getItems();
    List<ExpectedListing> expectedListings = fixture.expectedListings();

    assertFalse(expectedListings.isEmpty(), "Expected listings must not be empty");
    assertEquals(
        expectedListings.size(),
        actualItems.size(),
        String.format(
            "[%s] Találatok száma eltér! Elvárt: %d, Kapott: %d (Elvárt ID-k: %s, Kapott ID-k: %s)",
            fixture.portalId(),
            expectedListings.size(),
            actualItems.size(),
            expectedListings.stream().map(ExpectedListing::id).toList(),
            actualItems.stream().map(IngatlanItem::getId).toList()));

    Map<String, IngatlanItem> actualById =
        actualItems.stream().collect(Collectors.toMap(IngatlanItem::getId, Function.identity()));

    List<String> discrepancies = new ArrayList<>();
    for (ExpectedListing expected : expectedListings) {
      IngatlanItem actual = actualById.get(expected.id());
      if (actual == null) {
        discrepancies.add(
            String.format("[%s] Hiányzó rekord az elvárt találatok közül: ID=%s",
                fixture.portalId(), expected.id()));
        continue;
      }
      verifyListingFields(fixture.portalId(), expected, actual, discrepancies);
    }

    assertTrue(
        discrepancies.isEmpty(),
        String.format(
            "[%s] Eltérések a Golden Dataset mezőkben:\n%s",
            fixture.portalId(),
            String.join("\n", discrepancies)));
  }

  private void verifyListingFields(
      String portalId,
      ExpectedListing expected,
      IngatlanItem actual,
      List<String> diffs) {
    checkField(portalId, expected.id(), "url", expected.url(), actual.getUrl(), diffs);
    checkField(portalId, expected.id(), "title", expected.title(), actual.getTitle(), diffs);
    checkField(portalId, expected.id(), "city", expected.city(), actual.getCity(), diffs);

    String actualType = actual.getPropertyType() != null ? actual.getPropertyType().name() : null;
    checkField(portalId, expected.id(), "propertyType", expected.propertyType(), actualType, diffs);

    Long actualHuf = actual.getPriceInfo() != null ? actual.getPriceInfo().getAmountHuf() : null;
    checkField(portalId, expected.id(), "priceHuf", expected.priceHuf(), actualHuf, diffs);

    Double actualMillion =
        actual.getPriceInfo() != null ? actual.getPriceInfo().getAmountMillion() : null;
    checkField(portalId, expected.id(), "priceMillion", expected.priceMillion(), actualMillion, diffs);

    Double actualArea =
        actual.getDetailsInfo() != null ? actual.getDetailsInfo().getAreaSquareMeters() : null;
    checkField(portalId, expected.id(), "areaSquareMeters", expected.areaSquareMeters(), actualArea, diffs);

    Double actualRooms =
        actual.getDetailsInfo() != null ? actual.getDetailsInfo().getRoomCount() : null;
    checkField(portalId, expected.id(), "roomCount", expected.roomCount(), actualRooms, diffs);

    checkField(portalId, expected.id(), "imageUrl", expected.imageUrl(), actual.getImageUrl(), diffs);
    checkField(
        portalId,
        expected.id(),
        "newConstruction",
        expected.newConstruction(),
        actual.isNewConstruction(),
        diffs);
  }

  private void checkField(
      String portalId,
      String recordId,
      String fieldName,
      Object expected,
      Object actual,
      List<String> diffs) {
    if (!Objects.equals(expected, actual)) {
      diffs.add(
          String.format(
              "  - [%s] [Rekord: %s] [Mező: %s] Elvárt: <%s>, Kapott: <%s>",
              portalId, recordId, fieldName, expected, actual));
    }
  }

  private PortalFixture loadFixture(String portalId, String sourceFileName) throws IOException {
    String basePath = GOLDEN_BASE_PATH + portalId + "/";

    InputStream metaStream = getClass().getResourceAsStream(basePath + "metadata.json");
    assertNotNull(metaStream, "Metadata not found: " + basePath + "metadata.json");
    JsonNode metaNode = objectMapper.readTree(metaStream);

    InputStream sourceStream = getClass().getResourceAsStream(basePath + sourceFileName);
    assertNotNull(sourceStream, "Source not found: " + basePath + sourceFileName);
    String sourceContent = new String(sourceStream.readAllBytes(), StandardCharsets.UTF_8);

    InputStream expectedStream = getClass().getResourceAsStream(basePath + "expected.json");
    assertNotNull(expectedStream, "Expected not found: " + basePath + "expected.json");
    JsonNode expectedArray = objectMapper.readTree(expectedStream);

    List<ExpectedListing> expectedListings = new ArrayList<>();
    for (JsonNode node : expectedArray) {
      expectedListings.add(parseExpectedListing(node));
    }

    SearchCriteria criteria = parseCriteria(metaNode.get("searchCriteria"));
    return new PortalFixture(portalId, sourceContent, criteria, expectedListings);
  }

  private SearchCriteria parseCriteria(JsonNode criteriaNode) {
    SearchCriteria criteria = new SearchCriteria();
    if (criteriaNode != null && !criteriaNode.isNull()) {
      if (criteriaNode.has("city")) {
        criteria.setCity(criteriaNode.get("city").asString());
      }
      if (criteriaNode.has("propertyType")) {
        criteria.setPropertyType(criteriaNode.get("propertyType").asString());
      }
      if (criteriaNode.has("minPriceMillion")) {
        criteria.setMinPriceMillion(criteriaNode.get("minPriceMillion").asInt());
      }
      if (criteriaNode.has("maxPriceMillion")) {
        criteria.setMaxPriceMillion(criteriaNode.get("maxPriceMillion").asInt());
      }
      if (criteriaNode.has("minArea")) {
        criteria.setMinArea(criteriaNode.get("minArea").asInt());
      }
      if (criteriaNode.has("maxArea")) {
        criteria.setMaxArea(criteriaNode.get("maxArea").asInt());
      }
    }
    return criteria;
  }

  private ExpectedListing parseExpectedListing(JsonNode node) {
    return new ExpectedListing(
        node.has("id") && !node.get("id").isNull() ? node.get("id").asString() : null,
        node.has("url") && !node.get("url").isNull() ? node.get("url").asString() : null,
        node.has("title") && !node.get("title").isNull() ? node.get("title").asString() : null,
        node.has("propertyType") && !node.get("propertyType").isNull()
            ? node.get("propertyType").asString()
            : null,
        node.has("city") && !node.get("city").isNull() ? node.get("city").asString() : null,
        node.has("priceHuf") && !node.get("priceHuf").isNull()
            ? node.get("priceHuf").asLong()
            : null,
        node.has("priceMillion") && !node.get("priceMillion").isNull()
            ? node.get("priceMillion").asDouble()
            : null,
        node.has("areaSquareMeters") && !node.get("areaSquareMeters").isNull()
            ? node.get("areaSquareMeters").asDouble()
            : null,
        node.has("roomCount") && !node.get("roomCount").isNull()
            ? node.get("roomCount").asDouble()
            : null,
        node.has("imageUrl") && !node.get("imageUrl").isNull()
            ? node.get("imageUrl").asString()
            : null,
        node.has("newConstruction") && node.get("newConstruction").asBoolean(false));
  }

  private record PortalFixture(
      String portalId,
      String sourceContent,
      SearchCriteria criteria,
      List<ExpectedListing> expectedListings) {}

  private record ExpectedListing(
      String id,
      String url,
      String title,
      String propertyType,
      String city,
      Long priceHuf,
      Double priceMillion,
      Double areaSquareMeters,
      Double roomCount,
      String imageUrl,
      boolean newConstruction) {}
}
