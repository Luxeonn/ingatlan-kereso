package ingatlanok.web;

/**
 * Standard generic response for mutation actions (#401 / JAVA-017).
 */
public record ActionSuccessResponseDto(boolean success) {

  public static ActionSuccessResponseDto of(boolean success) {
    return new ActionSuccessResponseDto(success);
  }
}
