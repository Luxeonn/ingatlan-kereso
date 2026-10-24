package ingatlanok.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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

  public IngatlanItem() {}

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

  public String getRawCity() {
    return city;
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

  public boolean isNewConstruction() {
    return newConstruction;
  }

  public void setNewConstruction(boolean newConstruction) {
    this.newConstruction = newConstruction;
  }

  public String getStatus() {
    return status != null ? status : "ACTIVE";
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getPriceDelta() {
    return priceDelta;
  }

  public void setPriceDelta(Long priceDelta) {
    this.priceDelta = priceDelta;
  }

  public Double getPriceChangePercent() {
    return priceChangePercent;
  }

  public void setPriceChangePercent(Double priceChangePercent) {
    this.priceChangePercent = priceChangePercent;
  }

  public String getLastPriceChangeAt() {
    return lastPriceChangeAt;
  }

  public void setLastPriceChangeAt(String lastPriceChangeAt) {
    this.lastPriceChangeAt = lastPriceChangeAt;
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

  public void setSourcesCount(int sourcesCount) {
    this.sourcesCount = sourcesCount;
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

  public Long getReferenceMedianPricePerSquareMeter() {
    return referenceMedianPricePerSquareMeter;
  }

  public void setReferenceMedianPricePerSquareMeter(Long referenceMedianPricePerSquareMeter) {
    this.referenceMedianPricePerSquareMeter = referenceMedianPricePerSquareMeter;
  }

  public Double getPercentageBelowMedian() {
    return percentageBelowMedian;
  }

  public void setPercentageBelowMedian(Double percentageBelowMedian) {
    this.percentageBelowMedian = percentageBelowMedian;
  }

  public Integer getReferenceSampleSize() {
    return referenceSampleSize;
  }

  public void setReferenceSampleSize(Integer referenceSampleSize) {
    this.referenceSampleSize = referenceSampleSize;
  }

  public Integer getQualityScore() {
    return qualityScore;
  }

  public void setQualityScore(Integer qualityScore) {
    this.qualityScore = qualityScore;
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
