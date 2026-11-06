package ingatlanok.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class OpenApiSpecIntegrityTest {

  @Test
  @DisplayName("OpenAPI 3.1 YAML specifikáció létezésének és alapvető végpontjainak ellenőrzése")
  void testOpenApiSpecIntegrity() throws IOException {
    Path specPath = getSpecPath();
    assertTrue(
        Files.exists(specPath),
        "Az openapi/api-spec.yaml fájlnak léteznie kell a projekt gyökerében!");

    String content = Files.readString(specPath);
    assertTrue(content.contains("openapi: 3.1.0"), "Az OpenAPI verziónak 3.1.0-nak kell lennie");

    List<String> requiredEndpoints =
        List.of(
            "/api/status:",
            "/api/portals:",
            "/api/cities:",
            "/api/config:",
            "/api/history:",
            "/api/properties/price-history:",
            "/api/properties/{id}/price-history:",
            "/api/properties/status-log:",
            "/api/properties/{id}/status-log:",
            "/api/properties/observations:",
            "/api/properties/{id}/observations:",
            "/api/properties/discounted:",
            "/api/search:",
            "/api/open-urls:",
            "/api/items/viewed:",
            "/api/history/clear:",
            "/api/presets:",
            "/api/presets/{id}:");

    for (String endpoint : requiredEndpoints) {
      assertTrue(
          content.contains(endpoint),
          "Az OpenAPI specifikációnak tartalmaznia kell a(z) " + endpoint + " végpontot!");
    }

    List<String> requiredSchemas =
        List.of(
            "SearchCriteria:",
            "SearchPreset:",
            "SearchPresetInput:",
            "DashboardResponseDto:",
            "IngatlanItem:",
            "PortalDescriptor:",
            "PortalStatusDto:",
            "PriceHistoryEntry:",
            "StatusLogEntry:",
            "SourceListing:",
            "ListingObservation:",
            "MarketStatsDto:",
            "CitiesResponse:",
            "StatusResponse:",
            "PropertyType:");

    for (String schema : requiredSchemas) {
      assertTrue(
          content.contains(schema),
          "Az OpenAPI specifikációnak tartalmaznia kell a(z) " + schema + " modellt!");
    }
  }

  @Test
  @DisplayName("Minden RestApiController végpont szerepel az openapi/api-spec.yaml fájlban")
  void testAllRestApiEndpointsAreDocumentedInOpenApiSpec() throws IOException {
    Path specPath = getSpecPath();
    assertTrue(Files.exists(specPath), "Az openapi/api-spec.yaml fájlnak léteznie kell!");
    String content = Files.readString(specPath);

    RequestMapping baseMapping = RestApiController.class.getAnnotation(RequestMapping.class);
    String basePath = (baseMapping != null && baseMapping.value().length > 0) ? baseMapping.value()[0] : "";

    for (Method method : RestApiController.class.getDeclaredMethods()) {
      List<String> methodPaths = new ArrayList<>();
      GetMapping get = method.getAnnotation(GetMapping.class);
      if (get != null) {
        String[] values = get.value().length > 0 ? get.value() : get.path();
        methodPaths.addAll(Arrays.asList(values));
      }
      PostMapping post = method.getAnnotation(PostMapping.class);
      if (post != null) {
        String[] values = post.value().length > 0 ? post.value() : post.path();
        methodPaths.addAll(Arrays.asList(values));
      }
      PutMapping put = method.getAnnotation(PutMapping.class);
      if (put != null) {
        String[] values = put.value().length > 0 ? put.value() : put.path();
        methodPaths.addAll(Arrays.asList(values));
      }
      DeleteMapping delete = method.getAnnotation(DeleteMapping.class);
      if (delete != null) {
        String[] values = delete.value().length > 0 ? delete.value() : delete.path();
        methodPaths.addAll(Arrays.asList(values));
      }

      for (String path : methodPaths) {
        String fullPath = basePath + (path.startsWith("/") ? path : "/" + path);
        assertTrue(
            content.contains(fullPath + ":"),
            "A(z) " + fullPath + " kontroller végpontnak szerepelnie kell az openapi/api-spec.yaml specifikációban!");
      }
    }
  }

  private Path getSpecPath() {
    Path specPath = Paths.get("openapi", "api-spec.yaml");
    if (!Files.exists(specPath)) {
      specPath = Paths.get("..", "openapi", "api-spec.yaml");
    }
    return specPath;
  }
}
