package ingatlanok.presentation;

import ingatlanok.model.PropertyType;

/**
 * Request payload for importing user-supplied Ingatlan.com search result HTML (#455).
 */
public record IngatlanComHtmlImportRequestDto(
    String html, String sourceUrl, String city, PropertyType propertyType) {}
