package ingatlanok.portal.oc;

import ingatlanok.common.SlugUtils;
import ingatlanok.model.SearchCriteria;

/** Otthon Centrum keresési URL összeállító komponens. */
public class OtthonCentrumUrlBuilder {

  public static final String BASE_URL = "https://www.oc.hu";

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

    return BASE_URL + "/elado/" + typeSlug + "/" + citySlug;
  }
}
