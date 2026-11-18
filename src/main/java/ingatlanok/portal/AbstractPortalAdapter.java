package ingatlanok.portal;

import java.util.List;
import java.util.Objects;

/**
 * Shared base implementation for {@link PortalAdapter} holding the immutable portal descriptor,
 * URL builder, and ordered extraction strategies.
 */
public abstract class AbstractPortalAdapter implements PortalAdapter {

  private final PortalDescriptor descriptor;
  private final PortalUrlBuilder urlBuilder;
  private final List<ExtractionStrategy> strategies;

  protected AbstractPortalAdapter(
      PortalDescriptor descriptor,
      PortalUrlBuilder urlBuilder,
      List<ExtractionStrategy> strategies) {
    this.descriptor = Objects.requireNonNull(descriptor, "PortalDescriptor cannot be null");
    this.urlBuilder = Objects.requireNonNull(urlBuilder, "PortalUrlBuilder cannot be null");
    this.strategies = strategies != null ? List.copyOf(strategies) : List.of();
  }

  @Override
  public PortalDescriptor getDescriptor() {
    return descriptor;
  }

  @Override
  public PortalUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  @Override
  public List<ExtractionStrategy> getStrategies() {
    return strategies;
  }
}
