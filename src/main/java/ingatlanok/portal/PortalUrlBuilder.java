package ingatlanok.portal;

import ingatlanok.model.SearchCriteria;

/** Uniform contract for portal search and pagination URL builders. */
public interface PortalUrlBuilder {

  String buildSearchUrl(SearchCriteria criteria);

  default String buildPageUrl(String baseSearchUrl, int page) {
    return baseSearchUrl;
  }
}
