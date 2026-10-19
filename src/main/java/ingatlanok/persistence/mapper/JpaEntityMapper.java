package ingatlanok.persistence.mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ingatlanok.common.CityUtils;
import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceHistoryEntry;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.StatusLogEntry;
import ingatlanok.persistence.entity.PriceHistoryEntity;
import ingatlanok.persistence.entity.PropertyEntity;
import ingatlanok.persistence.entity.StatusLogEntity;

/** Kétirányú entitás-modell leképező komponens JPA entitások és domain modellek között. */
@Component
public class JpaEntityMapper {

  public IngatlanItem toItem(PropertyEntity entity) {
    if (entity == null) {
      return null;
    }
    IngatlanItem item = new IngatlanItem();
    item.setId(entity.getPropertyId());
    item.setUrl(entity.getUrl());
    item.setPortal(entity.getPortal());
    item.setTitle(entity.getTitle());
    item.setPrice(entity.getPriceRaw());
    item.setLocation(entity.getLocation());
    item.setCity(entity.getCity());
    item.setDetails(entity.getDetails());
    item.setDiscoveredAt(entity.getDiscoveredAt());
    item.setNew(entity.isNew());
    item.setViewed(entity.isViewed());
    item.setViewedAt(entity.getViewedAt());
    item.setImageUrl(entity.getImageUrl());
    item.setStatus(entity.getStatus());
    item.setPriceDelta(entity.getPriceDelta());
    item.setPriceChangePercent(entity.getPriceChangePercent());
    item.setLastPriceChangeAt(entity.getLastPriceChangeAt());

    if (entity.getPriceAmount() != null || entity.getPriceMillions() != null) {
      item.setPriceInfo(
          new PriceInfo(
              entity.getPriceRaw(),
              entity.getPriceAmount() != null ? entity.getPriceAmount() : 0L,
              entity.getPriceMillions() != null ? entity.getPriceMillions() : 0.0));
    }
    if (entity.getAreaSqm() != null
        || entity.getRooms() != null
        || entity.getPricePerSqm() != null) {
      item.setDetailsInfo(
          new PropertyDetails(
              entity.getDetails(),
              entity.getAreaSqm() != null ? entity.getAreaSqm() : 0.0,
              entity.getRooms() != null ? entity.getRooms() : 0.0,
              entity.getPricePerSqm() != null ? entity.getPricePerSqm() : 0L));
    }
    return item;
  }

  public List<IngatlanItem> toItems(Collection<PropertyEntity> entities) {
    if (entities == null || entities.isEmpty()) {
      return Collections.emptyList();
    }
    return entities.stream().map(this::toItem).collect(Collectors.toList());
  }

  public PropertyEntity toEntity(
      IngatlanItem item,
      PropertyEntity existing,
      String now,
      Long priceDelta,
      Double priceChangePercent,
      String lastPriceChangeAt,
      String targetStatus) {
    boolean isNew = (existing == null);
    PropertyEntity entity = isNew ? new PropertyEntity() : existing;
    String url = item.getUrl() != null ? item.getUrl().trim() : "";
    String portal = item.getPortal() != null ? item.getPortal().trim() : "Egyéb";
    String cityName =
        item.getCity() != null && !item.getCity().isBlank() ? item.getCity().trim() : "Ismeretlen";

    if (isNew) {
      entity.setUrl(url);
      entity.setPropertyId(generatePropertyId(portal, item.getId(), url));
      entity.setDiscoveredAt(item.getDiscoveredAt() != null ? item.getDiscoveredAt() : now);
    }

    boolean finalViewed = (!isNew && existing.isViewed()) || item.isViewed();
    String finalViewedAt =
        (!isNew && existing.isViewed())
            ? existing.getViewedAt()
            : (item.isViewed() ? (item.getViewedAt() != null ? item.getViewedAt() : now) : null);

    PriceInfo pi = item.getPriceInfo();
    PropertyDetails di = item.getDetailsInfo();

    entity.setPortal(portal);
    entity.setCity(cityName);
    entity.setNormalizedCity(CityUtils.normalizeCityKey(cityName));
    entity.setLocation(cityName);
    entity.setTitle(item.getTitle() != null ? item.getTitle() : "");
    entity.setPriceRaw(
        (pi != null && pi.getRawText() != null)
            ? pi.getRawText()
            : (item.getPrice() != null ? item.getPrice() : ""));
    entity.setPriceAmount(pi != null ? pi.getAmountHuf() : null);
    entity.setPriceMillions(pi != null ? pi.getAmountMillion() : null);
    entity.setPricePerSqm(di != null ? di.getPricePerSquareMeter() : null);
    entity.setDetails(item.getDetails() != null ? item.getDetails() : "");
    entity.setAreaSqm(di != null ? di.getAreaSquareMeters() : null);
    entity.setRooms(di != null ? di.getRoomCount() : null);
    entity.setImageUrl(item.getImageUrl());
    entity.setNew(item.isNew());
    entity.setViewed(finalViewed);
    entity.setViewedAt(finalViewedAt);
    entity.setStatus(targetStatus);
    entity.setPriceDelta(priceDelta);
    entity.setPriceChangePercent(priceChangePercent);
    entity.setLastPriceChangeAt(lastPriceChangeAt);
    entity.setUpdatedAt(now);
    return entity;
  }

  public String generatePropertyId(String portal, String rawId, String url) {
    String trimmed = rawId != null ? rawId.trim() : "";
    return !trimmed.isBlank()
        ? portal + "_" + trimmed
        : portal + "_" + Integer.toUnsignedString(url != null ? url.hashCode() : 0);
  }

  public void normalizeItem(IngatlanItem item) {
    if (item == null) {
      return;
    }
    if (item.getPriceInfo() == null && item.getPrice() != null && !item.getPrice().isBlank()) {
      item.setPriceInfo(PropertyParser.parsePrice(item.getPrice()));
    }
    if (item.getDetailsInfo() == null
        && item.getDetails() != null
        && !item.getDetails().isBlank()) {
      item.setDetailsInfo(PropertyParser.parseDetails(item.getDetails(), item.getPriceInfo()));
    }
  }

  public PriceHistoryEntity createPriceHistoryEntity(
      String propertyId,
      String url,
      String recordedAt,
      String priceRaw,
      Long priceAmount,
      Double priceMillions,
      Long pricePerSqm,
      Long prevAmount,
      Long delta,
      Double changePercent) {
    PriceHistoryEntity ph = new PriceHistoryEntity();
    ph.setPropertyId(propertyId);
    ph.setUrl(url);
    ph.setRecordedAt(recordedAt);
    ph.setPriceRaw(priceRaw);
    ph.setPriceAmount(priceAmount);
    ph.setPriceMillions(priceMillions);
    ph.setPricePerSqm(pricePerSqm);
    ph.setPreviousPriceAmount(prevAmount);
    ph.setPriceDelta(delta);
    ph.setChangePercent(changePercent);
    return ph;
  }

  public PriceHistoryEntry toPriceHistoryEntry(PriceHistoryEntity ph) {
    if (ph == null) {
      return null;
    }
    return new PriceHistoryEntry(
        ph.getId() != null ? ph.getId().intValue() : 0,
        ph.getPropertyId(),
        ph.getRecordedAt(),
        ph.getPriceRaw(),
        ph.getPriceAmount() != null ? ph.getPriceAmount() : 0L,
        ph.getPriceMillions() != null ? ph.getPriceMillions() : 0.0,
        ph.getPricePerSqm(),
        ph.getPreviousPriceAmount(),
        ph.getPriceDelta(),
        ph.getChangePercent());
  }

  public StatusLogEntity createStatusLogEntity(
      String propertyId, String url, String status, String changedAt, String note) {
    StatusLogEntity sl = new StatusLogEntity();
    sl.setPropertyId(propertyId);
    sl.setUrl(url);
    sl.setStatus(status);
    sl.setChangedAt(changedAt);
    sl.setNote(note);
    return sl;
  }

  public StatusLogEntry toStatusLogEntry(StatusLogEntity sl) {
    if (sl == null) {
      return null;
    }
    return new StatusLogEntry(
        sl.getId() != null ? sl.getId().intValue() : 0,
        sl.getPropertyId(),
        sl.getStatus(),
        sl.getChangedAt(),
        sl.getNote());
  }
}
