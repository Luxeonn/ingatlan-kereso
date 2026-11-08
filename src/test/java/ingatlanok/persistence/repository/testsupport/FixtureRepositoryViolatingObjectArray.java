package ingatlanok.persistence.repository.testsupport;

import java.util.List;

/**
 * Synthetic test fixture intentionally violating the repository typed record projection rule.
 * Excluded from production analysis via DoNotIncludeTests; used only by ArchitectureRulesNegativeTest.
 */
public interface FixtureRepositoryViolatingObjectArray {

  List<Object[]> rawAggregateQuery();
}
