package ingatlanok;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.presentation.DashboardPresentationService;
import ingatlanok.presentation.DashboardResponseDto;
import ingatlanok.presentation.MarketStatsDto;
import ingatlanok.presentation.PortalStatusDto;
import ingatlanok.scraper.IngatlanBazarLocationResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryAndPresentationCoverageTest {

    @Test
    @DisplayName("IngatlanRepository operations and edge cases")
    void testIngatlanRepositoryMethods() {
        IngatlanRepository repo = IngatlanRepository.getInstance();

        assertNotNull(repo);
        assertTrue(repo.getTotalCount() >= 0);
        assertNotNull(repo.getTrackedCitiesInfo());

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Eger");
        repo.saveLastCriteria(criteria);

        SearchCriteria loaded = repo.getLastCriteria();
        assertNotNull(loaded);
        assertEquals("Eger", loaded.getCity());

        IngatlanItem item = new IngatlanItem();
        item.setUrl("https://test.hu/item-" + System.currentTimeMillis());
        item.setCity("TesztVaros");
        item.setPrice("30 M Ft");

        repo.saveCityItems("TesztVaros", List.of(item));
        assertTrue(repo.hasCityDatabase("TesztVaros"));
        assertFalse(repo.isNewUrl("TesztVaros", item.getUrl()));

        List<IngatlanItem> byCity = repo.getItemsByCity("TesztVaros");
        assertNotNull(byCity);
        assertFalse(byCity.isEmpty());

        repo.clearCityHistory("TesztVaros");
        assertNotNull(repo.getLastRunTime());
    }

    @Test
    @DisplayName("DashboardPresentationService and DTO coverage")
    void testPresentationAndDtos() {
        DashboardPresentationService service = DashboardPresentationService.getInstance();
        assertNotNull(service);

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");
        criteria.setPropertyType("lakas");

        DashboardResponseDto dto = service.executeSearch(criteria);
        assertNotNull(dto);
        assertNotNull(dto.getItems());
        assertNotNull(dto.getMarketStats());
        assertNotNull(dto.getPortalStatuses());
        assertTrue(dto.getTotalFound() >= 0);

        MarketStatsDto stats = dto.getMarketStats();
        assertNotNull(stats.getCityBreakdown());

        PortalStatusDto portalStatus = new PortalStatusDto("dh", "Duna House", "SUCCESS", 5, 120L, "https://dh.hu", "OK");
        assertEquals("dh", portalStatus.getPortalId());
        assertEquals("Duna House", portalStatus.getPortalName());
        assertEquals("SUCCESS", portalStatus.getStatus());
        assertEquals(5, portalStatus.getFoundCount());
        assertEquals(120L, portalStatus.getLatencyMs());
        assertEquals("https://dh.hu", portalStatus.getFallbackUrl());
        assertEquals("OK", portalStatus.getMessage());

        portalStatus.setFoundCount(10);
        portalStatus.setLatencyMs(200L);
        portalStatus.setStatus("EMPTY");
        assertEquals(10, portalStatus.getFoundCount());
        assertEquals(200L, portalStatus.getLatencyMs());
        assertEquals("EMPTY", portalStatus.getStatus());
    }

    @Test
    @DisplayName("IngatlanBazarLocationResolver cache and resolving")
    void testIngatlanBazarLocationResolver() {
        Optional<Integer> debrecenId = IngatlanBazarLocationResolver.resolveLocationId("Debrecen");
        assertTrue(debrecenId.isPresent());
        assertEquals(1435, debrecenId.get());

        Optional<Integer> cachedDebrecen = IngatlanBazarLocationResolver.resolveLocationId("debrecen");
        assertTrue(cachedDebrecen.isPresent());
        assertEquals(1435, cachedDebrecen.get());

        Optional<Integer> budapestId = IngatlanBazarLocationResolver.resolveLocationId("Budapest");
        assertTrue(budapestId.isPresent());

        Optional<Integer> unknown = IngatlanBazarLocationResolver.resolveLocationId("IsmeretlenNemLetezoTelepules123");
        assertTrue(unknown.isEmpty());
    }
}
