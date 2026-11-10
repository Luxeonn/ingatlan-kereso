package ingatlanok.core.testsupport;

/**
 * Synthetic test fixture intentionally violating the ban on reflective class loading.
 * Excluded from production analysis via DoNotIncludeTests; used only by ArchitectureRulesNegativeTest.
 */
public class FixtureCoreViolatingReflection {

  public void invokeReflectiveLoad() throws ClassNotFoundException {
    Class.forName("java.lang.String");
  }
}
