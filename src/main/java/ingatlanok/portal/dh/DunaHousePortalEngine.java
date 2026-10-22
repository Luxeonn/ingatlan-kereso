package ingatlanok.portal.dh;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.nodes.Document;

import ingatlanok.common.AppLogger;
import ingatlanok.common.HttpFetcher;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;
import ingatlanok.portal.PortalSupport;

public class DunaHousePortalEngine implements PortalEngine {

  private static final AppLogger LOGGER = AppLogger.getLogger(DunaHousePortalEngine.class);
  private static final PortalDescriptor DESCRIPTOR =
      new PortalDescriptor(
          "dh",
          "Duna House",
          "#1d4ed8",
          "https://dh.hu",
          true,
          List.of("lakas", "haz", "telek"),
          true);

  private final DunaHouseUrlBuilder urlBuilder;
  private final DunaHouseParser parser;

  public DunaHousePortalEngine() {
    this(new DunaHouseUrlBuilder(), new DunaHouseParser());
  }

  public DunaHousePortalEngine(DunaHouseUrlBuilder urlBuilder, DunaHouseParser parser) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new DunaHouseUrlBuilder();
    this.parser = parser != null ? parser : new DunaHouseParser();
  }

  @Override
  public PortalDescriptor getDescriptor() {
    return DESCRIPTOR;
  }

  @Override
  public PortalScrapeResult scrape(SearchCriteria criteria) {
    return scrape(criteria, HttpFetcher::fetchDocument);
  }

  public PortalScrapeResult scrape(
      SearchCriteria criteria, java.util.function.Function<String, Document> documentFetcher) {
    long startTime = System.currentTimeMillis();
    List<IngatlanItem> items = new ArrayList<>();
    SearchCriteria safeCriteria = PortalSupport.normalizeCriteria(criteria);

    String searchUrl = urlBuilder.buildSearchUrl(safeCriteria);
    LOGGER.info("Duna House keresés indítása: {}", searchUrl);

    int maxPages = 2;
    String now = PortalSupport.nowTimestamp();
    Set<String> processedUrls = new HashSet<>();

    try {
      for (int i = 1; i <= maxPages; i++) {
        String pageUrl = urlBuilder.buildPageUrl(searchUrl, i);
        Document document = documentFetcher.apply(pageUrl);
        if (document == null) {
          LOGGER.warn("Nem sikerült letölteni a Duna House oldalt: {}", pageUrl);
          continue;
        }

        List<IngatlanItem> pageItems =
            parser.parseItems(document, safeCriteria, now, processedUrls);
        items.addAll(pageItems);
      }

      long elapsed = System.currentTimeMillis() - startTime;
      LOGGER.info("Duna House találatok feldolgozva: {} db [{} ms]", items.size(), elapsed);
      if (items.isEmpty()) {
        return PortalScrapeResult.empty(DESCRIPTOR.id(), DESCRIPTOR.name(), searchUrl, elapsed);
      }
      return PortalScrapeResult.success(DESCRIPTOR.id(), DESCRIPTOR.name(), items, elapsed);
    } catch (Exception e) {
      long elapsed = System.currentTimeMillis() - startTime;
      LOGGER.error("Hiba a Duna House keresésekor [{} ms]: {}", elapsed, e.getMessage(), e);
      return PortalScrapeResult.failure(
          DESCRIPTOR.id(), DESCRIPTOR.name(), e.getMessage(), elapsed, 500, searchUrl);
    }
  }

  public DunaHouseUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public DunaHouseParser getParser() {
    return parser;
  }
}
