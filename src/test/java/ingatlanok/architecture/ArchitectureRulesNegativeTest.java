package ingatlanok.architecture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.core.testsupport.FixtureCoreViolatingReflection;
import ingatlanok.core.testsupport.FixtureCoreViolatingSpringWeb;
import ingatlanok.core.testsupport.FixtureCoreViolatingSynchronized;
import ingatlanok.persistence.repository.testsupport.FixtureRepositoryViolatingObjectArray;
import ingatlanok.persistence.testsupport.FixtureEntityViolatingStringEnum;
import ingatlanok.portal.testsupport.FixturePortalViolatingPersistence;
import ingatlanok.web.testsupport.FixtureControllerViolatingUntypedMap;

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

  @Test
  @DisplayName("Virtuális szál útvonalon lévő synchronized metódus esetén az ArchUnit szabály AssertionError-t dob")
  void verifiesSynchronizedMethodViolationThrowsAssertionError() {
    JavaClasses classes = new ClassFileImporter().importClasses(FixtureCoreViolatingSynchronized.class);
    AssertionError error =
        assertThrows(
            AssertionError.class,
            () -> ArchitectureRulesTest.NO_SYNCHRONIZED_METHODS_IN_VIRTUAL_THREAD_PATHS.check(classes));
    assertTrue(
        error.getMessage().contains("was violated"),
        "ArchUnit szabálysértési üzenetnek meg kell jelennie, de ez volt: " + error.getMessage());
  }

  @Test
  @DisplayName("Vezérlőbeli Map<String, Object> vagy wildcard visszatérés esetén az ArchUnit szabály hibát dob")
  void verifiesControllerUntypedMapViolationThrowsAssertionError() {
    JavaClasses classes = new ClassFileImporter().importClasses(FixtureControllerViolatingUntypedMap.class);
    AssertionError error =
        assertThrows(
            AssertionError.class,
            () -> ArchitectureRulesTest.CONTROLLERS_DO_NOT_RETURN_OBJECT_MAPS_OR_WILDCARDS.check(classes));
    assertTrue(
        error.getMessage().contains("was violated"),
        "ArchUnit szabálysértési üzenetnek meg kell jelennie, de ez volt: " + error.getMessage());
  }

  @Test
  @DisplayName("Repository Object[] vagy List<Object[]> projekció esetén az ArchUnit szabály hibát dob")
  void verifiesRepositoryObjectArrayViolationThrowsAssertionError() {
    JavaClasses classes = new ClassFileImporter().importClasses(FixtureRepositoryViolatingObjectArray.class);
    AssertionError error =
        assertThrows(
            AssertionError.class,
            () -> ArchitectureRulesTest.REPOSITORIES_DO_NOT_RETURN_OBJECT_ARRAYS.check(classes));
    assertTrue(
        error.getMessage().contains("was violated"),
        "ArchUnit szabálysértési üzenetnek meg kell jelennie, de ez volt: " + error.getMessage());
  }

  @Test
  @DisplayName("Reflektív Class.forName hívás esetén az ArchUnit szabály hibát dob")
  void verifiesReflectiveClassLoadingViolationThrowsAssertionError() {
    JavaClasses classes = new ClassFileImporter().importClasses(FixtureCoreViolatingReflection.class);
    AssertionError error =
        assertThrows(
            AssertionError.class,
            () -> ArchitectureRulesTest.NO_REFLECTIVE_CLASS_LOADING_IN_PRODUCTION.check(classes));
    assertTrue(
        error.getMessage().contains("was violated"),
        "ArchUnit szabálysértési üzenetnek meg kell jelennie, de ez volt: " + error.getMessage());
  }

  @Test
  @DisplayName("JPA entitás zárt státusz mezőjének Stringként tárolása esetén az ArchUnit szabály hibát dob")
  void verifiesEntityStringClosedSetViolationThrowsAssertionError() {
    JavaClasses classes = new ClassFileImporter().importClasses(FixtureEntityViolatingStringEnum.class);
    AssertionError error =
        assertThrows(
            AssertionError.class,
            () -> ArchitectureRulesTest.DOMAIN_AND_ENTITY_CLOSED_SETS_USE_ENUMS.check(classes));
    assertTrue(
        error.getMessage().contains("was violated"),
        "ArchUnit szabálysértési üzenetnek meg kell jelennie, de ez volt: " + error.getMessage());
  }
}
