package ingatlanok.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Closed set of supported real estate portal providers (#401 / JAVA-002).
 */
public enum PortalId implements Serializable {
  DH("dh", "Duna House"),
  OC("oc", "Otthon Centrum"),
  JOFOGAS("jofogas", "Jófogás Ingatlan"),
  KOLTOZZBE("koltozzbe", "Költözzbe"),
  INGATLANBAZAR("ingatlanbazar", "Ingatlanbazár"),
  INGATLANCOM("ingatlancom", "Ingatlan.com"),
  UNKNOWN("unknown", "Ismeretlen");

  private static final Map<String, PortalId> LOOKUP = new HashMap<>();

  static {
    for (PortalId portal : values()) {
      LOOKUP.put(portal.id, portal);
      LOOKUP.put(portal.name().toLowerCase(Locale.ROOT), portal);
    }
  }

  private final String id;
  private final String displayName;

  PortalId(String id, String displayName) {
    this.id = id;
    this.displayName = displayName;
  }

  public String getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static PortalId fromString(String raw) {
    if (raw == null || raw.isBlank()) {
      return UNKNOWN;
    }
    String lower = raw.trim().toLowerCase(Locale.ROOT);
    PortalId direct = LOOKUP.get(lower);
    if (direct != null) {
      return direct;
    }
    return matchFuzzy(lower);
  }

  private static PortalId matchFuzzy(String lower) {
    if (lower.contains("duna")) {
      return DH;
    }
    if (lower.contains("otthon")) {
      return OC;
    }
    if (lower.contains("jofogas") || lower.contains("jófogás")) {
      return JOFOGAS;
    }
    if (lower.contains("ingatlan.com") || lower.contains("ingatlancom")) {
      return INGATLANCOM;
    }
    if (lower.contains("bazar") || lower.contains("bazár")) {
      return INGATLANBAZAR;
    }
    if (lower.contains("koltozzbe") || lower.contains("költözzbe")) {
      return KOLTOZZBE;
    }
    return UNKNOWN;
  }

  @JsonValue
  public String toCanonicalString() {
    return id;
  }
}
