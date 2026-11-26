package ingatlanok.web;

import jakarta.validation.constraints.Size;

/**
 * Request payload for marking a single listing as viewed (#401 / JAVA-019).
 */
public record MarkViewedRequest(
    @Size(max = 1000, message = "Az URL túl hosszú!") String url) {}
