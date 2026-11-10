package ingatlanok.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import ingatlanok.common.CityUtils;
import ingatlanok.common.PropertyParser;
import ingatlanok.common.SlugUtils;

@Getter
@Setter
public class SearchCriteria implements Serializable {

  private String city = "Hajdúszoboszló";
  private List<String> cities = new ArrayList<>();
  private PropertyType propertyType = PropertyType.HAZ;
  private Integer minPriceMillion = 70;
  private Integer maxPriceMillion = 120;
  private Integer minArea = 90;
  private Integer maxArea = 200;
  private Integer minRooms;
  private List<String> portals = new ArrayList<>();
  private boolean autoOpenBrowser = false;
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
    clone.setAutoOpenBrowser(this.autoOpenBrowser);
    clone.setExcludeViewed(this.excludeViewed);
    clone.setOnlyDiscounted(this.onlyDiscounted);
    clone.setOnlyNewConstruction(this.onlyNewConstruction);
    return clone;
  }

  public PropertyType getPropertyType() {
    return propertyType != null ? propertyType : PropertyType.HAZ;
  }

  public String getPropertyTypeSlug() {
    return getPropertyType().getSlug();
  }

  public void setPropertyType(PropertyType propertyType) {
    this.propertyType = propertyType != null ? propertyType : PropertyType.HAZ;
  }

  public void setPropertyType(String raw) {
    this.propertyType = PropertyType.fromString(raw);
  }

  public void validate() {
    validatePriceRanges();
    validateAreaRanges();
    if (minRooms != null && minRooms < 0) {
      throw new IllegalArgumentException("A minimum szobaszám nem lehet negatív: " + minRooms);
    }
    validateCities();
    validatePortals();
  }

  private void validatePriceRanges() {
    if (minPriceMillion != null && minPriceMillion < 0) {
      throw new IllegalArgumentException("A minimum ár nem lehet negatív: " + minPriceMillion);
    }
    if (maxPriceMillion != null && maxPriceMillion < 0) {
      throw new IllegalArgumentException("A maximum ár nem lehet negatív: " + maxPriceMillion);
    }
    if (minPriceMillion != null && maxPriceMillion != null && minPriceMillion > maxPriceMillion) {
      throw new IllegalArgumentException(
          "A minimum ár nem lehet nagyobb a maximum árnál: " + minPriceMillion + " > " + maxPriceMillion);
    }
  }

  private void validateAreaRanges() {
    if (minArea != null && minArea < 0) {
      throw new IllegalArgumentException("A minimum alapterület nem lehet negatív: " + minArea);
    }
    if (maxArea != null && maxArea < 0) {
      throw new IllegalArgumentException("A maximum alapterület nem lehet negatív: " + maxArea);
    }
    if (minArea != null && maxArea != null && minArea > maxArea) {
      throw new IllegalArgumentException(
          "A minimum alapterület nem lehet nagyobb a maximum alapterületnél: " + minArea + " > " + maxArea);
    }
  }

  private void validateCities() {
    if (city != null && city.length() > 200) {
      throw new IllegalArgumentException("A település neve túl hosszú (max. 200 karakter)!");
    }
    if (cities != null) {
      if (cities.size() > 50) {
        throw new IllegalArgumentException("Túl sok település lett megadva (max. 50 megengedett)!");
      }
      for (String c : cities) {
        if (c != null && c.length() > 200) {
          throw new IllegalArgumentException("A település neve túl hosszú: " + c);
        }
      }
    }
  }

  private void validatePortals() {
    if (portals != null) {
      for (String p : portals) {
        if (p != null && !p.isBlank() && PortalId.fromString(p) == PortalId.UNKNOWN) {
          throw new IllegalArgumentException("Ismeretlen portál azonosító a keresési feltételekben: " + p);
        }
      }
    }
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
        (item.getDetailsInfo() != null) ? item.getDetailsInfo().getAreaSquareMeters() : null;
    if (area == null && item.getDetails() != null) {
      PropertyDetails parsed = PropertyParser.parseDetails(item.getDetails(), item.getPriceInfo());
      area = parsed.getAreaSquareMeters();
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

  private static boolean isDirectOrSubstringMatch(
      String slug, String targetSlug, boolean allowReverseContains) {
    return slug.equals(targetSlug)
        || slug.contains(targetSlug)
        || (allowReverseContains && targetSlug.contains(slug));
  }
}
