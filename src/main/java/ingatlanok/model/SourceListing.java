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
  private String status = "ACTIVE";
  private Long priceHuf;
  private Long pricePerM2;
  private boolean viewed;
  private String viewedAt;

  @Builder
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

  public String getStatus() {
    return status != null ? status : "ACTIVE";
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
