package ingatlanok.portal.jofogas;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.nodes.Document;

import ingatlanok.common.AppLogger;
import ingatlanok.common.HttpFetcher;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngine;
import ingatlanok.model.PortalScrapeResult;

public class JofogasPortalEngine implements PortalEngine {

  private static final AppLogger LOGGER = AppLogger.getLogger(JofogasPortalEngine.class);
  private static final PortalDescriptor DESCRIPTOR =
      new PortalDescriptor(
          "jofogas",
          "Jófogás Ingatlan",
          "#059669",
          "https://ingatlan.jofogas.hu",
          true,
          List.of("lakas", "haz", "telek"),
          true);

  private final JofogasUrlBuilder urlBuilder;
  private final JofogasParser parser;

  public JofogasPortalEngine() {
    this(new JofogasUrlBuilder(), new JofogasParser());
  }

  public JofogasPortalEngine(JofogasUrlBuilder urlBuilder, JofogasParser parser) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new JofogasUrlBuilder();
    this.parser = parser != null ? parser : new JofogasParser();
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
    try {
      String searchUrl = urlBuilder.buildSearchUrl(criteria);
      LOGGER.info("Jófogás Ingatlan keresés indítása: {}", searchUrl);

      Document document = documentFetcher.apply(searchUrl);
      if (document == null) {
        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.warn(
            "Nem sikerült letölteni a Jófogás keresési oldalt [{} ms]: {}", elapsed, searchUrl);
        return PortalScrapeResult.failure(
            DESCRIPTOR.id(), DESCRIPTOR.name(), "Nem sikerült letölteni az oldalt", elapsed);
      }

      String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
      Set<String> processed = new HashSet<>();
      List<IngatlanItem> items = parser.parseItems(document, criteria, now, processed);

      long elapsed = System.currentTimeMillis() - startTime;
      LOGGER.info("jofogas.hu Találatok száma: {} db [{} ms]", items.size(), elapsed);
      return PortalScrapeResult.success(DESCRIPTOR.id(), DESCRIPTOR.name(), items, elapsed);
    } catch (Exception e) {
      long elapsed = System.currentTimeMillis() - startTime;
      LOGGER.error("Hiba a Jófogás keresésekor [{} ms]: {}", elapsed, e.getMessage(), e);
      return PortalScrapeResult.failure(
          DESCRIPTOR.id(), DESCRIPTOR.name(), e.getMessage(), elapsed);
    }
  }

  public JofogasUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public JofogasParser getParser() {
    return parser;
  }
}
