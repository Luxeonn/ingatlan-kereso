package ingatlanok.portal.ingatlanbazar;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalSupport;
import ingatlanok.portal.PortalUrlBuilder;

public class IngatlanBazarQueryBuilder implements PortalUrlBuilder {

  public static final String BASE_URL = "https://www.ingatlanbazar.hu";
  public static final String API_SEARCH_URL = "https://www.ingatlanbazar.hu/api/property-search";

  @Override
  public String buildSearchUrl(SearchCriteria criteria) {
    SearchCriteria safe = PortalSupport.normalizeCriteria(criteria);
    String city = PortalSupport.resolveDefaultCity(safe);
    String displayCity = PortalSupport.resolveAsciiCapitalizedCity(city);

    String rawType = PortalSupport.resolvePropertyTypeSlug(safe);
    String typeSlug =
        switch (rawType) {
          case "haz" -> "Haz";
          case "telek" -> "Telek";
          default -> "Lakas";
        };

    StringBuilder path =
        new StringBuilder(BASE_URL)
            .append("/ingatlan-")
            .append(displayCity)
            .append("-Elado-")
            .append(typeSlug);

    path.append(buildPricePathSlug(safe));
    path.append(buildAreaPathSlug(safe));

    List<String> queryParams = new ArrayList<>();
    Optional<Integer> locId = IngatlanBazarLocationResolver.resolveLocationId(city);
    locId.ifPresent(id -> queryParams.add("property_location=" + id));

    if (safe.getMinPriceMillion() != null) {
      queryParams.add("price_min=" + safe.getMinPriceMillion());
    }
    if (safe.getMaxPriceMillion() != null) {
      queryParams.add("price_max=" + safe.getMaxPriceMillion());
    }

    String floorAreaParam = buildFloorAreaQueryValue(safe);
    if (!floorAreaParam.isEmpty()) {
      queryParams.add("property_floor_area=" + floorAreaParam);
    }

    queryParams.add("property__2=" + resolveTypeCode(rawType));

    return path.append("?").append(String.join("&", queryParams)).toString();
  }

  public String buildApiSearchUrl(SearchCriteria criteria, int page) {
    SearchCriteria safe = PortalSupport.normalizeCriteria(criteria);
    StringBuilder sb = new StringBuilder(API_SEARCH_URL);
    sb.append("?property__1=1");

    String city = PortalSupport.resolveDefaultCity(safe);
    Optional<Integer> locId = IngatlanBazarLocationResolver.resolveLocationId(city);
    locId.ifPresent(id -> sb.append("&property_location=").append(id));

    String rawType = PortalSupport.resolvePropertyTypeSlug(safe);
    sb.append("&property__2=").append(resolveTypeCode(rawType));

    if (safe.getMinPriceMillion() != null) {
      sb.append("&price_min=").append(safe.getMinPriceMillion());
    }
    if (safe.getMaxPriceMillion() != null) {
      sb.append("&price_max=").append(safe.getMaxPriceMillion());
    }

    String floorAreaParam = buildFloorAreaQueryValue(safe);
    if (!floorAreaParam.isEmpty()) {
      sb.append("&property_floor_area=").append(floorAreaParam);
    }

    sb.append("&limit=50&page=").append(page);
    return sb.toString();
  }

  private String buildPricePathSlug(SearchCriteria criteria) {
    Integer minPrice = criteria.getMinPriceMillion();
    Integer maxPrice = criteria.getMaxPriceMillion();
    if (minPrice != null && maxPrice != null) {
      return "-" + minPrice + "-" + maxPrice + "-millio-Ft";
    }
    if (minPrice != null) {
      return "-" + minPrice + "-millio-Ft-tol";
    }
    if (maxPrice != null) {
      return "-" + maxPrice + "-millio-Ft-ig";
    }
    return "";
  }

  private String buildAreaPathSlug(SearchCriteria criteria) {
    Integer minArea = criteria.getMinArea();
    Integer maxArea = criteria.getMaxArea();
    if (minArea != null && maxArea != null) {
      return "-" + minArea + "m2-tol-" + maxArea + "m2-ig";
    }
    if (minArea != null) {
      return "-" + minArea + "m2-tol";
    }
    if (maxArea != null) {
      return "-" + maxArea + "m2-ig";
    }
    return "";
  }

  private String buildFloorAreaQueryValue(SearchCriteria criteria) {
    Integer minArea = criteria.getMinArea();
    Integer maxArea = criteria.getMaxArea();
    if (minArea != null && maxArea != null) {
      return minArea + "-" + maxArea;
    }
    if (minArea != null) {
      return minArea + "-";
    }
    if (maxArea != null) {
      return "-" + maxArea;
    }
    return "";
  }

  private int resolveTypeCode(String rawType) {
    return switch (rawType) {
      case "haz" -> 2;
      case "telek" -> 5;
      default -> 3;
    };
  }
}
