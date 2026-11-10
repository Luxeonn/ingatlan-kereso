package ingatlanok.persistence.testsupport;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Synthetic test fixture intentionally violating the JPA entity closed enum type-safety rule.
 * Excluded from production analysis via DoNotIncludeTests; used only by ArchitectureRulesNegativeTest.
 */
@Entity
public class FixtureEntityViolatingStringEnum {

  @Id
  private Long id;

  private String status;

  public Long getId() {
    return id;
  }

  public String getStatus() {
    return status;
  }
}
