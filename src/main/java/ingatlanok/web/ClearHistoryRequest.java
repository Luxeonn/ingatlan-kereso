package ingatlanok.web;

import java.io.Serializable;

/**
 * Request payload for clearing history for a specific city (#401 / JAVA-019).
 */
public record ClearHistoryRequest(String city) implements Serializable {}
