package ingatlanok.model;

import java.io.Serializable;
import java.util.Objects;

public class SourceListing implements Serializable {

  private String portal;
  private String externalListingId;
  private String url;
  private String title;
  private String imageUrl;
  private String firstSeenAt;
  private String lastSeenAt;
  private String status = "ACTIVE";
  private Long priceHuf;
  private Long pricePerM2;
  private boolean viewed;
  private String viewedAt;

  public SourceListing() {}

  public SourceListing(
      String portal,
      String externalListingId,
      String url,
      String title,
      String imageUrl,
      String firstSeenAt,
      String lastSeenAt,
      String status,
      Long priceHuf,
      Long pricePerM2,
      boolean viewed,
      String viewedAt) {
    this.portal = portal;
    this.externalListingId = externalListingId;
    this.url = url;
    this.title = title;
    this.imageUrl = imageUrl;
    this.firstSeenAt = firstSeenAt;
    this.lastSeenAt = lastSeenAt;
    this.status = status != null ? status : "ACTIVE";
    this.priceHuf = priceHuf;
    this.pricePerM2 = pricePerM2;
    this.viewed = viewed;
    this.viewedAt = viewedAt;
  }

  public String getPortal() {
    return portal;
  }

  public void setPortal(String portal) {
    this.portal = portal;
  }

  public String getExternalListingId() {
    return externalListingId;
  }

  public void setExternalListingId(String externalListingId) {
    this.externalListingId = externalListingId;
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

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getFirstSeenAt() {
    return firstSeenAt;
  }

  public void setFirstSeenAt(String firstSeenAt) {
    this.firstSeenAt = firstSeenAt;
  }

  public String getLastSeenAt() {
    return lastSeenAt;
  }

  public void setLastSeenAt(String lastSeenAt) {
    this.lastSeenAt = lastSeenAt;
  }

  public String getStatus() {
    return status != null ? status : "ACTIVE";
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getPriceHuf() {
    return priceHuf;
  }

  public void setPriceHuf(Long priceHuf) {
    this.priceHuf = priceHuf;
  }

  public Long getPricePerM2() {
    return pricePerM2;
  }

  public void setPricePerM2(Long pricePerM2) {
    this.pricePerM2 = pricePerM2;
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

  public boolean isActive() {
    return "ACTIVE".equalsIgnoreCase(status);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SourceListing that = (SourceListing) o;
    return Objects.equals(url, that.url);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(url);
  }

  @Override
  public String toString() {
    return "SourceListing{"
        + "portal='" + portal + '\''
        + ", url='" + url + '\''
        + ", priceHuf=" + priceHuf
        + ", status='" + status + '\''
        + '}';
  }
}
