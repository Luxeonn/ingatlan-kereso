package ingatlanok.web;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.yaml.snakeyaml.Yaml;

import ingatlanok.presentation.DashboardResponseDto;
import ingatlanok.presentation.MarketSegmentStatsDto;
import ingatlanok.presentation.MarketStatsDto;
import ingatlanok.presentation.PortalStatusDto;
import ingatlanok.presentation.PriceDropItemDto;

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

  @Test
  @DisplayName("Prezentációs DTO-k minden publikus getterének szerepelnie kell az openapi/api-spec.yaml sémában")
  void testAllPresentationDtoPropertiesAreDocumentedInOpenApiSpec() throws IOException {
    Path specPath = getSpecPath();
    assertTrue(Files.exists(specPath), "Az openapi/api-spec.yaml fájlnak léteznie kell!");

    Yaml yaml = new Yaml();
    Map<String, Object> root;
    try (InputStream is = Files.newInputStream(specPath)) {
      root = yaml.load(is);
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> components = (Map<String, Object>) root.get("components");
    assertNotNull(components, "components szekció nem található az OpenAPI fájlban!");
    @SuppressWarnings("unchecked")
    Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
    assertNotNull(schemas, "schemas szekció nem található az OpenAPI fájlban!");

    List<Class<?>> dtoClasses = List.of(
        DashboardResponseDto.class,
        PortalStatusDto.class,
        MarketStatsDto.class,
        MarketSegmentStatsDto.class,
        PriceDropItemDto.class);

    for (Class<?> dtoClass : dtoClasses) {
      String schemaName = dtoClass.getSimpleName();
      @SuppressWarnings("unchecked")
      Map<String, Object> schema = (Map<String, Object>) schemas.get(schemaName);
      assertNotNull(schema, "OpenAPI séma hiányzik a DTO-hoz: " + schemaName);
      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
      assertNotNull(properties, "Nincsenek 'properties' definiálva a sémában: " + schemaName);

      for (Method method : dtoClass.getDeclaredMethods()) {
        if (!Modifier.isPublic(method.getModifiers()) || method.getParameterCount() > 0) {
          continue;
        }
        String name = method.getName();
        String propName = null;
        if (name.startsWith("get") && name.length() > 3 && !name.equals("getClass")) {
          propName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
        } else if (name.startsWith("is") && name.length() > 2) {
          propName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
        }

        if (propName != null) {
          assertTrue(
              properties.containsKey(propName),
              String.format(
                  "A(z) %s DTO getteréből származtatott '%s' tulajdonság hiányzik az OpenAPI '%s' sémájából!",
                  schemaName, propName, schemaName));
        }
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
