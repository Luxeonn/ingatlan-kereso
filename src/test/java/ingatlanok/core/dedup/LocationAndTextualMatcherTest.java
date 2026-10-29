package ingatlanok.core.dedup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.core.dedup.LocationConflictDetector.ExtractedLocation;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PropertyType;

class LocationAndTextualMatcherTest {

  @Test
  @DisplayName("Budapest kerületek kinyerése római számokból, arab számokból, URL-ből és városrész nevekből")
  void testBudapestDistrictExtractionVariants() {
    IngatlanItem itemRoman = new IngatlanItem();
    itemRoman.setTitle("Kiadó lakás Budapest XI. kerület");
    itemRoman.setLocation("Budapest");
    ExtractedLocation locRoman = LocationConflictDetector.extractLocation(itemRoman, "budapest");
    assertEquals(11, locRoman.district());

    IngatlanItem itemArabic = new IngatlanItem();
    itemArabic.setTitle("13. ker. Angyalföldi tégla garzon");
    itemArabic.setLocation("Budapest");
    ExtractedLocation locArabic = LocationConflictDetector.extractLocation(itemArabic, "budapest");
    assertEquals(13, locArabic.district());

    IngatlanItem itemUrl = new IngatlanItem();
    itemUrl.setUrl("https://portal.hu/budapest-14-ker-elado-haz");
    itemUrl.setLocation("Budapest");
    ExtractedLocation locUrl = LocationConflictDetector.extractLocation(itemUrl, "budapest");
    assertEquals(14, locUrl.district());

    IngatlanItem itemName = new IngatlanItem();
    itemName.setTitle("Gazdagréti panorámás lakás");
    itemName.setLocation("Budapest");
    ExtractedLocation locName = LocationConflictDetector.extractLocation(itemName, "budapest");
    assertEquals(11, locName.district());
  }

  @Test
  @DisplayName("Római számok átváltása 1-től 23-ig és érvénytelen értékek kezelése")
  void testRomanNumeralsAll() {
    String[] romans = {
        "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
        "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX",
        "XXI", "XXII", "XXIII"
    };

    for (int i = 0; i < romans.length; i++) {
      IngatlanItem item = new IngatlanItem();
      item.setTitle("Budapest " + romans[i] + ". kerület lakás");
      ExtractedLocation loc = LocationConflictDetector.extractLocation(item, "budapest");
      assertEquals(i + 1, loc.district(), "Római számnak egyeznie kell: " + romans[i]);
    }

    IngatlanItem invalid = new IngatlanItem();
    invalid.setTitle("Budapest XXIV. kerület lakás");
    ExtractedLocation locInv = LocationConflictDetector.extractLocation(invalid, "budapest");
    assertNull(locInv.district());
  }

  @Test
  @DisplayName("Pozitív megerősítés (affirmative match) azonos kerület, utca vagy városrész esetén")
  void testAffirmativeLocationMatch() {
    ExtractedLocation locDist1 = new ExtractedLocation(11, null, null);
    ExtractedLocation locDist2 = new ExtractedLocation(11, null, null);
    assertTrue(LocationConflictDetector.hasAffirmativeMatch(locDist1, locDist2));

    ExtractedLocation locStreet1 = new ExtractedLocation(null, null, "kossuth-lajos");
    ExtractedLocation locStreet2 = new ExtractedLocation(null, null, "kossuth-lajos");
    assertTrue(LocationConflictDetector.hasAffirmativeMatch(locStreet1, locStreet2));

    ExtractedLocation locQuarter1 = new ExtractedLocation(null, "banomkert", null);
    ExtractedLocation locQuarter2 = new ExtractedLocation(null, "banomkert", null);
    assertTrue(LocationConflictDetector.hasAffirmativeMatch(locQuarter1, locQuarter2));

    ExtractedLocation locEmpty1 = new ExtractedLocation(null, null, null);
    ExtractedLocation locEmpty2 = new ExtractedLocation(null, null, null);
    assertFalse(LocationConflictDetector.hasAffirmativeMatch(locEmpty1, locEmpty2));
    assertFalse(LocationConflictDetector.hasAffirmativeMatch(null, locEmpty1));
  }

  @Test
  @DisplayName("LocationConflictDetector null és szélsőérték kezelése")
  void testLocationConflictDetectorNullSafe() {
    assertNotNull(LocationConflictDetector.extractLocation(null, "budapest"));
    assertFalse(LocationConflictDetector.hasConflict(null, null, "budapest"));

    ExtractedLocation loc = new ExtractedLocation(1, "var", "utca");
    assertFalse(LocationConflictDetector.hasConflict(loc, null, "budapest"));
    assertFalse(LocationConflictDetector.hasConflict(null, loc, "budapest"));
  }

  @Test
  @DisplayName("Vidéki városok városrész-kinyerése 'Város, Városrész' formátumból és címszavakból")
  void testProvincialQuarterExtraction() {
    IngatlanItem itemComma = new IngatlanItem();
    itemComma.setLocation("Debrecen, Kertváros");
    ExtractedLocation locComma = LocationConflictDetector.extractLocation(itemComma, "debrecen");
    assertEquals("kertvaros", locComma.quarter());

    IngatlanItem itemTitle = new IngatlanItem();
    itemTitle.setTitle("Nagyerdő szélén eladó ház");
    itemTitle.setLocation("Debrecen");
    ExtractedLocation locTitle = LocationConflictDetector.extractLocation(itemTitle, "debrecen");
    assertEquals("nagyerd", locTitle.quarter());
  }

  @Test
  @DisplayName("Jaccard hasonlóság és kulcsszó-átfedés vizsgálata")
  void testTextualSimilarityMatcherJaccardAndKeywords() {
    Set<String> setA = Set.of("ketgeneracios", "tegla", "tagas", "kert");
    Set<String> setB = Set.of("ketgeneracios", "tegla", "medence");

    double jaccard = TextualSimilarityMatcher.calculateJaccardSimilarity(setA, setB);
    assertEquals(2.0 / 5.0, jaccard, 0.001);

    assertTrue(TextualSimilarityMatcher.hasDistinctiveKeywordOverlap(setA, setB));

    Set<String> setEmpty = Collections.emptySet();
    assertEquals(0.0, TextualSimilarityMatcher.calculateJaccardSimilarity(setA, setEmpty));
    assertEquals(0.0, TextualSimilarityMatcher.calculateJaccardSimilarity(null, setB));
    assertFalse(TextualSimilarityMatcher.hasDistinctiveKeywordOverlap(setA, setEmpty));
    assertFalse(TextualSimilarityMatcher.hasDistinctiveKeywordOverlap(null, null));
  }

  @Test
  @DisplayName("Kétgenerációs és több épületből álló ingatlanok detektálása")
  void testMultiGenerationPatterns() {
    IngatlanItem item1 = new IngatlanItem();
    item1.setTitle("2 generációs családi ház");
    assertTrue(TextualSimilarityMatcher.isMultiGeneration(item1));

    IngatlanItem item2 = new IngatlanItem();
    item2.setTitle("Kétgenerációs ingatlan külön bejárattal");
    assertTrue(TextualSimilarityMatcher.isMultiGeneration(item2));

    IngatlanItem item3 = new IngatlanItem();
    item3.setTitle("2 épületből álló ház");
    assertTrue(TextualSimilarityMatcher.isMultiGeneration(item3));

    IngatlanItem item4 = new IngatlanItem();
    item4.setTitle("Hagyományos családi ház");
    assertFalse(TextualSimilarityMatcher.isMultiGeneration(item4));
    assertFalse(TextualSimilarityMatcher.isMultiGeneration(null));
  }

  @Test
  @DisplayName("SemanticConflictDetector emelet és tanya ellentmondások tesztelése")
  void testSemanticConflictFloorAndTanya() {
    IngatlanItem ground = new IngatlanItem();
    ground.setPropertyType(PropertyType.LAKAS);
    ground.setTitle("Földszinti akadálymentes lakás");

    IngatlanItem top = new IngatlanItem();
    top.setPropertyType(PropertyType.LAKAS);
    top.setTitle("Tetőtéri penthouse lakás");

    assertTrue(SemanticConflictDetector.hasConflict(ground, top));

    IngatlanItem tanya = new IngatlanItem();
    tanya.setPropertyType(PropertyType.HAZ);
    tanya.setTitle("Tanya 2 hektáros birtokkal");

    IngatlanItem iker = new IngatlanItem();
    iker.setPropertyType(PropertyType.HAZ);
    iker.setTitle("Ikerház garázzsal");

    assertTrue(SemanticConflictDetector.hasConflict(tanya, iker));
    assertFalse(SemanticConflictDetector.hasConflict(null, null));
  }
}
