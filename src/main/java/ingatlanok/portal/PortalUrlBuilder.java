package ingatlanok.portal;

import ingatlanok.model.SearchCriteria;

/** Uniform contract for portal search and pagination URL builders. */
public interface PortalUrlBuilder {

  String buildSearchUrl(SearchCriteria criteria);

  default String buildPageUrl(String baseSearchUrl, int page) {
    return baseSearchUrl;
  }

  /**
   * Appends a pagination query parameter to {@code baseSearchUrl} when {@code page > 1},
   * avoiding duplicate query parameters if the token is already present.
   */
  static String appendPageQueryParam(String baseSearchUrl, String paramName, int page) {
    if (baseSearchUrl == null || baseSearchUrl.isBlank() || page <= 1) {
      return baseSearchUrl;
    }
    if (paramName == null || paramName.isBlank()) {
      return baseSearchUrl;
    }
    String token = paramName + "=";
    if (baseSearchUrl.contains("?" + token) || baseSearchUrl.contains("&" + token)) {
      return baseSearchUrl;
    }
    String separator = baseSearchUrl.contains("?") ? "&" : "?";
    return baseSearchUrl + separator + token + page;
  }
}
