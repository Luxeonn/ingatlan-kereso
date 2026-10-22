package ingatlanok.portal.koltozzbe;

import java.util.ArrayList;
import java.util.List;

import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalSupport;
import ingatlanok.portal.PortalUrlBuilder;

public class KoltozzbeUrlBuilder implements PortalUrlBuilder {

  public static final String BASE_URL = "https://koltozzbe.hu";

  @Override
  public String buildSearchUrl(SearchCriteria criteria) {
    SearchCriteria safe = PortalSupport.normalizeCriteria(criteria);
    String citySlug = PortalSupport.resolveCitySlug(safe);
    String typeSlug = PortalSupport.resolvePropertyTypeSlug(safe);

    String route = "/elado-" + typeSlug + "-" + citySlug;
    List<String> params = new ArrayList<>();

    if (safe.getMinPriceMillion() != null) {
      params.add("p1=" + safe.getMinPriceMillion() * 1_000_000L);
    }
    if (safe.getMaxPriceMillion() != null) {
      params.add("p2=" + safe.getMaxPriceMillion() * 1_000_000L);
    }
    if (safe.getMinArea() != null) {
      params.add("a1=" + safe.getMinArea());
    }
    if (safe.getMaxArea() != null) {
      params.add("a2=" + safe.getMaxArea());
    }

    if (params.isEmpty()) {
      return BASE_URL + route;
    }
    return BASE_URL + route + "?" + String.join("&", params);
  }

  @Override
  public String buildPageUrl(String baseSearchUrl, int page) {
    if (page <= 1) {
      return baseSearchUrl;
    }
    return baseSearchUrl + (baseSearchUrl.contains("?") ? "&" : "?") + "page=" + page;
  }
}
