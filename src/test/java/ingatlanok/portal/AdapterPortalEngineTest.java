package ingatlanok.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.core.normalization.ListingNormalizer;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawExtractionResult;
import ingatlanok.model.raw.RawPropertyListing;

class AdapterPortalEngineTest {

  private static final PortalDescriptor DESCRIPTOR = new PortalDescriptor(
      "testportal",
      "Test Portal",
      "#000000",
      "https://testportal.hu",
      true,
      List.of("lakas"),
      true
  );

  @Test
  @DisplayName("AdapterPortalEngine prioritási lánc: első sikeres stratégia találatait adja vissza")
  void testFirstSuccessfulStrategyWins() {
    RawPropertyListing raw1 = RawPropertyListing.builder()
        .portalId("testportal")
        .nativeId("101")
        .url("https://testportal.hu/101")
        .title("Teszt lakás")
        .priceHuf(40_000_000L)
        .areaSquareMeters(50.0)
        .roomCount(2.0)
        .method(ExtractionMethod.API)
        .build();

    ExtractionStrategy apiStrategy = new ExtractionStrategy() {
      @Override
      public ExtractionMethod method() {
        return ExtractionMethod.API;
      }

      @Override
      public RawExtractionResult extract(SearchCriteria criteria) {
        return RawExtractionResult.success(List.of(raw1), method());
      }
    };

    ExtractionStrategy htmlStrategy = new ExtractionStrategy() {
      @Override
      public ExtractionMethod method() {
        return ExtractionMethod.HTML;
      }

      @Override
      public RawExtractionResult extract(SearchCriteria criteria) {
        return RawExtractionResult.failure(method(), "https://testportal.hu", "Nem hívódhat meg", 500);
      }
    };

    PortalAdapter adapter = new PortalAdapter() {
      @Override
      public PortalDescriptor getDescriptor() {
        return DESCRIPTOR;
      }

      @Override
      public PortalUrlBuilder getUrlBuilder() {
        return null;
      }

      @Override
      public List<ExtractionStrategy> getStrategies() {
        return List.of(apiStrategy, htmlStrategy);
      }
    };

    AdapterPortalEngine engine = new AdapterPortalEngine(adapter, new ListingNormalizer());
    PortalScrapeResult result = engine.scrape(new SearchCriteria());

    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    assertEquals(1, result.getItems().size());
    assertEquals("101", result.getItems().get(0).getId());
    assertNotNull(result.getTechnicalDetails());
    assertTrue(result.getTechnicalDetails().contains("API"));
  }

  @Test
  @DisplayName("AdapterPortalEngine fallback: első stratégia bot protection után a második sikerül")
  void testFallbackOnBotProtection() {
    RawPropertyListing rawHtml = RawPropertyListing.builder()
        .portalId("testportal")
        .nativeId("202")
        .url("https://testportal.hu/202")
        .title("HTML hirdetés")
        .priceHuf(45_000_000L)
        .areaSquareMeters(55.0)
        .roomCount(2.0)
        .method(ExtractionMethod.HTML)
        .build();

    ExtractionStrategy sidecarStrategy = new ExtractionStrategy() {
      @Override
      public ExtractionMethod method() {
        return ExtractionMethod.SIDECAR;
      }

      @Override
      public RawExtractionResult extract(SearchCriteria criteria) {
        return RawExtractionResult.botProtected(method(), "https://testportal.hu/bot", "Cloudflare WAF");
      }
    };

    ExtractionStrategy htmlStrategy = new ExtractionStrategy() {
      @Override
      public ExtractionMethod method() {
        return ExtractionMethod.HTML;
      }

      @Override
      public RawExtractionResult extract(SearchCriteria criteria) {
        return RawExtractionResult.success(List.of(rawHtml), method());
      }
    };

    PortalAdapter adapter = new PortalAdapter() {
      @Override
      public PortalDescriptor getDescriptor() {
        return DESCRIPTOR;
      }

      @Override
      public PortalUrlBuilder getUrlBuilder() {
        return null;
      }

      @Override
      public List<ExtractionStrategy> getStrategies() {
        return List.of(sidecarStrategy, htmlStrategy);
      }
    };

    AdapterPortalEngine engine = new AdapterPortalEngine(adapter);
    PortalScrapeResult result = engine.scrape(new SearchCriteria());

    assertEquals(PortalScrapeStatus.SUCCESS, result.getStatus());
    assertEquals(1, result.getItems().size());
    assertEquals("202", result.getItems().get(0).getId());
  }

  @Test
  @DisplayName("AdapterPortalEngine ha minden stratégia bot protected, BOT_PROTECTED eredményt ad vissza")
  void testAllBotProtectedReturnsBotProtected() {
    ExtractionStrategy strategy = new ExtractionStrategy() {
      @Override
      public ExtractionMethod method() {
        return ExtractionMethod.HTML;
      }

      @Override
      public RawExtractionResult extract(SearchCriteria criteria) {
        return RawExtractionResult.botProtected(method(), "https://testportal.hu/waf", "WAF blocked");
      }
    };

    PortalAdapter adapter = new PortalAdapter() {
      @Override
      public PortalDescriptor getDescriptor() {
        return DESCRIPTOR;
      }

      @Override
      public PortalUrlBuilder getUrlBuilder() {
        return null;
      }

      @Override
      public List<ExtractionStrategy> getStrategies() {
        return List.of(strategy);
      }
    };

    AdapterPortalEngine engine = new AdapterPortalEngine(adapter);
    PortalScrapeResult result = engine.scrape(new SearchCriteria());

    assertEquals(PortalScrapeStatus.BOT_PROTECTED, result.getStatus());
    assertEquals("https://testportal.hu/waf", result.getFallbackUrl());
  }
}
