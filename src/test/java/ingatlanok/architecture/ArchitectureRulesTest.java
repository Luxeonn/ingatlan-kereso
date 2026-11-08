package ingatlanok.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Bytecode-level automated architecture integrity rules enforcing 4-layer unidirectional flow,
 * persistence layer abstraction, read-only DTO immutability, MapStruct error reporting, single-point SSRF
 * containment, and Java 25 virtual thread non-pinning.
 */
@AnalyzeClasses(packages = "ingatlanok", importOptions = {ImportOption.DoNotIncludeTests.class})
public final class ArchitectureRulesTest {

  static final String READ_ONLY_DTO_PATTERN =
      ".*\\.(MarketStatsDto|MarketSegmentStatsDto|PriceDropItemDto"
          + "|MarketStats|MarketSegmentStats|PriceDropItem"
          + "|PortalStatusDto|DashboardResponseDto|PortalSource"
          + "|PriceInfo|PropertyDetails)";

  @ArchTest
  static final ArchRule MODEL_AND_COMMON_INDEPENDENT =
      noClasses()
          .that()
          .resideInAnyPackage("..ingatlanok.model..", "..ingatlanok.common..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..ingatlanok.core..",
              "..ingatlanok.persistence..",
              "..ingatlanok.portal..",
              "..ingatlanok.presentation..",
              "..ingatlanok.web..",
              "..ingatlanok.config..",
              "..ingatlanok.service..")
          .because("Foundational domain model and common utilities must be independent of higher layers");

  @ArchTest
  static final ArchRule CORE_AND_PERSISTENCE_AND_PORTAL_DO_NOT_DEPEND_ON_WEB_OR_CONFIG =
      noClasses()
          .that()
          .resideInAnyPackage("..ingatlanok.core..", "..ingatlanok.persistence..", "..ingatlanok.portal..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..ingatlanok.web..", "..ingatlanok.config..")
          .because("Lower layers must never depend on web or configuration layers");

  @ArchTest
  static final ArchRule PERSISTENCE_AND_PORTAL_DO_NOT_DEPEND_ON_PRESENTATION =
      noClasses()
          .that()
          .resideInAnyPackage("..ingatlanok.persistence..", "..ingatlanok.portal..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..ingatlanok.presentation..")
          .because("Persistence and scraper portal engines must never depend on the presentation layer");

  @ArchTest
  static final ArchRule CORE_DOES_NOT_DEPEND_ON_PRESENTATION =
      noClasses()
          .that()
          .resideInAnyPackage("..ingatlanok.core..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..ingatlanok.presentation..")
          .because("Core business logic engines must never depend on the presentation layer");

  @ArchTest
  static final ArchRule STORAGE_ABSTRACTION_INTEGRITY =
      noClasses()
          .that()
          .resideOutsideOfPackage("..ingatlanok.persistence..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..ingatlanok.persistence.repository..",
              "..ingatlanok.persistence.entity..",
              "jakarta.persistence..")
          .because("Storage details must be accessed exclusively through ListingStorage and ConfigStorage");

  @ArchTest
  static final ArchRule PERSISTENCE_INTERNALS_ENCAPSULATION =
      noClasses()
          .that()
          .resideOutsideOfPackage("..ingatlanok.persistence..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..ingatlanok.persistence.service..",
              "..ingatlanok.persistence.mapper..")
          .because("Internal persistence services and mappers must not leak outside persistence package");

  @ArchTest
  static final ArchRule JPA_STORAGE_IMPLEMENTATION_ENCAPSULATION =
      noClasses()
          .that()
          .resideOutsideOfPackage("..ingatlanok.persistence..")
          .should()
          .dependOnClassesThat()
          .haveFullyQualifiedName("ingatlanok.persistence.JpaIngatlanStorage")
          .because("JpaIngatlanStorage must be consumed strictly through ListingStorage and ConfigStorage");

  @ArchTest
  static final ArchRule PORTAL_DOES_NOT_DEPEND_ON_PERSISTENCE =
      noClasses()
          .that()
          .resideInAnyPackage("..ingatlanok.portal..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..ingatlanok.persistence..")
          .because("Portal scraper engines must never depend on the persistence layer");

  @ArchTest
  static final ArchRule PORTAL_DOES_NOT_DEPEND_ON_CORE =
      noClasses()
          .that()
          .resideInAnyPackage("..ingatlanok.portal..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..ingatlanok.core..")
          .because("Portal scraper engines must not depend on core business logic or normalizers");

  @ArchTest
  static final ArchRule CORE_AND_MODEL_AND_PORTAL_DO_NOT_DEPEND_ON_SPRING_WEB =
      noClasses()
          .that()
          .resideInAnyPackage(
              "..ingatlanok.core..",
              "..ingatlanok.model..",
              "..ingatlanok.common..",
              "..ingatlanok.persistence..",
              "..ingatlanok.portal..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("org.springframework.web..", "jakarta.servlet..")
          .because("Internal layers must remain independent of Spring Web and Servlet container APIs");

  @ArchTest
  static final ArchRule CONTROLLER_LOCATION_INTEGRITY =
      classes()
          .that()
          .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
          .or()
          .areAnnotatedWith("org.springframework.stereotype.Controller")
          .should()
          .resideInAPackage("..ingatlanok.web..")
          .because("REST and MVC controllers must reside exclusively in the web presentation layer");

  @ArchTest
  static final ArchRule CONTROLLERS_DO_NOT_DEPEND_ON_CORE_ENGINES =
      noClasses()
          .that()
          .resideInAnyPackage("..ingatlanok.web..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..ingatlanok.core..")
          .because("Web controllers must orchestrate via presentation facade and never directly access core engines");

  @ArchTest
  static final ArchRule READ_ONLY_DTOS_MUST_NOT_HAVE_SETTERS =
      noMethods()
          .that()
          .areDeclaredInClassesThat()
          .haveNameMatching(READ_ONLY_DTO_PATTERN)
          .should()
          .haveNameStartingWith("set")
          .because("Read-only DTOs and value objects must be immutable without setters");

  @ArchTest
  static final ArchRule MAPSTRUCT_MAPPERS_MUST_ENFORCE_ERROR_REPORTING =
      classes()
          .that()
          .areAnnotatedWith(Mapper.class)
          .should(
              new ArchCondition<JavaClass>("enforce unmappedTargetPolicy = ReportingPolicy.ERROR") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                  Mapper annotation = javaClass.getAnnotationOfType(Mapper.class);
                  if (annotation.unmappedTargetPolicy() != ReportingPolicy.ERROR) {
                    String message =
                        String.format(
                            "Mapper %s must declare unmappedTargetPolicy = ReportingPolicy.ERROR",
                            javaClass.getName());
                    events.add(SimpleConditionEvent.violated(javaClass, message));
                  }
                }
              })
          .because("MapStruct mappers must catch unmapped fields at compile time");

  @ArchTest
  static final ArchRule SINGLE_POINT_SSRF_PROTECTION =
      noClasses()
          .that()
          .doNotHaveFullyQualifiedName("ingatlanok.common.HttpFetcher")
          .should()
          .callMethod("org.jsoup.Jsoup", "connect", String.class.getName())
          .because("External HTTP calls via Jsoup must be routed exclusively through HttpFetcher");

  @ArchTest
  static final ArchRule NO_SYNCHRONIZED_METHODS_IN_VIRTUAL_THREAD_PATHS =
      noMethods()
          .that()
          .areDeclaredInClassesThat()
          .resideInAnyPackage(
              "..ingatlanok.common..",
              "..ingatlanok.portal..",
              "..ingatlanok.core..",
              "..ingatlanok.persistence..",
              "..ingatlanok.presentation..",
              "..ingatlanok.config..",
              "..ingatlanok.service..",
              "..ingatlanok.web..")
          .should()
          .haveModifier(JavaModifier.SYNCHRONIZED)
          .because("Synchronized methods cause carrier thread pinning on Java 25 virtual threads");

  private ArchitectureRulesTest() {}
}
