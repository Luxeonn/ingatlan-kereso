package ingatlanok.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Ingatlan hirdetési adatmodell.
 */
public class IngatlanItem implements Serializable {

    private String id;
    private String portal;
    private String url;
    private String title;
    private String price;
    private String location;
    private String city;
    private String details;
    private String discoveredAt;
    private boolean isNew;

    public IngatlanItem() {
    }

    public IngatlanItem(String id, String portal, String url, String title, String price, String location, String details, String discoveredAt, boolean isNew) {
        this.id = id;
        this.portal = portal;
        this.url = url;
        this.title = title;
        this.price = price;
        this.location = location;
        this.city = location;
        this.details = details;
        this.discoveredAt = discoveredAt;
        this.isNew = isNew;
    }

    public IngatlanItem(String id, String portal, String url, String title, String price, String location, String city, String details, String discoveredAt, boolean isNew) {
        this.id = id;
        this.portal = portal;
        this.url = url;
        this.title = title;
        this.price = price;
        this.location = location;
        this.city = city;
        this.details = details;
        this.discoveredAt = discoveredAt;
        this.isNew = isNew;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPortal() {
        return portal;
    }

    public void setPortal(String portal) {
        this.portal = portal;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
        if (this.city == null || this.city.isBlank()) {
            this.city = location;
        }
    }

    public String getCity() {
        if (city != null && !city.isBlank()) {
            return city;
        }
        return location != null && !location.isBlank() ? location : "Debrecen";
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getDiscoveredAt() {
        return discoveredAt;
    }

    public void setDiscoveredAt(String discoveredAt) {
        this.discoveredAt = discoveredAt;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IngatlanItem that = (IngatlanItem) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(url);
    }
}
