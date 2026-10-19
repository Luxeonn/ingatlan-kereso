package ingatlanok.portal;

import java.util.List;

/**
 * Portál metaadat rekord (azonosító, név, szín, kezdőoldal, támogatott típusok, mentett
 * hirdetésszám).
 */
public record PortalDescriptor(
    String id,
    String name,
    String brandColor,
    String homepageUrl,
    boolean enabledByDefault,
    List<String> supportedTypes,
    boolean directListingSupported,
    long savedListingCount) {
  public PortalDescriptor(
      String id,
      String name,
      String brandColor,
      String homepageUrl,
      boolean enabledByDefault,
      List<String> supportedTypes,
      boolean directListingSupported) {
    this(
        id,
        name,
        brandColor,
        homepageUrl,
        enabledByDefault,
        supportedTypes,
        directListingSupported,
        0L);
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
