package ingatlanok.architecture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.core.testsupport.FixtureCoreViolatingSpringWeb;
import ingatlanok.portal.testsupport.FixturePortalViolatingPersistence;

class ArchitectureRulesNegativeTest {

  @Test
  @DisplayName("Portal-persistence rétegsértés esetén az ArchUnit szabály AssertionError-t dob")
  void verifiesPortalPersistenceViolationThrowsAssertionError() {
    JavaClasses classes = new ClassFileImporter().importClasses(FixturePortalViolatingPersistence.class);
    AssertionError error =
        assertThrows(
            AssertionError.class,
            () -> ArchitectureRulesTest.PORTAL_DOES_NOT_DEPEND_ON_PERSISTENCE.check(classes));
    assertTrue(
        error.getMessage().contains("was violated"),
        "ArchUnit szabálysértési üzenetnek meg kell jelennie, de ez volt: " + error.getMessage());
  }

  @Test
  @DisplayName("Belső rétegbeli Spring Web függőség esetén az ArchUnit szabály AssertionError-t dob")
  void verifiesCoreSpringWebViolationThrowsAssertionError() {
    JavaClasses classes = new ClassFileImporter().importClasses(FixtureCoreViolatingSpringWeb.class);
    AssertionError error =
        assertThrows(
            AssertionError.class,
            () -> ArchitectureRulesTest.CORE_AND_MODEL_AND_PORTAL_DO_NOT_DEPEND_ON_SPRING_WEB.check(classes));
    assertTrue(
        error.getMessage().contains("was violated"),
        "ArchUnit szabálysértési üzenetnek meg kell jelennie, de ez volt: " + error.getMessage());
  }
}
