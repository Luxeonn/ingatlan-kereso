package ingatlanok.portal.ingatlancom;

import java.util.List;

import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalAdapter;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalUrlBuilder;

public class IngatlanComPortalAdapter implements PortalAdapter {

  private static final PortalDescriptor DESCRIPTOR =
      new PortalDescriptor(
          "ingatlancom",
          "Ingatlan.com",
          "#0284c7",
          "https://ingatlan.com",
          true,
          List.of("lakas", "haz", "telek"),
          false);

  private final IngatlanComUrlBuilder urlBuilder;
  private final List<ExtractionStrategy> strategies;

  public IngatlanComPortalAdapter() {
    this(new IngatlanComUrlBuilder(), List.of(new IngatlanComSidecarStrategy(), new IngatlanComHtmlStrategy()));
  }

  public IngatlanComPortalAdapter(
      IngatlanComUrlBuilder urlBuilder, List<ExtractionStrategy> strategies) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new IngatlanComUrlBuilder();
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
