package ingatlanok.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Keresési szűrési paraméterek.
 */
public class SearchCriteria implements Serializable {

    private String city = "Debrecen";
    private List<String> cities = new ArrayList<>();
    private String propertyType = "lakas"; // "lakas", "haz", "garazs", "telek", "nyaralo", "iroda", "osszes"
    private Integer minPriceMillion;
    private Integer maxPriceMillion;
    private Integer minRooms;
    private List<String> portals = new ArrayList<>();
    private boolean autoOpenBrowser = false;

    public SearchCriteria() {
        // Alapértelmezett város és aktív portálok
        if (cities.isEmpty()) {
            cities.add("Debrecen");
        }
        portals.add("dh");
        portals.add("oc");
        portals.add("jofogas");
        portals.add("ingatlancom");
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

    /**
     * Klónozza a keresési feltételeket egyetlen konkrét városra (többvárosos keresési ciklushoz).
     */
    public SearchCriteria cloneForCity(String singleCity) {
        SearchCriteria clone = new SearchCriteria();
        clone.setCity(singleCity);
        clone.setCities(List.of(singleCity));
        clone.setPropertyType(this.propertyType);
        clone.setMinPriceMillion(this.minPriceMillion);
        clone.setMaxPriceMillion(this.maxPriceMillion);
        clone.setMinRooms(this.minRooms);
        clone.setPortals(this.portals != null ? new ArrayList<>(this.portals) : new ArrayList<>());
        clone.setAutoOpenBrowser(this.autoOpenBrowser);
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

    public boolean isPortalEnabled(String portalKey) {
        return portals != null && portals.contains(portalKey);
    }
}
