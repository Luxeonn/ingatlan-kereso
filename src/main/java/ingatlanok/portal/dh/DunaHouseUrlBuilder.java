package ingatlanok.portal.dh;

import java.util.Optional;

import ingatlanok.common.CityUtils;
import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalSupport;
import ingatlanok.portal.PortalUrlBuilder;

public class DunaHouseUrlBuilder implements PortalUrlBuilder {

  public static final String BASE_URL = "https://dh.hu";

  @Override
  public String buildSearchUrl(SearchCriteria criteria) {
    SearchCriteria safe = PortalSupport.normalizeCriteria(criteria);
    String citySlug = PortalSupport.resolveCitySlug(safe);
    String typeSlug = PortalSupport.resolvePropertyTypeSlug(safe);

    String locationSegment;
    if ("budapest".equals(citySlug)) {
      locationSegment = "budapest";
    } else {
      Optional<String> countySlug = CityUtils.getCountySlug(safe.getCity());
      if (countySlug.isPresent() && !countySlug.get().isBlank()) {
        String county = countySlug.get();
        String countyWithSuffix = county.endsWith("-megye") ? county : county + "-megye";
        locationSegment = countyWithSuffix + "/" + citySlug;
      } else {
        locationSegment = citySlug;
      }
    }

    String priceSlug = buildPriceSlug(safe);
    String areaSlug = buildAreaSlug(safe);
    String filterSegment =
        (!priceSlug.isEmpty() || !areaSlug.isEmpty()) ? "/-" + priceSlug + areaSlug : "";

    return BASE_URL + "/elado-ingatlan/" + typeSlug + "/" + locationSegment + filterSegment;
  }

  @Override
  public String buildPageUrl(String baseSearchUrl, int page) {
    if (page <= 1) {
      return baseSearchUrl;
    }
    return baseSearchUrl + "/oldal-" + page;
  }

  private String buildPriceSlug(SearchCriteria criteria) {
    Integer minPrice = criteria.getMinPriceMillion();
    Integer maxPrice = criteria.getMaxPriceMillion();
    if (minPrice != null && maxPrice != null) {
      return "/" + minPrice + "-" + maxPrice + "-mFt";
    }
    if (minPrice != null) {
      return "/" + minPrice + "-mFt-tol";
    }
    if (maxPrice != null) {
      return "/" + maxPrice + "-mFt-ig";
    }
    return "";
  }

  private String buildAreaSlug(SearchCriteria criteria) {
    Integer minArea = criteria.getMinArea();
    Integer maxArea = criteria.getMaxArea();
    if (minArea != null && maxArea != null) {
      return "/" + minArea + "-" + maxArea + "-m2";
    }
    if (minArea != null) {
      return "/" + minArea + "--m2";
    }
    if (maxArea != null) {
      return "/-" + maxArea + "-m2";
    }
    return "";
  }
}
