package ingatlanok;

import com.google.gson.Gson;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.scraper.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class IngatlanComSidecarIntegrationTest {

    private File tempScript = null;

    @AfterEach
    void tearDown() {
        SidecarCliExecutor.resetCustomExecutable();
        if (tempScript != null && tempScript.exists()) {
            tempScript.delete();
        }
    }

    @Test
    void testSidecarResponseDeserializationAndMapping() {
        String json = """
                {
                  "portalId": "ingatlancom",
                  "status": "SUCCESS",
                  "items": [
                    {
                      "id": "12345678",
                      "url": "https://ingatlan.com/12345678",
                      "title": "Debrecen, Belvárosi 2 szobás lakás",
                      "priceMillion": 42.5,
                      "priceRaw": "42,5 M Ft",
                      "areaSquareMeters": 58.0,
                      "roomCount": 2.0
                    }
                  ],
                  "errorMessage": null,
                  "elapsedMs": 850
                }
                """;

        SidecarResponse response = new Gson().fromJson(json, SidecarResponse.class);
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("ingatlancom", response.getPortalId());
        assertEquals(1, response.getItems().size());

        SidecarResponse.SidecarItem sidecarItem = response.getItems().getFirst();
        assertEquals("12345678", sidecarItem.getId());
        assertEquals(42.5, sidecarItem.getPriceMillion());

        IngatlanItem item = sidecarItem.toIngatlanItem(
                ingatlanok.model.PortalSource.of("Ingatlan.com"),
                "Debrecen",
                "2026-10-13 20:00:00",
                true
        );

        assertNotNull(item);
        assertEquals("12345678", item.getId());
        assertEquals("Ingatlan.com", item.getPortal());
        assertEquals("https://ingatlan.com/12345678", item.getUrl());
        assertEquals("Debrecen", item.getCity());
        assertTrue(item.isNew());
        assertNotNull(item.getPriceInfo());
        assertEquals(42.5, item.getPriceInfo().getAmountMillion());
        assertEquals(42500000L, item.getPriceInfo().getAmountHuf());
        assertNotNull(item.getDetailsInfo());
        assertEquals(58.0, item.getDetailsInfo().getAreaSquareMeters());
        assertEquals(2.0, item.getDetailsInfo().getRoomCount());
    }

    @Test
    void testSidecarCliExecutorBuildArguments() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");
        criteria.setPropertyType("haz");
        criteria.setMinPriceMillion(40);
        criteria.setMaxPriceMillion(80);

        List<String> args = SidecarCliExecutor.buildArguments("ingatlancom", criteria);
        assertNotNull(args);
        assertTrue(args.contains("--portal"));
        assertTrue(args.contains("ingatlancom"));
        assertTrue(args.contains("--city"));
        assertTrue(args.contains("debrecen"));
        assertTrue(args.contains("--type"));
        assertTrue(args.contains("haz"));
        assertTrue(args.contains("--min-price"));
        assertTrue(args.contains("40"));
        assertTrue(args.contains("--max-price"));
        assertTrue(args.contains("80"));
    }

    @Test
    void testSidecarCliExecutorGracefulFallbackWhenMissing() {
        SidecarCliExecutor.setCustomExecutable("non_existing_sidecar_executable_binary_12345");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");

        Optional<SidecarResponse> response = SidecarCliExecutor.execute("ingatlancom", criteria);
        assertTrue(response.isEmpty(), "Non-existing executable must result in Optional.empty without crashing");
    }

    @Test
    void testIngatlanComScraperFallbackWhenNoSidecar() {
        SidecarCliExecutor.setCustomExecutable("non_existing_sidecar_executable_binary_12345");

        SearchCriteria criteria = new SearchCriteria();
        criteria.setCity("Debrecen");
        criteria.setPropertyType("lakas");

        IngatlanComScraper scraper = new IngatlanComScraper();
        PortalScrapeResult result = scraper.scrape(criteria);

        assertNotNull(result);
        assertEquals("ingatlancom", result.getPortalId());

        assertTrue(result.getStatus() == PortalScrapeStatus.BOT_PROTECTED || result.getStatus() == PortalScrapeStatus.FAILED);
        assertNotNull(result.getFallbackUrl());
        assertTrue(result.getFallbackUrl().contains("ingatlan.com/lista/elado+lakas+debrecen"));
    }

    @Test
    void testIngatlanComScraperUrlGeneration() {
        IngatlanComScraper scraper = new IngatlanComScraper();

        SearchCriteria c1 = new SearchCriteria();
        c1.setCity("Hajdúszoboszló");
        c1.setPropertyType("haz");
        c1.setMinPriceMillion(100);
        c1.setMaxPriceMillion(120);

        String url1 = scraper.buildSearchUrl(c1);
        assertEquals("https://ingatlan.com/lista/elado+haz+hajduszoboszlo+100-120-mFt", url1);

        SearchCriteria c2 = new SearchCriteria();
        c2.setCity("Budapest");
        c2.setPropertyType("telek");
        c2.setMinPriceMillion(5);
        c2.setMaxPriceMillion(null);

        String url2 = scraper.buildSearchUrl(c2);
        assertEquals("https://ingatlan.com/lista/elado+telek+budapest+5-mFt-tol", url2);
    }
}
