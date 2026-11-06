package ingatlanok.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;

/**
 * Tests for #298 and #307 multi-signal deduplication and clustering rationalization.
 */
class DeduplicationEngineRationalizationTest {

  private final DeduplicationEngine engine = new DeduplicationEngine(null);

  @Test
  @DisplayName("#298: Hajdúszoboszló 190 m² 2 generációs ház 7 vs 5 szobával sikeresen összevonásra kerül")
  void testIssue298HajduszoboszloMultiGenerationHouseMerged() {
    // Ingatlanbazár hirdetés: 190 m2, 7 szoba, 99.999.000 Ft (~99.99M Ft)
    IngatlanItem ib =
        createItem(
            "ib-1",
            "ingatlanbazar",
            "https://www.ingatlanbazar.hu/Hajduszoboszlo-elado-haz-190-m2-99999000-Ft",
            "Hajdúszoboszló eladó családi ház 190 m2 2 generációs 612 nm telek",
            "Hajdúszoboszló",
            PropertyType.HAZ,
            190.0,
            7.0,
            99_999_000L,
            "ACTIVE");

    // Jófogás hirdetés: 190 m2, 5 szoba, 95.000.000 Ft (95M Ft)
    IngatlanItem jf =
        createItem(
            "jf-2",
            "jofogas",
            "https://ingatlan.jofogas.hu/hajdu_bihar/Hajduszoboszlon___2_epuletbol_allo__2_generacios_haz",
            "Hajdúszoboszlón - 2 épületből álló, 2 generációs ház eladó!",
            "Hajdúszoboszló",
            PropertyType.HAZ,
            190.0,
            5.0,
            95_000_000L,
            "ACTIVE");

    assertTrue(engine.isMatch(ib, jf), "Azonos 190 m2-es 2 generációs háznak egyeznie kell");
    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(ib, jf));

    assertEquals(1, clustered.size(), "A két hirdetést egyetlen kártyává kell összevonni");
    IngatlanItem primary = clustered.get(0);
    assertEquals(2, primary.getSourcesCount());
    assertEquals(95_000_000L, primary.getPriceInfo().getAmountHuf(), "A legalacsonyabb ár a Jófogás 95M Ft");
    assertEquals(2, primary.getSourceListings().size());
  }

  @Test
  @DisplayName("#298: Hiányzó szobaszám esetén a hirdetés nem esik el az összevonástól, ha egyéb adatok egyeznek")
  void testIssue298MissingRoomCountFallbackAllowed() {
    IngatlanItem dh =
        createItem(
            "dh-1",
            "dh",
            "http://dh/1",
            "Debrecen Belvárosi felújított lakás",
            "Debrecen",
            PropertyType.LAKAS,
            65.0,
            2.0,
            50_000_000L,
            "ACTIVE");

    // Jófogás esetén a listaoldali parser nem talált szobaszámot (0 vagy null)
    IngatlanItem jf =
        createItem(
            "jf-1",
            "jofogas",
            "http://jf/1",
            "Debrecen Belvárosi felújított lakás",
            "Debrecen",
            PropertyType.LAKAS,
            65.0,
            0.0,
            50_000_000L,
            "ACTIVE");
    jf.setDetailsInfo(jf.getDetailsInfo().toBuilder().roomCount(null).build());

    assertTrue(engine.isMatch(dh, jf), "Hiányzó szobaszám nem tilthatja le az összevonást ha minden egyéb egyezik");
    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(dh, jf));
    assertEquals(1, clustered.size());
  }

  @Test
  @DisplayName("#298 / Guard: Hagyományos kis lakásnál az 1 vs 2 szoba különbség továbbra is konfliktus marad")
  void testStandardApartmentRoomConflictGuardMaintained() {
    IngatlanItem item1 =
        createItem(
            "1",
            "dh",
            "http://1",
            "1 szobás garzon",
            "Debrecen",
            PropertyType.LAKAS,
            45.0,
            1.0,
            35_000_000L,
            "ACTIVE");

    IngatlanItem item2 =
        createItem(
            "2",
            "oc",
            "http://2",
            "2 szobás lakás",
            "Debrecen",
            PropertyType.LAKAS,
            45.0,
            2.0,
            35_000_000L,
            "ACTIVE");

    assertFalse(engine.isMatch(item1, item2), "Kis lakásnál az 1 vs 2 szoba eltérés nem vonható össze");
    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(item1, item2));
    assertEquals(2, clustered.size());
  }

  @Test
  @DisplayName("#307: Eltérő városrész (Kertváros vs Bánomkert) esetén tilos az összevonás")
  void testIssue307QuarterConflictPreventsClustering() {
    IngatlanItem kertvaros =
        createItem(
            "1",
            "dh",
            "http://dh/1",
            "Kertvárosi csendes családi ház",
            "Hajdúszoboszló",
            PropertyType.HAZ,
            120.0,
            4.0,
            85_000_000L,
            "ACTIVE");
    kertvaros.setLocation("Hajdúszoboszló, Kertváros");

    IngatlanItem banomkert =
        createItem(
            "2",
            "oc",
            "http://oc/2",
            "Bánomkerti igényes családi ház",
            "Hajdúszoboszló",
            PropertyType.HAZ,
            120.0,
            4.0,
            88_000_000L,
            "ACTIVE");
    banomkert.setLocation("Hajdúszoboszló, Bánomkert");

    assertFalse(engine.isMatch(kertvaros, banomkert), "Kertváros és Bánomkert sosem vonható össze");
    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(kertvaros, banomkert));
    assertEquals(2, clustered.size(), "A két különböző városrészben lévő háznak külön kell maradnia");
  }

  @Test
  @DisplayName("#307: Budapest eltérő kerületek (XI. vs XIII.) nem vonhatók össze azonos méret és ár mellett sem")
  void testIssue307BudapestDistrictConflictPreventsClustering() {
    IngatlanItem dist11 =
        createItem(
            "1",
            "dh",
            "http://dh/1",
            "Budapest XI. kerület, Újbudai szép lakás",
            "Budapest",
            PropertyType.LAKAS,
            60.0,
            2.0,
            55_000_000L,
            "ACTIVE");

    IngatlanItem dist13 =
        createItem(
            "2",
            "oc",
            "http://oc/2",
            "Budapest XIII. kerület, Angyalföldi lakás",
            "Budapest",
            PropertyType.LAKAS,
            60.0,
            2.0,
            55_000_000L,
            "ACTIVE");

    assertFalse(engine.isMatch(dist11, dist13), "XI. és XIII. kerület nem vonható össze");
    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(dist11, dist13));
    assertEquals(2, clustered.size());
  }

  @Test
  @DisplayName("#307: Eltérő utcanév (Kossuth Lajos utca vs Petőfi Sándor utca) kizárja az összevonást")
  void testIssue307StreetConflictPreventsClustering() {
    IngatlanItem item1 =
        createItem(
            "1",
            "dh",
            "http://dh/1",
            "Kossuth Lajos utcai családi ház nagy kerttel",
            "Debrecen",
            PropertyType.HAZ,
            100.0,
            3.0,
            75_000_000L,
            "ACTIVE");

    IngatlanItem item2 =
        createItem(
            "2",
            "oc",
            "http://oc/2",
            "Petőfi Sándor utcai családi ház nagy kerttel",
            "Debrecen",
            PropertyType.HAZ,
            100.0,
            3.0,
            75_000_000L,
            "ACTIVE");

    assertFalse(engine.isMatch(item1, item2), "Két különböző utcában lévő ház nem vonható össze");
    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(item1, item2));
    assertEquals(2, clustered.size());
  }

  @Test
  @DisplayName("#307: Új építésű vs felújítandó/kádárkocka állapoti ellentmondás kizárja az összevonást")
  void testIssue307NewConstructionVsNeedsRenovationConflict() {
    IngatlanItem newBuild =
        createItem(
            "1",
            "dh",
            "http://dh/1",
            "Új építésű prémium ikerház",
            "Debrecen",
            PropertyType.HAZ,
            100.0,
            3.0,
            80_000_000L,
            "ACTIVE");
    newBuild.setNewConstruction(true);

    IngatlanItem reno =
        createItem(
            "2",
            "oc",
            "http://oc/2",
            "Bontandó felújítandó kádárkocka",
            "Debrecen",
            PropertyType.HAZ,
            100.0,
            3.0,
            80_000_000L,
            "ACTIVE");

    assertFalse(engine.isMatch(newBuild, reno), "Új építésű és felújítandó kádárkocka nem vonható össze");
    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(newBuild, reno));
    assertEquals(2, clustered.size());
  }

  @Test
  @DisplayName("#307: Panellakás vs polgári téglalakás szerkezeti ellentmondás kizárja az összevonást")
  void testIssue307PanelVsBrickConflict() {
    IngatlanItem panel =
        createItem(
            "1",
            "dh",
            "http://dh/1",
            "Felújított panellakás erkélyes",
            "Debrecen",
            PropertyType.LAKAS,
            60.0,
            2.0,
            45_000_000L,
            "ACTIVE");

    IngatlanItem tegla =
        createItem(
            "2",
            "oc",
            "http://oc/2",
            "Polgári téglalakás csendes udvarral",
            "Debrecen",
            PropertyType.LAKAS,
            60.0,
            2.0,
            45_000_000L,
            "ACTIVE");

    assertFalse(engine.isMatch(panel, tegla), "Panel és tégla lakás nem vonható össze");
    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(panel, tegla));
    assertEquals(2, clustered.size());
  }

  @Test
  @DisplayName("#307: Eltérő telekméret (600 m2 vs 3500 m2) esetén házak nem vonhatók össze")
  void testIssue307LotSizeConflictPreventsClustering() {
    IngatlanItem houseA =
        createItem(
            "1",
            "dh",
            "http://dh/1",
            "Családi ház 600 m2 telken eladó",
            "Debrecen",
            PropertyType.HAZ,
            120.0,
            4.0,
            80_000_000L,
            "ACTIVE");

    IngatlanItem houseB =
        createItem(
            "2",
            "oc",
            "http://oc/2",
            "Családi ház 3500 m2 telken eladó",
            "Debrecen",
            PropertyType.HAZ,
            120.0,
            4.0,
            80_000_000L,
            "ACTIVE");

    assertFalse(engine.isMatch(houseA, houseB), "600 m2 és 3500 m2 telek eltérés kizárja az összevonást");
    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(houseA, houseB));
    assertEquals(2, clustered.size());
  }

  @Test
  @DisplayName("#307: Gazdag leírású, teljesen eltérő tartalmú hirdetések közös kulcsszavak nélkül nem vonhatók össze")
  void testIssue307RichDivergentTitlesConflict() {
    IngatlanItem villa =
        createItem(
            "1",
            "dh",
            "http://dh/1",
            "Modern panorámás luxusvilla feszített víztükrű medencével",
            "Debrecen",
            PropertyType.HAZ,
            150.0,
            4.0,
            90_000_000L,
            "ACTIVE");

    IngatlanItem tanya =
        createItem(
            "2",
            "oc",
            "http://oc/2",
            "Parasztház borospincével és gazdasági melléképületekkel",
            "Debrecen",
            PropertyType.HAZ,
            150.0,
            4.0,
            90_000_000L,
            "ACTIVE");

    assertFalse(engine.isMatch(villa, tanya), "Luxusvilla és parasztház nem vonható össze");
    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(villa, tanya));
    assertEquals(2, clustered.size());
  }

  @Test
  @DisplayName("#354: Otthon Centrum Kertváros új építésű ház vs Belváros használt sorház nem vonható össze")
  void testIssue354OtthonCentrumHajduszoboszloKertvarosVsBelvarosNotMerged() {
    IngatlanItem u0050372 =
        createItem(
            "U0050372",
            "oc",
            "https://www.oc.hu/ingatlanok/U0050372",
            "Fedezze fel álmai otthonát Hajdúszoboszlón – új 4 szobás ház várja Önt!",
            "Hajdúszoboszló",
            PropertyType.HAZ,
            107.0,
            4.0,
            104_900_000L,
            "ACTIVE");
    u0050372.setDetails("107m² Kertváros 4 szoba");
    u0050372.setDetailsInfo(
        u0050372.getDetailsInfo().toBuilder().rawText("107m² Kertváros 4 szoba").build());
    u0050372.setNewConstruction(true);
    u0050372.setImageUrl("https://www.oc.hu/images/u0050372.jpg");

    IngatlanItem h526222 =
        createItem(
            "H526222",
            "oc",
            "https://www.oc.hu/ingatlanok/H526222",
            "Fedezze fel álmai otthonát Hajdúszoboszló szívében – Nappali +3 szobás sorház a városközpontban!",
            "Hajdúszoboszló",
            PropertyType.HAZ,
            107.0,
            4.0,
            111_000_000L,
            "ACTIVE");
    h526222.setDetails("107m² Belváros 4 szoba");
    h526222.setDetailsInfo(
        h526222.getDetailsInfo().toBuilder().rawText("107m² Belváros 4 szoba").build());
    h526222.setNewConstruction(false);
    h526222.setImageUrl("https://www.oc.hu/images/h526222.jpg");

    assertFalse(
        engine.isMatch(u0050372, h526222),
        "#354: Kertvárosi új építésű ház és Belvárosi használt sorház nem vonható össze");
    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(u0050372, h526222));
    assertEquals(2, clustered.size(), "A két hirdetésnek két különálló találati kártyaként kell megjelennie");
    assertEquals(1, clustered.get(0).getSourcesCount());
    assertEquals(1, clustered.get(1).getSourcesCount());
  }

  @Test
  @DisplayName("#354: Azonos portálon belüli eltérő hirdetések (>1% ár vagy eltérő kép/cím) nem vonhatók össze")
  void testIssue354IntraPortalStrictGuard() {
    // Ugyanaz a portál (oc), 5% árkülönbség, eltérő kép és cím -> NEM vonható össze
    IngatlanItem oc1 =
        createItem(
            "H1",
            "oc",
            "https://www.oc.hu/ingatlanok/H1",
            "Csendes utcában eladó tágas családi ház",
            "Debrecen",
            PropertyType.HAZ,
            110.0,
            4.0,
            95_000_000L,
            "ACTIVE");
    oc1.setImageUrl("https://www.oc.hu/img/h1.jpg");

    IngatlanItem oc2 =
        createItem(
            "H2",
            "oc",
            "https://www.oc.hu/ingatlanok/H2",
            "Jó elosztású eladó családi ház kerttel",
            "Debrecen",
            PropertyType.HAZ,
            110.0,
            4.0,
            100_000_000L,
            "ACTIVE");
    oc2.setImageUrl("https://www.oc.hu/img/h2.jpg");

    assertFalse(engine.isMatch(oc1, oc2), "Azonos portálon belül 5% áreltérés és eltérő kép/cím nem vonható össze");

    // Ugyanaz a portál, azonos ár (<=1%), és azonos kép -> duplikátum-spam összevonható (#338)
    IngatlanItem spam1 =
        createItem(
            "S1",
            "koltozzbe",
            "https://koltozzbe.hu/ingatlan/1",
            "Eladó lakás Debrecen 50 M Ft",
            "Debrecen",
            PropertyType.LAKAS,
            55.0,
            2.0,
            50_000_000L,
            "ACTIVE");
    spam1.setImageUrl("https://img.koltozzbe.hu/shared.jpg");

    IngatlanItem spam2 =
        createItem(
            "S2",
            "koltozzbe",
            "https://koltozzbe.hu/ingatlan/2",
            "Debreceni 2 szobás lakás eladó",
            "Debrecen",
            PropertyType.LAKAS,
            55.0,
            2.0,
            50_200_000L,
            "ACTIVE");
    spam2.setImageUrl("https://img.koltozzbe.hu/shared.jpg");

    assertTrue(engine.isMatch(spam1, spam2), "Azonos portálon belüli <=1% árú, azonos képű spam összevonható");
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
}
