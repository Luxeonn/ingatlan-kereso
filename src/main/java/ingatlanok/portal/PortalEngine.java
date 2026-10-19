package ingatlanok.portal;

import ingatlanok.model.SearchCriteria;
import ingatlanok.scraper.PortalScrapeResult;

/** Portál motor egységes interfész a moduláris Clean Architecture-ben. */
public interface PortalEngine {

  /** Visszaadja a portál metaadatait (leíróját). */
  PortalDescriptor getDescriptor();

  /** Végrehajtja a hirdetésgyűjtést a megadott keresési kritériumok alapján. */
  PortalScrapeResult scrape(SearchCriteria criteria);

  default String getId() {
    return getDescriptor().id();
  }

  default String getName() {
    return getDescriptor().name();
  }
}
