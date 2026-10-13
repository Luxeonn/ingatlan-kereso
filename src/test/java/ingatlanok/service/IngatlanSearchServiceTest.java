package ingatlanok.service;

import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IngatlanSearchServiceTest {

    @Test
    @DisplayName("IngatlanSearchService null criteria handling")
    void testSearchNullCriteria() {
        IngatlanSearchService service = IngatlanSearchService.getInstance();
        SearchResult result = service.executeSearch(null);
        assertNotNull(result);
        assertNotNull(result.getItems());
    }

    @Test
    @DisplayName("IngatlanSearchService execution with selected portal")
    void testSearchExecution() {
        IngatlanSearchService service = IngatlanSearchService.getInstance();
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCities(List.of("Debrecen"));
        criteria.setPropertyType("lakas");
        criteria.setMinPriceMillion(40);
        criteria.setMaxPriceMillion(60);
        criteria.setPortals(List.of("ingatlanbazar"));

        SearchResult result = service.executeSearch(criteria);
        assertNotNull(result);
        assertNotNull(result.getItems());
        assertNotNull(result.getPortalCounts());
        assertTrue(result.getPortalCounts().containsKey("Ingatlanbazár"));
        assertFalse(result.getItems().isEmpty());
    }
}
