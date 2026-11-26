package ingatlanok.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/**
 * Query parameters for property lookup endpoints ({@code /api/properties/price-history}, {@code
 * /api/properties/status-log}, {@code /api/properties/observations}).
 */
public record PropertyLookupQueryDto(
    @Size(max = 1000, message = "A megadott azonosító vagy URL túl hosszú!") String id,
    @Size(max = 1000, message = "A megadott azonosító vagy URL túl hosszú!") String url) {

  @JsonIgnore
  @AssertTrue(message = "Hiányzó 'id' vagy 'url' paraméter!")
  public boolean isLookupKeyProvided() {
    return (id != null && !id.isBlank()) || (url != null && !url.isBlank());
  }

  public String resolveKey() {
    if (id != null && !id.isBlank()) {
      return id;
    }
    return url;
  }
}
