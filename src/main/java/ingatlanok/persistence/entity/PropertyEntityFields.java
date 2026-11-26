package ingatlanok.persistence.entity;

/**
 * Type-safe field name constants for JPA queries and specifications (#401 / JAVA-025).
 */
public final class PropertyEntityFields {

  public static final String PORTAL = "portal";
  public static final String NORMALIZED_CITY = "normalizedCity";
  public static final String AREA_SQM = "areaSqm";
  public static final String ROOMS = "rooms";
  public static final String VIEWED = "viewed";
  public static final String HIDDEN = "hidden";
  public static final String PRICE_MILLIONS = "priceMillions";
  public static final String PRICE_DELTA = "priceDelta";
  public static final String NEW_CONSTRUCTION = "newConstruction";

  private PropertyEntityFields() {}
}
