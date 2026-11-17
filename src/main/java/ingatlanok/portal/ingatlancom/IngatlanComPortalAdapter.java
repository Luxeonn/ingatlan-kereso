package ingatlanok.portal.ingatlancom;

import java.util.List;

import ingatlanok.model.PropertyType;
import ingatlanok.portal.AbstractPortalAdapter;
import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalUrlBuilder;

public class IngatlanComPortalAdapter extends AbstractPortalAdapter {

  private static final PortalDescriptor DESCRIPTOR =
      new PortalDescriptor(
          "ingatlancom",
          "Ingatlan.com",
          "#0284c7",
          "https://ingatlan.com",
          true,
          List.of(PropertyType.LAKAS, PropertyType.HAZ, PropertyType.TELEK),
          false);

  public IngatlanComPortalAdapter() {
    this(
        new IngatlanComUrlBuilder(),
        List.of(new IngatlanComSidecarStrategy(), new IngatlanComHtmlStrategy()));
  }

  public IngatlanComPortalAdapter(
      PortalUrlBuilder urlBuilder, List<ExtractionStrategy> strategies) {
    super(DESCRIPTOR, urlBuilder != null ? urlBuilder : new IngatlanComUrlBuilder(), strategies);
  }
}
