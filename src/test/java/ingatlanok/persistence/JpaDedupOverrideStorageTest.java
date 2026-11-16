package ingatlanok.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ingatlanok.model.DedupOverride;
import ingatlanok.model.DedupOverrideType;

@SpringBootTest
@DisplayName("JpaDedupOverrideStorage Integration Tests")
class JpaDedupOverrideStorageTest {

  @Autowired private JpaDedupOverrideStorage overrideStorage;
  @Autowired private JpaIngatlanStorage ingatlanStorage;

  @BeforeEach
  void setUp() {
    ingatlanStorage.clearHistory();
    overrideStorage.clearAllOverrides();
  }

  @Test
  @DisplayName("Rendezett URL-pár egyediség és split <-> merge felülírás")
  void testCanonicalPairUniquenessAndTypeSwap() {
    DedupOverride first =
        overrideStorage.saveOverride(
            "https://oc.hu/2", "https://dh.hu/1", DedupOverrideType.FORCE_SPLIT, "Debrecen");

    assertNotNull(first.id());
    assertEquals("https://dh.hu/1", first.urlA());
    assertEquals("https://oc.hu/2", first.urlB());
    assertEquals(DedupOverrideType.FORCE_SPLIT, first.type());

    DedupOverride swapped =
        overrideStorage.saveOverride(
            "https://dh.hu/1", "https://oc.hu/2", DedupOverrideType.FORCE_MERGE, "Debrecen");

    assertEquals(first.id(), swapped.id());
    assertEquals(DedupOverrideType.FORCE_MERGE, swapped.type());
    assertEquals(1, overrideStorage.getAllOverrides().size());
  }

  @Test
  @DisplayName("Városi és teljes törlés helyesen tisztítja a felülbírálásokat")
  void testClearCityAndClearAllHistory() {
    overrideStorage.saveOverride(
        "https://dh.hu/d1", "https://oc.hu/d2", DedupOverrideType.FORCE_SPLIT, "Debrecen");
    overrideStorage.saveOverride(
        "https://dh.hu/e1", "https://oc.hu/e2", DedupOverrideType.FORCE_MERGE, "Eger");

    ingatlanStorage.clearCityHistory("Debrecen");

    List<DedupOverride> remaining = overrideStorage.getAllOverrides();
    assertEquals(1, remaining.size());
    assertEquals("eger", remaining.get(0).normalizedCity());

    ingatlanStorage.clearHistory();
    assertTrue(overrideStorage.getAllOverrides().isEmpty());
  }

  @Test
  @DisplayName("URL lekérdezés és azonosító alapú törlés")
  void testGetByUrlAndDeleteById() {
    DedupOverride saved =
        overrideStorage.saveOverride(
            "https://dh.hu/a", "https://oc.hu/b", DedupOverrideType.FORCE_SPLIT, "Debrecen");

    assertEquals(1, overrideStorage.getOverridesForUrl("https://oc.hu/b").size());
    assertEquals(1, overrideStorage.getOverridesForUrls(List.of("https://dh.hu/a")).size());
    assertTrue(overrideStorage.deleteOverrideById(saved.id()));
    assertFalse(overrideStorage.deleteOverrideById(saved.id()));
    assertTrue(overrideStorage.getAllOverrides().isEmpty());
  }
}
