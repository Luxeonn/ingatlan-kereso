package ingatlanok.persistence.entity;

/**
 * Type-safe field name constants for JPA queries and specifications (#401 / JAVA-025).
 */
public final class PropertyEntityFields {

  public static final String ID = "id";
  public static final String CANONICAL_ID = "canonicalId";
  public static final String PORTAL = "portal";
  public static final String URL = "url";
  public static final String TITLE = "title";
  public static final String PRICE = "price";
  public static final String CITY = "city";
  public static final String NORMALIZED_CITY = "normalizedCity";
  public static final String DETAILS = "details";
  public static final String AREA_SQM = "areaSqm";
  public static final String ROOMS = "rooms";
  public static final String DISCOVERED_AT = "discoveredAt";
  public static final String IS_NEW = "isNew";
  public static final String VIEWED = "viewed";
  public static final String VIEWED_AT = "viewedAt";
  public static final String HIDDEN = "hidden";
  public static final String HIDDEN_AT = "hiddenAt";
  public static final String IMAGE_URL = "imageUrl";
  public static final String STATUS = "status";
  public static final String PROPERTY_TYPE = "propertyType";
  public static final String PRICE_AMOUNT = "priceAmount";
  public static final String PRICE_MILLIONS = "priceMillions";
  public static final String PRICE_PER_SQM = "pricePerSqm";
  public static final String PRICE_DELTA = "priceDelta";
  public static final String PRICE_CHANGE_PERCENT = "priceChangePercent";
  public static final String LAST_PRICE_CHANGE_AT = "lastPriceChangeAt";
  public static final String NEW_CONSTRUCTION = "newConstruction";
  public static final String QUALITY_SCORE = "qualityScore";

  private PropertyEntityFields() {}
}
