package ingatlanok.model;

import java.io.Serializable;

/**
 * Structured deduction entry resulting from data quality evaluation (#401 / JAVA-010).
 */
public record QualityDeduction(QualityDeductionRule rule, int penalty, String details)
    implements Serializable {}
