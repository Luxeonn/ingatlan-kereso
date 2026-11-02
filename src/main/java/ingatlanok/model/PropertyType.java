package ingatlanok.model;

import java.util.Locale;

/** Canonical property type representation (#258). */
public enum PropertyType {
  LAKAS("Lakás", "lakas"),
  HAZ("Ház", "haz"),
  TELEK("Telek", "telek"),
  GARAZS("Garázs", "garazs"),
  IPARI_KERESKEDELMI("Ipari / Kereskedelmi", "ipari-kereskedelmi"),
  EGYEB_ISMERETLEN("Egyéb / Ismeretlen", "egyeb-ismeretlen");

  private final String displayName;
  private final String slug;

  PropertyType(String displayName, String slug) {
    this.displayName = displayName;
    this.slug = slug;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getSlug() {
    return slug;
  }

  public boolean isResidential() {
    return this == LAKAS || this == HAZ;
  }

  public boolean isPlot() {
    return this == TELEK;
  }

  public static PropertyType fromString(String raw) {
    if (raw == null || raw.isBlank()) {
      return EGYEB_ISMERETLEN;
    }
    String trimmed = raw.trim();
    String upper = trimmed.toUpperCase(Locale.ROOT);
    for (PropertyType type : values()) {
      if (type.name().equals(upper)
          || type.slug.equalsIgnoreCase(trimmed)
          || type.displayName.equalsIgnoreCase(trimmed)) {
        return type;
      }
    }
    return switch (trimmed.toLowerCase(Locale.ROOT)) {
      case "flat", "apartment", "lakás" -> LAKAS;
      case "house", "ház", "üdülő", "udulo", "üdülőház", "udulohaz" -> HAZ;
      case "land", "plot" -> TELEK;
      case "garage" -> GARAZS;
      case "industrial", "commercial", "ipari", "kereskedelmi" -> IPARI_KERESKEDELMI;
      default -> EGYEB_ISMERETLEN;
    };
  }
}
