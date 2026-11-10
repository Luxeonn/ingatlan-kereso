package ingatlanok.web.testsupport;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Synthetic test fixture intentionally violating the controller typed-DTO return rule.
 * Excluded from production analysis via DoNotIncludeTests; used only by ArchitectureRulesNegativeTest.
 */
@RestController
public class FixtureControllerViolatingUntypedMap {

  @GetMapping("/bad-map")
  public ResponseEntity<Map<String, Object>> badMapEndpoint() {
    return null;
  }
}
