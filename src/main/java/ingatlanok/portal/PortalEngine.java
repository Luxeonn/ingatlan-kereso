package ingatlanok.portal;

import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.SearchCriteria;

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
