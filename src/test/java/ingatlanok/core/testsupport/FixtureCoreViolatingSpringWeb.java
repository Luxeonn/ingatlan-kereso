package ingatlanok.core.testsupport;

import org.springframework.web.bind.annotation.GetMapping;

/**
 * Synthetic test fixture intentionally violating the core-to-spring-web architectural boundary.
 * Excluded from production analysis via DoNotIncludeTests; used only by ArchitectureRulesNegativeTest.
 */
public class FixtureCoreViolatingSpringWeb {

  @GetMapping
  public String handleRequest() {
    return "violation";
  }
}
