package ingatlanok.core.normalization;

import java.net.URI;

import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.PortalDescriptor;

public class ListingValidator {

  public boolean isValid(RawPropertyListing raw, PortalDescriptor descriptor) {
    if (raw == null || !isValidUrl(raw.url())) {
      return false;
    }
    if (!isMatchingPortalHost(raw.url(), descriptor)) {
      return false;
    }
    if (!isValidArea(raw.areaSquareMeters())) {
      return false;
    }
    return isValidPrice(raw.priceHuf());
  }

  private boolean isValidUrl(String url) {
    return url != null && !url.isBlank();
  }

  private boolean isMatchingPortalHost(String url, PortalDescriptor descriptor) {
    if (descriptor == null || descriptor.homepageUrl() == null || descriptor.homepageUrl().isBlank()) {
      return true;
    }
    try {
      URI itemUri = URI.create(url.trim());
      if (!itemUri.isAbsolute()) {
        return true;
      }
      URI baseUri = URI.create(descriptor.homepageUrl().trim());
      String itemHost = itemUri.getHost();
      String baseHost = baseUri.getHost();
      if (itemHost == null || baseHost == null) {
        return true;
      }
      String cleanItemHost = itemHost.replaceFirst("^www\\.", "");
      String cleanBaseHost = baseHost.replaceFirst("^www\\.", "");
      return cleanItemHost.equalsIgnoreCase(cleanBaseHost)
          || cleanItemHost.endsWith("." + cleanBaseHost);
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isValidArea(Double area) {
    if (area == null) {
      return true;
    }
    return area >= 5.0 && area <= 50_000.0;
  }

  private boolean isValidPrice(Long price) {
    if (price == null) {
      return true;
    }
    return price > 0 && price <= 50_000_000_000L;
  }
}
