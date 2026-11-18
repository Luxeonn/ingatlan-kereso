package ingatlanok.portal.ingatlanbazar;

import java.util.List;

import ingatlanok.model.PropertyType;
import ingatlanok.portal.AbstractPortalAdapter;
import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalUrlBuilder;

public class IngatlanBazarPortalAdapter extends AbstractPortalAdapter {

  private static final PortalDescriptor DESCRIPTOR =
      new PortalDescriptor(
          "ingatlanbazar",
          "Ingatlanbazár",
          "#ea580c",
          "https://www.ingatlanbazar.hu",
          true,
          List.of(PropertyType.LAKAS, PropertyType.HAZ, PropertyType.TELEK),
          true);

  public IngatlanBazarPortalAdapter() {
    this(new IngatlanBazarQueryBuilder(), List.of(new IngatlanBazarApiStrategy()));
  }

  public IngatlanBazarPortalAdapter(
      PortalUrlBuilder queryBuilder, List<ExtractionStrategy> strategies) {
    super(
        DESCRIPTOR,
        queryBuilder != null ? queryBuilder : new IngatlanBazarQueryBuilder(),
        strategies);
  }
}
