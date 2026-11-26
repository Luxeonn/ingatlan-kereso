package ingatlanok.web;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/**
 * Query parameters for {@code GET /api/dedup/overrides}.
 */
public record DedupOverridesQueryDto(
    @Size(max = 1000, message = "Az URL túl hosszú!") String url,
    @Size(max = 30, message = "Egyszerre legfeljebb 30 URL kérdezhető le")
        List<@Size(max = 1000, message = "Az URL túl hosszú!") String> urls) {

  @JsonIgnore
  @AssertTrue(message = "A lekérdezett URL paraméter kötelező")
  public boolean isAnyUrlProvided() {
    return !collectDistinctNonBlankUrls().isEmpty();
  }

  @JsonIgnore
  @AssertTrue(message = "Egyszerre legfeljebb 30 URL kérdezhető le")
  public boolean isTotalUniqueUrlCountValid() {
    return collectDistinctNonBlankUrls().size() <= 30;
  }

  private Set<String> collectDistinctNonBlankUrls() {
    Set<String> queryUrls = new LinkedHashSet<>();
    if (url != null && !url.isBlank()) {
      queryUrls.add(url.trim());
    }
    if (urls == null) {
      return queryUrls;
    }
    for (String candidate : urls) {
      if (candidate != null && !candidate.isBlank()) {
        queryUrls.add(candidate.trim());
      }
    }
    return queryUrls;
  }
}
