package ingatlanok.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.PropertyType;
import ingatlanok.model.SourceListing;

/**
 * Tests for Issue #338: Deduplication and clustering with missing or varied unit floor area.
 */
class DeduplicationAreaFallbackTest {

  private final DeduplicationEngine engine = new DeduplicationEngine(null);

  @Test
  @DisplayName("#338: 6 db azonos Költözzbe panellakás alapterület nélkül sikeresen 1 kártyává olvad")
  void testIssue338MiskolcKoltozzbePanellakasDuplicatesConsolidated() {
    List<IngatlanItem> items = new ArrayList<>();
    String imageUrl = "https://img.koltozzbe.hu/photos/miskolc-panel-123.jpg";
    for (int i = 1; i <= 6; i++) {
      items.add(createItem(
          "k-p-" + i,
          "koltozzbe",
          "https://koltozzbe.hu/ingatlan/miskolc-panel-" + i,
          "Eladó panellakás, Miskolcon 22 M Ft, 1 szobás",
          "Miskolc",
          PropertyType.LAKAS,
          null,
          1.0,
          22_000_000L,
          imageUrl,
          "ACTIVE"));
    }

    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(items);

    assertEquals(1, clustered.size(), "A 6 db azonos panellakást 1 csoportosított kártyába kell vonni");
    IngatlanItem primary = clustered.get(0);
    assertEquals(6, primary.getSourcesCount());
    assertEquals(6, primary.getSourceListings().size());
    assertEquals(22_000_000L, primary.getPriceInfo().getAmountHuf());
  }

  @Test
  @DisplayName("#338: 5 db azonos Költözzbe téglalakás alapterület nélkül sikeresen 1 kártyává olvad")
  void testIssue338MiskolcKoltozzbeTeglalakasDuplicatesConsolidated() {
    List<IngatlanItem> items = new ArrayList<>();
    String imageUrl = "https://img.koltozzbe.hu/photos/miskolc-tegla-456.jpg";
    for (int i = 1; i <= 5; i++) {
      items.add(createItem(
          "k-t-" + i,
          "koltozzbe",
          "https://koltozzbe.hu/ingatlan/miskolc-tegla-" + i,
          "Eladó téglalakás, Miskolcon 20 M Ft, 1 szobás",
          "Miskolc",
          PropertyType.LAKAS,
          null,
          1.0,
          20_000_000L,
          imageUrl,
          "ACTIVE"));
    }

    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(items);

    assertEquals(1, clustered.size(), "Az 5 db azonos téglalakást 1 csoportosított kártyába kell vonni");
    IngatlanItem primary = clustered.get(0);
    assertEquals(5, primary.getSourcesCount());
    assertEquals(5, primary.getSourceListings().size());
    assertEquals(20_000_000L, primary.getPriceInfo().getAmountHuf());
  }

  @Test
  @DisplayName("#338: Költözzbe (alapterület nélkül) és Jófogás (35 nm) sikeresen egyesül és örökli az alapterületet")
  void testIssue338KoltozzbeMergedWithJofogasAndInheritsArea() {
    String sharedPhoto = "https://cdn.ingatlan.hu/shared-property-photo-999.jpg";

    List<IngatlanItem> items = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      items.add(createItem(
          "k-" + i,
          "koltozzbe",
          "https://koltozzbe.hu/ingatlan/miskolc-tegla-" + i,
          "Eladó téglalakás, Miskolcon 20 M Ft, 1 szobás",
          "Miskolc",
          PropertyType.LAKAS,
          null,
          1.0,
          20_000_000L,
          sharedPhoto,
          "ACTIVE"));
    }

    IngatlanItem jofogas = createItem(
        "jf-1",
        "jofogas",
        "https://jofogas.hu/miskolc/35_nm_lakas_12345.htm",
        "35 nm-es lakás eladó Miskolc",
        "Miskolc",
        PropertyType.LAKAS,
        35.0,
        1.0,
        20_000_000L,
        sharedPhoto,
        "ACTIVE");
    items.add(jofogas);

    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(items);

    assertEquals(1, clustered.size(), "A Költözzbe tételeknek és a Jófogás tételnek egybe kell olvadniuk");
    IngatlanItem primary = clustered.get(0);
    assertEquals(6, primary.getSourcesCount());
    assertEquals(6, primary.getSourceListings().size());

    // Alapterület öröklésének ellenőrzése
    assertNotNull(primary.getDetailsInfo());
    assertEquals(35.0, primary.getDetailsInfo().getAreaSquareMeters(), "A konszolidált tétel örökölte a 35 m²-t");
    long expectedPricePerM2 = Math.round(20_000_000.0 / 35.0);
    assertEquals(expectedPricePerM2, primary.getDetailsInfo().getPricePerSquareMeter());

    // Forráslistában is meg kell jelennie a számított m² árnak
    for (SourceListing source : primary.getSourceListings()) {
      assertEquals(expectedPricePerM2, source.getPricePerM2());
    }
  }

  @Test
  @DisplayName("#338 / Guard: Hiányzó alapterületnél az áreltérés szigorúan <=1% lehet")
  void testMissingAreaPriceDiffBoundary() {
    String sharedPhoto = "https://cdn.photos.hu/img.jpg";

    IngatlanItem base = createItem(
        "1", "dh", "http://dh/1", "Eladó lakás Miskolc", "Miskolc",
        PropertyType.LAKAS, null, 1.0, 20_000_000L, sharedPhoto, "ACTIVE");

    // 20.1M vs 20.0M -> diff 100k / 20.1M = 0.49% <= 1% -> engedélyezve
    IngatlanItem itemWithin1Pct = createItem(
        "2", "oc", "http://oc/2", "Eladó lakás Miskolc", "Miskolc",
        PropertyType.LAKAS, null, 1.0, 20_100_000L, sharedPhoto, "ACTIVE");
    assertTrue(engine.isMatch(base, itemWithin1Pct));

    // 20.3M vs 20.0M -> diff 300k / 20.3M = 1.48% > 1% -> elutasítva
    IngatlanItem itemOver1Pct = createItem(
        "3", "oc", "http://oc/3", "Eladó lakás Miskolc", "Miskolc",
        PropertyType.LAKAS, null, 1.0, 20_300_000L, sharedPhoto, "ACTIVE");
    assertFalse(engine.isMatch(base, itemOver1Pct));
  }

  @Test
  @DisplayName("#338 / Guard: Hiányzó alapterületnél megerősítő jel (kép vagy szöveg) nélkül nem vonható össze")
  void testMissingAreaRequiresCorroboratingSignal() {
    // Két teljesen független hirdetés különböző képekkel és különböző címekkel Miskolcon
    IngatlanItem itemA = createItem(
        "1", "dh", "http://dh/1", "Avas lakótelepi garzon felújítva", "Miskolc",
        PropertyType.LAKAS, null, 1.0, 20_000_000L, "https://photoA.jpg", "ACTIVE");

    IngatlanItem itemB = createItem(
        "2", "oc", "http://oc/2", "Belvárosi tégla kislakás azonnal költözhető", "Miskolc",
        PropertyType.LAKAS, null, 1.0, 20_000_000L, "https://photoB.jpg", "ACTIVE");

    assertFalse(engine.isMatch(itemA, itemB), "Megerősítő jel nélkül nem vonható össze");
    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(itemA, itemB));
    assertEquals(2, clustered.size());
  }

  @Test
  @DisplayName("#338: Hiányzó alapterület esetén azonos cím megerősítő jelként szolgál képek nélkül is")
  void testMissingAreaTitleSimilarityWithoutImages() {
    IngatlanItem itemA = createItem(
        "1", "dh", "http://dh/1", "Eladó panellakás Miskolcon 22 M Ft", "Miskolc",
        PropertyType.LAKAS, null, 1.0, 22_000_000L, null, "ACTIVE");

    IngatlanItem itemB = createItem(
        "2", "oc", "http://oc/2", "Eladó panellakás Miskolcon 22 M Ft", "Miskolc",
        PropertyType.LAKAS, null, 1.0, 22_000_000L, null, "ACTIVE");

    assertTrue(engine.isMatch(itemA, itemB), "Azonos cím megerősítő jelként szolgál");
    List<IngatlanItem> clustered = engine.deduplicateMultiSignal(List.of(itemA, itemB));
    assertEquals(1, clustered.size());
  }

  private IngatlanItem createItem(
      String id,
      String portal,
      String url,
      String title,
      String city,
      PropertyType type,
      Double area,
      Double rooms,
      long priceHuf,
      String imageUrl,
      String status) {
    IngatlanItem item = new IngatlanItem();
    item.setId(id);
    item.setPortal(portal);
    item.setUrl(url);
    item.setTitle(title);
    item.setCity(city);
    item.setLocation(city);
    item.setPropertyType(type);
    item.setImageUrl(imageUrl);
    String detailsText = "";
    Long pricePerM2 = null;
    if (area != null && area > 0) {
      detailsText = Math.round(area) + " m² • " + Math.round(rooms) + " szoba";
      pricePerM2 = Math.round((double) priceHuf / area);
    } else if (rooms != null) {
      detailsText = Math.round(rooms) + " szoba";
    }
    item.setDetailsInfo(
        PropertyDetails.builder()
            .rawText(detailsText)
            .areaSquareMeters(area)
            .roomCount(rooms)
            .pricePerSquareMeter(pricePerM2)
            .build());
    item.setPriceInfo(
        PriceInfo.builder()
            .rawText((priceHuf / 1_000_000.0) + " M Ft")
            .amountHuf(priceHuf)
            .amountMillion(priceHuf / 1_000_000.0)
            .build());
    item.setPrice((priceHuf / 1_000_000.0) + " M Ft");
    item.setStatus(status);
    return item;
  }
}
