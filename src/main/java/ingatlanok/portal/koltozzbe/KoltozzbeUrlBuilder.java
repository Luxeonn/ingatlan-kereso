package ingatlanok.portal.koltozzbe;

import java.util.ArrayList;
import java.util.List;

import ingatlanok.common.SlugUtils;
import ingatlanok.model.SearchCriteria;

public class KoltozzbeUrlBuilder {

  public static final String BASE_URL = "https://koltozzbe.hu";

  public String buildSearchUrl(SearchCriteria criteria) {
    if (criteria == null) {
      criteria = new SearchCriteria();
    }

    String citySlug = SlugUtils.toSlug(criteria.getCity());
    if (citySlug.isEmpty()) {
      citySlug = "debrecen";
    }

    String rawType =
        criteria.getPropertyType() != null
            ? criteria.getPropertyType().toLowerCase().trim()
            : "lakas";
    String typeSlug =
        switch (rawType) {
          case "haz" -> "haz";
          case "telek" -> "telek";
          default -> "lakas";
        };

    String route = "/elado-" + typeSlug + "-" + citySlug;
    List<String> params = new ArrayList<>();

    if (criteria.getMinPriceMillion() != null) {
      params.add("p1=" + criteria.getMinPriceMillion() * 1_000_000L);
    }
    if (criteria.getMaxPriceMillion() != null) {
      params.add("p2=" + criteria.getMaxPriceMillion() * 1_000_000L);
    }
    if (criteria.getMinArea() != null) {
      params.add("a1=" + criteria.getMinArea());
    }
    if (criteria.getMaxArea() != null) {
      params.add("a2=" + criteria.getMaxArea());
    }

    if (params.isEmpty()) {
      return BASE_URL + route;
    }
    return BASE_URL + route + "?" + String.join("&", params);
  }
}
