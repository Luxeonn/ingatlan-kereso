package ingatlanok.model;

import jakarta.validation.groups.Default;

/**
 * Bean Validation group markers distinguishing preset creation from partial updates.
 */
public final class ValidationGroups {

  public interface OnCreate extends Default {}

  public interface OnUpdate extends Default {}

  private ValidationGroups() {}
}
