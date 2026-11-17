package ingatlanok.portal.dh;

import java.util.List;

import ingatlanok.model.PropertyType;
import ingatlanok.portal.AbstractPortalAdapter;
import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalUrlBuilder;

public class DunaHousePortalAdapter extends AbstractPortalAdapter {

  private static final PortalDescriptor DESCRIPTOR =
      new PortalDescriptor(
          "dh",
          "Duna House",
          "#1d4ed8",
          "https://dh.hu",
          true,
          List.of(PropertyType.LAKAS, PropertyType.HAZ, PropertyType.TELEK),
          true);

  public DunaHousePortalAdapter() {
    this(new DunaHouseUrlBuilder(), List.of(new DunaHouseHtmlStrategy()));
  }

  public DunaHousePortalAdapter(
      PortalUrlBuilder urlBuilder, List<ExtractionStrategy> strategies) {
    super(DESCRIPTOR, urlBuilder != null ? urlBuilder : new DunaHouseUrlBuilder(), strategies);
  }
}
