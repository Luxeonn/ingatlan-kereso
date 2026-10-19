package ingatlanok.portal.ingatlanbazar;

import java.util.Optional;

import ingatlanok.model.SearchCriteria;

public class IngatlanBazarQueryBuilder {

  public static final String BASE_URL = "https://www.ingatlanbazar.hu";
  public static final String API_SEARCH_URL = "https://www.ingatlanbazar.hu/api/property-search";

  public String buildSearchUrl(SearchCriteria criteria) {
    if (criteria == null) {
      criteria = new SearchCriteria();
    }
    String city = criteria.getCity();
    if (city == null || city.isBlank()) {
      city = "Debrecen";
    }
    String displayCity = Character.toUpperCase(city.charAt(0)) + city.substring(1);

    String rawType =
        criteria.getPropertyType() != null
            ? criteria.getPropertyType().toLowerCase().trim()
            : "lakas";
    String typeSlug =
        switch (rawType) {
          case "haz" -> "Haz";
          case "telek" -> "Telek";
          default -> "Lakas";
        };

    StringBuilder query = new StringBuilder("?");
    Optional<Integer> locId = IngatlanBazarLocationResolver.resolveLocationId(city);
    locId.ifPresent(id -> query.append("property_location=").append(id).append("&"));

    if (criteria.getMinPriceMillion() != null) {
      query.append("price_min=").append(criteria.getMinPriceMillion()).append("&");
    }
    if (criteria.getMaxPriceMillion() != null) {
      query.append("price_max=").append(criteria.getMaxPriceMillion()).append("&");
    }

    if ("haz".equals(rawType)) {
      query.append("property__2=2&");
    } else if ("telek".equals(rawType)) {
      query.append("property__2=5&");
    } else {
      query.append("property__2=3&");
    }

    String q = query.toString();
    if (q.endsWith("&") || q.endsWith("?")) {
      q = q.substring(0, q.length() - 1);
    }

    return BASE_URL + "/ingatlan-" + displayCity + "-Elado-" + typeSlug + q;
  }

  public String buildApiSearchUrl(SearchCriteria criteria, int page) {
    if (criteria == null) {
      criteria = new SearchCriteria();
    }
    StringBuilder sb = new StringBuilder(API_SEARCH_URL);
    sb.append("?property__1=1");

    String city = criteria.getCity();
    Optional<Integer> locId = IngatlanBazarLocationResolver.resolveLocationId(city);
    locId.ifPresent(id -> sb.append("&property_location=").append(id));

    String rawType =
        criteria.getPropertyType() != null
            ? criteria.getPropertyType().toLowerCase().trim()
            : "lakas";
    if ("haz".equals(rawType)) {
      sb.append("&property__2=2");
    } else if ("telek".equals(rawType)) {
      sb.append("&property__2=5");
    } else {
      sb.append("&property__2=3");
    }

    if (criteria.getMinPriceMillion() != null) {
      sb.append("&price_min=").append(criteria.getMinPriceMillion());
    }
    if (criteria.getMaxPriceMillion() != null) {
      sb.append("&price_max=").append(criteria.getMaxPriceMillion());
    }

    sb.append("&limit=50&page=").append(page);
    return sb.toString();
  }
}
