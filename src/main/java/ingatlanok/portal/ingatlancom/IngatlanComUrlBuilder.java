package ingatlanok.portal.ingatlancom;

import ingatlanok.common.SlugUtils;
import ingatlanok.model.SearchCriteria;

public class IngatlanComUrlBuilder {

  public static final String BASE_URL = "https://ingatlan.com";

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

    String priceSlug = "";
    if (criteria.getMinPriceMillion() != null && criteria.getMaxPriceMillion() != null) {
      priceSlug =
          "+" + criteria.getMinPriceMillion() + "-" + criteria.getMaxPriceMillion() + "-mFt";
    } else if (criteria.getMinPriceMillion() != null) {
      priceSlug = "+" + criteria.getMinPriceMillion() + "-mFt-tol";
    } else if (criteria.getMaxPriceMillion() != null) {
      priceSlug = "+" + criteria.getMaxPriceMillion() + "-mFt-ig";
    }

    return BASE_URL + "/lista/elado+" + typeSlug + "+" + citySlug + priceSlug;
  }
}
