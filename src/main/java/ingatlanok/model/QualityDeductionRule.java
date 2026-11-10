package ingatlanok.model;

import java.io.Serializable;

/**
 * Closed set of deterministic data quality deduction rules (#401 / JAVA-010).
 */
public enum QualityDeductionRule implements Serializable {
  INVALID_PRICE("Érvénytelen vagy irreális ár"),
  MISSING_AREA("Hiányzó vagy érvénytelen alapterület"),
  UNREALISTIC_AREA("Irreális alapterület"),
  MISSING_CITY("Hiányzó vagy érvénytelen település"),
  UNKNOWN_TYPE("Ismeretlen vagy hiányzó ingatlantípus"),
  SUSPICIOUS_SQM_PRICE("Gyanús négyzetméterár"),
  UNREALISTIC_ROOMS("Irreális szobaszám"),
  MISSING_IMAGES("Hiányzó kép"),
  MISSING_DESCRIPTION("Hiányzó vagy túl rövid leírás"),
  MISSING_DETAILS("Hiányos részletes paraméterek");

  private final String description;

  QualityDeductionRule(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
