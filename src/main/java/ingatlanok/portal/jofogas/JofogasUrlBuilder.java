package ingatlanok.portal.jofogas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import ingatlanok.common.CityUtils;
import ingatlanok.common.SlugUtils;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalSupport;
import ingatlanok.portal.PortalUrlBuilder;

public class JofogasUrlBuilder implements PortalUrlBuilder {

  public static final String BASE_URL = "https://ingatlan.jofogas.hu";

  @Override
  public String buildSearchUrl(SearchCriteria criteria) {
    if (criteria == null) {
      throw new IllegalArgumentException("SearchCriteria must not be null");
    }
    if (criteria.getCity() == null || criteria.getCity().isBlank()) {
      throw new IllegalArgumentException("City must not be null or blank");
    }

    String locationSegment = resolveLocationSegment(criteria.getCity());
    String typeSlug = PortalSupport.resolvePropertyTypeSlug(criteria);
    List<String> queryParams = buildQueryParams(criteria);

    String route = "/" + locationSegment + "/" + typeSlug;
    return BASE_URL + route + "?" + String.join("&", queryParams);
  }

  private String resolveLocationSegment(String city) {
    String citySlug = SlugUtils.toSlug(city);
    if (citySlug.isEmpty()) {
      throw new IllegalArgumentException("Invalid city name: " + city);
    }
    if ("budapest".equals(citySlug)) {
      return "budapest";
    }
    Optional<String> countySlug = CityUtils.getCountySlug(city);
    if (countySlug.isPresent() && !countySlug.get().equals(citySlug)) {
      return countySlug.get() + "/" + citySlug;
    }
    return countySlug.orElse(citySlug);
  }

  private List<String> buildQueryParams(SearchCriteria criteria) {
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
    return queryParams;
  }
}
