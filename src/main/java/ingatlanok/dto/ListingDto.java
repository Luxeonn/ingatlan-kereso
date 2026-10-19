package ingatlanok.dto;

import java.io.Serializable;

import ingatlanok.model.IngatlanItem;

/**
 * Típusbiztos hirdetés DTO (Java 21 Record), amely az adatbázis és a webes réteg között közvetít.
 */
public record ListingDto(
    String id,
    String portal,
    String url,
    String title,
    String price,
    String location,
    String city,
    String details,
    String discoveredAt,
    boolean isNew,
    boolean viewed,
    String viewedAt,
    String imageUrl,
    String status,
    Long priceDelta,
    Double priceChangePercent,
    String lastPriceChangeAt,
    PriceInfoDto priceInfo,
    PropertyDetailsDto detailsInfo)
    implements Serializable {

  public IngatlanItem toItem() {
    IngatlanItem item = new IngatlanItem();
    item.setId(id);
    item.setPortal(portal);
    item.setUrl(url);
    item.setTitle(title);
    item.setPrice(price);
    item.setLocation(location);
    item.setCity(city);
    item.setDetails(details);
    item.setDiscoveredAt(discoveredAt);
    item.setNew(isNew);
    item.setViewed(viewed);
    item.setViewedAt(viewedAt);
    item.setImageUrl(imageUrl);
    item.setStatus(status != null ? status : "ACTIVE");
    if (priceDelta != null) {
      item.setPriceDelta(priceDelta);
    }
    if (priceChangePercent != null) {
      item.setPriceChangePercent(priceChangePercent);
    }
    item.setLastPriceChangeAt(lastPriceChangeAt);
    if (priceInfo != null) {
      item.setPriceInfo(priceInfo.toModel());
    }
    if (detailsInfo != null) {
      item.setDetailsInfo(detailsInfo.toModel());
    }
    return item;
  }

  public static ListingDto fromItem(IngatlanItem item) {
    if (item == null) {
      return null;
    }
    return new ListingDto(
        item.getId(),
        item.getPortal(),
        item.getUrl(),
        item.getTitle(),
        item.getPrice(),
        item.getLocation(),
        item.getCity(),
        item.getDetails(),
        item.getDiscoveredAt(),
        item.isNew(),
        item.isViewed(),
        item.getViewedAt(),
        item.getImageUrl(),
        item.getStatus() != null ? item.getStatus() : "ACTIVE",
        (item.getPriceDelta() != null && item.getPriceDelta() != 0) ? item.getPriceDelta() : null,
        (item.getPriceChangePercent() != null && item.getPriceChangePercent() != 0.0)
            ? item.getPriceChangePercent()
            : null,
        item.getLastPriceChangeAt(),
        item.getPriceInfo() != null ? PriceInfoDto.fromModel(item.getPriceInfo()) : null,
        item.getDetailsInfo() != null ? PropertyDetailsDto.fromModel(item.getDetailsInfo()) : null);
  }
}
