package ingatlanok.portal.oc;

import java.util.ArrayList;
import java.util.List;

import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.PortalSupport;
import ingatlanok.portal.PortalUrlBuilder;

public class OtthonCentrumUrlBuilder implements PortalUrlBuilder {

  public static final String BASE_URL = "https://www.oc.hu";

  @Override
  public String buildSearchUrl(SearchCriteria criteria) {
    SearchCriteria safe = PortalSupport.normalizeCriteria(criteria);
    String citySlug = PortalSupport.resolveCitySlug(safe);
    String typeSlug = PortalSupport.resolvePropertyTypeSlug(safe);

    List<String> segments = new ArrayList<>();
    segments.add("jelleg:" + typeSlug);
    segments.add("ertekesites:elado");

    String priceSegment = buildRangeSegment("ar", safe.getMinPriceMillion(), safe.getMaxPriceMillion());
    if (!priceSegment.isEmpty()) {
      segments.add(priceSegment);
    }

    segments.add("elhelyezkedes:" + citySlug);

    String areaSegment = buildRangeSegment("meret", safe.getMinArea(), safe.getMaxArea());
    if (!areaSegment.isEmpty()) {
      segments.add(areaSegment);
    }

    return BASE_URL + "/ingatlanok/lista/" + String.join(";", segments);
  }

  private String buildRangeSegment(String key, Integer min, Integer max) {
    if (min != null && max != null) {
      return key + ":" + min + "~" + max;
    }
    if (min != null) {
      return key + ":" + min + "~";
    }
    if (max != null) {
      return key + ":~" + max;
    }
    return "";
  }
}
