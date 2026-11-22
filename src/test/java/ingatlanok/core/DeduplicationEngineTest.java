package ingatlanok.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.DedupOverride;
import ingatlanok.model.DedupOverrideType;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;
import ingatlanok.persistence.DedupOverrideStorage;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.testsupport.TestFixtures;

class DeduplicationEngineTest {

  @Test
  @DisplayName("Deduplikáció URL alapján a sorrend megőrzésével")
  void testDeduplicateByUrl() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();

    IngatlanItem item1 =
        new IngatlanItem(
            "1", "dh", "http://1", "Első", "50 M Ft", "Debrecen", "", "2026-10-14", false);
    IngatlanItem item2 =
        new IngatlanItem(
            "2", "dh", "http://2", "Második", "60 M Ft", "Debrecen", "", "2026-10-14", false);
    IngatlanItem item3 =
        new IngatlanItem(
            "3",
            "dh",
            "http://1",
            "Első duplikátum",
            "50 M Ft",
            "Debrecen",
            "",
            "2026-10-14",
            false);

    List<IngatlanItem> deduped = engine.deduplicateByUrl(List.of(item1, item2, item3));
    assertEquals(2, deduped.size());
    assertEquals("http://1", deduped.get(0).getUrl());
    assertEquals("Első", deduped.get(0).getTitle());
    assertEquals("http://2", deduped.get(1).getUrl());
  }

  @Test
  @DisplayName("Baseline snapshot esetén minden találat isNew = false értéket kap")
  void testApplyNewStatusBaseline() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();

    IngatlanItem item =
        new IngatlanItem(
            "1", "dh", "http://1", "Új városi", "50 M Ft", "Eger", "", "2026-10-14", true);
    engine.applyNewStatus("Eger", List.of(item), true);

    assertFalse(item.isNew());
  }

  @Test
  @DisplayName("Ismert város esetén a tároló alapján kap új státuszt")
  void testApplyNewStatusKnownCity() {
    ListingStorage mockStorage =
        new ListingStorage() {
          @Override
          public void saveCityItems(String city, Collection<IngatlanItem> items) {}

          @Override
          public void saveAll(Collection<IngatlanItem> items) {}

          @Override
          public List<IngatlanItem> getItemsByCity(String city) {
            return List.of();
          }

          @Override
          public List<IngatlanItem> getAll() {
            return List.of();
          }

          @Override
          public boolean hasCityDatabase(String city) {
            return true;
          }

          @Override
          public boolean isNewUrl(String city, String url) {
            return "http://new".equals(url);
          }

          @Override
          public boolean containsUrl(String url) {
            return false;
          }

          @Override
          public boolean markAsViewed(String url) {
            return false;
          }

          @Override
          public boolean isViewed(String url) {
            return false;
          }

          @Override
          public Set<String> getAllUrls() {
            return Set.of();
          }

          @Override
          public List<ingatlanok.model.TrackedCityInfo> getTrackedCitiesInfo() {
            return List.of();
          }

          @Override
          public void clearCityHistory(String city) {}

          @Override
          public void clearHistory() {}

          @Override
          public int getTotalCount() {
            return 0;
          }

          @Override
          public String getLastRunTime() {
            return null;
          }
        };

    DeduplicationEngine engine = TestFixtures.deduplicationEngine(mockStorage);
    IngatlanItem item1 =
        new IngatlanItem(
            "1", "dh", "http://new", "Új tétel", "50 M Ft", "Debrecen", "", "2026-10-14", false);
    IngatlanItem item2 =
        new IngatlanItem(
            "2", "dh", "http://old", "Régi tétel", "50 M Ft", "Debrecen", "", "2026-10-14", false);

    engine.applyNewStatus("Debrecen", List.of(item1, item2), false);

    assertTrue(item1.isNew());
    assertFalse(item2.isNew());
  }

  @Test
  @DisplayName("Kereszt-portál klaszterezés azonos fizikai ingatlan esetén és legolcsóbb forrás kiemelése")
  void testMultiPortalClusteringMatchesAndLowestPriceSelected() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();

    IngatlanItem dh = createItem("dh-1", "Duna House", "http://dh/1", "DH Családi Lakás",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 80_000_000L, "ACTIVE");
    dh.setLocation("Debrecen, Belváros");
    IngatlanItem oc = createItem("oc-2", "Otthon Centrum", "http://oc/2", "OC Belvárosi Lakás",
        "Debrecen", PropertyType.LAKAS, 104.0, 3.0, 75_000_000L, "ACTIVE");
    oc.setLocation("Debrecen, Belváros");
    IngatlanItem jf = createItem("jf-3", "Jófogás", "http://jf/3", "Jófogás 3 szobás",
        "Debrecen", PropertyType.LAKAS, 102.0, 3.5, 78_000_000L, "ACTIVE");
    jf.setLocation("Debrecen, Belváros");

    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(dh, oc, jf));

    assertEquals(1, clustered.size());
    IngatlanItem primary = clustered.get(0);
    assertEquals(3, primary.getSourcesCount());
    assertEquals(3, primary.getSourceListings().size());
    // Lowest active price is 75M from Otthon Centrum
    assertEquals("http://oc/2", primary.getUrl());
    assertEquals("OC Belvárosi Lakás", primary.getTitle());
    assertEquals(75_000_000L, primary.getPriceInfo().getAmountHuf());
    // Sources sorted by price ascending: 75M, 78M, 80M
    assertEquals(75_000_000L, primary.getSourceListings().get(0).getPriceHuf());
    assertEquals(78_000_000L, primary.getSourceListings().get(1).getPriceHuf());
    assertEquals(80_000_000L, primary.getSourceListings().get(2).getPriceHuf());
  }

  @Test
  @DisplayName("Alapterület tolerancia határ: <=5% csoportosítva, >5% elutasítva")
  void testAreaToleranceBoundary() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();

    // 100m2 és 105m2: diff 5, max 105 -> 5/105 = 0.0476 <= 0.05 -> klaszterezve
    IngatlanItem itemA = createItem("1", "dh", "http://1", "Lakás A",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 80_000_000L, "ACTIVE");
    itemA.setLocation("Debrecen, Belváros");
    IngatlanItem itemB = createItem("2", "oc", "http://2", "Lakás B",
        "Debrecen", PropertyType.LAKAS, 105.0, 3.0, 80_000_000L, "ACTIVE");
    itemB.setLocation("Debrecen, Belváros");

    List<IngatlanItem> resultWithin5 = engine.deduplicateMultiSignal(List.of(itemA, itemB));
    assertEquals(1, resultWithin5.size());
    assertEquals(2, resultWithin5.get(0).getSourcesCount());

    // 100m2 és 106m2: diff 6, max 106 -> 6/106 = 0.0566 > 0.05 -> konfliktus védelem
    IngatlanItem itemC = createItem("3", "oc", "http://3", "Lakás C",
        "Debrecen", PropertyType.LAKAS, 106.0, 3.0, 80_000_000L, "ACTIVE");
    itemC.setLocation("Debrecen, Belváros");

    List<IngatlanItem> resultOver5 = engine.deduplicateMultiSignal(List.of(itemA, itemC));
    assertEquals(2, resultOver5.size());
  }

  @Test
  @DisplayName("Szobaszám konfliktusvédelem: >=1 szoba eltérés esetén nem vonható össze")
  void testRoomCountConflictGuard() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();

    // 3 szoba vs 4 szoba -> diff >= 1.0 -> elutasítva
    IngatlanItem item3 = createItem("1", "dh", "http://1", "3 szobás",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 80_000_000L, "ACTIVE");
    item3.setLocation("Debrecen, Belváros");
    IngatlanItem item4 = createItem("2", "oc", "http://2", "4 szobás",
        "Debrecen", PropertyType.LAKAS, 100.0, 4.0, 80_000_000L, "ACTIVE");
    item4.setLocation("Debrecen, Belváros");

    List<IngatlanItem> resultDiff1 = engine.deduplicateMultiSignal(List.of(item3, item4));
    assertEquals(2, resultDiff1.size());

    // 3 szoba vs 3.5 szoba -> diff 0.5 <= 0.5 -> engedélyezve
    IngatlanItem item35 = createItem("3", "jf", "http://3", "3 és fél szobás",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.5, 80_000_000L, "ACTIVE");
    item35.setLocation("Debrecen, Belváros");

    List<IngatlanItem> resultDiffHalf = engine.deduplicateMultiSignal(List.of(item3, item35));
    assertEquals(1, resultDiffHalf.size());
  }

  @Test
  @DisplayName("Ársáv érvényesség: <=20% eltérés megengedett, >20% eltérés elutasítva")
  void testPriceBandSanity() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();

    // 100M vs 82M: diff 18M / 100M = 0.18 <= 0.20 -> csoportosítva
    IngatlanItem item100 = createItem("1", "dh", "http://1", "Lakás 100M",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 100_000_000L, "ACTIVE");
    item100.setLocation("Debrecen, Belváros");
    IngatlanItem item82 = createItem("2", "oc", "http://2", "Lakás 82M",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 82_000_000L, "ACTIVE");
    item82.setLocation("Debrecen, Belváros");

    List<IngatlanItem> resultWithin20 = engine.deduplicateMultiSignal(List.of(item100, item82));
    assertEquals(1, resultWithin20.size());

    // 100M vs 75M: diff 25M / 100M = 0.25 > 0.20 -> külön marad
    IngatlanItem item75 = createItem("3", "jf", "http://3", "Lakás 75M",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 75_000_000L, "ACTIVE");
    item75.setLocation("Debrecen, Belváros");

    List<IngatlanItem> resultOver20 = engine.deduplicateMultiSignal(List.of(item100, item75));
    assertEquals(2, resultOver20.size());
  }

  @Test
  @DisplayName("Eltérő kanonikus típusok és nem alaptípusok (EGYEB_ISMERETLEN) nem vonhatók össze")
  void testPropertyTypeRejection() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();

    IngatlanItem lakas = createItem("1", "dh", "http://1", "Lakás",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 80_000_000L, "ACTIVE");
    IngatlanItem haz = createItem("2", "oc", "http://2", "Ház",
        "Debrecen", PropertyType.HAZ, 100.0, 3.0, 80_000_000L, "ACTIVE");

    List<IngatlanItem> diffTypes = engine.deduplicateMultiSignal(List.of(lakas, haz));
    assertEquals(2, diffTypes.size());

    // EGYEB_ISMERETLEN nem képez klasztert
    IngatlanItem other1 = createItem("3", "dh", "http://3", "Egyéb 1",
        "Debrecen", PropertyType.EGYEB_ISMERETLEN, 100.0, 3.0, 80_000_000L, "ACTIVE");
    IngatlanItem other2 = createItem("4", "oc", "http://4", "Egyéb 2",
        "Debrecen", PropertyType.EGYEB_ISMERETLEN, 100.0, 3.0, 80_000_000L, "ACTIVE");

    List<IngatlanItem> unknownTypes = engine.deduplicateMultiSignal(List.of(other1, other2));
    assertEquals(2, unknownTypes.size());
  }

  @Test
  @DisplayName("TELEK típusok összevonása alapterület és ár alapján")
  void testTelekGrouping() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();

    IngatlanItem telek1 = createItem("1", "dh", "http://t1", "Építési telek",
        "Debrecen", PropertyType.TELEK, 800.0, 0.0, 25_000_000L, "ACTIVE");
    telek1.setLocation("Debrecen, Kertváros");
    IngatlanItem telek2 = createItem("2", "oc", "http://t2", "Kiváló telek",
        "Debrecen", PropertyType.TELEK, 820.0, 0.0, 24_000_000L, "ACTIVE");
    telek2.setLocation("Debrecen, Kertváros");

    List<IngatlanItem> clusteredTelek = engine.deduplicateMultiSignal(List.of(telek1, telek2));
    assertEquals(1, clusteredTelek.size());
    assertEquals(2, clusteredTelek.get(0).getSourcesCount());
    assertEquals(24_000_000L, clusteredTelek.get(0).getPriceInfo().getAmountHuf());
  }

  @Test
  @DisplayName("Eltérő városok nem vonhatók össze, de kis- és nagybetű normalizálás működik")
  void testCityNormalizationAndSeparation() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();

    IngatlanItem deb = createItem("1", "dh", "http://1", "Debreceni lakás",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 80_000_000L, "ACTIVE");
    IngatlanItem eger = createItem("2", "oc", "http://2", "Egri lakás",
        "Eger", PropertyType.LAKAS, 100.0, 3.0, 80_000_000L, "ACTIVE");

    List<IngatlanItem> diffCity = engine.deduplicateMultiSignal(List.of(deb, eger));
    assertEquals(2, diffCity.size());

    IngatlanItem debLower = createItem("3", "oc", "http://3", "debreceni kisbetűs",
        "debrecen", PropertyType.LAKAS, 100.0, 3.0, 80_000_000L, "ACTIVE");

    List<IngatlanItem> sameCityNormalized = engine.deduplicateMultiSignal(List.of(deb, debLower));
    assertEquals(1, sameCityNormalized.size());
    assertEquals(2, sameCityNormalized.get(0).getSourcesCount());
  }

  @Test
  @DisplayName("Inaktív forrás helyett az aktív legalacsonyabb árú forrás válik elsődlegessé")
  void testLowestActivePriceSelectedOverInactive() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();

    IngatlanItem dhInactive = createItem("1", "dh", "http://dh/inactive", "DH Inaktív Olcsó",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 65_000_000L, "INACTIVE");
    dhInactive.setLocation("Debrecen, Belváros");
    IngatlanItem ocActive = createItem("2", "oc", "http://oc/active", "OC Aktív Reális",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 75_000_000L, "ACTIVE");
    ocActive.setLocation("Debrecen, Belváros");
    IngatlanItem jfActive = createItem("3", "jf", "http://jf/active", "JF Aktív Drágább",
        "Debrecen", PropertyType.LAKAS, 100.0, 3.0, 80_000_000L, "ACTIVE");
    jfActive.setLocation("Debrecen, Belváros");

    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(dhInactive, ocActive, jfActive));
    assertEquals(1, clustered.size());
    IngatlanItem primary = clustered.get(0);
    assertEquals("http://oc/active", primary.getUrl());
    assertEquals(75_000_000L, primary.getPriceInfo().getAmountHuf());
  }

  @Test
  @DisplayName("Egyedi forrású hirdetés sourcesCount = 1 és pontosan 1 sourceListing elemet kap")
  void testSingleSourceListingHandling() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();

    IngatlanItem single = createItem("1", "dh", "http://single", "Egyedi hirdetés",
        "Debrecen", PropertyType.LAKAS, 60.0, 2.0, 50_000_000L, "ACTIVE");

    List<IngatlanItem> result = engine.deduplicateMultiSignal(List.of(single));
    assertEquals(1, result.size());
    assertEquals(1, result.get(0).getSourcesCount());
    assertEquals(1, result.get(0).getSourceListings().size());
    assertEquals("http://single", result.get(0).getSourceListings().get(0).getUrl());
  }

  @Test
  @DisplayName("Null és üres kollekciók kezelése")
  void testNullAndEmptyHandling() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();
    assertTrue(engine.deduplicateMultiSignal(null).isEmpty());
    assertTrue(engine.deduplicateMultiSignal(List.of()).isEmpty());
    assertEquals(0, engine.deduplicate(List.of()).size());
  }

  @Test
  @DisplayName("#426: Legjobb egyezés alapú klaszterezés megakadályozza a 24.49M vs 28.9M keresztbe párosítást")
  void testIssue426BestMatchClusteringPreventsCrossPricePairing() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();

    IngatlanItem k28 = createItem("k28", "Költözzbe", "https://koltozzbe.hu/h10978033",
        "Eladó panellakás, Miskolcon 28.9 M Ft, 1+1 szobás",
        "Miskolc", PropertyType.LAKAS, 37.0, 1.0, 28_900_000L, "ACTIVE");
    IngatlanItem k24 = createItem("k24", "Költözzbe", "https://koltozzbe.hu/h10946115",
        "Eladó panellakás, Miskolcon, Szentgyörgy úton 24.49 M Ft",
        "Miskolc", PropertyType.LAKAS, 37.0, 1.0, 24_490_000L, "ACTIVE");
    IngatlanItem j24 = createItem("j24", "Jófogás Ingatlan", "https://ingatlan.jofogas.hu/161990027.htm",
        "Eladó panel lakás Miskolc, Avas Szentgyörgy utcában!",
        "Miskolc", PropertyType.LAKAS, 37.0, 1.0, 24_490_000L, "ACTIVE");
    IngatlanItem j28 = createItem("j28", "Jófogás Ingatlan", "https://ingatlan.jofogas.hu/162625428.htm",
        "Eladó Lakás, Miskolc 28.900.000 Ft",
        "Miskolc", PropertyType.LAKAS, 37.0, 1.0, 28_900_000L, "ACTIVE");

    // Kedvezőtlen bejárási sorrendben is az azonos árú/utcájú pároknak kell összeállniuk
    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(k28, j24, k24, j28));

    assertEquals(2, clustered.size(), "Két külön 2-forrásos klaszternek kell létrejönnie");
    for (IngatlanItem cluster : clustered) {
      assertEquals(2, cluster.getSourcesCount());
      long firstPrice = cluster.getSourceListings().get(0).getPriceHuf();
      long secondPrice = cluster.getSourceListings().get(1).getPriceHuf();
      assertEquals(firstPrice, secondPrice,
          "Azonos árú és utcájú pár helyett nem köthető össze 24.49M és 28.9M");
    }
  }

  @Test
  @DisplayName("Ragozott utcanév egyezés, prózazaj elutasítása és intraportál sabloncím védelem")
  void testInflectedStreetsAndIntraPortalGenericTitleGuard() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();

    IngatlanItem miskolcA = createItem("ma", "Költözzbe", "https://koltozzbe.hu/h1",
        "Eladó panellakás, Miskolcon, Szentgyörgy úton 24.49 M Ft",
        "Miskolc", PropertyType.LAKAS, 37.0, 1.0, 24_490_000L, "ACTIVE");
    IngatlanItem miskolcB = createItem("mb", "Jófogás", "https://ingatlan.jofogas.hu/1.htm",
        "Eladó panel lakás Miskolc, Avas Szentgyörgy utcában!",
        "Miskolc", PropertyType.LAKAS, 37.0, 1.0, 24_490_000L, "ACTIVE");

    var locA = ingatlanok.core.dedup.LocationConflictDetector.extractLocation(miskolcA, "miskolc");
    var locB = ingatlanok.core.dedup.LocationConflictDetector.extractLocation(miskolcB, "miskolc");
    assertTrue(ingatlanok.core.dedup.LocationConflictDetector.hasSameStreet(locA, locB),
        "Ragozott 'úton' és 'utcában' utcanevek azonosnak minősülnek");
    assertFalse(ingatlanok.core.dedup.LocationConflictDetector.hasConflict(locA, locB, "miskolc"));

    IngatlanItem proseNoise = createItem("pn", "Otthon Centrum", "https://oc.hu/1",
        "Eladó családi ház egyirányú utcában, az út jelenleg föld út",
        "Debrecen", PropertyType.HAZ, 120.0, 4.0, 95_000_000L, "ACTIVE");
    var proseLoc = ingatlanok.core.dedup.LocationConflictDetector.extractLocation(proseNoise, "debrecen");
    assertFalse(proseLoc.hasStreet(), "Prózában lévő 'egyirányú utcában' / 'föld út' nem utcanév");

    IngatlanItem ibGeneric1 = createItem("ib1", "ingatlanbazar", "https://ingatlanbazar.hu/1",
        "Debrecen, Debreceni kistérség, ingatlan, eladó, ház, 100 m2",
        "Debrecen", PropertyType.HAZ, 100.0, 3.0, 95_000_000L, "ACTIVE");
    IngatlanItem ibGeneric2 = createItem("ib2", "ingatlanbazar", "https://ingatlanbazar.hu/2",
        "Debrecen, Debreceni kistérség, ingatlan, eladó, ház, 100 m2",
        "Debrecen", PropertyType.HAZ, 100.0, 3.0, 95_000_000L, "ACTIVE");
    assertFalse(engine.isMatch(ibGeneric1, ibGeneric2),
        "Azonos portálon belüli sabloncímű eltérő ingatlanok nem olvaszthatók össze");
  }

  @Test
  @DisplayName("#437: Sabloncímű és eltérő fotójú intraportál duplikátumok összevonása Miskolcon")
  void testIssue437MiskolcTemplateTitledIntraPortalAndCrossPortalClustering() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();

    // 2. emeleti 1.5 szobás panellakás: több Költözzbe + 1 Ingatlan.com eltérő CDN képekkel
    IngatlanItem k2FloorA = createItem("k2a", "Költözzbe", "https://koltozzbe.hu/ingatlan/h10880350",
        "Eladó panellakás, Miskolcon 25.9 M Ft, 1+1 szobás",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.5, 25_900_000L, "ACTIVE");
    k2FloorA.setLocation("Miskolc, 2. emelet");
    k2FloorA.setImageUrl("https://cdn.koltozzbe.hu/img/photo-a.jpg");

    IngatlanItem k2FloorB = createItem("k2b", "Költözzbe", "https://koltozzbe.hu/ingatlan/h10882116",
        "Eladó panellakás, Miskolcon 25.9 M Ft, 1+1 szobás",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.5, 25_900_000L, "ACTIVE");
    k2FloorB.setLocation("Miskolc, 2. emelet");
    k2FloorB.setImageUrl("https://cdn.koltozzbe.hu/img/photo-b.jpg");

    IngatlanItem k2FloorC = createItem("k2c", "Költözzbe", "https://koltozzbe.hu/ingatlan/h10955540",
        "Eladó panellakás, Miskolcon 25.9 M Ft, 1+1 szobás",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.5, 25_900_000L, "ACTIVE");
    k2FloorC.setLocation("Miskolc, 2. emelet");
    k2FloorC.setImageUrl("https://cdn.koltozzbe.hu/img/photo-c.jpg");

    IngatlanItem ic2Floor = createItem("ic2", "Ingatlan.com", "https://ingatlan.com/35129856",
        "Eladó 35 nm-es panellakás Miskolc",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.5, 25_900_000L, "ACTIVE");
    ic2Floor.setLocation("Miskolc");
    ic2Floor.setImageUrl("https://cdn.ingatlan.com/img/photo-ic.jpg");

    // 10. emeleti 1.0 szobás panellakás: több Költözzbe + több Jófogás eltérő fotókkal
    IngatlanItem k10FloorA = createItem("k10a", "Költözzbe", "https://koltozzbe.hu/ingatlan/h10910001",
        "Eladó panellakás, Miskolcon 25.9 M Ft, 1 szobás",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.0, 25_900_000L, "ACTIVE");
    k10FloorA.setLocation("Miskolc, 10. emelet");
    k10FloorA.setImageUrl("https://cdn.koltozzbe.hu/img/photo-10a.jpg");

    IngatlanItem k10FloorB = createItem("k10b", "Költözzbe", "https://koltozzbe.hu/ingatlan/h10910002",
        "Eladó panellakás, Miskolcon 25.9 M Ft, 1 szobás",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.0, 25_900_000L, "ACTIVE");
    k10FloorB.setLocation("Miskolc, 10. emelet");
    k10FloorB.setImageUrl("https://cdn.koltozzbe.hu/img/photo-10b.jpg");

    IngatlanItem jf10FloorA = createItem("jf10a", "Jófogás", "https://ingatlan.jofogas.hu/1111111.htm",
        "35 nm-es lakás eladó Miskolc",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.0, 25_900_000L, "ACTIVE");
    jf10FloorA.setLocation("Miskolc");
    jf10FloorA.setImageUrl("https://img.jofogas.hu/photo-jf1.jpg");

    IngatlanItem jf10FloorB = createItem("jf10b", "Jófogás", "https://ingatlan.jofogas.hu/2222222.htm",
        "35 nm-es lakás eladó Miskolc",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.0, 25_900_000L, "ACTIVE");
    jf10FloorB.setLocation("Miskolc");
    jf10FloorB.setImageUrl("https://img.jofogas.hu/photo-jf2.jpg");

    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(
        k2FloorA, k10FloorA, jf10FloorA, k2FloorB, ic2Floor, jf10FloorB, k10FloorB, k2FloorC));

    assertEquals(2, clustered.size(),
        "A 2. emeleti (1.5 szobás) és a 10. emeleti (1.0 szobás) lakásnak pontosan 2 klaszterbe kell összeolvadnia");
    for (IngatlanItem cluster : clustered) {
      assertEquals(4, cluster.getSourcesCount(),
          "Mindkét klaszternek mind a 4 hirdetési forrást tartalmaznia kell");
    }
  }

  @Test
  @DisplayName("#437: Explicit emeletszám-konfliktus megakadályozza eltérő emeleti lakások hamis összevonását")
  void testIssue437ExplicitFloorConflictPreventsFalseMerge() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();

    IngatlanItem flat2ndFloor = createItem("f2", "Költözzbe", "https://koltozzbe.hu/h2",
        "Eladó panellakás, Miskolcon 25.9 M Ft, 1+1 szobás",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.5, 25_900_000L, "ACTIVE");
    flat2ndFloor.setLocation("Miskolc, 2. emelet");

    IngatlanItem flat3rdFloor = createItem("f3", "Költözzbe", "https://koltozzbe.hu/h3",
        "Eladó panellakás, Miskolcon 25.9 M Ft, 1+1 szobás",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.5, 25_900_000L, "ACTIVE");
    flat3rdFloor.setLocation("Miskolc, 3. emelet");

    IngatlanItem flat10thFloor = createItem("f10", "Otthon Centrum", "https://oc.hu/h10",
        "Eladó panellakás, Miskolcon 25.9 M Ft, 1+1 szobás",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.5, 25_900_000L, "ACTIVE");
    flat10thFloor.setLocation("Miskolc, 10. emelet");

    IngatlanItem flatGroundFloor = createItem("f0", "Duna House", "https://dh.hu/h0",
        "Eladó panellakás, Miskolcon 25.9 M Ft, 1+1 szobás",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.5, 25_900_000L, "ACTIVE");
    flatGroundFloor.setLocation("Miskolc, földszint");

    assertFalse(engine.isMatch(flat2ndFloor, flat3rdFloor), "2. emelet és 3. emelet nem vonható össze");
    assertFalse(engine.isMatch(flat2ndFloor, flat10thFloor), "2. emelet és 10. emelet nem vonható össze");
    assertFalse(engine.isMatch(flat2ndFloor, flatGroundFloor), "2. emelet és földszint nem vonható össze");
    assertEquals(4, engine.deduplicateMultiSignal(
        List.of(flat2ndFloor, flat3rdFloor, flat10thFloor, flatGroundFloor)).size());

    IngatlanItem fourStoryBuilding2ndFloor = createItem("f2-stories", "Jófogás", "https://ingatlan.jofogas.hu/f2.htm",
        "4 emeletes társasházban, 2. emeleten eladó panellakás",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.5, 25_900_000L, "ACTIVE");
    fourStoryBuilding2ndFloor.setLocation("Miskolc, Avas");

    assertTrue(engine.isMatch(flat2ndFloor, fourStoryBuilding2ndFloor),
        "A '4 emeletes' épületszint-szám és '2. emelet' vs 'Avas' nem generálhat álkonfliktust");
  }

  @Test
  @DisplayName("#440: Pozitív bizonyíték nélküli kereszt-portál párosítás elutasítása Miskolc 35 m2 panellakásoknál")
  void testIssue440PreventsFalseCrossPortalClusteringWithoutPositiveEvidence() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();

    IngatlanItem ic = createItem("ic-35573255", "Ingatlan.com", "https://ingatlan.com/35573255",
        "Miskolc, Győri kapu", "Miskolc", PropertyType.LAKAS, 35.0, 1.5, 25_000_000L, "ACTIVE");
    ic.setLocation("Miskolc, Győri kapu");

    IngatlanItem koltozzbe = createItem("k-10627044", "Költözzbe",
        "https://koltozzbe.hu/elado-panellakas-miskolc/h10627044",
        "Eladó panellakás, Miskolcon 25 M Ft, 1+1 szobás",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.5, 25_000_000L, "ACTIVE");

    IngatlanItem jofogas = createItem("jf-161634453", "Jófogás Ingatlan",
        "https://ingatlan.jofogas.hu/borsod_abauj_zemplen/Elado_1_1_felszobas_lakas___Miskolc__Gyori_kapu_161634453.htm",
        "Eladó 1+1 félszobás lakás - Miskolc, Győri kapu",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.0, 25_900_000L, "ACTIVE");
    jofogas.setLocation("Miskolc, Győri kapu");

    assertFalse(engine.isMatch(koltozzbe, jofogas),
        "Generikus lokáció nélküli 25 M Ft-os és eltérő szobaszámú 25.9 M Ft-os hirdetés nem olvadhat össze");

    List<IngatlanItem> trioResult = engine.deduplicateMultiSignal(List.of(ic, koltozzbe, jofogas));
    assertTrue(trioResult.size() >= 2,
        "A 3 miskolci hirdetés nem olvadhat össze egyetlen hamis klaszterbe");

    // Eltérő árú (25M, 26.2M, 27.5M), pozitív lokáció/kép nélküli 35 m2-es panellakások külön maradnak
    IngatlanItem panel25 = createItem("p1", "Költözzbe", "https://koltozzbe.hu/p1",
        "Eladó panellakás, Miskolcon 25 M Ft, 1+1 szobás",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.5, 25_000_000L, "ACTIVE");
    IngatlanItem panel262 = createItem("p2", "Jófogás Ingatlan", "https://ingatlan.jofogas.hu/p2.htm",
        "Eladó panellakás Miskolc",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.5, 26_200_000L, "ACTIVE");
    IngatlanItem panel275 = createItem("p3", "Duna House", "https://dh.hu/p3",
        "Miskolc eladó lakás 35 nm",
        "Miskolc", PropertyType.LAKAS, 36.0, 1.5, 27_500_000L, "ACTIVE");

    assertFalse(engine.isMatch(panel25, panel262), "4.6% áreltérés pozitív bizonyíték nélkül nem vonható össze");
    assertFalse(engine.isMatch(panel25, panel275), "9.1% áreltérés pozitív bizonyíték nélkül nem vonható össze");
    assertFalse(engine.isMatch(panel262, panel275), "4.7% áreltérés pozitív bizonyíték nélkül nem vonható össze");
    assertEquals(3, engine.deduplicateMultiSignal(List.of(panel25, panel262, panel275)).size());
  }

  @Test
  @DisplayName("#440: Pozitív utca- vagy képegyezéssel a ±20% ársávon belüli kereszt-portál klaszterezés megmarad")
  void testIssue440AllowsCrossPortalMergeWithPositiveEvidenceWithinPriceBand() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();

    IngatlanItem streetA = createItem("s1", "Duna House", "https://dh.hu/s1",
        "Miskolc, Szentgyörgy utca felújított lakás",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.5, 25_000_000L, "ACTIVE");
    IngatlanItem streetB = createItem("s2", "Otthon Centrum", "https://oc.hu/s2",
        "Szentgyörgy utcában eladó 1.5 szobás panellakás",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.5, 27_500_000L, "ACTIVE");

    assertTrue(engine.isMatch(streetA, streetB),
        "Azonos utcanévvel a 10% áreltérés továbbra is összevonható");

    IngatlanItem imgA = createItem("i1", "Költözzbe", "https://koltozzbe.hu/i1",
        "Eladó panellakás, Miskolcon 25 M Ft, 1+1 szobás",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.5, 25_000_000L, "ACTIVE");
    imgA.setImageUrl("https://cdn.example.com/flat-42.jpg");
    IngatlanItem imgB = createItem("i2", "Jófogás Ingatlan", "https://ingatlan.jofogas.hu/i2.htm",
        "Eladó lakás Miskolc",
        "Miskolc", PropertyType.LAKAS, 35.0, 1.5, 27_500_000L, "ACTIVE");
    imgB.setImageUrl("https://cdn.example.com/flat-42.jpg");

    assertTrue(engine.isMatch(imgA, imgB),
        "Azonos képpel a 10% áreltérés továbbra is összevonható");
  }

  @Test
  @DisplayName("#450: Hajdúszoboszló 11 hirdetésből álló multi-portál és intra-portál ikerház/családi ház klaszter")
  void testIssue450HajduszoboszloKoltozzbeAndMultiPortalHouseConsolidation() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();
    IngatlanItem k140a = createHouseWithImg("k1", "Költözzbe", "https://koltozzbe.hu/ingatlan/k1",
        "Eladó ikerház, Hajdúszoboszlón 109 M Ft, 3+1 szobás",
        140.0, 3.5, 109_000_000L, "https://cdn.koltozzbe.hu/img-k1.jpg");
    IngatlanItem k140b = createHouseWithImg("k2", "Költözzbe", "https://koltozzbe.hu/ingatlan/k2",
        "Eladó ikerház, Hajdúszoboszlón 109 M Ft, 3+1 szobás",
        140.0, 3.5, 109_000_000L, "https://cdn.koltozzbe.hu/img-k2.jpg");
    IngatlanItem k140c = createHouseWithImg("k3", "Költözzbe", "https://koltozzbe.hu/ingatlan/k3",
        "Eladó ikerház, Hajdúszoboszlón 109 M Ft, 3+1 szobás",
        140.0, 3.5, 109_000_000L, "https://cdn.koltozzbe.hu/img-k3.jpg");
    IngatlanItem k140d = createHouseWithImg("k4", "Költözzbe", "https://koltozzbe.hu/ingatlan/k4",
        "Eladó ikerház, Hajdúszoboszlón 109 M Ft, 3+1 szobás",
        140.0, 3.5, 109_000_000L, "https://cdn.koltozzbe.hu/img-k4.jpg");
    IngatlanItem k140e = createHouseWithImg("k5", "Költözzbe", "https://koltozzbe.hu/ingatlan/k5",
        "Eladó ikerház, Hajdúszoboszlón 109 M Ft, 3+1 szobás",
        140.0, 3.5, 109_000_000L, "https://cdn.koltozzbe.hu/img-k5.jpg");
    IngatlanItem k132a = createHouseWithImg("k6", "Költözzbe", "https://koltozzbe.hu/ingatlan/k6",
        "Eladó családi ház, Hajdúszoboszlón 109 M Ft, 4 szobás",
        132.0, 4.0, 109_000_000L, "https://cdn.koltozzbe.hu/img-k6.jpg");
    IngatlanItem k132b = createHouseWithImg("k7", "Költözzbe", "https://koltozzbe.hu/ingatlan/k7",
        "Eladó családi ház, Hajdúszoboszlón 109 M Ft, 4 szobás",
        132.0, 4.0, 109_000_000L, "https://cdn.koltozzbe.hu/img-k7.jpg");
    IngatlanItem k132c = createHouseWithImg("k8", "Költözzbe", "https://koltozzbe.hu/ingatlan/k8",
        "Eladó családi ház, Hajdúszoboszlón 109 M Ft, 4 szobás",
        132.0, 4.0, 109_000_000L, "https://cdn.koltozzbe.hu/img-k8.jpg");
    IngatlanItem k132d = createHouseWithImg("k9", "Költözzbe", "https://koltozzbe.hu/ingatlan/k9",
        "Eladó családi ház, Hajdúszoboszlón 109 M Ft, 4 szobás",
        132.0, 4.0, 109_000_000L, "https://cdn.koltozzbe.hu/img-k9.jpg");
    IngatlanItem dh = createHouseWithImg("dh1", "Duna House", "https://dh.hu/ingatlan/HZ080885",
        "4200 Hajdúszoboszló Ikerház a fürdőtől 300 méterre HZ080885",
        132.0, 4.0, 109_000_000L, "https://cdn.dh.hu/dh-1.jpg");
    IngatlanItem ib = createHouseWithImg("ib1", "Ingatlanbazár",
        "https://www.ingatlanbazar.com/ingatlan-Elado-haz-Hajduszoboszlo-Furdonegyed-Hoforras-utca/user-id-9/1",
        "Hajdúszoboszló, Fürdőnegyed - Ikerház a fürdőtől 300 méterre",
        132.0, 4.0, 109_000_000L, "https://cdn.ib.hu/ib-1.jpg");

    assertTrue(engine.isMatch(k140a, k132a), "Intra-portál 140m2 ikerház és 132m2 családi ház összevonandó");
    assertTrue(engine.isMatch(k140a, dh), "Költözzbe 140m2/3.5 és DH 132m2/4.0 azonos áron összevonandó");
    assertTrue(engine.isMatch(dh, ib), "DH és Ingatlanbazár 132m2 ikerház összevonandó");

    List<IngatlanItem> clusters = engine.deduplicateMultiSignal(
        List.of(k140a, k140b, k140c, k140d, k140e, k132a, k132b, k132c, k132d, dh, ib));
    assertEquals(1, clusters.size(), "A 11 azonos ingatlan 1etlen közös klaszterbe kell olvadjon");
    assertEquals(11, clusters.get(0).getSourceListings().size());

    IngatlanItem differentPrice = createHouseWithImg("k10", "Költözzbe", "https://koltozzbe.hu/ingatlan/k10",
        "Eladó ikerház, Hajdúszoboszlón 115 M Ft, 3+1 szobás",
        140.0, 3.5, 115_000_000L, "https://cdn.koltozzbe.hu/img-k10.jpg");
    assertFalse(engine.isMatch(k140a, differentPrice),
        "Eltérő árú intra-portál sablonhirdetés nem olvadhat össze");
  }

  @Test
  @DisplayName("#450: Intra-portál TELEK, azonos utca variáns és kisterseg user-id védelem")
  void testIssue450IntraPortalTelekAndSameStreetAndKistersegUserIdRules() {
    DeduplicationEngine engine = TestFixtures.deduplicationEngine();

    IngatlanItem telekA = createItem("t1", "Költözzbe", "https://koltozzbe.hu/t1",
        "Eladó telek, Hajdúszoboszlón 35 M Ft", "Hajdúszoboszló",
        PropertyType.TELEK, 800.0, 0.0, 35_000_000L, "ACTIVE");
    telekA.setImageUrl("https://cdn.koltozzbe.hu/telek-1.jpg");
    IngatlanItem telekB = createItem("t2", "Költözzbe", "https://koltozzbe.hu/t2",
        "Eladó telek, Hajdúszoboszlón 35 M Ft", "Hajdúszoboszló",
        PropertyType.TELEK, 800.0, 0.0, 35_000_000L, "ACTIVE");
    telekB.setImageUrl("https://cdn.koltozzbe.hu/telek-2.jpg");
    assertTrue(engine.isMatch(telekA, telekB), "Intra-portál azonos sabloncímű TELEK összevonandó");

    IngatlanItem sameStreetA = createHouseWithImg("hs1", "Duna House", "https://dh.hu/hs1",
        "Hajdúszoboszló Hőforrás utca ikerház", 132.0, 4.0, 109_000_000L, "https://cdn.dh.hu/a.jpg");
    IngatlanItem sameStreetB = createHouseWithImg("hs2", "Duna House", "https://dh.hu/hs2",
        "Eladó modern otthon a Hőforrás utca közelében", 136.0, 4.0, 109_000_000L, "https://cdn.dh.hu/b.jpg");
    assertTrue(engine.isMatch(sameStreetA, sameStreetB),
        "Intra-portál azonos utcanév + 0.5% ár + 6% m2 variáns összevonandó");

    IngatlanItem ibUser1a = createItem("u1", "Ingatlanbazár",
        "https://www.ingatlanbazar.com/elado-haz-debreceni-kisterseg/user-id-111/aaa",
        "Debreceni kistérség eladó családi ház", "Debrecen", PropertyType.HAZ, 110.0, 3.0, 65_000_000L, "ACTIVE");
    IngatlanItem ibUser1b = createItem("u2", "Ingatlanbazár",
        "https://www.ingatlanbazar.com/elado-haz-debreceni-kisterseg/user-id-111/bbb",
        "Debreceni kistérség eladó családi ház", "Debrecen", PropertyType.HAZ, 110.0, 3.0, 65_000_000L, "ACTIVE");
    IngatlanItem ibUser2 = createItem("u3", "Ingatlanbazár",
        "https://www.ingatlanbazar.com/elado-haz-debreceni-kisterseg/user-id-222/ccc",
        "Debreceni kistérség eladó családi ház", "Debrecen", PropertyType.HAZ, 110.0, 3.0, 65_000_000L, "ACTIVE");
    assertTrue(engine.isMatch(ibUser1a, ibUser1b), "Azonos user-id esetén a kistérség sablonhirdetés összevonandó");
    assertFalse(engine.isMatch(ibUser1a, ibUser2), "Eltérő user-id esetén a kistérség sablonhirdetés védett marad");

    IngatlanItem detached = createHouseWithImg("d1", "Duna House", "https://dh.hu/d1",
        "Önálló családi ház Debrecenben", 132.0, 4.0, 109_000_000L, "https://cdn.dh.hu/shared.jpg");
    IngatlanItem twin = createHouseWithImg("d2", "Otthon Centrum", "https://oc.hu/d2",
        "Ikerház fél Debrecenben", 132.0, 4.0, 109_000_000L, "https://cdn.dh.hu/shared.jpg");
    assertFalse(engine.isMatch(detached, twin), "Explicit önálló családi ház és ikerház továbbra sem olvad össze");
  }

  private IngatlanItem createHouseWithImg(
      String id, String portal, String url, String title, double area, double rooms, long priceHuf, String imgUrl) {
    IngatlanItem item = createItem(
        id, portal, url, title, "Hajdúszoboszló", PropertyType.HAZ, area, rooms, priceHuf, "ACTIVE");
    item.setImageUrl(imgUrl);
    return item;
  }

  private IngatlanItem createItem(
      String id,
      String portal,
      String url,
      String title,
      String city,
      PropertyType type,
      double area,
      double rooms,
      long priceHuf,
      String status) {
    IngatlanItem item = new IngatlanItem();
    item.setId(id);
    item.setPortal(portal);
    item.setUrl(url);
    item.setTitle(title);
    item.setCity(city);
    item.setLocation(city);
    item.setPropertyType(type);
    item.setDetailsInfo(
        new PropertyDetails(
            area + " m2, " + rooms + " szoba",
            area,
            rooms,
            Math.round((double) priceHuf / area)));
    item.setPriceInfo(
        new PriceInfo((priceHuf / 1_000_000.0) + " M Ft", priceHuf, priceHuf / 1_000_000.0));
    item.setPrice((priceHuf / 1_000_000.0) + " M Ft");
    item.setStatus(status);
    return item;
  }

  @Test
  @DisplayName("FORCE_SPLIT felülírja az egyébként egyező párt")
  void testForceSplitOverridesHeuristicMatch() {
    IngatlanItem a = createHouseWithImg("s1", "Duna House", "https://dh.hu/split-1",
        "Kossuth utca 12 eladó lakás", 65.0, 2.0, 50_000_000L, "https://cdn.dh.hu/img1.jpg");
    IngatlanItem b = createHouseWithImg("s2", "Otthon Centrum", "https://oc.hu/split-2",
        "Kossuth utca 12 eladó lakás", 65.0, 2.0, 50_500_000L, "https://cdn.dh.hu/img1.jpg");
    a.setPropertyType(PropertyType.LAKAS);
    b.setPropertyType(PropertyType.LAKAS);

    DedupOverrideStorage overrides = stubOverrideStorage(List.of(
        new DedupOverride("https://dh.hu/split-1", "https://oc.hu/split-2", DedupOverrideType.FORCE_SPLIT)));
    DeduplicationEngine engine = new DeduplicationEngine(null, overrides);

    List<IngatlanItem> result = engine.deduplicateMultiSignal(List.of(a, b));
    assertEquals(2, result.size());
    assertTrue(result.stream().allMatch(IngatlanItem::isManuallySplit));
  }

  @Test
  @DisplayName("FORCE_MERGE összevon heurisztikusan eltérő párt és beállítja a manuallyMerged jelzőt")
  void testForceMergeCombinesHeuristicallyDifferentItems() {
    IngatlanItem a = createItem("m1", "Duna House", "https://dh.hu/merge-1",
        "Belvárosi lakás", "Debrecen", PropertyType.LAKAS, 45.0, 1.5, 35_000_000L, "ACTIVE");
    IngatlanItem b = createItem("m2", "Jófogás", "https://jofogas.hu/merge-2",
        "Teljesen más című hirdetés", "Debrecen", PropertyType.LAKAS, 90.0, 4.0, 85_000_000L, "ACTIVE");

    DedupOverrideStorage overrides = stubOverrideStorage(List.of(
        new DedupOverride("https://dh.hu/merge-1", "https://jofogas.hu/merge-2", DedupOverrideType.FORCE_MERGE)));
    DeduplicationEngine engine = new DeduplicationEngine(null, overrides);

    List<IngatlanItem> result = engine.deduplicateMultiSignal(List.of(a, b));
    assertEquals(1, result.size());
    assertEquals(2, result.get(0).getSourcesCount());
    assertTrue(result.get(0).isManuallyMerged());
  }

  @Test
  @DisplayName("Ütközésnél (A-B merge, B-C merge, A-C split) a FORCE_SPLIT nyer tranzitívan is")
  void testForceSplitWinsTransitivelyOverForceMerge() {
    IngatlanItem a = createItem("c1", "Duna House", "https://dh.hu/c-1",
        "Hirdetés A", "Debrecen", PropertyType.LAKAS, 50.0, 2.0, 40_000_000L, "ACTIVE");
    IngatlanItem b = createItem("c2", "Otthon Centrum", "https://oc.hu/c-2",
        "Hirdetés B", "Debrecen", PropertyType.LAKAS, 80.0, 3.0, 70_000_000L, "ACTIVE");
    IngatlanItem c = createItem("c3", "Jófogás", "https://jofogas.hu/c-3",
        "Hirdetés C", "Debrecen", PropertyType.LAKAS, 110.0, 4.0, 95_000_000L, "ACTIVE");

    DedupOverrideStorage overrides = stubOverrideStorage(List.of(
        new DedupOverride("https://dh.hu/c-1", "https://oc.hu/c-2", DedupOverrideType.FORCE_MERGE),
        new DedupOverride("https://jofogas.hu/c-3", "https://oc.hu/c-2", DedupOverrideType.FORCE_MERGE),
        new DedupOverride("https://dh.hu/c-1", "https://jofogas.hu/c-3", DedupOverrideType.FORCE_SPLIT)));
    DeduplicationEngine engine = new DeduplicationEngine(null, overrides);

    List<IngatlanItem> result = engine.deduplicateMultiSignal(List.of(a, b, c));
    assertEquals(2, result.size());
    boolean aAndCTogether = result.stream().anyMatch(cluster -> {
      Set<String> urls = cluster.getSourceListings().stream()
          .map(s -> s.getUrl())
          .collect(java.util.stream.Collectors.toSet());
      return urls.contains("https://dh.hu/c-1") && urls.contains("https://jofogas.hu/c-3");
    });
    assertFalse(aAndCTogether, "FORCE_SPLIT pár tranzitívan sem kerülhet egy klaszterbe");
  }

  @Test
  @DisplayName("Bucketen kívüli (eltérő PropertyType) FORCE_MERGE összevon és a legolcsóbb aktív típusát veszi fel")
  void testCrossBucketForceMergeAdoptsLowestActivePriceType() {
    IngatlanItem cheapFlat = createItem("cb1", "Duna House", "https://dh.hu/cb-1",
        "Olcsó lakás", "Debrecen", PropertyType.LAKAS, 60.0, 2.0, 45_000_000L, "ACTIVE");
    IngatlanItem expensiveHouse = createItem("cb2", "Otthon Centrum", "https://oc.hu/cb-2",
        "Drágább ház", "Debrecen", PropertyType.HAZ, 62.0, 2.0, 52_000_000L, "ACTIVE");

    DedupOverrideStorage overrides = stubOverrideStorage(List.of(
        new DedupOverride("https://dh.hu/cb-1", "https://oc.hu/cb-2", DedupOverrideType.FORCE_MERGE)));
    DeduplicationEngine engine = new DeduplicationEngine(null, overrides);

    List<IngatlanItem> result = engine.deduplicateMultiSignal(List.of(expensiveHouse, cheapFlat));
    assertEquals(1, result.size());
    assertEquals(PropertyType.LAKAS, result.get(0).getPropertyType());
    assertTrue(result.get(0).isManuallyMerged());
  }

  @Test
  @DisplayName("Eltérő városok között a FORCE_MERGE nem von össze")
  void testForceMergeDoesNotCrossCities() {
    IngatlanItem deb = createItem("ct1", "Duna House", "https://dh.hu/deb-1",
        "Debreceni lakás", "Debrecen", PropertyType.LAKAS, 60.0, 2.0, 45_000_000L, "ACTIVE");
    IngatlanItem eger = createItem("ct2", "Otthon Centrum", "https://oc.hu/eger-2",
        "Egri lakás", "Eger", PropertyType.LAKAS, 60.0, 2.0, 45_000_000L, "ACTIVE");

    DedupOverrideStorage overrides = stubOverrideStorage(List.of(
        new DedupOverride("https://dh.hu/deb-1", "https://oc.hu/eger-2", DedupOverrideType.FORCE_MERGE)));
    DeduplicationEngine engine = new DeduplicationEngine(null, overrides);

    List<IngatlanItem> result = engine.deduplicateMultiSignal(List.of(deb, eger));
    assertEquals(2, result.size());
  }

  @Test
  @DisplayName("Csillag-topológiájú FORCE_MERGE (N-1 él) tranzitívan egy klaszterbe vonja az összes tagot (#488)")
  void testTransitiveForceMergeStarClustersAllMembers() {
    IngatlanItem a = createItem("tm1", "Költözzbe", "https://koltozzbe.hu/h1001",
        "Lakás 1", "Hajdúszoboszló", PropertyType.HAZ, 60.0, 2.0, 30_000_000L, "ACTIVE");
    IngatlanItem b = createItem("tm2", "Költözzbe", "https://koltozzbe.hu/h1002",
        "Ház 2", "Hajdúszoboszló", PropertyType.HAZ, 110.0, 4.0, 75_000_000L, "ACTIVE");
    IngatlanItem c = createItem("tm3", "Költözzbe", "https://koltozzbe.hu/h1003",
        "Villa 3", "Hajdúszoboszló", PropertyType.HAZ, 180.0, 6.0, 140_000_000L, "ACTIVE");
    IngatlanItem d = createItem("tm4", "Költözzbe", "https://koltozzbe.hu/h1004",
        "Tanya 4", "Hajdúszoboszló", PropertyType.HAZ, 250.0, 8.0, 210_000_000L, "ACTIVE");

    DedupOverrideStorage overrides = stubOverrideStorage(List.of(
        new DedupOverride("https://koltozzbe.hu/h1001", "https://koltozzbe.hu/h1002", DedupOverrideType.FORCE_MERGE),
        new DedupOverride("https://koltozzbe.hu/h1001", "https://koltozzbe.hu/h1003", DedupOverrideType.FORCE_MERGE),
        new DedupOverride("https://koltozzbe.hu/h1001", "https://koltozzbe.hu/h1004", DedupOverrideType.FORCE_MERGE)));
    DeduplicationEngine engine = new DeduplicationEngine(null, overrides);

    List<IngatlanItem> result = engine.deduplicateMultiSignal(List.of(a, b, c, d));
    assertEquals(1, result.size());
    assertEquals(4, result.get(0).getSourcesCount());
    assertTrue(result.get(0).isManuallyMerged());
  }

  @Test
  @DisplayName("FORCE_SPLIT felülírja a tranzitív csillag FORCE_MERGE kapcsolatot (#488)")
  void testForceSplitBlocksTransitiveStarForceMerge() {
    IngatlanItem a = createItem("ts1", "Költözzbe", "https://koltozzbe.hu/s1",
        "Ház 1", "Hajdúszoboszló", PropertyType.HAZ, 60.0, 2.0, 30_000_000L, "ACTIVE");
    IngatlanItem b = createItem("ts2", "Költözzbe", "https://koltozzbe.hu/s2",
        "Ház 2", "Hajdúszoboszló", PropertyType.HAZ, 110.0, 4.0, 75_000_000L, "ACTIVE");
    IngatlanItem c = createItem("ts3", "Költözzbe", "https://koltozzbe.hu/s3",
        "Ház 3", "Hajdúszoboszló", PropertyType.HAZ, 180.0, 6.0, 140_000_000L, "ACTIVE");

    DedupOverrideStorage overrides = stubOverrideStorage(List.of(
        new DedupOverride("https://koltozzbe.hu/s1", "https://koltozzbe.hu/s2", DedupOverrideType.FORCE_MERGE),
        new DedupOverride("https://koltozzbe.hu/s1", "https://koltozzbe.hu/s3", DedupOverrideType.FORCE_MERGE),
        new DedupOverride("https://koltozzbe.hu/s2", "https://koltozzbe.hu/s3", DedupOverrideType.FORCE_SPLIT)));
    DeduplicationEngine engine = new DeduplicationEngine(null, overrides);

    List<IngatlanItem> result = engine.deduplicateMultiSignal(List.of(a, b, c));
    assertEquals(2, result.size());
    boolean bAndCTogether = result.stream().anyMatch(cluster -> {
      Set<String> urls = cluster.getSourceListings().stream()
          .map(s -> s.getUrl())
          .collect(java.util.stream.Collectors.toSet());
      return urls.contains("https://koltozzbe.hu/s2") && urls.contains("https://koltozzbe.hu/s3");
    });
    assertFalse(bAndCTogether);
  }

  private DedupOverrideStorage stubOverrideStorage(List<DedupOverride> list) {
    return new DedupOverrideStorage() {
      @Override
      public List<DedupOverride> getAllOverrides() {
        return list;
      }

      @Override
      public List<DedupOverride> getOverridesForUrls(Collection<String> urls) {
        return list;
      }

      @Override
      public List<DedupOverride> getOverridesForUrl(String url) {
        return list;
      }

      @Override
      public java.util.Optional<DedupOverride> findOverrideById(Long id) {
        return java.util.Optional.empty();
      }

      @Override
      public DedupOverride saveOverride(
          String url1, String url2, DedupOverrideType type, String normalizedCity) {
        return new DedupOverride(url1, url2, type);
      }

      @Override
      public boolean deleteOverrideById(Long id) {
        return false;
      }

      @Override
      public void clearCityOverrides(String city) {}

      @Override
      public void clearAllOverrides() {}
    };
  }
}
