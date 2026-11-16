package ingatlanok.presentation;

import ingatlanok.model.DedupOverrideType;

public record DedupOverrideDto(
    Long id,
    String urlA,
    String urlB,
    DedupOverrideType type,
    String normalizedCity,
    String createdAt) {}
