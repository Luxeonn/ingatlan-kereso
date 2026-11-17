package ingatlanok.portal.jofogas;

import java.util.List;

import ingatlanok.model.PropertyType;
import ingatlanok.portal.AbstractPortalAdapter;
import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalUrlBuilder;

public class JofogasPortalAdapter extends AbstractPortalAdapter {

  private static final PortalDescriptor DESCRIPTOR =
      new PortalDescriptor(
          "jofogas",
          "Jófogás Ingatlan",
          "#f97316",
          "https://ingatlan.jofogas.hu",
          true,
          List.of(PropertyType.LAKAS, PropertyType.HAZ, PropertyType.TELEK),
          true);

  public JofogasPortalAdapter() {
    this(new JofogasUrlBuilder(), List.of(new JofogasHtmlStrategy()));
  }

  public JofogasPortalAdapter(
      PortalUrlBuilder urlBuilder, List<ExtractionStrategy> strategies) {
    super(DESCRIPTOR, urlBuilder != null ? urlBuilder : new JofogasUrlBuilder(), strategies);
  }
}
