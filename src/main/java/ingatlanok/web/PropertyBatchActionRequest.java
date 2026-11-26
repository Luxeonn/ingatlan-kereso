package ingatlanok.web;

import java.util.List;

import jakarta.validation.constraints.Size;

/**
 * Request payload for hiding or restoring properties with optional batch URLs (#401 / JAVA-019).
 */
public record PropertyBatchActionRequest(
    @Size(max = 1000, message = "Az URL túl hosszú!") String url,
    @Size(max = 100, message = "Egyszerre legfeljebb 100 URL adható meg!")
        List<@Size(max = 1000, message = "Az URL túl hosszú!") String> urls) {}
