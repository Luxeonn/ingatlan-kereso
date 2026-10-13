package ingatlanok.scraper;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ingatlan portál scraper interfész, állapotgazdag és izolált hibakezeléssel.
 */
public interface IngatlanScraper {

    /**
     * Portál belső azonosítója (pl. "dh", "oc", "jofogas", "ingatlancom", "ingatlanbazar").
     */
    String getPortalId();

    /**
     * Portál emberi olvasásra alkalmas neve (pl. "Duna House").
     */
    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Futtatja a web scraping keresést, és állapotgazdag PortalScrapeResult-tal tér vissza.
     */
    PortalScrapeResult scrape(SearchCriteria criteria);

    /**
     * Visszafelé kompatibilis keresési hívás, amely közvetlenül a talált hirdetések listáját adja vissza.
     */
    default List<IngatlanItem> search(SearchCriteria criteria) {
        PortalScrapeResult result = scrape(criteria);
        return result != null ? result.getItems() : Collections.emptyList();
    }

    default Set<String> getUrls() {
        return search(new SearchCriteria()).stream()
                .filter(IngatlanItem::isNew)
                .map(IngatlanItem::getUrl)
                .collect(Collectors.toSet());
    }
}
