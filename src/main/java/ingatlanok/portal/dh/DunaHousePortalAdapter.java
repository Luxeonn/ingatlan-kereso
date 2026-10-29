package ingatlanok.portal.dh;

import java.util.List;

import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalAdapter;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalUrlBuilder;

public class DunaHousePortalAdapter implements PortalAdapter {

  private static final PortalDescriptor DESCRIPTOR =
      new PortalDescriptor(
          "dh",
          "Duna House",
          "#1d4ed8",
          "https://dh.hu",
          true,
          List.of("lakas", "haz", "telek"),
          true);

  private final DunaHouseUrlBuilder urlBuilder;
  private final List<ExtractionStrategy> strategies;

  public DunaHousePortalAdapter() {
    this(new DunaHouseUrlBuilder(), List.of(new DunaHouseHtmlStrategy()));
  }

  public DunaHousePortalAdapter(
      DunaHouseUrlBuilder urlBuilder, List<ExtractionStrategy> strategies) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new DunaHouseUrlBuilder();
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
