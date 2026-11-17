package ingatlanok.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalScrapeResult;
import ingatlanok.model.PortalScrapeStatus;
import ingatlanok.model.PortalSource;
import ingatlanok.model.SourceListing;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.testsupport.TestFixtures;

class SearchPersistenceSynchronizerTest {

  private SearchPersistenceSynchronizer createSynchronizer() {
    PortalEngineRegistry registry =
        new PortalEngineRegistry(
            List.of(
                TestFixtures.dhEngine(),
                TestFixtures.ocEngine(),
                TestFixtures.jofogasEngine(),
                TestFixtures.koltozzbeEngine()));
    return new SearchPersistenceSynchronizer(registry, Mockito.mock(ListingStorage.class));
  }

  @Test
  @DisplayName(
      "recalculateRelevantPortalCounts csak az elsődleges (nyertes) portált számolja, így az összeg"
          + " pontosan megegyezik a nettó egyedi ingatlanszámmal (#432)")
  void testRecalculateRelevantPortalCountsSumsToNetUniqueItems() {
    SearchPersistenceSynchronizer sync = createSynchronizer();

    IngatlanItem kbWinnerWithJofogasAndDhDuplicates =
        TestFixtures.validResidentialItem("KB-1", "Miskolc", 23_900_000L, 35.0, 1.5);
    kbWinnerWithJofogasAndDhDuplicates.setPortal("Költözzbe");
    kbWinnerWithJofogasAndDhDuplicates.setSource(PortalSource.of("Költözzbe"));
    kbWinnerWithJofogasAndDhDuplicates.setSourcesCount(3);
    kbWinnerWithJofogasAndDhDuplicates.setSourceListings(
        List.of(
            SourceListing.builder()
                .portal("Költözzbe")
                .url("https://koltozzbe.hu/1")
                .priceHuf(23_900_000L)
                .build(),
            SourceListing.builder()
                .portal("Jófogás Ingatlan")
                .url("https://ingatlan.jofogas.hu/1")
                .priceHuf(24_500_000L)
                .build(),
            SourceListing.builder()
                .portal("Duna House")
                .url("https://dh.hu/1")
                .priceHuf(24_900_000L)
                .build()));

    IngatlanItem dhSingle =
        TestFixtures.validResidentialItem("DH-2", "Miskolc", 26_000_000L, 36.0, 1.0);
    dhSingle.setPortal("Duna House");
    dhSingle.setSource(PortalSource.of("Duna House"));
    dhSingle.setSourcesCount(1);

    IngatlanItem ocWinnerWithDhDuplicate =
        TestFixtures.validResidentialItem("OC-3", "Miskolc", 21_500_000L, 34.0, 1.0);
    ocWinnerWithDhDuplicate.setPortal("Otthon Centrum");
    ocWinnerWithDhDuplicate.setSource(PortalSource.of("Otthon Centrum"));
    ocWinnerWithDhDuplicate.setSourcesCount(2);
    ocWinnerWithDhDuplicate.setSourceListings(
        List.of(
            SourceListing.builder()
                .portal("Otthon Centrum")
                .url("https://oc.hu/3")
                .priceHuf(21_500_000L)
                .build(),
            SourceListing.builder()
                .portal("Duna House")
                .url("https://dh.hu/3")
                .priceHuf(22_000_000L)
                .build()));

    List<IngatlanItem> netUniqueItems =
        List.of(kbWinnerWithJofogasAndDhDuplicates, dhSingle, ocWinnerWithDhDuplicate);

    Map<String, Integer> portalCounts = new LinkedHashMap<>();
    portalCounts.put("Költözzbe", 99);
    portalCounts.put("Duna House", 99);
    portalCounts.put("Otthon Centrum", 99);
    portalCounts.put("Jófogás Ingatlan", 99);

    sync.recalculateRelevantPortalCounts(portalCounts, netUniqueItems);

    assertEquals(1, portalCounts.get("Költözzbe"));
    assertEquals(1, portalCounts.get("Duna House"));
    assertEquals(1, portalCounts.get("Otthon Centrum"));
    assertEquals(0, portalCounts.get("Jófogás Ingatlan"));

    int sumOfPortalCounts =
        portalCounts.values().stream().mapToInt(Integer::intValue).sum();
    assertEquals(
        netUniqueItems.size(),
        sumOfPortalCounts,
        "A portálokhoz rendelt darabszámok összegének pontosan ki kell adnia a nettó találatszámot");
  }

  @Test
  @DisplayName(
      "aggregatePortalResultsByPortal továbbra is SUCCESS státuszon tartja a másodlagos forrásként"
          + " beolvadt portált (#432)")
  void testAggregatePortalResultsByPortalRetainsSuccessForSecondarySource() {
    SearchPersistenceSynchronizer sync = createSynchronizer();

    IngatlanItem ocPrimary =
        TestFixtures.validResidentialItem("OC-1", "Debrecen", 39_500_000L, 60.0, 2.0);
    ocPrimary.setPortal("Otthon Centrum");
    ocPrimary.setSource(PortalSource.of("Otthon Centrum"));
    ocPrimary.setSourcesCount(2);
    ocPrimary.setSourceListings(
        List.of(
            SourceListing.builder()
                .portal("Otthon Centrum")
                .url("https://oc.hu/1")
                .priceHuf(39_500_000L)
                .build(),
            SourceListing.builder()
                .portal("Duna House")
                .url("https://dh.hu/1")
                .priceHuf(42_000_000L)
                .build()));

    List<PortalScrapeResult> rawResults =
        List.of(
            PortalScrapeResult.builder()
                .portalId("dh")
                .portalName("Duna House")
                .status(PortalScrapeStatus.SUCCESS)
                .items(List.of(ocPrimary))
                .build());

    List<PortalScrapeResult> aggregated =
        sync.aggregatePortalResultsByPortal(rawResults, List.of(ocPrimary));
    assertEquals(1, aggregated.size());
    assertEquals(PortalScrapeStatus.SUCCESS, aggregated.getFirst().getStatus());
  }
}
