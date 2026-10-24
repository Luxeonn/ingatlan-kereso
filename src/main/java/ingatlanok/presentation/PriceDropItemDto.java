package ingatlanok.presentation;

import java.io.Serializable;

import ingatlanok.model.PropertyType;

/**
 * Summary DTO representing a listing with a registered price reduction.
 */
public class PriceDropItemDto implements Serializable {

  private String id;
  private String url;
  private String title;
  private String city;
  private String portal;
  private PropertyType propertyType;
  private Long currentPriceHuf;
  private Double currentPriceMillion;
  private Long priceDelta;
  private Double changePercent;
  private Long pricePerM2;
  private String imageUrl;
  private String lastPriceChangeAt;

  public PriceDropItemDto() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getPortal() {
    return portal;
  }

  public void setPortal(String portal) {
    this.portal = portal;
  }

  public PropertyType getPropertyType() {
    return propertyType;
  }

  public void setPropertyType(PropertyType propertyType) {
    this.propertyType = propertyType;
  }

  public Long getCurrentPriceHuf() {
    return currentPriceHuf;
  }

  public void setCurrentPriceHuf(Long currentPriceHuf) {
    this.currentPriceHuf = currentPriceHuf;
  }

  public Double getCurrentPriceMillion() {
    return currentPriceMillion;
  }

  public void setCurrentPriceMillion(Double currentPriceMillion) {
    this.currentPriceMillion = currentPriceMillion;
  }

  public Long getPriceDelta() {
    return priceDelta;
  }

  public void setPriceDelta(Long priceDelta) {
    this.priceDelta = priceDelta;
  }

  public Double getChangePercent() {
    return changePercent;
  }

  public void setChangePercent(Double changePercent) {
    this.changePercent = changePercent;
  }

  public Long getPricePerM2() {
    return pricePerM2;
  }

  public void setPricePerM2(Long pricePerM2) {
    this.pricePerM2 = pricePerM2;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getLastPriceChangeAt() {
    return lastPriceChangeAt;
  }

  public void setLastPriceChangeAt(String lastPriceChangeAt) {
    this.lastPriceChangeAt = lastPriceChangeAt;
  }
}
