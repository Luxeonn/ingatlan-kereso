package ingatlanok.portal.koltozzbe;

import java.util.List;

import ingatlanok.model.PropertyType;
import ingatlanok.portal.AbstractPortalAdapter;
import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalUrlBuilder;

public class KoltozzbePortalAdapter extends AbstractPortalAdapter {

  private static final PortalDescriptor DESCRIPTOR =
      new PortalDescriptor(
          "koltozzbe",
          "Költözzbe",
          "#0284c7",
          "https://koltozzbe.hu",
          true,
          List.of(PropertyType.LAKAS, PropertyType.HAZ, PropertyType.TELEK),
          true);

  public KoltozzbePortalAdapter() {
    this(new KoltozzbeUrlBuilder(), List.of(new KoltozzbeExtractionStrategy()));
  }

  public KoltozzbePortalAdapter(
      PortalUrlBuilder urlBuilder, List<ExtractionStrategy> strategies) {
    super(DESCRIPTOR, urlBuilder != null ? urlBuilder : new KoltozzbeUrlBuilder(), strategies);
  }
}
