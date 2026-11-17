package ingatlanok.portal.jofogas;

import java.util.function.Function;

import org.jsoup.nodes.Document;

import ingatlanok.common.HttpFetcher;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.AbstractPaginatedHtmlStrategy;

public class JofogasHtmlStrategy extends AbstractPaginatedHtmlStrategy {

  public static final int DEFAULT_MAX_PAGES = AbstractPaginatedHtmlStrategy.DEFAULT_MAX_PAGES;

  public JofogasHtmlStrategy() {
    this(new JofogasUrlBuilder(), new JofogasRawParser(), HttpFetcher::fetchDocument);
  }

  public JofogasHtmlStrategy(
      JofogasUrlBuilder urlBuilder,
      JofogasRawParser rawParser,
      Function<String, Document> documentFetcher) {
    this(urlBuilder, rawParser, documentFetcher, DEFAULT_MAX_PAGES);
  }

  public JofogasHtmlStrategy(
      JofogasUrlBuilder urlBuilder,
      JofogasRawParser rawParser,
      Function<String, Document> documentFetcher,
      int maxPages) {
    super(
        urlBuilder != null ? urlBuilder : new JofogasUrlBuilder(),
        rawParser != null ? rawParser : new JofogasRawParser(),
        documentFetcher,
        maxPages,
        DEFAULT_MAX_PAGES);
  }

  @Override
  protected String resolveSearchUrl(SearchCriteria criteria) {
    if (criteria == null || criteria.getCity() == null || criteria.getCity().isBlank()) {
      return null;
    }
    return super.resolveSearchUrl(criteria);
  }

  @Override
  protected String getPortalLogName() {
    return "Jófogás";
  }

  @Override
  protected String getFallbackBaseUrl() {
    return "https://ingatlan.jofogas.hu";
  }

  @Override
  protected String getPaginationParamName() {
    return "o";
  }
}
