package ingatlanok.presentation;

import java.util.List;

import ingatlanok.model.IngatlanItem;

public record DedupActionResponseDto(
    List<IngatlanItem> items, List<DedupOverrideDto> overrides, String message) {}
