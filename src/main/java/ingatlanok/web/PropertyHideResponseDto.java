package ingatlanok.web;

import java.io.Serializable;

/**
 * Strongly typed response for property hide and restore mutations (#401 / JAVA-017).
 */
public record PropertyHideResponseDto(boolean success, boolean hidden, String id)
    implements Serializable {

  public static PropertyHideResponseDto of(boolean success, boolean hidden, String id) {
    return new PropertyHideResponseDto(success, hidden, id);
  }
}
