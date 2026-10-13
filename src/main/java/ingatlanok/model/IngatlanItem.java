package ingatlanok.model;

import java.io.Serializable;
import java.util.Objects;

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
    private boolean viewed;
    private String viewedAt;
    private String imageUrl;

    private PriceInfo priceInfo;
    private PropertyDetails detailsInfo;
    private PortalSource source;

    public IngatlanItem() {
    }

    public IngatlanItem(String id, String portal, String url, String title, String price, String location, String details, String discoveredAt, boolean isNew) {
        this(id, portal, url, title, price, location, location, details, discoveredAt, isNew);
    }

    public IngatlanItem(String id, String portal, String url, String title, String price, String location, String city, String details, String discoveredAt, boolean isNew) {
        this.id = id;
        this.portal = portal;
        this.source = PortalSource.of(portal);
        this.url = url;
        this.title = title;
        this.price = price;
        this.location = location;
        this.city = city != null && !city.isBlank() ? city : location;
        this.details = details;
        this.discoveredAt = discoveredAt;
        this.isNew = isNew;
    }

    public IngatlanItem(String id, PortalSource source, String url, String title, PriceInfo priceInfo, String city, PropertyDetails detailsInfo, String discoveredAt, boolean isNew) {
        this.id = id;
        this.source = source;
        this.portal = source != null ? source.getPortalName() : "Ismeretlen";
        this.url = url;
        this.title = title;
        this.priceInfo = priceInfo;
        this.price = priceInfo != null ? priceInfo.getRawText() : "Ár a hirdetésben";
        this.city = city;
        this.location = city;
        this.detailsInfo = detailsInfo;
        this.details = detailsInfo != null ? detailsInfo.getRawText() : "";
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
        this.source = PortalSource.of(portal);
    }

    public PortalSource getSource() {
        if (source == null && portal != null) {
            source = PortalSource.of(portal);
        }
        return source;
    }

    public void setSource(PortalSource source) {
        this.source = source;
        if (source != null) {
            this.portal = source.getPortalName();
        }
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

    public PriceInfo getPriceInfo() {
        return priceInfo;
    }

    public void setPriceInfo(PriceInfo priceInfo) {
        this.priceInfo = priceInfo;
        if (priceInfo != null && priceInfo.getRawText() != null) {
            this.price = priceInfo.getRawText();
        }
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

    public PropertyDetails getDetailsInfo() {
        return detailsInfo;
    }

    public void setDetailsInfo(PropertyDetails detailsInfo) {
        this.detailsInfo = detailsInfo;
        if (detailsInfo != null && detailsInfo.getRawText() != null) {
            this.details = detailsInfo.getRawText();
        }
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

    public boolean isViewed() {
        return viewed;
    }

    public void setViewed(boolean viewed) {
        this.viewed = viewed;
    }

    public String getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(String viewedAt) {
        this.viewedAt = viewedAt;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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
