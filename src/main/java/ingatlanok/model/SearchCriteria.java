package ingatlanok.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import ingatlanok.common.CityUtils;
import ingatlanok.common.PropertyParser;
import ingatlanok.common.SlugUtils;

@Getter
@Setter
public class SearchCriteria {

  @Size(max = 200, message = "A település neve túl hosszú (max. 200 karakter)!")
  private String city = "Hajdúszoboszló";

  @Size(max = 50, message = "Túl sok település lett megadva (max. 50 megengedett)!")
  private List<@Size(max = 200, message = "A település neve túl hosszú (max. 200 karakter)!") String>
      cities = new ArrayList<>();

  private PropertyType propertyType = PropertyType.HAZ;

  @PositiveOrZero(message = "A minimum ár nem lehet negatív")
  private Integer minPriceMillion = 70;

  @PositiveOrZero(message = "A maximum ár nem lehet negatív")
  private Integer maxPriceMillion = 120;

  @PositiveOrZero(message = "A minimum alapterület nem lehet negatív")
  private Integer minArea = 90;

  @PositiveOrZero(message = "A maximum alapterület nem lehet negatív")
  private Integer maxArea = 200;

  @PositiveOrZero(message = "A minimum szobaszám nem lehet negatív")
  private Integer minRooms;
  private List<String> portals = new ArrayList<>();
  private boolean excludeViewed = false;
  private boolean onlyDiscounted = false;
  private boolean onlyNewConstruction = false;

  public SearchCriteria() {

    if (cities.isEmpty()) {
      cities.add("Hajdúszoboszló");
    }
    portals.add("dh");
    portals.add("oc");
    portals.add("jofogas");
    portals.add("ingatlancom");
    portals.add("ingatlanbazar");
    portals.add("koltozzbe");
  }

  public String getCity() {
    if (cities != null && !cities.isEmpty()) {
      String first = cities.get(0);
      if (first != null && !first.isBlank()) {
        return first.trim();
      }
    }
    return city != null && !city.isBlank() ? city.trim() : "Hajdúszoboszló";
  }

  public void setCity(String city) {
    this.city = city;
    if (city != null && !city.isBlank()) {
      if (cities == null) {
        cities = new ArrayList<>();
      }
      if (cities.isEmpty()) {
        cities.add(city.trim());
      } else if (!cities.contains(city.trim())) {
        cities.set(0, city.trim());
      }
    }
  }

  public List<String> getCities() {
    if (cities != null && !cities.isEmpty()) {
      List<String> cleaned = new ArrayList<>();
      for (String c : cities) {
        if (c != null && !c.isBlank() && !cleaned.contains(c.trim())) {
          cleaned.add(c.trim());
        }
      }
      if (!cleaned.isEmpty()) {
        return cleaned;
      }
    }
    return List.of(getCity());
  }

  public void setCities(List<String> cities) {
    this.cities = cities != null ? new ArrayList<>(cities) : new ArrayList<>();
    if (!this.cities.isEmpty()) {
      this.city = this.cities.get(0);
    }
  }

  public SearchCriteria cloneForCity(String singleCity) {
    SearchCriteria clone = new SearchCriteria();
    clone.setCity(singleCity);
    clone.setCities(List.of(singleCity));
    clone.setPropertyType(this.propertyType);
    clone.setMinPriceMillion(this.minPriceMillion);
    clone.setMaxPriceMillion(this.maxPriceMillion);
    clone.setMinArea(this.minArea);
    clone.setMaxArea(this.maxArea);
    clone.setMinRooms(this.minRooms);
    clone.setPortals(this.portals != null ? new ArrayList<>(this.portals) : new ArrayList<>());
    clone.setExcludeViewed(this.excludeViewed);
    clone.setOnlyDiscounted(this.onlyDiscounted);
    clone.setOnlyNewConstruction(this.onlyNewConstruction);
    return clone;
  }

  public PropertyType getPropertyType() {
    return propertyType != null ? propertyType : PropertyType.HAZ;
  }

  public void setPropertyType(PropertyType propertyType) {
    this.propertyType = propertyType != null ? propertyType : PropertyType.HAZ;
  }

  public void setPropertyType(String raw) {
    this.propertyType = PropertyType.fromString(raw);
  }

  @JsonIgnore
  @AssertTrue(message = "A minimum ár nem lehet nagyobb a maximum árnál")
  public boolean isPriceRangeValid() {
    return minPriceMillion == null || maxPriceMillion == null || minPriceMillion <= maxPriceMillion;
  }

  @JsonIgnore
  @AssertTrue(message = "A minimum alapterület nem lehet nagyobb a maximum alapterületnél")
  public boolean isAreaRangeValid() {
    return minArea == null || maxArea == null || minArea <= maxArea;
  }

  @JsonIgnore
  @AssertTrue(message = "Ismeretlen portál azonosító a keresési feltételekben")
  public boolean isPortalsValid() {
    if (portals == null) {
      return true;
    }
    for (String p : portals) {
      if (p != null && !p.isBlank() && PortalId.fromString(p) == PortalId.UNKNOWN) {
        return false;
      }
    }
    return true;
  }

  public boolean isPortalEnabled(String portalKey) {
    return portals != null && portals.contains(portalKey);
  }

  public void setPortalEnabled(String portalKey, boolean enabled) {
    if (this.portals == null) {
      this.portals = new ArrayList<>();
    }
    if (enabled && !this.portals.contains(portalKey)) {
      this.portals.add(portalKey);
    } else if (!enabled) {
      this.portals.remove(portalKey);
    }
  }

  public boolean matches(IngatlanItem item) {
    return matchesRelevance(item) && matchesView(item);
  }

  public boolean matchesRelevance(IngatlanItem item) {
    if (item == null) {
      return false;
    }

    if (!PropertyParser.isPriceInRange(item.getPriceInfo(), minPriceMillion, maxPriceMillion)) {
      return false;
    }

    Double area =
        (item.getDetailsInfo() != null) ? item.getDetailsInfo().areaSquareMeters() : null;
    if (area == null && item.getDetails() != null) {
      PropertyDetails parsed = PropertyParser.parseDetails(item.getDetails(), item.getPriceInfo());
      area = parsed.areaSquareMeters();
    }
    if (!PropertyParser.isAreaInRange(area, minArea, maxArea)) {
      return false;
    }

    String targetCity = getCity();
    if (targetCity != null && !targetCity.isBlank()) {
      return matchesCity(item, targetCity);
    }

    return true;
  }

  public boolean matchesView(IngatlanItem item) {
    if (item == null) {
      return false;
    }

    if (excludeViewed && item.isViewed()) {
      return false;
    }

    if (onlyDiscounted && !item.isDiscounted()) {
      return false;
    }

    if (onlyNewConstruction && !item.isNewConstruction()) {
      return false;
    }

    return true;
  }

  public static boolean matchesCity(IngatlanItem item, String targetCity) {
    if (item == null || targetCity == null || targetCity.isBlank()) {
      return true;
    }

    String targetSlug = SlugUtils.toSlug(targetCity);
    if (targetSlug.isEmpty()) {
      return true;
    }

    Boolean locDecision = evaluateSlugMatch(item.getLocation(), targetSlug, true);
    if (locDecision != null) {
      return locDecision;
    }
    if (hasDifferentLeadingSettlement(item.getLocation(), targetSlug)) {
      return false;
    }
    Boolean cityDecision = evaluateSlugMatch(item.getCity(), targetSlug, true);
    if (cityDecision != null) {
      return cityDecision;
    }
    Boolean urlDecision = evaluateSlugMatch(item.getUrl(), targetSlug, false);
    if (urlDecision != null) {
      return urlDecision;
    }
    Boolean titleDecision = evaluateSlugMatch(item.getTitle(), targetSlug, false);
    if (titleDecision != null) {
      return titleDecision;
    }

    return true;
  }

  private static Boolean evaluateSlugMatch(
      String value, String targetSlug, boolean allowReverseContains) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String slug = SlugUtils.toSlug(value);
    if (isDirectOrSubstringMatch(slug, targetSlug, allowReverseContains)) {
      return true;
    }
    if (CityUtils.containsDifferentKnownCity(slug, targetSlug)) {
      return false;
    }
    return null;
  }

  private static boolean hasDifferentLeadingSettlement(String location, String targetSlug) {
    String settlement = CityUtils.extractLeadingSettlement(location);
    if (settlement == null) {
      return false;
    }
    String settlementSlug = SlugUtils.toSlug(settlement);
    return !isDirectOrSubstringMatch(settlementSlug, targetSlug, true);
  }

  private static boolean isDirectOrSubstringMatch(
      String slug, String targetSlug, boolean allowReverseContains) {
    return slug.equals(targetSlug)
        || slug.contains(targetSlug)
        || (allowReverseContains && targetSlug.contains(slug));
  }
}
