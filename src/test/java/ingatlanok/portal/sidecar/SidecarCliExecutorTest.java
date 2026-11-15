package ingatlanok.portal.sidecar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ingatlanok.common.JsonSupport;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawExtractionResult;
import ingatlanok.portal.ingatlancom.IngatlanComSidecarStrategy;

class SidecarCliExecutorUnitTest {

  @AfterEach
  void tearDown() {
    SidecarCliExecutor.resetCustomExecutable();
    IngatlanComSidecarStrategy.clearCache();
  }

  @Test
  @DisplayName("SidecarCliExecutor handles null inputs safely")
  void testExecuteWithNullInputs() {
    SearchCriteria criteria = new SearchCriteria();
    Optional<SidecarResponse> resp1 = SidecarCliExecutor.execute(null, criteria);
    assertTrue(resp1.isEmpty());

    Optional<SidecarResponse> resp2 = SidecarCliExecutor.execute("dh", null);
    assertTrue(resp2.isEmpty());
  }

  @Test
  @DisplayName("SidecarCliExecutor custom executable configuration and argument building")
  void testCustomExecutable() {
    SidecarCliExecutor.setCustomExecutable("test-fake-sidecar");
    assertTrue(SidecarCliExecutor.isAvailable());

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType("haz");
    criteria.setMinPriceMillion(50);
    criteria.setMaxPriceMillion(100);
    criteria.setMinArea(80);
    criteria.setMaxArea(180);

    var builtArgs = SidecarCliExecutor.buildArguments("ingatlancom", criteria);
    assertTrue(builtArgs.contains("--min-area"));
    assertTrue(builtArgs.contains("80"));
    assertTrue(builtArgs.contains("--max-area"));
    assertTrue(builtArgs.contains("180"));
    assertTrue(builtArgs.contains("--max-pages"));
    assertTrue(builtArgs.contains(String.valueOf(SidecarCliExecutor.DEFAULT_MAX_PAGES)));
    assertEquals(20, SidecarCliExecutor.DEFAULT_MAX_PAGES);
    assertEquals(20, IngatlanComSidecarStrategy.DEFAULT_MAX_PAGES);
    int timeoutSec20 = SidecarCliExecutor.calculateTimeoutSeconds(20);
    int softTimeoutMs20 = SidecarCliExecutor.calculateSoftTimeoutMs(20);
    assertTrue(
        timeoutSec20 >= 60 && timeoutSec20 <= 110,
        "Sidecar timeout legyen 60..110s között: " + timeoutSec20);
    assertTrue(
        softTimeoutMs20 >= 45_000 && softTimeoutMs20 <= 95_000,
        "Playwright soft timeout <= 95s: " + softTimeoutMs20);

    var customPageArgs = SidecarCliExecutor.buildArguments("ingatlancom", criteria, 12);
    assertTrue(customPageArgs.contains("--max-pages"));
    assertTrue(customPageArgs.contains("12"));
    assertTrue(customPageArgs.contains("--timeout-ms"));
    assertTrue(customPageArgs.contains(String.valueOf(SidecarCliExecutor.calculateSoftTimeoutMs(12))));

    Optional<SidecarResponse> res = SidecarCliExecutor.execute("ingatlancom", criteria);
    assertTrue(res.isEmpty());

    SidecarCliExecutor.resetCustomExecutable();
  }

  @Test
  @DisplayName("SidecarResponse and SidecarItem model deserialization and conversion")
  void testSidecarResponseMapping() {
    String json =
        """
        {
            "status": "SUCCESS",
            "portalId": "ingatlancom",
            "items": [
                {
                    "id": "12345",
                    "url": "https://ingatlan.com/12345",
                    "title": "Szép lakás",
                    "priceRaw": "65 M Ft",
                    "priceMillion": 65.0,
                    "areaSquareMeters": 72.0,
                    "roomCount": 2.5,
                    "imageUrl": "https://ingatlan.com/photo.jpg"
                }
            ],
            "errorMessage": null,
            "pagesProcessed": 6,
            "stopReason": "NO_MORE_PAGES"
        }
        """;

    SidecarResponse response = JsonSupport.mapper().readValue(json, SidecarResponse.class);
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals("ingatlancom", response.getPortalId());
    assertEquals(1, response.getItems().size());
    assertEquals(6, response.getPagesProcessed());
    assertEquals("NO_MORE_PAGES", response.getStopReason());

    SidecarResponse.SidecarItem item = response.getItems().get(0);
    assertEquals("12345", item.getId());
    assertEquals("https://ingatlan.com/12345", item.getUrl());
    assertEquals(72.0, item.getAreaSquareMeters());
    assertEquals(2.5, item.getRoomCount());

    IngatlanItem domainItem =
        item.toIngatlanItem(
            PortalSource.of("Ingatlan.com"), "Debrecen", "2026-10-13 12:00:00", true);
    assertNotNull(domainItem);
    assertEquals("12345", domainItem.getId());
    assertEquals("https://ingatlan.com/12345", domainItem.getUrl());
    assertTrue(domainItem.isNew());
    assertNotNull(domainItem.getPriceInfo());
    assertEquals(65.0, domainItem.getPriceInfo().getAmountMillion());
    assertNotNull(domainItem.getDetailsInfo());
    assertEquals(72.0, domainItem.getDetailsInfo().getAreaSquareMeters());
    assertEquals(2.5, domainItem.getDetailsInfo().getRoomCount());
  }

  @Test
  @DisplayName("SidecarItem conversion handles missing price and property details")
  void testSidecarItemConversionFallbacks() {
    SidecarResponse.SidecarItem withoutPrice =
        new SidecarResponse.SidecarItem("1", "https://example.test/1", " ", null, null, null, null);
    IngatlanItem unavailable = withoutPrice.toIngatlanItem(null, "Debrecen", "today", false);
    assertEquals("Debrecen - Ingatlan (1)", unavailable.getTitle());
    assertEquals("Ár a hirdetésben", unavailable.getPriceInfo().getRawText());
    assertEquals(0, unavailable.getDetailsInfo().getRawText().length());

    SidecarResponse.SidecarItem withRawPrice =
        new SidecarResponse.SidecarItem("2", "https://example.test/2", "Ház", null, "12 M Ft", null, null);
    IngatlanItem rawPrice = withRawPrice.toIngatlanItem(null, "Szeged", "today", false);
    assertEquals("12 M Ft", rawPrice.getPriceInfo().getRawText());
    assertEquals("Szeged", rawPrice.getCity());
  }

  @Test
  @DisplayName("IngatlanComSidecarStrategy handles non-existent custom executable gracefully")
  void testSidecarStrategyWithFakeExecutable() {
    IngatlanComSidecarStrategy.clearCache();
    SidecarCliExecutor.setCustomExecutable("non-existent-sidecar-bin");
    IngatlanComSidecarStrategy strategy = new IngatlanComSidecarStrategy();
    assertEquals(ExtractionMethod.SIDECAR, strategy.method());

    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Szeged");
    criteria.setPropertyType("haz");

    RawExtractionResult result = strategy.extract(criteria);
    assertNotNull(result);
    assertTrue(result.listings().isEmpty());
  }

  @Test
  @DisplayName("IngatlanComSidecarStrategy ingests 102 multi-page items and pagination metadata")
  void testSidecarStrategyMultiPage102Items(@TempDir Path tempDir) throws Exception {
    IngatlanComSidecarStrategy.clearCache();
    StringBuilder itemsJson = new StringBuilder();
    for (int i = 1; i <= 102; i++) {
      if (i > 1) {
        itemsJson.append(",");
      }
      int id = 3000000 + i;
      itemsJson.append(
          String.format(
              "{\"id\":\"%d\",\"url\":\"https://ingatlan.com/%d\","
                  + "\"title\":\"Hajduszoboszlo haz %d\",\"priceMillion\":85.0,"
                  + "\"priceRaw\":\"85 M Ft\",\"areaSquareMeters\":120.0,\"roomCount\":4.0}",
              id, id, i));
    }

    Path payloadFile = tempDir.resolve("sidecar-102.json");
    String payload =
        String.format(
            "{\"portalId\":\"ingatlancom\",\"status\":\"SUCCESS\",\"items\":[%s],"
                + "\"errorMessage\":null,\"elapsedMs\":1200,\"pagesProcessed\":6,"
                + "\"stopReason\":\"NO_MORE_PAGES\"}",
            itemsJson);
    Files.writeString(payloadFile, payload, StandardCharsets.UTF_8);

    boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
    Path scriptFile = tempDir.resolve(isWindows ? "mock-sidecar.cmd" : "mock-sidecar.sh");
    String scriptContent = isWindows
        ? "@echo off\r\ntype \"" + payloadFile.toAbsolutePath() + "\"\r\n"
        : "#!/bin/sh\ncat \"" + payloadFile.toAbsolutePath() + "\"\n";
    Files.writeString(scriptFile, scriptContent, StandardCharsets.UTF_8);
    if (!isWindows) {
      scriptFile.toFile().setExecutable(true);
    }

    SidecarCliExecutor.setCustomExecutable(scriptFile.toAbsolutePath().toString());
    IngatlanComSidecarStrategy strategy = new IngatlanComSidecarStrategy();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Hajdúszoboszló");
    criteria.setPropertyType("haz");
    criteria.setMinPriceMillion(60);
    criteria.setMaxPriceMillion(130);
    criteria.setMinArea(95);
    criteria.setMaxArea(200);

    RawExtractionResult result = strategy.extract(criteria);
    assertEquals(102, result.listings().size());
    assertNotNull(result.technicalDetails());
    assertTrue(result.technicalDetails().contains("pages=6"));
    assertTrue(result.technicalDetails().contains("stopReason=NO_MORE_PAGES"));
  }

  @Test
  @DisplayName("SidecarCliExecutor falls back to secondary sidecar on partial bot-protected stop")
  void testSidecarFallbackRecoversMoreListings(@TempDir Path tempDir) throws Exception {
    IngatlanComSidecarStrategy.clearCache();
    Path primaryScript = createMockSidecarScript(
        tempDir,
        "primary",
        buildMockPayloadJson(20, 1, "INTERMEDIATE_PAGE_BOT_PROTECTED", "SUCCESS", null));
    Path fallbackScript = createMockSidecarScript(
        tempDir,
        "fallback",
        buildMockPayloadJson(62, 4, "NO_MORE_PAGES", "SUCCESS", null));

    SidecarCliExecutor.setCustomExecutables(
        List.of(primaryScript.toAbsolutePath().toString(), fallbackScript.toAbsolutePath().toString()));
    IngatlanComSidecarStrategy strategy = new IngatlanComSidecarStrategy();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Hajdúszoboszló");
    criteria.setPropertyType("haz");

    RawExtractionResult result = strategy.extract(criteria);
    assertEquals(62, result.listings().size());
    assertNotNull(result.technicalDetails());
    assertTrue(result.technicalDetails().contains("pages=4"));
    assertTrue(result.technicalDetails().contains("stopReason=NO_MORE_PAGES"));
  }

  @Test
  @DisplayName("SidecarCliExecutor keeps primary partial listings when fallback also fails")
  void testSidecarFallbackPreservesPrimaryOnFailure(@TempDir Path tempDir) throws Exception {
    IngatlanComSidecarStrategy.clearCache();
    Path primaryScript = createMockSidecarScript(
        tempDir,
        "primary-only",
        buildMockPayloadJson(20, 1, "INTERMEDIATE_PAGE_BOT_PROTECTED", "SUCCESS", null));
    Path failedFallbackScript = createMockSidecarScript(
        tempDir,
        "fallback-fail",
        buildMockPayloadJson(0, 0, "FIRST_PAGE_BOT_PROTECTED", "FAILED", "HTTP 429"));

    SidecarCliExecutor.setCustomExecutables(
        List.of(
            primaryScript.toAbsolutePath().toString(),
            failedFallbackScript.toAbsolutePath().toString()));
    IngatlanComSidecarStrategy strategy = new IngatlanComSidecarStrategy();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Hajdúszoboszló");
    criteria.setPropertyType("haz");

    RawExtractionResult result = strategy.extract(criteria);
    assertEquals(20, result.listings().size());
    assertNotNull(result.message());
    assertTrue(result.message().contains("Cloudflare"));
  }

  @Test
  @DisplayName("IngatlanComSidecarStrategy gyorsítótár megőrzi a partial warning és details mezőket")
  void testSidecarStrategyCachePreservesDetailsAndWarning(@TempDir Path tempDir) throws Exception {
    IngatlanComSidecarStrategy.clearCache();
    Path primaryScript = createMockSidecarScript(
        tempDir,
        "partial-cache",
        buildMockPayloadJson(20, 1, "INTERMEDIATE_PAGE_BOT_PROTECTED", "SUCCESS", null));
    SidecarCliExecutor.setCustomExecutables(List.of(primaryScript.toAbsolutePath().toString()));
    IngatlanComSidecarStrategy strategy = new IngatlanComSidecarStrategy();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Hajdúszoboszló");
    criteria.setPropertyType("haz");

    RawExtractionResult first = strategy.extract(criteria);
    assertEquals(20, first.listings().size());
    assertNotNull(first.technicalDetails());
    assertNotNull(first.message());

    SidecarCliExecutor.setCustomExecutables(List.of("non-existent-sidecar"));
    RawExtractionResult cached = strategy.extract(criteria);
    assertEquals(20, cached.listings().size());
    assertNotNull(cached.technicalDetails(), "A cache találatnak meg kell őriznie a details mezőt");
    assertTrue(cached.technicalDetails().contains("pages=1"));
    assertNotNull(cached.message(), "A cache találatnak meg kell őriznie a warningMessage mezőt");
    assertTrue(cached.message().contains("Cloudflare"));
  }

  @Test
  @DisplayName("SidecarCliExecutor megszakításkor visszaállítja a szál interrupt jelzőjét")
  void testSidecarCliExecutorRestoresInterruptFlagOnInterruption(@TempDir Path tempDir)
      throws Exception {
    IngatlanComSidecarStrategy.clearCache();
    boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
    Path slowScript = tempDir.resolve(isWindows ? "slow-sidecar.cmd" : "slow-sidecar.sh");
    String scriptContent = isWindows
        ? "@echo off\r\nping -n 6 127.0.0.1 >nul\r\n"
        : "#!/bin/sh\nsleep 5\n";
    Files.writeString(slowScript, scriptContent, StandardCharsets.UTF_8);
    if (!isWindows) {
      slowScript.toFile().setExecutable(true);
    }

    SidecarCliExecutor.setCustomExecutables(List.of(slowScript.toAbsolutePath().toString()));
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");
    criteria.setPropertyType("haz");

    AtomicBoolean interruptFlagPreserved = new AtomicBoolean(false);
    AtomicReference<Optional<SidecarResponse>> resultRef = new AtomicReference<>();
    Thread worker =
        new Thread(
            () -> {
              resultRef.set(SidecarCliExecutor.execute("ingatlancom", criteria));
              interruptFlagPreserved.set(Thread.currentThread().isInterrupted());
            });
    worker.start();
    Thread.sleep(250);
    worker.interrupt();
    worker.join(3000);

    assertTrue(!worker.isAlive(), "A megszakított sidecar hívásnak azonnal le kell állnia");
    assertNotNull(resultRef.get());
    assertTrue(resultRef.get().isEmpty());
    assertTrue(
        interruptFlagPreserved.get(),
        "InterruptedException után a szál interrupt állapotát vissza kell állítani");
  }

  private static String buildMockPayloadJson(
      int itemCount, int pagesProcessed, String stopReason, String status, String errorMessage) {
    StringBuilder itemsJson = new StringBuilder();
    for (int i = 1; i <= itemCount; i++) {
      if (i > 1) {
        itemsJson.append(",");
      }
      int id = 4000000 + i;
      itemsJson.append(
          String.format(
              "{\"id\":\"%d\",\"url\":\"https://ingatlan.com/%d\","
                  + "\"title\":\"Hajduszoboszlo haz %d\",\"priceMillion\":85.0,"
                  + "\"priceRaw\":\"85 M Ft\",\"areaSquareMeters\":120.0,\"roomCount\":4.0}",
              id, id, i));
    }
    String errJson = errorMessage == null ? "null" : "\"" + errorMessage + "\"";
    return String.format(
        "{\"portalId\":\"ingatlancom\",\"status\":\"%s\",\"items\":[%s],"
            + "\"errorMessage\":%s,\"elapsedMs\":500,\"pagesProcessed\":%d,\"stopReason\":\"%s\"}",
        status, itemsJson, errJson, pagesProcessed, stopReason);
  }

  private static Path createMockSidecarScript(Path tempDir, String name, String payloadJson)
      throws Exception {
    Path payloadFile = tempDir.resolve(name + ".json");
    Files.writeString(payloadFile, payloadJson, StandardCharsets.UTF_8);

    boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
    Path scriptFile = tempDir.resolve(name + (isWindows ? ".cmd" : ".sh"));
    String scriptContent = isWindows
        ? "@echo off\r\ntype \"" + payloadFile.toAbsolutePath() + "\"\r\n"
        : "#!/bin/sh\ncat \"" + payloadFile.toAbsolutePath() + "\"\n";
    Files.writeString(scriptFile, scriptContent, StandardCharsets.UTF_8);
    if (!isWindows) {
      scriptFile.toFile().setExecutable(true);
    }
    return scriptFile;
  }
}
