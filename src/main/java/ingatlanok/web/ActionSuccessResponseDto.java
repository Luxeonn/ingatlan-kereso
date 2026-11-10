package ingatlanok.web;

import java.io.Serializable;

/**
 * Standard generic response for mutation actions (#401 / JAVA-017).
 */
public record ActionSuccessResponseDto(boolean success) implements Serializable {

  public static ActionSuccessResponseDto of(boolean success) {
    return new ActionSuccessResponseDto(success);
  }
}
