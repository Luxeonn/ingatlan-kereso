package ingatlanok.scraper;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;

public interface IngatlanScraper {

  String getPortalId();

  default String getName() {
    return getClass().getSimpleName();
  }

  PortalScrapeResult scrape(SearchCriteria criteria);

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
