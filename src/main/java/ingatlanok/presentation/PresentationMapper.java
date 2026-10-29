package ingatlanok.presentation;

import java.util.ArrayList;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchResult;
import ingatlanok.model.SourceListing;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    imports = ArrayList.class)
public interface PresentationMapper {

  PresentationMapper INSTANCE = Mappers.getMapper(PresentationMapper.class);

  @Mapping(
      target = "status",
      expression = "java(pr.getStatus() != null ? pr.getStatus().name() : \"UNKNOWN\")")
  @Mapping(target = "foundCount", expression = "java(pr.getItems() != null ? pr.getItems().size() : 0)")
  @Mapping(target = "latencyMs", source = "executionTimeMs")
  PortalStatusDto toPortalStatusDto(PortalScrapeResult pr);

  @Mapping(target = "items", source = "searchResult.items")
  @Mapping(target = "newCount", source = "searchResult.newCount")
  @Mapping(target = "totalFound", source = "searchResult.totalFound")
  @Mapping(target = "executedAt", source = "searchResult.executedAt")
  @Mapping(target = "portalCounts", source = "searchResult.portalCounts")
  @Mapping(target = "cityCounts", source = "searchResult.cityCounts")
  @Mapping(target = "newCitiesInitialized", source = "searchResult.newCitiesInitialized")
  @Mapping(target = "marketStats", source = "marketStats")
  @Mapping(
      target = "portalStatuses",
      source = "portalStatuses",
      defaultExpression = "java(new ArrayList<>())")
  DashboardResponseDto toDashboardResponseDto(
      SearchResult searchResult, MarketStatsDto marketStats, List<PortalStatusDto> portalStatuses);

  @Mapping(target = "currentPriceHuf", source = "item.priceInfo.amountHuf")
  @Mapping(target = "currentPriceMillion", source = "item.priceInfo.amountMillion")
  @Mapping(target = "changePercent", source = "item.priceChangePercent")
  @Mapping(target = "pricePerM2", source = "pricePerM2")
  PriceDropItemDto toPriceDropItemDto(IngatlanItem item, Long pricePerM2);

  @Mapping(target = "portal", source = "portal")
  @Mapping(target = "externalListingId", source = "item.id")
  @Mapping(target = "firstSeenAt", source = "item.discoveredAt")
  @Mapping(target = "lastSeenAt", source = "item.discoveredAt")
  @Mapping(target = "status", source = "item.status", defaultValue = "ACTIVE")
  @Mapping(target = "priceHuf", source = "priceHuf")
  @Mapping(target = "pricePerM2", source = "pricePerM2")
  SourceListing toSourceListingFromItem(
      IngatlanItem item, String portal, Long priceHuf, Long pricePerM2);
}
