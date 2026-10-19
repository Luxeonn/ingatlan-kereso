package ingatlanok.portal.dh;

import java.util.Optional;

import ingatlanok.common.CityUtils;
import ingatlanok.common.SlugUtils;
import ingatlanok.model.SearchCriteria;

/** Duna House keresési URL előállító komponens. */
public class DunaHouseUrlBuilder {

  public static final String BASE_URL = "https://dh.hu";

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
          "/" + criteria.getMinPriceMillion() + "-" + criteria.getMaxPriceMillion() + "-mFt";
    } else if (criteria.getMinPriceMillion() != null) {
      priceSlug = "/" + criteria.getMinPriceMillion() + "-mFt-tol";
    } else if (criteria.getMaxPriceMillion() != null) {
      priceSlug = "/" + criteria.getMaxPriceMillion() + "-mFt-ig";
    }

    String locationSegment;
    if (citySlug.equals("budapest")) {
      locationSegment = "budapest";
    } else {
      Optional<String> countySlug = CityUtils.getCountySlug(criteria.getCity());
      if (countySlug.isPresent()) {
        locationSegment = countySlug.get() + "/" + citySlug;
      } else {
        locationSegment = citySlug;
      }
    }

    String searchRoute = "/elado-ingatlan/" + typeSlug + "/" + locationSegment + priceSlug;
    return BASE_URL + searchRoute;
  }
}
