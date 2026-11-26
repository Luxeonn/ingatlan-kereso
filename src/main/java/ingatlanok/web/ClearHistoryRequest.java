package ingatlanok.web;

import jakarta.validation.constraints.Size;

/**
 * Request payload for clearing history for a specific city (#401 / JAVA-019).
 */
public record ClearHistoryRequest(
    @Size(max = 200, message = "A település neve túl hosszú") String city) {}
