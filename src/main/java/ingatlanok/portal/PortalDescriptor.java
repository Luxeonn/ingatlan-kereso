package ingatlanok.portal;

import java.util.List;

/**
 * Portál metaadat rekord (azonosító, név, szín, kezdőoldal, támogatott típusok).
 */
public record PortalDescriptor(
        String id,
        String name,
        String brandColor,
        String homepageUrl,
        boolean enabledByDefault,
        List<String> supportedTypes,
        boolean directListingSupported
) {
}
