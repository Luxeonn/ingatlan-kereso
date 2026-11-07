package ingatlanok.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawExtractionResult;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.dh.DunaHouseHtmlStrategy;
import ingatlanok.portal.dh.DunaHouseRawParser;
import ingatlanok.portal.dh.DunaHouseUrlBuilder;
import ingatlanok.portal.ingatlancom.IngatlanComHtmlStrategy;
import ingatlanok.portal.ingatlancom.IngatlanComRawParser;
import ingatlanok.portal.ingatlancom.IngatlanComUrlBuilder;
import ingatlanok.portal.jofogas.JofogasHtmlStrategy;
import ingatlanok.portal.jofogas.JofogasRawParser;
import ingatlanok.portal.jofogas.JofogasUrlBuilder;
import ingatlanok.portal.koltozzbe.KoltozzbeExtractionStrategy;
import ingatlanok.portal.koltozzbe.KoltozzbeRawParser;
import ingatlanok.portal.koltozzbe.KoltozzbeUrlBuilder;
import ingatlanok.portal.oc.OtthonCentrumHtmlStrategy;
import ingatlanok.portal.oc.OtthonCentrumRawParser;
import ingatlanok.portal.oc.OtthonCentrumUrlBuilder;

class PortalChallengeAndFallbackRegressionTest {

  private Document cloudflareDoc;
  private Document recaptchaDoc;
  private Document normalDoc;

  @BeforeEach
  void setUp() throws Exception {
    try (InputStream is = getClass().getResourceAsStream(
        "/fixtures/edgecases/challenges/cloudflare-challenge.html")) {
      assertNotNull(is);
      cloudflareDoc = Jsoup.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
    }
    try (InputStream is = getClass().getResourceAsStream(
        "/fixtures/edgecases/challenges/recaptcha-challenge.html")) {
      assertNotNull(is);
      recaptchaDoc = Jsoup.parse(new String(is.readAllBytes(), StandardCharsets.UTF_8));
    }
    normalDoc = Jsoup.parse("<html><head><title>Eladó lakások Debrecen</title></head><body>Normal</body></html>");
  }

  @Test
  @DisplayName("PortalSupport: Cloudflare challenge oldal felismerése")
  void testCloudflareChallengeDetectionRecognizedByPortalSupport() {
    assertTrue(PortalSupport.isCommonBotChallenge(cloudflareDoc));
    assertFalse(PortalSupport.isCommonBotChallenge(normalDoc));
  }

  @Test
  @DisplayName("PortalSupport: reCAPTCHA és robot ellenőrzés challenge felismerése")
  void testRecaptchaChallengeDetectionRecognizedByPortalSupport() {
    assertTrue(PortalSupport.isCommonBotChallenge(recaptchaDoc));
  }

  @Test
  @DisplayName("Portál HTML stratégiák: challenge lap esetén BOT_PROTECTED státuszt adnak vissza")
  void testAllHtmlStrategiesReturnBotProtectedOnChallengePage() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");

    // Duna House
    DunaHouseHtmlStrategy dhStrategy = new DunaHouseHtmlStrategy(
        new DunaHouseUrlBuilder(), new DunaHouseRawParser(), url -> cloudflareDoc);
    RawExtractionResult dhResult = dhStrategy.extract(criteria);
    assertEquals(PortalScrapeStatus.BOT_PROTECTED, dhResult.status(),
        "Duna House-nak bot-védelmet kell detektálnia");

    // Otthon Centrum
    OtthonCentrumHtmlStrategy ocStrategy = new OtthonCentrumHtmlStrategy(
        new OtthonCentrumUrlBuilder(), new OtthonCentrumRawParser(), url -> cloudflareDoc);
    RawExtractionResult ocResult = ocStrategy.extract(criteria);
    assertEquals(PortalScrapeStatus.BOT_PROTECTED, ocResult.status(),
        "Otthon Centrumnak bot-védelmet kell detektálnia");

    // Jófogás
    JofogasHtmlStrategy jfStrategy = new JofogasHtmlStrategy(
        new JofogasUrlBuilder(), new JofogasRawParser(), url -> recaptchaDoc);
    RawExtractionResult jfResult = jfStrategy.extract(criteria);
    assertEquals(PortalScrapeStatus.BOT_PROTECTED, jfResult.status(),
        "Jófogásnak bot-védelmet kell detektálnia");

    // Költözzbe
    KoltozzbeExtractionStrategy kbStrategy = new KoltozzbeExtractionStrategy(
        new KoltozzbeUrlBuilder(), new KoltozzbeRawParser(), url -> cloudflareDoc, 1);
    RawExtractionResult kbResult = kbStrategy.extract(criteria);
    assertEquals(PortalScrapeStatus.BOT_PROTECTED, kbResult.status(),
        "Költözzbe-nek bot-védelmet kell detektálnia");

    // Ingatlan.com
    IngatlanComHtmlStrategy icStrategy = new IngatlanComHtmlStrategy(
        new IngatlanComUrlBuilder(), new IngatlanComRawParser(), url -> cloudflareDoc);
    RawExtractionResult icResult = icStrategy.extract(criteria);
    assertEquals(PortalScrapeStatus.BOT_PROTECTED, icResult.status(),
        "Ingatlan.com-nak bot-védelmet kell detektálnia");
  }

  @Test
  @DisplayName("AdapterPortalEngine: challenge esetén BOT_PROTECTED kimenetet és fallback kereső URL-t ad")
  void testAdapterPortalEngineProducesBotProtectedStatusWithFallbackUrlOnChallenge() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");

    PortalDescriptor desc = new PortalDescriptor(
        "test-portal", "Test Portal", "#000", "https://test.hu", true, List.of("lakas"), false);
    ExtractionStrategy failingStrategy = new ExtractionStrategy() {
      @Override
      public ExtractionMethod method() {
        return ExtractionMethod.HTML;
      }

      @Override
      public RawExtractionResult extract(SearchCriteria c) {
        return RawExtractionResult.botProtected(
            ExtractionMethod.HTML, "https://test.hu/kereses?q=debrecen", "Cloudflare WAF challenge");
      }
    };

    PortalAdapter adapter = new PortalAdapter() {
      @Override
      public PortalDescriptor getDescriptor() {
        return desc;
      }

      @Override
      public PortalUrlBuilder getUrlBuilder() {
        return c -> "https://test.hu/kereses?q=debrecen";
      }

      @Override
      public List<ExtractionStrategy> getStrategies() {
        return List.of(failingStrategy);
      }
    };

    AdapterPortalEngine engine = new AdapterPortalEngine(adapter);
    PortalScrapeResult result = engine.scrape(criteria);

    assertEquals(PortalScrapeStatus.BOT_PROTECTED, result.getStatus());
    assertEquals("https://test.hu/kereses?q=debrecen", result.getFallbackUrl());
  }

  @Test
  @DisplayName("AdapterPortalEngine: fallback lánc sorrend teszt - első stratégia hiba esetén a második fut le")
  void testStrategyFallbackSequenceFallsBackToNextStrategyWhenFirstFails() {
    SearchCriteria criteria = new SearchCriteria();
    criteria.setCity("Debrecen");

    PortalDescriptor desc = new PortalDescriptor(
        "fallback-portal", "Fallback Portal", "#000", "https://fallback.hu", true, List.of("lakas"), false);

    ExtractionStrategy firstStrategy = new ExtractionStrategy() {
      @Override
      public ExtractionMethod method() {
        return ExtractionMethod.API;
      }

      @Override
      public RawExtractionResult extract(SearchCriteria c) {
        return RawExtractionResult.failure(
            ExtractionMethod.API, "https://fallback.hu", "503 Service Unavailable", 503);
      }
    };

    ExtractionStrategy secondStrategy = new ExtractionStrategy() {
      @Override
      public ExtractionMethod method() {
        return ExtractionMethod.HTML;
      }

      @Override
      public RawExtractionResult extract(SearchCriteria c) {
        RawPropertyListing raw = RawPropertyListing.builder()
            .portalId("fallback-portal")
            .nativeId("FB101")
            .url("https://fallback.hu/ingatlan/FB101")
            .title("Debrecen Lakás")
            .priceText("50 M Ft")
            .detailsText("60 m2 • 2 szoba")
            .locationText("Debrecen")
            .method(ExtractionMethod.HTML)
            .build();
        return RawExtractionResult.success(List.of(raw), ExtractionMethod.HTML);
      }
    };

    PortalAdapter adapter = new PortalAdapter() {
      @Override
      public PortalDescriptor getDescriptor() {
        return desc;
      }

      @Override
      public PortalUrlBuilder getUrlBuilder() {
        return c -> "https://fallback.hu/search";
      }

      @Override
      public List<ExtractionStrategy> getStrategies() {
        return List.of(firstStrategy, secondStrategy);
      }
    };

    AdapterPortalEngine engine = new AdapterPortalEngine(adapter);
    PortalScrapeResult result = engine.scrape(criteria);

    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    assertEquals(1, result.getItems().size());
    assertEquals("FB101", result.getItems().get(0).getId());
    assertEquals("Strategy: HTML", result.getTechnicalDetails());
  }
}
