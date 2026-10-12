package ingatlanok.service;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Ingatlan scraper szolgáltatás interfész.
 */
public interface Ingatlan {

    Set<String> getUrls();

    default List<IngatlanItem> search(SearchCriteria criteria) {
        return Collections.emptyList();
    }

    default String getName() {
        return getClass().getSimpleName();
    }
}
