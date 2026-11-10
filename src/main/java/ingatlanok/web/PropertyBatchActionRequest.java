package ingatlanok.web;

import java.io.Serializable;
import java.util.List;

/**
 * Request payload for hiding or restoring properties with optional batch URLs (#401 / JAVA-019).
 */
public record PropertyBatchActionRequest(String url, List<String> urls) implements Serializable {}
