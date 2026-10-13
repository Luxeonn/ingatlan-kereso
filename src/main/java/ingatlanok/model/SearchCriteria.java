package ingatlanok.model;

import ingatlanok.common.CityUtils;
import ingatlanok.common.PropertyParser;
import ingatlanok.common.SlugUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SearchCriteria implements Serializable {

    private String city = "Debrecen";
    private List<String> cities = new ArrayList<>();
    private String propertyType = "lakas";
    private Integer minPriceMillion = 70;
    private Integer maxPriceMillion = 120;
    private Integer minArea = 90;
    private Integer maxArea = 200;
    private Integer minRooms;
    private List<String> portals = new ArrayList<>();
    private boolean autoOpenBrowser = false;
    private boolean excludeViewed = false;

    public SearchCriteria() {

        if (cities.isEmpty()) {
            cities.add("Debrecen");
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
        return city != null && !city.isBlank() ? city.trim() : "Debrecen";
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
        return clone;
    }

    public String getPropertyType() {
        return propertyType != null ? propertyType : "lakas";
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    public Integer getMinPriceMillion() {
        return minPriceMillion;
    }

    public void setMinPriceMillion(Integer minPriceMillion) {
        this.minPriceMillion = minPriceMillion;
    }

    public Integer getMaxPriceMillion() {
        return maxPriceMillion;
    }

    public void setMaxPriceMillion(Integer maxPriceMillion) {
        this.maxPriceMillion = maxPriceMillion;
    }

    public Integer getMinRooms() {
        return minRooms;
    }

    public void setMinRooms(Integer minRooms) {
        this.minRooms = minRooms;
    }

    public Integer getMinArea() {
        return minArea;
    }

    public void setMinArea(Integer minArea) {
        this.minArea = minArea;
    }

    public Integer getMaxArea() {
        return maxArea;
    }

    public void setMaxArea(Integer maxArea) {
        this.maxArea = maxArea;
    }

    public List<String> getPortals() {
        return portals;
    }

    public void setPortals(List<String> portals) {
        this.portals = portals;
    }

    public boolean isAutoOpenBrowser() {
        return autoOpenBrowser;
    }

    public void setAutoOpenBrowser(boolean autoOpenBrowser) {
        this.autoOpenBrowser = autoOpenBrowser;
    }

    public boolean isExcludeViewed() {
        return excludeViewed;
    }

    public void setExcludeViewed(boolean excludeViewed) {
        this.excludeViewed = excludeViewed;
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
        if (item == null) {
            return false;
        }

        if (excludeViewed && item.isViewed()) {
            return false;
        }

        if (!PropertyParser.isPriceInRange(item.getPriceInfo(), minPriceMillion, maxPriceMillion)) {
            return false;
        }

        Double area = (item.getDetailsInfo() != null) ? item.getDetailsInfo().getAreaSquareMeters() : null;
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

    public static boolean matchesCity(IngatlanItem item, String targetCity) {
        if (item == null || targetCity == null || targetCity.isBlank()) {
            return true;
        }

        String targetSlug = SlugUtils.toSlug(targetCity);
        if (targetSlug.isEmpty()) {
            return true;
        }

        String itemLoc = item.getLocation();
        if (itemLoc != null && !itemLoc.isBlank()) {
            String locSlug = SlugUtils.toSlug(itemLoc);
            if (locSlug.equals(targetSlug) || locSlug.contains(targetSlug) || targetSlug.contains(locSlug)) {
                return true;
            }
            if (CityUtils.containsDifferentKnownCity(locSlug, targetSlug)) {
                return false;
            }
        }

        String itemCity = item.getCity();
        if (itemCity != null && !itemCity.isBlank()) {
            String citySlug = SlugUtils.toSlug(itemCity);
            if (citySlug.equals(targetSlug) || citySlug.contains(targetSlug) || targetSlug.contains(citySlug)) {
                return true;
            }
            if (CityUtils.containsDifferentKnownCity(citySlug, targetSlug)) {
                return false;
            }
        }

        String url = item.getUrl();
        if (url != null && !url.isBlank()) {
            String urlSlug = SlugUtils.toSlug(url);
            if (urlSlug.contains(targetSlug)) {
                return true;
            }
            if (CityUtils.containsDifferentKnownCity(urlSlug, targetSlug)) {
                return false;
            }
        }

        String title = item.getTitle();
        if (title != null && !title.isBlank()) {
            String titleSlug = SlugUtils.toSlug(title);
            if (titleSlug.contains(targetSlug)) {
                return true;
            }
            if (CityUtils.containsDifferentKnownCity(titleSlug, targetSlug)) {
                return false;
            }
        }

        return true;
    }
}
