package ingatlanok.portal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import ingatlanok.model.SearchCriteria;
import ingatlanok.portal.dh.DunaHousePortalEngine;
import ingatlanok.portal.ingatlanbazar.IngatlanBazarPortalEngine;
import ingatlanok.portal.ingatlancom.IngatlanComPortalEngine;
import ingatlanok.portal.jofogas.JofogasPortalEngine;
import ingatlanok.portal.koltozzbe.KoltozzbePortalEngine;
import ingatlanok.portal.oc.OtthonCentrumPortalEngine;

/**
 * Dinamikus Provider Factory és nyilvántartás az elérhető portál motorokhoz. Biztosítja a plug &
 * play portálregisztrációt és a /api/portals végpont kiszolgálását.
 */
public class PortalEngineRegistry {

  private static final PortalEngineRegistry INSTANCE = new PortalEngineRegistry();
  private final Map<String, PortalEngine> engines = new ConcurrentHashMap<>();

  private PortalEngineRegistry() {
    register(new DunaHousePortalEngine());
    register(new OtthonCentrumPortalEngine());
    register(new JofogasPortalEngine());
    register(new IngatlanBazarPortalEngine());
    register(new KoltozzbePortalEngine());
    register(new IngatlanComPortalEngine());
  }

  public static PortalEngineRegistry getInstance() {
    return INSTANCE;
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
