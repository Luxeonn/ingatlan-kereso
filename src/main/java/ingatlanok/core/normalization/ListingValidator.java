package ingatlanok.core.normalization;

import java.net.URI;

import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.PortalDescriptor;

public class ListingValidator {

  public boolean isValid(RawPropertyListing raw, PortalDescriptor descriptor) {
    if (raw == null) {
      return false;
    }
    String url = raw.url();
    if (url == null || url.isBlank()) {
      return false;
    }

    if (descriptor != null && descriptor.homepageUrl() != null && !descriptor.homepageUrl().isBlank()) {
      try {
        URI itemUri = URI.create(url.trim());
        if (itemUri.isAbsolute()) {
          URI baseUri = URI.create(descriptor.homepageUrl().trim());
          String itemHost = itemUri.getHost();
          String baseHost = baseUri.getHost();
          if (itemHost != null && baseHost != null) {
            String cleanItemHost = itemHost.replaceFirst("^www\\.", "");
            String cleanBaseHost = baseHost.replaceFirst("^www\\.", "");
            if (!cleanItemHost.equalsIgnoreCase(cleanBaseHost)
                && !cleanItemHost.endsWith("." + cleanBaseHost)) {
              return false;
            }
          }
        }
      } catch (Exception e) {
        return false;
      }
    }

    if (raw.areaSquareMeters() != null) {
      double area = raw.areaSquareMeters();
      if (area < 5.0 || area > 50_000.0) {
        return false;
      }
    }

    if (raw.priceHuf() != null) {
      long price = raw.priceHuf();
      if (price <= 0 || price > 50_000_000_000L) {
        return false;
      }
    }

    return true;
  }
}
