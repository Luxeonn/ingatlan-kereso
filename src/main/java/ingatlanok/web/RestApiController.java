package ingatlanok.web;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ingatlanok.common.CityUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.ListingObservation;
import ingatlanok.model.PriceHistoryEntry;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchPreset;
import ingatlanok.model.StatusLogEntry;
import ingatlanok.model.ValidationGroups;
import ingatlanok.persistence.ConfigStorage;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.portal.PortalDescriptor;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.presentation.ArchivePresentationService;
import ingatlanok.presentation.DashboardPresentationService;
import ingatlanok.presentation.DashboardResponseDto;
import ingatlanok.presentation.IngatlanComHtmlImportRequestDto;
import ingatlanok.presentation.IngatlanComHtmlImportResponseDto;

@RestController
@RequestMapping("/api")
public class RestApiController {

  private final ListingStorage listingStorage;
  private final ConfigStorage configStorage;
  private final DashboardPresentationService dashboardPresentationService;
  private final ArchivePresentationService archivePresentationService;
  private final PortalEngineRegistry portalEngineRegistry;

  @Value("${server.port:8080}")
  private int port;

  @Autowired
  public RestApiController(
      ListingStorage listingStorage,
      @Autowired(required = false) ConfigStorage configStorage,
      DashboardPresentationService dashboardPresentationService,
      ArchivePresentationService archivePresentationService,
      PortalEngineRegistry portalEngineRegistry) {
    this.listingStorage = listingStorage;
    this.configStorage = configStorage;
    this.dashboardPresentationService = dashboardPresentationService;
    this.archivePresentationService = archivePresentationService;
    this.portalEngineRegistry = portalEngineRegistry;
  }

  @GetMapping("/status")
  public ResponseEntity<StatusResponseDto> getStatus() {
    StatusResponseDto status =
        new StatusResponseDto(
            "UP",
            port,
            listingStorage.getTotalCount(),
            listingStorage.getLastRunTime(),
            listingStorage.getTrackedCitiesInfo());
    return ResponseEntity.ok(status);
  }

  @GetMapping("/portals")
  @Cacheable("portals")
  public ResponseEntity<List<PortalDescriptor>> getPortals() {
    Map<String, Long> portalCounts = listingStorage.countByPortal();
    List<PortalDescriptor> descriptors =
        portalEngineRegistry.getAllDescriptors().stream()
            .map(
                d -> {
                  long count = 0L;
                  String dId = d.id().trim().toLowerCase(Locale.ROOT);
                  String dName = d.name().trim().toLowerCase(Locale.ROOT);
                  for (Map.Entry<String, Long> entry : portalCounts.entrySet()) {
                    String k = entry.getKey().trim().toLowerCase(Locale.ROOT);
                    if (k.equals(dId) || k.equals(dName) || k.contains(dId) || dName.contains(k)) {
                      count += entry.getValue();
                    }
                  }
                  return d.withSavedListingCount(count);
                })
            .sorted(
                Comparator.comparingLong(PortalDescriptor::savedListingCount)
                    .reversed()
                    .thenComparing(PortalDescriptor::name))
            .toList();
    return ResponseEntity.ok(descriptors);
  }

  @GetMapping("/cities")
  @Cacheable("cities")
  public ResponseEntity<CitiesResponseDto> getCities() {
    CitiesResponseDto data =
        new CitiesResponseDto(
            CityUtils.getPredefinedCities(),
            CityUtils.getPresets(),
            listingStorage.getTrackedCitiesInfo());
    return ResponseEntity.ok(data);
  }

  @GetMapping("/config")
  public ResponseEntity<SearchCriteria> getLastCriteria() {
    SearchCriteria criteria = configStorage != null ? configStorage.getLastCriteria() : null;
    if (criteria == null) {
      criteria = new SearchCriteria();
    }
    return ResponseEntity.ok(criteria);
  }

  @PostMapping("/config")
  public ResponseEntity<ActionSuccessResponseDto> saveLastCriteria(
      @Valid @RequestBody(required = false) SearchCriteria criteria) {
    if (criteria != null && configStorage != null) {
      configStorage.saveLastCriteria(criteria);
    }
    return ResponseEntity.ok(ActionSuccessResponseDto.of(true));
  }

  @GetMapping("/presets")
  public ResponseEntity<List<SearchPreset>> getPresets() {
    if (configStorage == null) {
      return ResponseEntity.ok(List.of());
    }
    return ResponseEntity.ok(configStorage.getPresets());
  }

  @PostMapping("/presets")
  public ResponseEntity<SearchPreset> createPreset(
      @Validated(ValidationGroups.OnCreate.class) @RequestBody SearchPreset preset) {
    if (configStorage == null) {
      return ResponseEntity.badRequest().build();
    }
    SearchPreset created = configStorage.savePreset(preset);
    return ResponseEntity.ok(created);
  }

  @PutMapping("/presets/{id}")
  public ResponseEntity<SearchPreset> updatePreset(
      @PathVariable("id") @Positive(message = "Érvénytelen preset azonosító") Long id,
      @Validated(ValidationGroups.OnUpdate.class) @RequestBody SearchPreset preset) {
    if (configStorage == null) {
      return ResponseEntity.badRequest().build();
    }
    return configStorage.updatePreset(id, preset)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @DeleteMapping("/presets/{id}")
  public ResponseEntity<ActionSuccessResponseDto> deletePreset(
      @PathVariable("id") @Positive(message = "Érvénytelen preset azonosító") Long id) {
    if (configStorage == null) {
      return ResponseEntity.badRequest().build();
    }
    boolean deleted = configStorage.deletePreset(id);
    if (!deleted) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(ActionSuccessResponseDto.of(true));
  }

  @PostMapping("/presets/{id}/use")
  public ResponseEntity<SearchPreset> usePreset(
      @PathVariable("id") @Positive(message = "Érvénytelen preset azonosító") Long id) {
    if (configStorage == null) {
      return ResponseEntity.badRequest().build();
    }
    return configStorage.markPresetAsUsed(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/history")
  public ResponseEntity<List<IngatlanItem>> getHistory(
      @Valid @ModelAttribute HistoryQueryDto query) {
    HistoryQueryDto effectiveQuery =
        query != null
            ? query
            : new HistoryQueryDto(
                null, false, null, null, null, null, null, null, null, false, false, "DISCOVERED_DESC", 50, 0);
    List<IngatlanItem> dtoList = archivePresentationService.getHistory(effectiveQuery.toCriteria());
    return ResponseEntity.ok(dtoList);
  }

  @GetMapping("/properties/price-history")
  public ResponseEntity<List<PriceHistoryEntry>> getPriceHistoryByParam(
      @Valid @ModelAttribute PropertyLookupQueryDto query) {
    List<PriceHistoryEntry> history = listingStorage.getPriceHistory(query.resolveKey());
    return ResponseEntity.ok(history);
  }

  @GetMapping("/properties/{id}/price-history")
  public ResponseEntity<List<PriceHistoryEntry>> getPriceHistoryById(
      @PathVariable("id")
          @NotBlank(message = "Hiányzó 'id' vagy 'url' paraméter!")
          @Size(max = 1000, message = "A megadott azonosító vagy URL túl hosszú!")
          String id) {
    List<PriceHistoryEntry> history = listingStorage.getPriceHistory(id);
    return ResponseEntity.ok(history);
  }

  @GetMapping("/properties/status-log")
  public ResponseEntity<List<StatusLogEntry>> getStatusLogByParam(
      @Valid @ModelAttribute PropertyLookupQueryDto query) {
    List<StatusLogEntry> log = listingStorage.getStatusLog(query.resolveKey());
    return ResponseEntity.ok(log);
  }

  @GetMapping("/properties/{id}/status-log")
  public ResponseEntity<List<StatusLogEntry>> getStatusLogById(
      @PathVariable("id")
          @NotBlank(message = "Hiányzó 'id' vagy 'url' paraméter!")
          @Size(max = 1000, message = "A megadott azonosító vagy URL túl hosszú!")
          String id) {
    List<StatusLogEntry> log = listingStorage.getStatusLog(id);
    return ResponseEntity.ok(log);
  }

  @GetMapping("/properties/observations")
  public ResponseEntity<List<ListingObservation>> getListingObservationsByParam(
      @Valid @ModelAttribute PropertyLookupQueryDto query) {
    List<ListingObservation> observations =
        listingStorage.getListingObservations(query.resolveKey());
    return ResponseEntity.ok(observations);
  }

  @GetMapping("/properties/{id}/observations")
  public ResponseEntity<List<ListingObservation>> getListingObservationsById(
      @PathVariable("id")
          @NotBlank(message = "Hiányzó 'id' vagy 'url' paraméter!")
          @Size(max = 1000, message = "A megadott azonosító vagy URL túl hosszú!")
          String id) {
    List<ListingObservation> observations = listingStorage.getListingObservations(id);
    return ResponseEntity.ok(observations);
  }

  @GetMapping("/properties/hidden")
  public ResponseEntity<List<IngatlanItem>> getHiddenProperties() {
    List<IngatlanItem> hidden = archivePresentationService.getHiddenProperties();
    return ResponseEntity.ok(hidden);
  }

  @PostMapping("/properties/{id}/hide")
  public ResponseEntity<PropertyHideResponseDto> hideProperty(
      @PathVariable("id")
          @NotBlank(message = "Az azonosító nem lehet üres!")
          @Size(max = 500, message = "Az azonosító túl hosszú!")
          String id,
      @Valid @RequestBody(required = false) PropertyBatchActionRequest body) {
    boolean success = processBatchAction(id, body, listingStorage::hideProperty);
    if (!success) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(PropertyHideResponseDto.of(true, true, id));
  }

  @PostMapping("/properties/{id}/restore")
  public ResponseEntity<PropertyHideResponseDto> restoreProperty(
      @PathVariable("id")
          @NotBlank(message = "Az azonosító nem lehet üres!")
          @Size(max = 500, message = "Az azonosító túl hosszú!")
          String id,
      @Valid @RequestBody(required = false) PropertyBatchActionRequest body) {
    boolean success = processBatchAction(id, body, listingStorage::restoreProperty);
    if (!success) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(PropertyHideResponseDto.of(true, false, id));
  }

  private boolean processBatchAction(
      String id, PropertyBatchActionRequest body, Predicate<String> action) {
    boolean success = action.test(id);
    if (body != null) {
      if (body.url() != null && !body.url().isBlank()) {
        success = action.test(body.url()) || success;
      }
      if (body.urls() != null) {
        for (String item : body.urls()) {
          if (item != null && !item.isBlank()) {
            success = action.test(item) || success;
          }
        }
      }
    }
    return success;
  }

  @GetMapping("/properties/discounted")
  public ResponseEntity<List<IngatlanItem>> getDiscountedProperties(
      @RequestParam(value = "city", required = false)
          @Size(max = 200, message = "A település neve túl hosszú")
          String city) {
    List<IngatlanItem> discounted = archivePresentationService.getDiscountedProperties(city);
    return ResponseEntity.ok(discounted);
  }

  @PostMapping("/portals/ingatlancom/import-html")
  public ResponseEntity<IngatlanComHtmlImportResponseDto> importIngatlanComHtml(
      @Valid @RequestBody IngatlanComHtmlImportRequestDto body) {
    return ResponseEntity.ok(dashboardPresentationService.importIngatlanComHtml(body));
  }

  @PostMapping("/search")
  public ResponseEntity<DashboardResponseDto> executeSearch(
      @Valid @RequestBody(required = false) SearchCriteria criteria) {
    SearchCriteria effectiveCriteria = criteria != null ? criteria : new SearchCriteria();
    DashboardResponseDto result = dashboardPresentationService.executeSearch(effectiveCriteria);
    return ResponseEntity.ok(result);
  }

  @PostMapping("/items/viewed")
  public ResponseEntity<MarkViewedResponseDto> markViewed(
      @Valid @RequestBody(required = false) MarkViewedRequest body) {
    String url = body != null ? body.url() : null;
    boolean marked = false;
    if (url != null && !url.isBlank()) {
      marked = listingStorage.markAsViewed(url);
    }
    return ResponseEntity.ok(MarkViewedResponseDto.of(true, marked, url != null ? url : ""));
  }

  @PostMapping("/history/clear")
  public ResponseEntity<ClearHistoryResponseDto> clearHistory(
      @Valid @RequestBody(required = false) ClearHistoryRequest body,
      @RequestParam(value = "city", required = false)
          @Size(max = 200, message = "A település neve túl hosszú")
          String paramCity) {
    String city = paramCity;
    if ((city == null || city.isBlank()) && body != null && body.city() != null) {
      city = body.city();
    }

    if (city != null && !city.isBlank()) {
      listingStorage.clearCityHistory(city);
      return ResponseEntity.ok(ClearHistoryResponseDto.forCity(city));
    } else {
      listingStorage.clearHistory();
      return ResponseEntity.ok(ClearHistoryResponseDto.forAll());
    }
  }
}
