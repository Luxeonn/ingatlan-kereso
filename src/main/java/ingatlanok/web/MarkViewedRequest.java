package ingatlanok.web;

/**
 * Request payload for marking a single listing as viewed (#401 / JAVA-019).
 */
public record MarkViewedRequest(String url) {}
