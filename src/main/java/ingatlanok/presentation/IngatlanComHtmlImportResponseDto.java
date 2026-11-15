package ingatlanok.presentation;

import java.util.List;

import ingatlanok.model.IngatlanItem;

/**
 * Response summary for importing user-supplied Ingatlan.com search result HTML (#455).
 */
public record IngatlanComHtmlImportResponseDto(
    int parsedCount,
    int savedCount,
    int newlyAddedCount,
    int duplicateCount,
    String city,
    List<IngatlanItem> items) {}
