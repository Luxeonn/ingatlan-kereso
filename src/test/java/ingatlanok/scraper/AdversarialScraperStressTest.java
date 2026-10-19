package ingatlanok.scraper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;

@DisplayName("Adversarial Scraper Stress & Dynamic Dispatch Test")
class AdversarialScraperStressTest {

  // =========================================================================
  // 1. DYNAMIC DISPATCH INTERCEPTION VERIFICATION
  // =========================================================================
  @Nested
  @DisplayName("Dynamic Dispatch Interception Tests")
  class DynamicDispatchTests {

    @Test
    @DisplayName("DunaHouseScraper: Overridden getDocument is genuinely invoked")
    void testDunaHouseDynamicDispatch() {
      AtomicInteger callCount = new AtomicInteger(0);
      DunaHouseScraper scraper =
          new DunaHouseScraper() {
            @Override
            public Document getDocument(String targetUrl) {
              callCount.incrementAndGet();
              return Jsoup.parse(
                  "<html><body><a href='/ingatlan/dh-test-1'>Eladó lakás Debrecenben 60 M"
                      + " Ft</a></body></html>");
            }
          };

      SearchCriteria criteria = new SearchCriteria();
      criteria.setCity("Debrecen");
      PortalScrapeResult result = scraper.scrape(criteria);

      assertNotNull(result);
      assertTrue(callCount.get() >= 1, "getDocument override MUST be called via dynamic dispatch");
    }

    @Test
    @DisplayName("OtthonCentrumScraper: Overridden getDocument is genuinely invoked")
    void testOtthonCentrumDynamicDispatch() {
      AtomicBoolean invoked = new AtomicBoolean(false);
      OtthonCentrumScraper scraper =
          new OtthonCentrumScraper() {
            @Override
            public Document getDocument(String targetUrl) {
              invoked.set(true);
              return Jsoup.parse("<html><body></body></html>");
            }
          };

      SearchCriteria criteria = new SearchCriteria();
      criteria.setCity("Debrecen");
      PortalScrapeResult result = scraper.scrape(criteria);

      assertNotNull(result);
      assertTrue(invoked.get(), "getDocument override MUST be called via dynamic dispatch");
    }

    @Test
    @DisplayName("JofogasScraper: Overridden getDocument is genuinely invoked")
    void testJofogasDynamicDispatch() {
      AtomicBoolean invoked = new AtomicBoolean(false);
      JofogasScraper scraper =
          new JofogasScraper() {
            @Override
            public Document getDocument(String targetUrl) {
              invoked.set(true);
              return Jsoup.parse("<html><body></body></html>");
            }
          };

      SearchCriteria criteria = new SearchCriteria();
      criteria.setCity("Debrecen");
      PortalScrapeResult result = scraper.scrape(criteria);

      assertNotNull(result);
      assertTrue(invoked.get(), "getDocument override MUST be called via dynamic dispatch");
    }

    @Test
    @DisplayName("KoltozzbeScraper: Overridden getDocument is genuinely invoked")
    void testKoltozzbeDynamicDispatch() {
      AtomicInteger callCount = new AtomicInteger(0);
      KoltozzbeScraper scraper =
          new KoltozzbeScraper() {
            @Override
            public Document getDocument(String targetUrl) {
              callCount.incrementAndGet();
              return Jsoup.parse("<html><body></body></html>");
            }
          };

      SearchCriteria criteria = new SearchCriteria();
      criteria.setCity("Debrecen");
      PortalScrapeResult result = scraper.scrape(criteria);

      assertNotNull(result);
      assertTrue(callCount.get() >= 1, "getDocument override MUST be called via dynamic dispatch");
    }

    @Test
    @DisplayName("IngatlanBazarScraper: Overridden getJson is genuinely invoked")
    void testIngatlanBazarDynamicDispatch() {
      AtomicBoolean invoked = new AtomicBoolean(false);
      IngatlanBazarScraper scraper =
          new IngatlanBazarScraper() {
            @Override
            public String getJson(String targetUrl) {
              invoked.set(true);
              return "{\"hits\": []}";
            }
          };

      SearchCriteria criteria = new SearchCriteria();
      criteria.setCity("Debrecen");
      PortalScrapeResult result = scraper.scrape(criteria);

      assertNotNull(result);
      assertTrue(invoked.get(), "getJson override MUST be called via dynamic dispatch");
    }

    @Test
    @DisplayName("IngatlanComScraper: Overridden getDocument is genuinely invoked")
    void testIngatlanComDynamicDispatch() {
      AtomicBoolean invoked = new AtomicBoolean(false);
      IngatlanComScraper scraper =
          new IngatlanComScraper() {
            @Override
            public Document getDocument(String targetUrl) {
              invoked.set(true);
              return Jsoup.parse("<html><body></body></html>");
            }
          };

      SearchCriteria criteria = new SearchCriteria();
      criteria.setCity("Debrecen");
      PortalScrapeResult result = scraper.scrape(criteria);

      assertNotNull(result);
      assertTrue(invoked.get(), "getDocument override MUST be called via dynamic dispatch");
    }
  }

  // =========================================================================
  // 2. NULL AND MALFORMED SEARCH CRITERIA RESILIENCE
  // =========================================================================
  @Nested
  @DisplayName("Null & Malformed Criteria Stress Tests")
  class NullAndMalformedCriteriaTests {

    @Test
    @DisplayName("All 6 scrapers survive null criteria without throwing NPE")
    void testAllScrapersHandleNullCriteria() {
      List<IngatlanScraper> scrapers =
          List.of(
              new DunaHouseScraper() {
                @Override
                public Document getDocument(String url) {
                  return Jsoup.parse("<html></html>");
                }
              },
              new OtthonCentrumScraper() {
                @Override
                public Document getDocument(String url) {
                  return Jsoup.parse("<html></html>");
                }
              },
              new JofogasScraper() {
                @Override
                public Document getDocument(String url) {
                  return Jsoup.parse("<html></html>");
                }
              },
              new KoltozzbeScraper() {
                @Override
                public Document getDocument(String url) {
                  return Jsoup.parse("<html></html>");
                }
              },
              new IngatlanBazarScraper() {
                @Override
                public String getJson(String url) {
                  return "{\"hits\":[]}";
                }
              },
              new IngatlanComScraper() {
                @Override
                public Document getDocument(String url) {
                  return Jsoup.parse("<html></html>");
                }
              });

      for (IngatlanScraper scraper : scrapers) {
        assertDoesNotThrow(
            () -> {
              PortalScrapeResult res = scraper.scrape(null);
              assertNotNull(
                  res, scraper.getName() + " returned null PortalScrapeResult for null criteria");
            },
            scraper.getName() + " threw an uncaught exception on null criteria");

        assertDoesNotThrow(
            () -> {
              List<IngatlanItem> items = scraper.search(null);
              assertNotNull(items, scraper.getName() + " returned null items for null criteria");
            },
            scraper.getName() + " threw an uncaught exception on search(null)");
      }
    }

    @Test
    @DisplayName("All 6 scrapers survive SearchCriteria with all fields null / empty")
    void testAllScrapersHandleEmptyCriteriaFields() {
      SearchCriteria emptyCriteria = new SearchCriteria();
      emptyCriteria.setCity(null);
      emptyCriteria.setCities(null);
      emptyCriteria.setPropertyType(null);
      emptyCriteria.setMinPriceMillion(null);
      emptyCriteria.setMaxPriceMillion(null);
      emptyCriteria.setMinArea(null);
      emptyCriteria.setMaxArea(null);
      emptyCriteria.setMinRooms(null);

      List<IngatlanScraper> scrapers =
          List.of(
              new DunaHouseScraper() {
                @Override
                public Document getDocument(String url) {
                  return Jsoup.parse("<html></html>");
                }
              },
              new OtthonCentrumScraper() {
                @Override
                public Document getDocument(String url) {
                  return Jsoup.parse("<html></html>");
                }
              },
              new JofogasScraper() {
                @Override
                public Document getDocument(String url) {
                  return Jsoup.parse("<html></html>");
                }
              },
              new KoltozzbeScraper() {
                @Override
                public Document getDocument(String url) {
                  return Jsoup.parse("<html></html>");
                }
              },
              new IngatlanBazarScraper() {
                @Override
                public String getJson(String url) {
                  return "{\"hits\":[]}";
                }
              },
              new IngatlanComScraper() {
                @Override
                public Document getDocument(String url) {
                  return Jsoup.parse("<html></html>");
                }
              });

      for (IngatlanScraper scraper : scrapers) {
        assertDoesNotThrow(
            () -> {
              PortalScrapeResult res = scraper.scrape(emptyCriteria);
              assertNotNull(
                  res, scraper.getName() + " returned null result on empty criteria fields");
            },
            scraper.getName() + " threw exception on criteria with null fields");
      }
    }
  }

  // =========================================================================
  // 3. MALFORMED HTML & DOM CORRUPTION RESILIENCE
  // =========================================================================
  @Nested
  @DisplayName("Malformed HTML & Extreme DOM Tests")
  class MalformedHtmlTests {

    private static final String TRUNCATED_HTML = "<div><a href='/ingatlan/";
    private static final String CORRUPT_NESTED_HTML =
        """
<html>
  <body>
    <div class='listing-card' data-lid='bad-id'>
      <a href='' class='listing-item-link'>Broken link</a>
      <div class='item-type h2'>-9999999999999999999999999999999999999999999999999999 Ft</div>
      <div class='listing-parameters'>$$$ INVALID DETAILS &&&</div>
    </div>
    <div class='real-estate-card' data-seo-select-item-value='{corrupted-json:'>
      <a href='/ingatlanok/H12345'><div class='price-huf'>NINCS AR</div></a>
    </div>
    <a href='/ingatlan/dh-malformed'>
      <span class='title'>Unclosed span
    </a>
  </body>
</html>
""";

    @Test
    @DisplayName("DunaHouseScraper gracefully handles truncated and corrupt DOM")
    void testDunaHouseMalformedHtml() {
      DunaHouseScraper scraper =
          new DunaHouseScraper() {
            @Override
            public Document getDocument(String url) {
              return Jsoup.parse(TRUNCATED_HTML);
            }
          };
      assertDoesNotThrow(
          () -> {
            PortalScrapeResult res = scraper.scrape(new SearchCriteria());
            assertNotNull(res);
          });

      DunaHouseScraper scraper2 =
          new DunaHouseScraper() {
            @Override
            public Document getDocument(String url) {
              return Jsoup.parse(CORRUPT_NESTED_HTML);
            }
          };
      assertDoesNotThrow(
          () -> {
            PortalScrapeResult res = scraper2.scrape(new SearchCriteria());
            assertNotNull(res);
          });
    }

    @Test
    @DisplayName(
        "OtthonCentrumScraper gracefully handles corrupt JSON in data-seo-select-item-value")
    void testOtthonCentrumCorruptSeoAttr() {
      OtthonCentrumScraper scraper =
          new OtthonCentrumScraper() {
            @Override
            public Document getDocument(String url) {
              return Jsoup.parse(CORRUPT_NESTED_HTML);
            }
          };
      assertDoesNotThrow(
          () -> {
            PortalScrapeResult res = scraper.scrape(new SearchCriteria());
            assertNotNull(res);
          });
    }

    @Test
    @DisplayName("JofogasScraper gracefully handles malformed anchor structures")
    void testJofogasMalformedHtml() {
      JofogasScraper scraper =
          new JofogasScraper() {
            @Override
            public Document getDocument(String url) {
              return Jsoup.parse(CORRUPT_NESTED_HTML);
            }
          };
      assertDoesNotThrow(
          () -> {
            PortalScrapeResult res = scraper.scrape(new SearchCriteria());
            assertNotNull(res);
          });
    }

    @Test
    @DisplayName("KoltozzbeScraper gracefully handles broken JSON-LD and corrupted cards")
    void testKoltozzbeCorruptJsonLd() {
      String corruptJsonLdHtml =
          """
<html>
<head>
  <script type="application/ld+json">
    { "@type": "ItemList", "itemListElement": [ { "@type": "ListItem", "item": { "broken json
  </script>
  <script type="application/ld+json">
    not even json
  </script>
</head>
<body>
  <div class="listing-card">
    <!-- missing linkElem -->
  </div>
</body>
</html>
""";
      KoltozzbeScraper scraper =
          new KoltozzbeScraper() {
            @Override
            public Document getDocument(String url) {
              return Jsoup.parse(corruptJsonLdHtml);
            }
          };
      assertDoesNotThrow(
          () -> {
            PortalScrapeResult res = scraper.scrape(new SearchCriteria());
            assertNotNull(res);
          });
    }

    @Test
    @DisplayName("IngatlanComScraper gracefully handles malformed HTML cards")
    void testIngatlanComMalformedHtml() {
      IngatlanComScraper scraper =
          new IngatlanComScraper() {
            @Override
            public Document getDocument(String url) {
              return Jsoup.parse(CORRUPT_NESTED_HTML);
            }
          };
      assertDoesNotThrow(
          () -> {
            PortalScrapeResult res = scraper.scrape(new SearchCriteria());
            assertNotNull(res);
          });
    }
  }

  // =========================================================================
  // 4. MALFORMED JSON STRESS FOR INGATLANBAZAR
  // =========================================================================
  @Nested
  @DisplayName("IngatlanBazar Malformed JSON Stress Tests")
  class IngatlanBazarMalformedJsonTests {

    @Test
    @DisplayName(
        "IngatlanBazar handles null, empty, whitespace, and invalid JSON strings without crashing")
    void testIngatlanBazarMalformedJsonScenarios() {
      List<String> malformedJsons =
          List.of(
              "",
              "   ",
              "{broken json",
              "[]",
              "\"just a string\"",
              "{\"hits\": null}",
              "{\"hits\": \"not an array\"}",
              "{\"hits\": [\"not an object\"]}",
              "{\"hits\": [{}]}",
              "{\"hits\": [{\"propertyId\": null, \"seoUrl\": null}]}",
              "{\"hits\": [{\"seoUrl\": \"/test/1\", \"priceHUF\": \"NaN\", \"area\": \"invalid\","
                  + " \"rooms\": \"invalid\"}]}");

      for (String badJson : malformedJsons) {
        IngatlanBazarScraper scraper =
            new IngatlanBazarScraper() {
              @Override
              public String getJson(String targetUrl) {
                return badJson;
              }
            };

        assertDoesNotThrow(
            () -> {
              PortalScrapeResult res = scraper.scrape(new SearchCriteria());
              assertNotNull(res, "Result must not be null for json: " + badJson);
            },
            "Failed on bad json: " + badJson);
      }
    }
  }

  // =========================================================================
  // 5. NETWORK / EXCEPTION RESILIENCE (0-CRASH PRINCIPLE)
  // =========================================================================
  @Nested
  @DisplayName("Exception & Network Failure Resilience (0-Crash)")
  class ZeroCrashNetworkExceptionTests {

    @Test
    @DisplayName(
        "All 6 scrapers catch and wrap network exceptions into failure or botProtected results")
    void testAllScrapersSurviveNetworkExceptions() {
      List<IngatlanScraper> scrapers =
          List.of(
              new DunaHouseScraper() {
                @Override
                public Document getDocument(String url) {
                  throw new RuntimeException("Simulated Connection Reset");
                }
              },
              new OtthonCentrumScraper() {
                @Override
                public Document getDocument(String url) {
                  throw new RuntimeException("Simulated Socket Timeout");
                }
              },
              new JofogasScraper() {
                @Override
                public Document getDocument(String url) {
                  throw new RuntimeException("Simulated HTTP 503");
                }
              },
              new KoltozzbeScraper() {
                @Override
                public Document getDocument(String url) {
                  throw new RuntimeException("Simulated SSL Handshake Failure");
                }
              },
              new IngatlanBazarScraper() {
                @Override
                public String getJson(String url) {
                  throw new RuntimeException("Simulated DNS Resolution Failure");
                }
              },
              new IngatlanComScraper() {
                @Override
                public Document getDocument(String url) {
                  throw new RuntimeException("Simulated Connection Timeout");
                }
              });

      for (IngatlanScraper scraper : scrapers) {
        assertDoesNotThrow(
            () -> {
              PortalScrapeResult res = scraper.scrape(new SearchCriteria());
              assertNotNull(
                  res, scraper.getName() + " scrape() returned null instead of failure result");
              assertTrue(
                  res.getStatus() == PortalScrapeStatus.FAILED
                      || res.getStatus() == PortalScrapeStatus.BOT_PROTECTED,
                  scraper.getName()
                      + " expected FAILED or BOT_PROTECTED but got "
                      + res.getStatus());
            },
            scraper.getName()
                + " threw an unhandled exception when getDocument/getJson threw RuntimeException");
      }
    }

    @Test
    @DisplayName("All 6 scrapers survive null returned from getDocument / getJson")
    void testAllScrapersSurviveNullReturns() {
      List<IngatlanScraper> scrapers =
          List.of(
              new DunaHouseScraper() {
                @Override
                public Document getDocument(String url) {
                  return null;
                }
              },
              new OtthonCentrumScraper() {
                @Override
                public Document getDocument(String url) {
                  return null;
                }
              },
              new JofogasScraper() {
                @Override
                public Document getDocument(String url) {
                  return null;
                }
              },
              new KoltozzbeScraper() {
                @Override
                public Document getDocument(String url) {
                  return null;
                }
              },
              new IngatlanBazarScraper() {
                @Override
                public String getJson(String url) {
                  return null;
                }
              },
              new IngatlanComScraper() {
                @Override
                public Document getDocument(String url) {
                  return null;
                }
              });

      for (IngatlanScraper scraper : scrapers) {
        assertDoesNotThrow(
            () -> {
              PortalScrapeResult res = scraper.scrape(new SearchCriteria());
              assertNotNull(
                  res,
                  scraper.getName() + " returned null PortalScrapeResult when fetch returned null");
            },
            scraper.getName() + " threw exception when fetch returned null");
      }
    }
  }
}
