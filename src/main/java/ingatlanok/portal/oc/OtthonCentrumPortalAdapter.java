package ingatlanok.portal.oc;

import java.util.List;

import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalAdapter;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalUrlBuilder;

public class OtthonCentrumPortalAdapter implements PortalAdapter {

  private static final PortalDescriptor DESCRIPTOR =
      new PortalDescriptor(
          "oc",
          "Otthon Centrum",
          "#dc2626",
          "https://www.oc.hu",
          true,
          List.of("lakas", "haz", "telek"),
          true);

  private final OtthonCentrumUrlBuilder urlBuilder;
  private final List<ExtractionStrategy> strategies;

  public OtthonCentrumPortalAdapter() {
    this(new OtthonCentrumUrlBuilder(), List.of(new OtthonCentrumHtmlStrategy()));
  }

  public OtthonCentrumPortalAdapter(
      OtthonCentrumUrlBuilder urlBuilder, List<ExtractionStrategy> strategies) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new OtthonCentrumUrlBuilder();
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
