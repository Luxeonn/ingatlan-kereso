package ingatlanok.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Keresési szűrési paraméterek.
 */
public class SearchCriteria implements Serializable {

    private String city = "Debrecen";
    private String propertyType = "lakas"; // "lakas", "haz", "garazs", "telek", "nyaralo", "iroda", "osszes"
    private Integer minPriceMillion;
    private Integer maxPriceMillion;
    private Integer minRooms;
    private List<String> portals = new ArrayList<>();
    private boolean autoOpenBrowser = false;

    public SearchCriteria() {
        // Alapértelmezett aktív portálok
        portals.add("dh");
        portals.add("oc");
        portals.add("jofogas");
        portals.add("ingatlancom");
    }

    public String getCity() {
        return city != null && !city.isBlank() ? city.trim() : "Debrecen";
    }

    public void setCity(String city) {
        this.city = city;
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
