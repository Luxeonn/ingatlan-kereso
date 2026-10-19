package ingatlanok.portal.jofogas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import ingatlanok.common.CityUtils;
import ingatlanok.common.SlugUtils;
import ingatlanok.model.SearchCriteria;

public class JofogasUrlBuilder {

  public static final String BASE_URL = "https://ingatlan.jofogas.hu";

  public String buildSearchUrl(SearchCriteria criteria) {
    if (criteria == null) {
      throw new IllegalArgumentException("SearchCriteria must not be null");
    }
    if (criteria.getCity() == null || criteria.getCity().isBlank()) {
      throw new IllegalArgumentException("City must not be null or blank");
    }

    String citySlug = SlugUtils.toSlug(criteria.getCity());
    if (citySlug.isEmpty()) {
      throw new IllegalArgumentException("Invalid city name: " + criteria.getCity());
    }

    String locationSegment;
    if (citySlug.equals("budapest")) {
      locationSegment = "budapest";
    } else {
      Optional<String> countySlug = CityUtils.getCountySlug(criteria.getCity());
      if (countySlug.isPresent() && !countySlug.get().equals(citySlug)) {
        locationSegment = countySlug.get() + "/" + citySlug;
      } else if (countySlug.isPresent()) {
        locationSegment = countySlug.get();
      } else {
        locationSegment = citySlug;
      }
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

    List<String> queryParams = new ArrayList<>();
    queryParams.add("st=s");

    if (criteria.getMinPriceMillion() != null) {
      queryParams.add("min_price=" + (criteria.getMinPriceMillion() * 1_000_000L));
    }
    if (criteria.getMaxPriceMillion() != null) {
      queryParams.add("max_price=" + (criteria.getMaxPriceMillion() * 1_000_000L));
    }
    if (criteria.getMinArea() != null) {
      queryParams.add("min_size=" + criteria.getMinArea());
    }
    if (criteria.getMaxArea() != null) {
      queryParams.add("max_size=" + criteria.getMaxArea());
    }

    Collections.sort(queryParams);

    String route = "/" + locationSegment + "/" + typeSlug;
    return BASE_URL + route + "?" + String.join("&", queryParams);
  }
}
