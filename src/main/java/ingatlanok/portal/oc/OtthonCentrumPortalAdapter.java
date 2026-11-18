package ingatlanok.portal.oc;

import java.util.List;

import ingatlanok.model.PropertyType;
import ingatlanok.portal.AbstractPortalAdapter;
import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalUrlBuilder;

public class OtthonCentrumPortalAdapter extends AbstractPortalAdapter {

  private static final PortalDescriptor DESCRIPTOR =
      new PortalDescriptor(
          "oc",
          "Otthon Centrum",
          "#dc2626",
          "https://www.oc.hu",
          true,
          List.of(PropertyType.LAKAS, PropertyType.HAZ, PropertyType.TELEK),
          true);

  public OtthonCentrumPortalAdapter() {
    this(new OtthonCentrumUrlBuilder(), List.of(new OtthonCentrumHtmlStrategy()));
  }

  public OtthonCentrumPortalAdapter(
      PortalUrlBuilder urlBuilder, List<ExtractionStrategy> strategies) {
    super(DESCRIPTOR, urlBuilder != null ? urlBuilder : new OtthonCentrumUrlBuilder(), strategies);
  }
}
