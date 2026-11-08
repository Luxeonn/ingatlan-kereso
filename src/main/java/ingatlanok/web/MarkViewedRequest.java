package ingatlanok.web;

import java.io.Serializable;

/**
 * Request payload for marking a single listing as viewed (#401 / JAVA-019).
 */
public record MarkViewedRequest(String url) implements Serializable {}
