package ingatlanok.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiSpecIntegrityTest {

    @Test
    @DisplayName("OpenAPI 3.1 YAML specifikáció létezésének és alapvető végpontjainak ellenőrzése")
    void testOpenApiSpecIntegrity() throws IOException {
        Path specPath = Paths.get("openapi", "api-spec.yaml");
        if (!Files.exists(specPath)) {
            specPath = Paths.get("..", "openapi", "api-spec.yaml");
        }

        assertTrue(Files.exists(specPath), "Az openapi/api-spec.yaml fájlnak léteznie kell a projekt gyökerében!");

        String content = Files.readString(specPath);
        assertTrue(content.contains("openapi: 3.1.0"), "Az OpenAPI verziónak 3.1.0-nak kell lennie");

        List<String> requiredEndpoints = List.of(
                "/api/status:",
                "/api/portals:",
                "/api/cities:",
                "/api/config:",
                "/api/history:",
                "/api/properties/price-history:",
                "/api/properties/{id}/price-history:",
                "/api/properties/status-log:",
                "/api/properties/{id}/status-log:",
                "/api/properties/discounted:",
                "/api/search:",
                "/api/open-urls:",
                "/api/items/viewed:",
                "/api/history/clear:"
        );

        for (String endpoint : requiredEndpoints) {
            assertTrue(content.contains(endpoint), "Az OpenAPI specifikációnak tartalmaznia kell a(z) " + endpoint + " végpontot!");
        }

        List<String> requiredSchemas = List.of(
                "SearchCriteria:",
                "DashboardResponseDto:",
                "IngatlanItem:",
                "ListingDto:",
                "PortalDescriptor:",
                "PortalStatusDto:",
                "PriceHistoryEntry:",
                "StatusLogEntry:",
                "MarketStatsDto:",
                "CitiesResponse:",
                "StatusResponse:"
        );

        for (String schema : requiredSchemas) {
            assertTrue(content.contains(schema), "Az OpenAPI specifikációnak tartalmaznia kell a(z) " + schema + " modellt!");
        }
    }
}
