package ingatlanok.portal;

import ingatlanok.model.SearchCriteria;
import ingatlanok.model.PortalScrapeResult;

public interface PortalEngine {

  PortalDescriptor getDescriptor();

  PortalScrapeResult scrape(SearchCriteria criteria);

  default String getId() {
    return getDescriptor().id();
  }

  default String getName() {
    return getDescriptor().name();
  }
}
