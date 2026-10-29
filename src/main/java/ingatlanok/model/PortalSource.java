package ingatlanok.model;

import java.io.Serializable;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PortalSource implements Serializable {

  private String portalId;
  private String portalName;
  private String themeColor;
  private boolean directListing;

  @Builder
  public PortalSource(
      String portalId, String portalName, String themeColor, boolean directListing) {
    this.portalId = portalId;
    this.portalName = portalName;
    this.themeColor = themeColor;
    this.directListing = directListing;
  }

  public static PortalSource of(String portalName) {
    if (portalName == null) {
      return new PortalSource("unknown", "Ismeretlen", "#6b7280", false);
    }
    String lower = portalName.toLowerCase();
    if (lower.contains("duna")) {
      return new PortalSource("dh", "Duna House", "#008638", true);
    } else if (lower.contains("otthon")) {
      return new PortalSource("oc", "Otthon Centrum", "#d9232a", true);
    } else if (lower.contains("jófogás") || lower.contains("jofogas")) {
      return new PortalSource("jofogas", "Jófogás Ingatlan", "#f39200", true);
    } else if (lower.contains("ingatlan.com")) {
      return new PortalSource("ingatlancom", "Ingatlan.com", "#1d4ed8", false);
    } else if (lower.contains("bazár") || lower.contains("bazar")) {
      return new PortalSource("ingatlanbazar", "Ingatlanbazár", "#d80f31", true);
    } else if (lower.contains("költözzbe") || lower.contains("koltozzbe")) {
      return new PortalSource("koltozzbe", "Költözzbe", "#e05338", true);
    }
    return new PortalSource(lower.replaceAll("[^a-z0-9]", ""), portalName, "#6b7280", true);
  }
}
