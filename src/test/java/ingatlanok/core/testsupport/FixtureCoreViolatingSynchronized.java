package ingatlanok.core.testsupport;

/**
 * Synthetic test fixture intentionally violating the virtual thread non-pinning architectural rule.
 * Excluded from production analysis via DoNotIncludeTests; used only by ArchitectureRulesNegativeTest.
 */
public class FixtureCoreViolatingSynchronized {

  public synchronized void pinnedMethod() {
    // intentional synchronized method to verify ArchUnit fail-closed rule
  }
}
