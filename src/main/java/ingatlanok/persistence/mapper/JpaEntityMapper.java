package ingatlanok.persistence.mapper;

import java.util.Collection;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueMappingStrategy;
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

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    imports = {CityUtils.class, PropertyType.class})
public abstract class JpaEntityMapper {

  @BeanMapping(builder = @Builder(disableBuilder = true))
  @Mapping(target = "id", source = "propertyId")
  @Mapping(target = "price", source = "priceRaw")
  @Mapping(
      target = "propertyType",
      expression =
          "java(entity.getPropertyType() != null"
              + " ? entity.getPropertyType() : PropertyType.EGYEB_ISMERETLEN)")
  @Mapping(target = "sourcesCount", ignore = true)
  @Mapping(target = "sourceListings", ignore = true)
  @Mapping(target = "isBargain", ignore = true)
  @Mapping(target = "bargain", ignore = true)
  @Mapping(target = "referenceMedianPricePerSquareMeter", ignore = true)
  @Mapping(target = "percentageBelowMedian", ignore = true)
  @Mapping(target = "referenceSampleSize", ignore = true)
  @Mapping(target = "qualityScore", ignore = true)
  @Mapping(target = "qualityDeductions", ignore = true)
  @Mapping(target = "priceInfo", ignore = true)
  @Mapping(target = "detailsInfo", ignore = true)
  @Mapping(target = "source", ignore = true)
  public abstract IngatlanItem toItem(PropertyEntity entity);

  @AfterMapping
  protected void enrichItem(PropertyEntity entity, @MappingTarget IngatlanItem item) {
    item.setLocation(entity.getLocation());
    item.setCity(entity.getCity());
    if (entity.getPriceAmount() != null || entity.getPriceMillions() != null) {
      item.setPriceInfo(
          PriceInfo.builder()
              .rawText(entity.getPriceRaw())
              .amountHuf(entity.getPriceAmount() != null ? entity.getPriceAmount() : 0L)
              .amountMillion(entity.getPriceMillions() != null ? entity.getPriceMillions() : 0.0)
              .build());
    }
    if (entity.getAreaSqm() != null
        || entity.getRooms() != null
        || entity.getPricePerSqm() != null) {
      item.setDetailsInfo(
          PropertyDetails.builder()
              .rawText(entity.getDetails())
              .areaSquareMeters(entity.getAreaSqm() != null ? entity.getAreaSqm() : 0.0)
              .roomCount(entity.getRooms() != null ? entity.getRooms() : 0.0)
              .pricePerSquareMeter(entity.getPricePerSqm() != null ? entity.getPricePerSqm() : 0L)
              .build());
    }
    SourceListing source = toSourceListing(entity);
    if (source != null) {
      item.setSourceListings(List.of(source));
      item.setSourcesCount(1);
    }
  }

  @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
  public abstract List<IngatlanItem> toItems(Collection<PropertyEntity> entities);

  @Mapping(target = "internalId", ignore = true)
  @Mapping(target = "propertyId", ignore = true)
  @Mapping(target = "url", ignore = true)
  @Mapping(target = "discoveredAt", ignore = true)
  @Mapping(target = "canonicalId", ignore = true)
  @Mapping(
      target = "portal",
      expression = "java(item.getPortal() != null ? item.getPortal().trim() : \"Egyéb\")")
  @Mapping(
      target = "city",
      expression =
          "java(item.getCity() != null && !item.getCity().isBlank()"
              + " ? item.getCity().trim() : \"Ismeretlen\")")
  @Mapping(
      target = "normalizedCity",
      expression =
          "java(CityUtils.normalizeCityKey(item.getCity() != null && !item.getCity().isBlank()"
              + " ? item.getCity().trim() : \"Ismeretlen\"))")
  @Mapping(
      target = "location",
      expression =
          "java(item.getCity() != null && !item.getCity().isBlank()"
              + " ? item.getCity().trim() : \"Ismeretlen\")")
  @Mapping(target = "title", source = "item.title", defaultValue = "")
  @Mapping(
      target = "propertyType",
      source = "item.propertyType",
      defaultExpression = "java(PropertyType.EGYEB_ISMERETLEN)")
  @Mapping(
      target = "priceRaw",
      expression =
          "java((item.getPriceInfo() != null && item.getPriceInfo().getRawText() != null)"
              + " ? item.getPriceInfo().getRawText()"
              + " : (item.getPrice() != null ? item.getPrice() : \"\"))")
  @Mapping(target = "priceAmount", source = "item.priceInfo.amountHuf")
  @Mapping(target = "priceMillions", source = "item.priceInfo.amountMillion")
  @Mapping(target = "pricePerSqm", source = "item.detailsInfo.pricePerSquareMeter")
  @Mapping(target = "details", source = "item.details", defaultValue = "")
  @Mapping(target = "areaSqm", source = "item.detailsInfo.areaSquareMeters")
  @Mapping(target = "rooms", source = "item.detailsInfo.roomCount")
  @Mapping(target = "imageUrl", source = "item.imageUrl")
  @Mapping(target = "newConstruction", ignore = true)
  @Mapping(target = "new", source = "item.new")
  @Mapping(target = "viewed", ignore = true)
  @Mapping(target = "viewedAt", ignore = true)
  @Mapping(target = "status", source = "targetStatus")
  @Mapping(target = "priceDelta", source = "priceDelta")
  @Mapping(target = "priceChangePercent", source = "priceChangePercent")
  @Mapping(target = "lastPriceChangeAt", source = "lastPriceChangeAt")
  @Mapping(target = "firstSeenAt", ignore = true)
  @Mapping(target = "lastSeenAt", source = "now")
  @Mapping(target = "updatedAt", source = "now")
  protected abstract void updateEntityFromItem(
      IngatlanItem item,
      @MappingTarget PropertyEntity entity,
      String now,
      Long priceDelta,
      Double priceChangePercent,
      String lastPriceChangeAt,
      String targetStatus);

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
    boolean prevViewed = !isNew && existing.isViewed();
    String prevViewedAt = !isNew ? existing.getViewedAt() : null;
    boolean prevNewConstruction = !isNew && existing.isNewConstruction();
    String prevFirstSeenAt = !isNew ? existing.getFirstSeenAt() : null;
    String prevCanonicalId = !isNew ? existing.getCanonicalId() : null;

    updateEntityFromItem(
        item, entity, now, priceDelta, priceChangePercent, lastPriceChangeAt, targetStatus);

    if (isNew) {
      String url = item.getUrl() != null ? item.getUrl().trim() : "";
      entity.setUrl(url);
      entity.setPropertyId(generatePropertyId(entity.getPortal(), item.getId(), url));
      entity.setDiscoveredAt(item.getDiscoveredAt() != null ? item.getDiscoveredAt() : now);
    }
    entity.setNewConstruction(item.isNewConstruction() || prevNewConstruction);
    entity.setViewed(prevViewed || item.isViewed());
    entity.setViewedAt(
        prevViewed
            ? prevViewedAt
            : (item.isViewed() ? (item.getViewedAt() != null ? item.getViewedAt() : now) : null));
    entity.setFirstSeenAt(
        prevFirstSeenAt != null
            ? prevFirstSeenAt
            : (item.getDiscoveredAt() != null ? item.getDiscoveredAt() : now));
    if (prevCanonicalId != null) {
      entity.setCanonicalId(prevCanonicalId);
    }
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
  @Mapping(
      target = "canonicalId",
      source = "entity.canonicalId",
      defaultExpression = "java(entity.getPropertyId())")
  @Mapping(
      target = "propertyType",
      source = "entity.propertyType",
      defaultExpression = "java(PropertyType.EGYEB_ISMERETLEN)")
  @Mapping(target = "floorAreaSquareMeters", source = "entity.areaSqm")
  @Mapping(target = "roomCount", source = "entity.rooms")
  @Mapping(target = "minActivePriceHuf", source = "entity.priceAmount")
  @Mapping(target = "sources", ignore = true)
  public abstract Property toProperty(PropertyEntity entity, List<SourceListing> sources);

  @AfterMapping
  protected void populatePropertySources(
      PropertyEntity entity, List<SourceListing> sources, @MappingTarget Property prop) {
    if (sources != null && !sources.isEmpty()) {
      prop.setSources(sources);
      prop.setMinActivePriceHuf(prop.calculateMinActivePriceHuf());
    } else {
      SourceListing singleSource = toSourceListing(entity);
      if (singleSource != null) {
        prop.setSources(List.of(singleSource));
      }
    }
  }
}
