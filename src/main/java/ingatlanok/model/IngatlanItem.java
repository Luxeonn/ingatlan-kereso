package ingatlanok.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
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
  private boolean newConstruction;
  private String status = "ACTIVE";
  private Long priceDelta;
  private Double priceChangePercent;
  private String lastPriceChangeAt;
  private PropertyType propertyType = PropertyType.EGYEB_ISMERETLEN;
  private int sourcesCount = 1;
  private List<SourceListing> sourceListings = new ArrayList<>();

  // Milestone R4 Bargain fields (#263 & #264)
  private Boolean isBargain = false;
  private Long referenceMedianPricePerSquareMeter;
  private Double percentageBelowMedian;
  private Integer referenceSampleSize;

  // Milestone R5 Data Quality fields (#267)
  private Integer qualityScore;
  private List<String> qualityDeductions = new ArrayList<>();

  private PriceInfo priceInfo;
  private PropertyDetails detailsInfo;
  private PortalSource source;

  public IngatlanItem(
      String id,
      String portal,
      String url,
      String title,
      String price,
      String location,
      String details,
      String discoveredAt,
      boolean isNew) {
    this(id, portal, url, title, price, location, location, details, discoveredAt, isNew);
  }

  public IngatlanItem(
      String id,
      String portal,
      String url,
      String title,
      String price,
      String location,
      String city,
      String details,
      String discoveredAt,
      boolean isNew) {
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

  public IngatlanItem(
      String id,
      PortalSource source,
      String url,
      String title,
      PriceInfo priceInfo,
      String city,
      PropertyDetails detailsInfo,
      String discoveredAt,
      boolean isNew) {
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

  public IngatlanItem(
      String id,
      PortalSource source,
      String url,
      String title,
      PriceInfo priceInfo,
      String city,
      PropertyDetails detailsInfo,
      String discoveredAt,
      boolean isNew,
      PropertyType propertyType) {
    this(id, source, url, title, priceInfo, city, detailsInfo, discoveredAt, isNew);
    this.propertyType = propertyType != null ? propertyType : PropertyType.EGYEB_ISMERETLEN;
  }

  @Builder
  public IngatlanItem(
      String id,
      PortalSource source,
      String url,
      String title,
      PriceInfo priceInfo,
      String city,
      String location,
      PropertyDetails detailsInfo,
      String discoveredAt,
      boolean isNew,
      PropertyType propertyType,
      String imageUrl,
      boolean newConstruction) {
    this(id, source, url, title, priceInfo, city, detailsInfo, discoveredAt, isNew, propertyType);
    if (location != null) {
      this.location = location;
    }
    this.imageUrl = imageUrl;
    this.newConstruction = newConstruction;
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

  public void setPriceInfo(PriceInfo priceInfo) {
    this.priceInfo = priceInfo;
    if (priceInfo != null && priceInfo.getRawText() != null) {
      this.price = priceInfo.getRawText();
    }
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

  public String getRawCity() {
    return city;
  }

  public void setDetailsInfo(PropertyDetails detailsInfo) {
    this.detailsInfo = detailsInfo;
    if (detailsInfo != null && detailsInfo.getRawText() != null) {
      this.details = detailsInfo.getRawText();
    }
  }

  public String getStatus() {
    return status != null ? status : "ACTIVE";
  }

  public PropertyType getPropertyType() {
    return propertyType != null ? propertyType : PropertyType.EGYEB_ISMERETLEN;
  }

  public void setPropertyType(PropertyType propertyType) {
    this.propertyType = propertyType != null ? propertyType : PropertyType.EGYEB_ISMERETLEN;
  }

  public void setPropertyType(String typeStr) {
    this.propertyType = PropertyType.fromString(typeStr);
  }

  public boolean isDiscounted() {
    return priceDelta != null && priceDelta < 0;
  }

  public int getSourcesCount() {
    if (sourceListings != null && !sourceListings.isEmpty()) {
      return sourceListings.size();
    }
    return Math.max(sourcesCount, 1);
  }

  public List<SourceListing> getSourceListings() {
    if (sourceListings == null) {
      sourceListings = new ArrayList<>();
    }
    return sourceListings;
  }

  public void setSourceListings(List<SourceListing> sourceListings) {
    this.sourceListings = sourceListings != null ? new ArrayList<>(sourceListings) : new ArrayList<>();
    this.sourcesCount = Math.max(this.sourceListings.size(), 1);
  }

  public void addSourceListing(SourceListing sourceListing) {
    if (sourceListing != null) {
      if (this.sourceListings == null) {
        this.sourceListings = new ArrayList<>();
      }
      this.sourceListings.add(sourceListing);
      this.sourcesCount = this.sourceListings.size();
    }
  }

  public Boolean isBargain() {
    return Boolean.TRUE.equals(isBargain);
  }

  public Boolean getIsBargain() {
    return isBargain;
  }

  public void setBargain(Boolean bargain) {
    this.isBargain = bargain;
  }

  public void setIsBargain(Boolean bargain) {
    this.isBargain = bargain;
  }

  public List<String> getQualityDeductions() {
    if (qualityDeductions == null) {
      qualityDeductions = new ArrayList<>();
    }
    return qualityDeductions;
  }

  public void setQualityDeductions(List<String> qualityDeductions) {
    this.qualityDeductions =
        qualityDeductions != null ? new ArrayList<>(qualityDeductions) : new ArrayList<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IngatlanItem that = (IngatlanItem) o;
    return Objects.equals(url, that.url);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(url);
  }
}
