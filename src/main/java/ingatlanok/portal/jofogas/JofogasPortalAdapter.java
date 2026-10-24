package ingatlanok.portal.jofogas;

import java.util.List;

import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalAdapter;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalUrlBuilder;

public class JofogasPortalAdapter implements PortalAdapter {

  private static final PortalDescriptor DESCRIPTOR =
      new PortalDescriptor(
          "jofogas",
          "Jófogás Ingatlan",
          "#f97316",
          "https://ingatlan.jofogas.hu",
          true,
          List.of("lakas", "haz", "telek"),
          true);

  private final JofogasUrlBuilder urlBuilder;
  private final List<ExtractionStrategy> strategies;

  public JofogasPortalAdapter() {
    this(new JofogasUrlBuilder(), List.of(new JofogasHtmlStrategy()));
  }

  public JofogasPortalAdapter(
      JofogasUrlBuilder urlBuilder, List<ExtractionStrategy> strategies) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new JofogasUrlBuilder();
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
