package ingatlanok.portal.koltozzbe;

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

public class KoltozzbePortalEngine implements PortalEngine {

  public static final int DEFAULT_MAX_PAGES = 5;

  private static final AppLogger LOGGER = AppLogger.getLogger(KoltozzbePortalEngine.class);
  private static final PortalDescriptor DESCRIPTOR =
      new PortalDescriptor(
          "koltozzbe",
          "Költözzbe",
          "#0284c7",
          "https://koltozzbe.hu",
          true,
          List.of("lakas", "haz", "telek"),
          true);

  private final KoltozzbeUrlBuilder urlBuilder;
  private final KoltozzbeParser parser;
  private final int maxPages;

  public KoltozzbePortalEngine() {
    this(new KoltozzbeUrlBuilder(), new KoltozzbeParser(), DEFAULT_MAX_PAGES);
  }

  public KoltozzbePortalEngine(KoltozzbeUrlBuilder urlBuilder, KoltozzbeParser parser) {
    this(urlBuilder, parser, DEFAULT_MAX_PAGES);
  }

  public KoltozzbePortalEngine(
      KoltozzbeUrlBuilder urlBuilder, KoltozzbeParser parser, int maxPages) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new KoltozzbeUrlBuilder();
    this.parser = parser != null ? parser : new KoltozzbeParser();
    this.maxPages = maxPages > 0 ? maxPages : DEFAULT_MAX_PAGES;
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
    LOGGER.info("Költözzbe keresés indítása: {}", searchUrl);

    String now = PortalSupport.nowTimestamp();
    Set<String> processedUrls = new HashSet<>();

    try {
      for (int page = 1; page <= maxPages; page++) {
        String pageUrl = urlBuilder.buildPageUrl(searchUrl, page);
        Document doc = documentFetcher.apply(pageUrl);
        if (doc == null) {
          LOGGER.warn("Nem sikerült letölteni a Költözzbe oldalt: {}", pageUrl);
          if (page == 1) {
            long elapsed = System.currentTimeMillis() - startTime;
            return PortalScrapeResult.failure(
                DESCRIPTOR.id(),
                DESCRIPTOR.name(),
                "Nem sikerült letölteni az oldalt",
                elapsed,
                500,
                searchUrl);
          }
          break;
        }

        List<IngatlanItem> pageItems = parser.parseItems(doc, safeCriteria, now, processedUrls);
        items.addAll(pageItems);

        if (pageItems.isEmpty() || !parser.hasNextPage(doc, page)) {
          break;
        }
      }

      long elapsed = System.currentTimeMillis() - startTime;
      LOGGER.info("Költözzbe találatok feldolgozva: {} db [{} ms]", items.size(), elapsed);
      if (items.isEmpty()) {
        return PortalScrapeResult.empty(DESCRIPTOR.id(), DESCRIPTOR.name(), searchUrl, elapsed);
      }
      return PortalScrapeResult.success(DESCRIPTOR.id(), DESCRIPTOR.name(), items, elapsed);
    } catch (Exception e) {
      long elapsed = System.currentTimeMillis() - startTime;
      LOGGER.error("Hiba a Költözzbe keresésekor [{} ms]: {}", elapsed, e.getMessage(), e);
      return PortalScrapeResult.failure(
          DESCRIPTOR.id(), DESCRIPTOR.name(), e.getMessage(), elapsed, 500, searchUrl);
    }
  }

  public KoltozzbeUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public KoltozzbeParser getParser() {
    return parser;
  }

  public int getMaxPages() {
    return maxPages;
  }
}
