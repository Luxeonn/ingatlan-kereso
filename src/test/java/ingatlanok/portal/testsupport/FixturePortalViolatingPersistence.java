package ingatlanok.portal.testsupport;

import ingatlanok.persistence.ListingStorage;

/**
 * Synthetic test fixture intentionally violating the portal-to-persistence architectural boundary.
 * Excluded from production analysis via DoNotIncludeTests; used only by ArchitectureRulesNegativeTest.
 */
public class FixturePortalViolatingPersistence {

  private ListingStorage listingStorage;

  public ListingStorage getListingStorage() {
    return listingStorage;
  }
}
