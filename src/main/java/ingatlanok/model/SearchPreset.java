package ingatlanok.model;

import java.io.Serializable;

/**
 * Immutable search preset record storing a user-defined name, search criteria, and audit timestamps.
 */
public record SearchPreset(
    Long id,
    String name,
    SearchCriteria criteria,
    String createdAt,
    String updatedAt,
    String lastUsedAt)
    implements Serializable {}
