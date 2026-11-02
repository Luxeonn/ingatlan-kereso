package ingatlanok.portal.ingatlancom;

import java.util.ArrayList;
import java.util.List;

import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalSupport;
import ingatlanok.portal.PortalUrlBuilder;

public class IngatlanComUrlBuilder implements PortalUrlBuilder {

  public static final String BASE_URL = "https://ingatlan.com";

  @Override
  public String buildSearchUrl(SearchCriteria criteria) {
    SearchCriteria safe = PortalSupport.normalizeCriteria(criteria);
    String citySlug = PortalSupport.resolveCitySlug(safe);
    String typeSlug = PortalSupport.resolvePropertyTypeSlug(safe);

    List<String> segments = new ArrayList<>();
    segments.add("elado");
    segments.add(typeSlug);

    String areaSlug = buildRangeSlug(safe.getMinArea(), safe.getMaxArea(), "m2");
    if (!areaSlug.isEmpty()) {
      segments.add(areaSlug);
    }

    String priceSlug = buildRangeSlug(safe.getMinPriceMillion(), safe.getMaxPriceMillion(), "mFt");
    if (!priceSlug.isEmpty()) {
      segments.add(priceSlug);
    }

    segments.add(citySlug);

    return BASE_URL + "/lista/" + String.join("+", segments);
  }

  private String buildRangeSlug(Integer min, Integer max, String unit) {
    if (min != null && max != null) {
      return min + "-" + max + "-" + unit;
    }
    if (min != null) {
      return min + "-" + unit + "-tol";
    }
    if (max != null) {
      return max + "-" + unit + "-ig";
    }
    return "";
  }
}
