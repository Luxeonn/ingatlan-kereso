package ingatlanok.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.SearchCriteria;

class PortalEngineRegistryTest {

  @Test
  @DisplayName("Beépített 6 portál motor elérhető és lekérdezhető")
  void testBuiltinEngines() {
    PortalEngineRegistry registry = PortalEngineRegistry.getInstance();
    List<PortalEngine> engines = registry.getAllEngines();

    assertTrue(engines.size() >= 6);

    Optional<PortalEngine> dh = registry.getEngine("dh");
    Optional<PortalEngine> oc = registry.getEngine("oc");
    Optional<PortalEngine> jf = registry.getEngine("jofogas");
    Optional<PortalEngine> ib = registry.getEngine("ingatlanbazar");
    Optional<PortalEngine> kb = registry.getEngine("koltozzbe");
    Optional<PortalEngine> ic = registry.getEngine("ingatlancom");

    assertTrue(dh.isPresent());
    assertTrue(oc.isPresent());
    assertTrue(jf.isPresent());
    assertTrue(ib.isPresent());
    assertTrue(kb.isPresent());
    assertTrue(ic.isPresent());

    assertEquals("Duna House", dh.get().getName());
    assertEquals("Otthon Centrum", oc.get().getName());
    assertEquals("Jófogás Ingatlan", jf.get().getName());
    assertEquals("Ingatlanbazár", ib.get().getName());
    assertEquals("Költözzbe", kb.get().getName());
    assertEquals("Ingatlan.com", ic.get().getName());
  }

  @Test
  @DisplayName("getAllDescriptors visszaadja az összes portál metaadatait")
  void testGetAllDescriptors() {
    PortalEngineRegistry registry = PortalEngineRegistry.getInstance();
    List<PortalDescriptor> descriptors = registry.getAllDescriptors();

    assertNotNull(descriptors);
    assertTrue(descriptors.size() >= 6);
    assertTrue(descriptors.stream().anyMatch(d -> "dh".equals(d.id())));
    assertTrue(descriptors.stream().anyMatch(d -> "oc".equals(d.id())));
  }

  @Test
  @DisplayName("Kritériumok szerinti aktív motorok szűrése")
  void testGetActiveEngines() {
    PortalEngineRegistry registry = PortalEngineRegistry.getInstance();
    SearchCriteria criteria = new SearchCriteria();
    criteria.setPortals(List.of("dh", "oc"));

    List<PortalEngine> active = registry.getActiveEngines(criteria);
    assertEquals(2, active.size());
    assertTrue(active.stream().anyMatch(e -> "dh".equals(e.getId())));
    assertTrue(active.stream().anyMatch(e -> "oc".equals(e.getId())));
    assertFalse(active.stream().anyMatch(e -> "jofogas".equals(e.getId())));
  }
}
