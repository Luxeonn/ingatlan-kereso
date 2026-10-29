package ingatlanok.portal.koltozzbe;

import java.util.List;

import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalAdapter;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalUrlBuilder;

public class KoltozzbePortalAdapter implements PortalAdapter {

  private static final PortalDescriptor DESCRIPTOR =
      new PortalDescriptor(
          "koltozzbe",
          "Költözzbe",
          "#0284c7",
          "https://koltozzbe.hu",
          true,
          List.of("lakas", "haz", "telek"),
          true);

  private final KoltozzbeUrlBuilder urlBuilder;
  private final List<ExtractionStrategy> strategies;

  public KoltozzbePortalAdapter() {
    this(new KoltozzbeUrlBuilder(), List.of(new KoltozzbeExtractionStrategy()));
  }

  public KoltozzbePortalAdapter(
      KoltozzbeUrlBuilder urlBuilder, List<ExtractionStrategy> strategies) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new KoltozzbeUrlBuilder();
    this.strategies = strategies != null ? List.copyOf(strategies) : List.of();
  }

  @Override
  public PortalDescriptor getDescriptor() {
    return DESCRIPTOR;
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
