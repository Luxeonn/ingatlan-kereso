package ingatlanok.portal;

import java.util.concurrent.atomic.AtomicReference;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.RawPropertyListing;

/**
 * Functional interface for normalizing raw extracted portal listings into canonical items.
 * Allows decoupling portal engines from core normalization business logic.
 */
@FunctionalInterface
public interface RawListingNormalizer {

  AtomicReference<RawListingNormalizer> DEFAULT_HOLDER =
      new AtomicReference<>(createDefaultNormalizer());

  IngatlanItem normalize(
      RawPropertyListing raw,
      PortalDescriptor descriptor,
      SearchCriteria safeCriteria,
      String nowTimestamp);

  static void setDefault(RawListingNormalizer normalizer) {
    if (normalizer != null) {
      DEFAULT_HOLDER.set(normalizer);
    }
  }

  static RawListingNormalizer getDefault() {
    return DEFAULT_HOLDER.get();
  }

  private static RawListingNormalizer createDefaultNormalizer() {
    try {
      Class<?> clazz = Class.forName("ingatlanok.core.normalization.ListingNormalizer");
      return (RawListingNormalizer) clazz.getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException | LinkageError e) {
      return (raw, descriptor, criteria, now) -> {
        if (raw == null) {
          return null;
        }
        return IngatlanItem.builder()
            .id(raw.nativeId())
            .source(PortalSource.of(raw.portalId()))
            .url(raw.url())
            .title(raw.title())
            .location(raw.locationText())
            .build();
      };
    }
  }
}
