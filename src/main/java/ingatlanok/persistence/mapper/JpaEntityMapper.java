package ingatlanok.persistence.mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import ingatlanok.common.CityUtils;
import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.ListingObservation;
import ingatlanok.model.PriceHistoryEntry;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.Property;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SourceListing;
import ingatlanok.model.StatusLogEntry;
import ingatlanok.persistence.entity.ListingObservationEntity;
import ingatlanok.persistence.entity.PriceHistoryEntity;
import ingatlanok.persistence.entity.PropertyEntity;
import ingatlanok.persistence.entity.StatusLogEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class JpaEntityMapper {

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
    item.setNewConstruction(entity.isNewConstruction());
    item.setStatus(entity.getStatus());
    item.setPriceDelta(entity.getPriceDelta());
    item.setPriceChangePercent(entity.getPriceChangePercent());
    item.setLastPriceChangeAt(entity.getLastPriceChangeAt());
    item.setPropertyType(
        entity.getPropertyType() != null ? entity.getPropertyType() : PropertyType.EGYEB_ISMERETLEN);

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
    SourceListing source = toSourceListing(entity);
    if (source != null) {
      item.setSourceListings(List.of(source));
      item.setSourcesCount(1);
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
    entity.setPropertyType(
        item.getPropertyType() != null ? item.getPropertyType() : PropertyType.EGYEB_ISMERETLEN);
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
    entity.setNewConstruction(
        item.isNewConstruction() || (!isNew && existing.isNewConstruction()));
    entity.setNew(item.isNew());
    entity.setViewed(finalViewed);
    entity.setViewedAt(finalViewedAt);
    entity.setStatus(targetStatus);
    entity.setPriceDelta(priceDelta);
    entity.setPriceChangePercent(priceChangePercent);
    entity.setLastPriceChangeAt(lastPriceChangeAt);
    entity.setFirstSeenAt(
        !isNew && existing.getFirstSeenAt() != null
            ? existing.getFirstSeenAt()
            : (item.getDiscoveredAt() != null ? item.getDiscoveredAt() : now));
    entity.setLastSeenAt(now);
    if (!isNew && existing.getCanonicalId() != null) {
      entity.setCanonicalId(existing.getCanonicalId());
    }
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
    if (item.getPropertyType() == null) {
      item.setPropertyType(PropertyType.EGYEB_ISMERETLEN);
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

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "previousPriceAmount", source = "prevAmount")
  @Mapping(target = "priceDelta", source = "delta")
  public abstract PriceHistoryEntity createPriceHistoryEntity(
      String propertyId,
      String url,
      String recordedAt,
      String priceRaw,
      Long priceAmount,
      Double priceMillions,
      Long pricePerSqm,
      Long prevAmount,
      Long delta,
      Double changePercent);

  public abstract PriceHistoryEntry toPriceHistoryEntry(PriceHistoryEntity ph);

  @Mapping(target = "id", ignore = true)
  public abstract StatusLogEntity createStatusLogEntity(
      String propertyId, String url, String status, String changedAt, String note);

  public abstract StatusLogEntry toStatusLogEntry(StatusLogEntity sl);

  @Mapping(target = "externalListingId", source = "propertyId")
  @Mapping(
      target = "firstSeenAt",
      source = "firstSeenAt",
      defaultExpression = "java(entity.getDiscoveredAt())")
  @Mapping(
      target = "lastSeenAt",
      source = "lastSeenAt",
      defaultExpression = "java(entity.getUpdatedAt())")
  @Mapping(target = "priceHuf", source = "priceAmount")
  @Mapping(target = "pricePerM2", source = "pricePerSqm")
  public abstract SourceListing toSourceListing(PropertyEntity entity);

  @Mapping(
      target = "statusEvent",
      expression = "java(ListingObservation.StatusEvent.fromString(entity.getStatusEvent()))")
  public abstract ListingObservation toListingObservation(ListingObservationEntity entity);

  @Mapping(target = "id", ignore = true)
  public abstract ListingObservationEntity createListingObservationEntity(
      String propertyId,
      String url,
      String canonicalId,
      String observedAt,
      Long priceHuf,
      Long pricePerM2,
      String statusEvent,
      Long priceDelta,
      Double changePercent);
  public Property toProperty(PropertyEntity entity, List<SourceListing> sources) {
    if (entity == null) {
      return null;
    }
    Property prop = new Property();
    prop.setCanonicalId(
        entity.getCanonicalId() != null ? entity.getCanonicalId() : entity.getPropertyId());
    prop.setNormalizedCity(entity.getNormalizedCity());
    prop.setCity(entity.getCity());
    prop.setPropertyType(
        entity.getPropertyType() != null ? entity.getPropertyType() : PropertyType.EGYEB_ISMERETLEN);
    prop.setFloorAreaSquareMeters(entity.getAreaSqm());
    prop.setRoomCount(entity.getRooms());
    prop.setLocation(entity.getLocation());
    prop.setMinActivePriceHuf(entity.getPriceAmount());
    if (sources != null && !sources.isEmpty()) {
      prop.setSources(sources);
      prop.setMinActivePriceHuf(prop.calculateMinActivePriceHuf());
    } else {
      SourceListing singleSource = toSourceListing(entity);
      if (singleSource != null) {
        prop.setSources(List.of(singleSource));
      }
    }
    return prop;
  }
}
