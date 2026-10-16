package ingatlanok.persistence.service;

import ingatlanok.common.CityUtils;
import ingatlanok.dto.ListingDto;
import ingatlanok.dto.ListingFilterDto;
import ingatlanok.model.IngatlanItem;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Állapotmentes szűrési és rendezési szolgáltatás ingatlanhirdetések memóriabeli listájához.
 */
@Component
public class ListingFilterService {

    public List<ListingDto> filter(Collection<IngatlanItem> items, ListingFilterDto filter) {
        if (items == null || items.isEmpty()) return Collections.emptyList();
        return applyFilterStream(items, filter).map(ListingDto::fromItem).collect(Collectors.toList());
    }

    public List<IngatlanItem> filterItems(Collection<IngatlanItem> items, ListingFilterDto filter) {
        if (items == null || items.isEmpty()) return Collections.emptyList();
        return applyFilterStream(items, filter).collect(Collectors.toList());
    }

    private Stream<IngatlanItem> applyFilterStream(Collection<IngatlanItem> items, ListingFilterDto filter) {
        Stream<IngatlanItem> stream = items.stream();
        if (filter == null) return stream;

        if (filter.cities() != null && !filter.cities().isEmpty()) {
            Set<String> normCities = filter.cities().stream().map(CityUtils::normalizeCityKey).collect(Collectors.toSet());
            stream = stream.filter(item -> normCities.contains(CityUtils.normalizeCityKey(item.getCity())));
        }
        if (filter.portals() != null && !filter.portals().isEmpty()) {
            Set<String> normPortals = filter.portals().stream().map(String::toLowerCase).collect(Collectors.toSet());
            stream = stream.filter(item -> normPortals.contains(item.getPortal() != null ? item.getPortal().toLowerCase() : ""));
        }
        if (filter.minPriceMillion() != null) {
            stream = stream.filter(i -> i.getPriceInfo() != null && i.getPriceInfo().getAmountMillion() >= filter.minPriceMillion());
        }
        if (filter.maxPriceMillion() != null) {
            stream = stream.filter(i -> i.getPriceInfo() != null && i.getPriceInfo().getAmountMillion() <= filter.maxPriceMillion());
        }
        if (filter.minArea() != null) {
            stream = stream.filter(i -> i.getDetailsInfo() != null && i.getDetailsInfo().getAreaSquareMeters() >= filter.minArea());
        }
        if (filter.maxArea() != null) {
            stream = stream.filter(i -> i.getDetailsInfo() != null && i.getDetailsInfo().getAreaSquareMeters() <= filter.maxArea());
        }
        if (filter.minRooms() != null) {
            stream = stream.filter(i -> i.getDetailsInfo() != null && i.getDetailsInfo().getRoomCount() >= filter.minRooms());
        }
        if (filter.maxRooms() != null) {
            stream = stream.filter(i -> i.getDetailsInfo() != null && i.getDetailsInfo().getRoomCount() <= filter.maxRooms());
        }
        if (filter.excludeViewed()) {
            stream = stream.filter(item -> !item.isViewed());
        }
        if (filter.onlyDiscounted()) {
            stream = stream.filter(item -> item.getPriceDelta() != null && item.getPriceDelta() < 0);
        }

        stream = stream.sorted(buildComparator(filter.sortBy()));
        if (filter.offset() != null && filter.offset() > 0) stream = stream.skip(filter.offset());
        if (filter.limit() != null && filter.limit() > 0) stream = stream.limit(filter.limit());
        return stream;
    }

    public Comparator<IngatlanItem> buildComparator(String sortBy) {
        String key = sortBy != null ? sortBy.toUpperCase() : "DISCOVERED_DESC";
        return switch (key) {
            case "PRICE_ASC" -> Comparator.comparingDouble(i -> (i.getPriceInfo() != null ? i.getPriceInfo().getAmountMillion() : Double.MAX_VALUE));
            case "PRICE_DESC" -> Comparator.comparingDouble((IngatlanItem i) -> (i.getPriceInfo() != null ? i.getPriceInfo().getAmountMillion() : 0.0)).reversed();
            case "AREA_ASC" -> Comparator.comparingDouble(i -> (i.getDetailsInfo() != null ? i.getDetailsInfo().getAreaSquareMeters() : Double.MAX_VALUE));
            case "AREA_DESC" -> Comparator.comparingDouble((IngatlanItem i) -> (i.getDetailsInfo() != null ? i.getDetailsInfo().getAreaSquareMeters() : 0.0)).reversed();
            case "DISCOUNT_DESC" -> Comparator.comparingLong((IngatlanItem i) -> (i.getPriceDelta() != null ? i.getPriceDelta() : 0L));
            default -> Comparator.comparing(IngatlanItem::getDiscoveredAt, Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }
}
