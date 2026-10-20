package ingatlanok.portal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import ingatlanok.model.SearchCriteria;

public class PortalEngineRegistry {

  private final Map<String, PortalEngine> engines = new ConcurrentHashMap<>();

  public PortalEngineRegistry(List<PortalEngine> portalEngines) {
    if (portalEngines != null) {
      portalEngines.forEach(this::register);
    }
  }

  public void register(PortalEngine engine) {
    if (engine != null && engine.getDescriptor() != null && engine.getDescriptor().id() != null) {
      engines.put(engine.getDescriptor().id().toLowerCase(Locale.ROOT), engine);
    }
  }

  public Optional<PortalEngine> getEngine(String portalId) {
    if (portalId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(engines.get(portalId.toLowerCase(Locale.ROOT)));
  }

  public List<PortalEngine> getAllEngines() {
    return new ArrayList<>(engines.values());
  }

  public List<PortalDescriptor> getAllDescriptors() {
    List<PortalDescriptor> descriptors = new ArrayList<>();
    for (PortalEngine engine : engines.values()) {
      descriptors.add(engine.getDescriptor());
    }
    return descriptors;
  }

  public List<PortalEngine> getActiveEngines(SearchCriteria criteria) {
    if (criteria == null) {
      return getAllEngines();
    }
    List<PortalEngine> active = new ArrayList<>();
    for (PortalEngine engine : engines.values()) {
      if (criteria.isPortalEnabled(engine.getDescriptor().id())) {
        active.add(engine);
      }
    }
    return active;
  }
}
