package ingatlanok.presentation;

import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.scraper.PortalScrapeResult;
import ingatlanok.scraper.PortalScrapeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PortalStatusDiagnosticTest {

    @Test
    @DisplayName("PortalStatusDto full constructor and property accessors")
    void testPortalStatusDtoProperties() {
        PortalStatusDto dto = new PortalStatusDto(
                "oc",
                "Otthon Centrum",
                "FAILED",
                0,
                350L,
                "https://oc.hu/elado/lakas",
                "Hiba történt",
                500,
                "Read timed out after 15000ms"
        );

        assertEquals("oc", dto.getPortalId());
        assertEquals("Otthon Centrum", dto.getPortalName());
        assertEquals("FAILED", dto.getStatus());
        assertEquals(0, dto.getFoundCount());
        assertEquals(350L, dto.getLatencyMs());
        assertEquals("https://oc.hu/elado/lakas", dto.getFallbackUrl());
        assertEquals("Hiba történt", dto.getMessage());
        assertEquals(500, dto.getHttpStatusCode());
        assertEquals("Read timed out after 15000ms", dto.getTechnicalDetails());

        dto.setPortalId("dh");
        dto.setPortalName("Duna House");
        dto.setStatus("SUCCESS");
        dto.setFoundCount(15);
        dto.setLatencyMs(420L);
        dto.setFallbackUrl("https://dh.hu");
        dto.setMessage("Sikeres");
        dto.setHttpStatusCode(200);
        dto.setTechnicalDetails("Minden rendben");

        assertEquals("dh", dto.getPortalId());
        assertEquals("Duna House", dto.getPortalName());
        assertEquals("SUCCESS", dto.getStatus());
        assertEquals(15, dto.getFoundCount());
        assertEquals(420L, dto.getLatencyMs());
        assertEquals("https://dh.hu", dto.getFallbackUrl());
        assertEquals("Sikeres", dto.getMessage());
        assertEquals(200, dto.getHttpStatusCode());
        assertEquals("Minden rendben", dto.getTechnicalDetails());
    }

    @Test
    @DisplayName("PortalScrapeResult diagnostic factory methods with status codes")
    void testPortalScrapeResultDiagnostics() {
        PortalScrapeResult success = PortalScrapeResult.success("dh", "Duna House", Collections.emptyList(), 150L);
        assertEquals(PortalScrapeStatus.EMPTY, success.getStatus());
        assertEquals(200, success.getHttpStatusCode());
        assertNotNull(success.getTechnicalDetails());

        PortalScrapeResult botBlocked = PortalScrapeResult.botProtected("ingatlancom", "Ingatlan.com", "https://ingatlan.com", 80L);
        assertEquals(PortalScrapeStatus.BOT_PROTECTED, botBlocked.getStatus());
        assertEquals(403, botBlocked.getHttpStatusCode());
        assertTrue(botBlocked.getTechnicalDetails().contains("403 Forbidden"));

        PortalScrapeResult failureWithCode = PortalScrapeResult.failure("oc", "Otthon Centrum", "Kapcsolati hiba", 200L, 503, "https://oc.hu");
        assertEquals(PortalScrapeStatus.FAILED, failureWithCode.getStatus());
        assertEquals(503, failureWithCode.getHttpStatusCode());
        assertEquals("https://oc.hu", failureWithCode.getFallbackUrl());
        assertEquals("Kapcsolati hiba", failureWithCode.getTechnicalDetails());
    }

    @Test
    @DisplayName("SearchResult contains portalStatuses list")
    void testSearchResultPortalStatuses() {
        SearchResult result = new SearchResult();
        assertNotNull(result.getPortalStatuses());
        assertTrue(result.getPortalStatuses().isEmpty());

        PortalStatusDto dto = new PortalStatusDto("ib", "Ingatlanbazár", "SUCCESS", 25, 300L, null, "Sikeres");
        result.setPortalStatuses(List.of(dto));

        assertEquals(1, result.getPortalStatuses().size());
        assertEquals("ib", result.getPortalStatuses().get(0).getPortalId());
    }

    @Test
    @DisplayName("DashboardPresentationService populates portalStatuses in response")
    void testDashboardPresentationServicePortalStatuses() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCities(List.of("Debrecen"));
        criteria.setPortals(List.of("ingatlanbazar"));

        DashboardResponseDto response = DashboardPresentationService.getInstance().executeSearch(criteria);
        assertNotNull(response);
        assertNotNull(response.getPortalStatuses());
        assertFalse(response.getPortalStatuses().isEmpty());

        PortalStatusDto first = response.getPortalStatuses().get(0);
        assertNotNull(first.getPortalId());
        assertNotNull(first.getPortalName());
        assertNotNull(first.getStatus());
    }
}
