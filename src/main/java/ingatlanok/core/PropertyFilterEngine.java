package ingatlanok.core;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;

import java.util.ArrayList;
import java.util.List;

/**
 * Ingatlanhirdetések keresési kritériumok szerinti szűréséért felelős motor.
 */
public class PropertyFilterEngine {

    public record FilterResult(
            List<IngatlanItem> matchingItems,
            int totalInputCount,
            int filteredByPriceCount,
            int filteredByAreaCount,
            int filteredByViewedCount,
            int filteredByCityCount
    ) {
    }

    public FilterResult filter(List<IngatlanItem> items, SearchCriteria criteria, String targetCity) {
        List<IngatlanItem> matching = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return new FilterResult(matching, 0, 0, 0, 0, 0);
        }

        int filteredPrice = 0;
        int filteredArea = 0;
        int filteredViewed = 0;
        int filteredCity = 0;

        for (IngatlanItem item : items) {
            if (item == null) continue;

            if (item.getCity() == null || item.getCity().isBlank()) {
                item.setCity(targetCity);
            }
            if (item.getLocation() == null || item.getLocation().isBlank()) {
                item.setLocation(targetCity);
            }

            if (criteria != null && criteria.isExcludeViewed() && item.isViewed()) {
                filteredViewed++;
                continue;
            }

            PriceInfo priceInfo = item.getPriceInfo();
            if (priceInfo == null && item.getPrice() != null && !item.getPrice().isBlank()) {
                priceInfo = PropertyParser.parsePrice(item.getPrice());
                item.setPriceInfo(priceInfo);
            }

            if (criteria != null && !PropertyParser.isPriceInRange(priceInfo, criteria.getMinPriceMillion(), criteria.getMaxPriceMillion())) {
                filteredPrice++;
                continue;
            }

            Double area = (item.getDetailsInfo() != null) ? item.getDetailsInfo().getAreaSquareMeters() : null;
            if (area == null && item.getDetails() != null && !item.getDetails().isBlank()) {
                PropertyDetails parsed = PropertyParser.parseDetails(item.getDetails(), priceInfo);
                item.setDetailsInfo(parsed);
                area = parsed.getAreaSquareMeters();
            }
            if (criteria != null && !PropertyParser.isAreaInRange(area, criteria.getMinArea(), criteria.getMaxArea())) {
                filteredArea++;
                continue;
            }

            if (targetCity != null && !SearchCriteria.matchesCity(item, targetCity)) {
                filteredCity++;
                continue;
            }

            matching.add(item);
        }

        return new FilterResult(matching, items.size(), filteredPrice, filteredArea, filteredViewed, filteredCity);
    }
}
