package ingatlanok.portal.ingatlanbazar;

import java.util.List;

import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalAdapter;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalUrlBuilder;

public class IngatlanBazarPortalAdapter implements PortalAdapter {

  private static final PortalDescriptor DESCRIPTOR =
      new PortalDescriptor(
          "ingatlanbazar",
          "Ingatlanbazár",
          "#ea580c",
          "https://www.ingatlanbazar.hu",
          true,
          List.of("lakas", "haz", "telek"),
          true);

  private final IngatlanBazarQueryBuilder queryBuilder;
  private final List<ExtractionStrategy> strategies;

  public IngatlanBazarPortalAdapter() {
    this(new IngatlanBazarQueryBuilder(), List.of(new IngatlanBazarApiStrategy()));
  }

  public IngatlanBazarPortalAdapter(
      IngatlanBazarQueryBuilder queryBuilder, List<ExtractionStrategy> strategies) {
    this.queryBuilder = queryBuilder != null ? queryBuilder : new IngatlanBazarQueryBuilder();
    this.strategies = strategies != null ? List.copyOf(strategies) : List.of();
  }

  @Override
  public PortalDescriptor getDescriptor() {
    return DESCRIPTOR;
  }

  @Override
  public PortalUrlBuilder getUrlBuilder() {
    return queryBuilder;
  }

  @Override
  public List<ExtractionStrategy> getStrategies() {
    return strategies;
  }
}
