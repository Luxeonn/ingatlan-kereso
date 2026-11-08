package ingatlanok.model;

import java.io.Serializable;
import java.util.Objects;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SourceListing implements Serializable {

  private String portal;
  private String externalListingId;
  private String url;
  private String title;
  private String imageUrl;
  private String firstSeenAt;
  private String lastSeenAt;
  private ListingStatus status = ListingStatus.ACTIVE;
  private Long priceHuf;
  private Long pricePerM2;
  private boolean viewed;
  private String viewedAt;
  private boolean hidden;
  private String hiddenAt;

  @Builder
  public SourceListing(
      String portal,
      String externalListingId,
      String url,
      String title,
      String imageUrl,
      String firstSeenAt,
      String lastSeenAt,
      ListingStatus status,
      Long priceHuf,
      Long pricePerM2,
      boolean viewed,
      String viewedAt,
      boolean hidden,
      String hiddenAt) {
    this.portal = portal;
    this.externalListingId = externalListingId;
    this.url = url;
    this.title = title;
    this.imageUrl = imageUrl;
    this.firstSeenAt = firstSeenAt;
    this.lastSeenAt = lastSeenAt;
    this.status = status != null ? status : ListingStatus.ACTIVE;
    this.priceHuf = priceHuf;
    this.pricePerM2 = pricePerM2;
    this.viewed = viewed;
    this.viewedAt = viewedAt;
    this.hidden = hidden;
    this.hiddenAt = hiddenAt;
  }

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
      String viewedAt,
      boolean hidden,
      String hiddenAt) {
    this(
        portal,
        externalListingId,
        url,
        title,
        imageUrl,
        firstSeenAt,
        lastSeenAt,
        ListingStatus.fromString(status),
        priceHuf,
        pricePerM2,
        viewed,
        viewedAt,
        hidden,
        hiddenAt);
  }

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
    this(
        portal,
        externalListingId,
        url,
        title,
        imageUrl,
        firstSeenAt,
        lastSeenAt,
        status,
        priceHuf,
        pricePerM2,
        viewed,
        viewedAt,
        false,
        null);
  }

  public ListingStatus getStatus() {
    return status != null ? status : ListingStatus.ACTIVE;
  }

  public void setStatus(ListingStatus status) {
    this.status = status;
  }

  public void setStatus(String raw) {
    this.status = raw != null ? ListingStatus.fromString(raw) : null;
  }

  public boolean isActive() {
    return status == ListingStatus.ACTIVE;
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
