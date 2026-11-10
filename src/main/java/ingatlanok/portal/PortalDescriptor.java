package ingatlanok.portal;

import java.util.List;

import ingatlanok.model.PropertyType;

/**
 * Metadata descriptor for supported real estate portals (#401 / JAVA-003).
 */
public record PortalDescriptor(
    String id,
    String name,
    String brandColor,
    String homepageUrl,
    boolean enabledByDefault,
    List<PropertyType> supportedTypes,
    boolean directListingSupported,
    long savedListingCount) {

  public PortalDescriptor {
    supportedTypes = normalizeTypes(supportedTypes);
  }

  public PortalDescriptor(
      String id,
      String name,
      String brandColor,
      String homepageUrl,
      boolean enabledByDefault,
      List<?> supportedTypes,
      boolean directListingSupported) {
    this(
        id,
        name,
        brandColor,
        homepageUrl,
        enabledByDefault,
        normalizeTypes(supportedTypes),
        directListingSupported,
        0L);
  }

  private static List<PropertyType> normalizeTypes(List<?> raw) {
    if (raw == null) {
      return List.of();
    }
    return raw.stream()
        .map(
            item ->
                item instanceof PropertyType pt
                    ? pt
                    : PropertyType.fromString(String.valueOf(item)))
        .toList();
  }

  public PortalDescriptor withSavedListingCount(long count) {
    return new PortalDescriptor(
        id,
        name,
        brandColor,
        homepageUrl,
        enabledByDefault,
        supportedTypes,
        directListingSupported,
        count);
  }
}
