package ingatlanok.core;

import ingatlanok.common.AppLogger;
import ingatlanok.common.BrowserUtils;
import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.ConfigStorage;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.persistence.ListingStorage;
import ingatlanok.portal.PortalEngine;
import ingatlanok.portal.PortalEngineRegistry;
import ingatlanok.scraper.PortalScrapeResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Párhuzamos Virtual Threads keresésvezérlő és orchestrator motor.
 * Clean Architecture elv: szigorú réteg-izoláció, 0-Crash garancia és egyedi portál timeoutok.
 */
public class SearchExecutionEngine {

    private static final AppLogger LOGGER = AppLogger.getLogger(SearchExecutionEngine.class);
    private static final long PORTAL_TIMEOUT_SECONDS = 15;
    private static final SearchExecutionEngine INSTANCE = new SearchExecutionEngine();

    private final PortalEngineRegistry portalRegistry;
    private final ListingStorage listingStorage;
    private final ConfigStorage configStorage;
    private final PropertyFilterEngine filterEngine;
    private final DeduplicationEngine dedupEngine;
    private final MarketAnalyticsEngine analyticsEngine;

    private SearchExecutionEngine() {
        this(
                PortalEngineRegistry.getInstance(),
                IngatlanRepository.getInstance().getListingStorage(),
                IngatlanRepository.getInstance().getConfigStorage(),
                new PropertyFilterEngine(),
                new DeduplicationEngine(IngatlanRepository.getInstance().getListingStorage()),
                new MarketAnalyticsEngine()
        );
    }

    public SearchExecutionEngine(PortalEngineRegistry portalRegistry,
                                 ListingStorage listingStorage,
                                 ConfigStorage configStorage,
                                 PropertyFilterEngine filterEngine,
                                 DeduplicationEngine dedupEngine,
                                 MarketAnalyticsEngine analyticsEngine) {
        this.portalRegistry = portalRegistry != null ? portalRegistry : PortalEngineRegistry.getInstance();
        this.listingStorage = listingStorage != null ? listingStorage : IngatlanRepository.getInstance().getListingStorage();
        this.configStorage = configStorage != null ? configStorage : IngatlanRepository.getInstance().getConfigStorage();
        this.filterEngine = filterEngine != null ? filterEngine : new PropertyFilterEngine();
        this.dedupEngine = dedupEngine != null ? dedupEngine : new DeduplicationEngine(this.listingStorage);
        this.analyticsEngine = analyticsEngine != null ? analyticsEngine : new MarketAnalyticsEngine();
    }

    public static SearchExecutionEngine getInstance() {
        return INSTANCE;
    }

    public SearchResult executeSearch(SearchCriteria criteria) {
        if (criteria == null) {
            criteria = new SearchCriteria();
        }

        List<String> citiesToSearch = criteria.getCities();
        if (citiesToSearch.isEmpty()) {
            citiesToSearch = List.of("Debrecen");
        }

        LOGGER.info("==================================================");
        LOGGER.info("Keresés indítása: Városok={}, Jelleg={}, Ársáv={}-{} M Ft, Alapterület={}-{} m²",
                citiesToSearch, criteria.getPropertyType(), criteria.getMinPriceMillion(), criteria.getMaxPriceMillion(),
                criteria.getMinArea(), criteria.getMaxArea());
        LOGGER.info("Aktív portálok: {}", criteria.getPortals());
        LOGGER.info("==================================================");

        SearchResult result = new SearchResult();
        List<IngatlanItem> allAggregatedItems = new ArrayList<>();
        Map<String, Integer> portalCounts = new LinkedHashMap<>();
        Map<String, Integer> cityCounts = new LinkedHashMap<>();

        // Portál számlálók alapértelmezett inicializálása
        for (PortalEngine engine : portalRegistry.getAllEngines()) {
            portalCounts.put(engine.getName(), 0);
        }

        int cityIndex = 0;
        for (String currentCity : citiesToSearch) {
            cityIndex++;
            if (cityIndex > 1) {
                try {
                    Thread.sleep(400);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }

            boolean isFirstSearch = !listingStorage.hasCityDatabase(currentCity);
            if (isFirstSearch) {
                result.getNewCitiesInitialized().add(currentCity);
                LOGGER.info("[Új Város] '{}' még nem szerepel az adatbázisban. Alapállapot (baseline snapshot) rögzítése...", currentCity);
            } else {
                LOGGER.info("[Ismert Város] '{}' adatbázisa létezik. Differenciál-keresés...", currentCity);
            }

            SearchCriteria cityCriteria = criteria.cloneForCity(currentCity);
            List<PortalEngine> activeEngines = portalRegistry.getActiveEngines(cityCriteria);
            List<IngatlanItem> cityFoundItems = new ArrayList<>();

            // Párhuzamos portál lekérdezés Virtual Threads alapon, szigorú timeouttal és 0-Crash garanciával
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Map<PortalEngine, Future<PortalScrapeResult>> futures = new LinkedHashMap<>();
                for (PortalEngine engine : activeEngines) {
                    futures.put(engine, executor.submit(() -> engine.scrape(cityCriteria)));
                }

                for (Map.Entry<PortalEngine, Future<PortalScrapeResult>> entry : futures.entrySet()) {
                    PortalEngine engine = entry.getKey();
                    Future<PortalScrapeResult> future = entry.getValue();
                    try {
                        PortalScrapeResult scrapeResult = future.get(PORTAL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        result.getPortalResults().add(scrapeResult);
                        List<IngatlanItem> items = scrapeResult.getItems();
                        if (items != null) {
                            cityFoundItems.addAll(items);
                            portalCounts.put(engine.getName(), portalCounts.getOrDefault(engine.getName(), 0) + items.size());
                        }
                        LOGGER.info("  ├─ [{}] {} db hirdetés letöltve (Válaszidő: {} ms, Státusz: {})",
                                engine.getName(), items != null ? items.size() : 0, scrapeResult.getExecutionTimeMs(), scrapeResult.getStatus());
                    } catch (TimeoutException te) {
                        future.cancel(true);
                        LOGGER.warn("Időtúllépés ({} mp) a(z) {} keresésekor ({})", PORTAL_TIMEOUT_SECONDS, engine.getName(), currentCity);
                        PortalScrapeResult timeoutResult = PortalScrapeResult.failure(
                                engine.getId(), engine.getName(), "Időtúllépés (" + PORTAL_TIMEOUT_SECONDS + " mp)", PORTAL_TIMEOUT_SECONDS * 1000
                        );
                        result.getPortalResults().add(timeoutResult);
                    } catch (Exception e) {
                        LOGGER.error("Hiba a(z) {} keresésekor ({}): {}", engine.getName(), currentCity, e.getMessage(), e);
                        PortalScrapeResult failResult = PortalScrapeResult.failure(
                                engine.getId(), engine.getName(), e.getMessage(), 0
                        );
                        result.getPortalResults().add(failResult);
                    }
                }
            }

            // Kritérium szerinti szűrés a PropertyFilterEngine segítségével
            PropertyFilterEngine.FilterResult filterResult = filterEngine.filter(cityFoundItems, cityCriteria, currentCity);
            List<IngatlanItem> filteredCityItems = filterResult.matchingItems();

            // Új státusz vizsgálat és baseline snapshot kezelés
            dedupEngine.applyNewStatus(currentCity, filteredCityItems, isFirstSearch);

            // Városszintű duplikátumszűrés URL alapján
            List<IngatlanItem> uniqueCityItems = dedupEngine.deduplicateByUrl(filteredCityItems);

            // Városi találatok atomi mentése a ListingStorage segítségével
            listingStorage.saveCityItems(currentCity, uniqueCityItems);

            int cityMatchingCount = uniqueCityItems.size();
            int cityTotalFiltered = cityFoundItems.size() - cityMatchingCount;
            LOGGER.info("  └─ [{}] Összesítés: {} letöltve -> {} megfelelő találat (Kiszűrve: {} db | Ár: {}, Méret: {}, Megtekintett: {})",
                    currentCity, cityFoundItems.size(), cityMatchingCount, cityTotalFiltered,
                    filterResult.filteredByPriceCount(), filterResult.filteredByAreaCount(), filterResult.filteredByViewedCount());

            cityCounts.put(currentCity, uniqueCityItems.size());
            allAggregatedItems.addAll(uniqueCityItems);
        }

        // Globális duplikátumszűrés
        List<IngatlanItem> uniqueAggregatedList = dedupEngine.deduplicateByUrl(allAggregatedItems);

        List<IngatlanItem> newItems = uniqueAggregatedList.stream()
                .filter(IngatlanItem::isNew)
                .collect(Collectors.toList());

        // Keresési konfiguráció atomi mentése
        configStorage.saveLastCriteria(criteria);

        // Opcionális böngésző megnyitás
        if (criteria.isAutoOpenBrowser() && !newItems.isEmpty()) {
            Set<String> urlsToOpen = newItems.stream()
                    .map(IngatlanItem::getUrl)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            BrowserUtils.openUrls(urlsToOpen);
        }

        // Rendezés ár növekvő szerint (alapértelmezett), majd új találat és dátum
        uniqueAggregatedList.sort((a, b) -> {
            Long priceA = (a.getPriceInfo() != null) ? a.getPriceInfo().getAmountHuf() : null;
            if (priceA == null && a.getPrice() != null) {
                priceA = PropertyParser.parsePrice(a.getPrice()).getAmountHuf();
            }
            Long priceB = (b.getPriceInfo() != null) ? b.getPriceInfo().getAmountHuf() : null;
            if (priceB == null && b.getPrice() != null) {
                priceB = PropertyParser.parsePrice(b.getPrice()).getAmountHuf();
            }

            if (priceA != null && priceB != null) {
                int cmp = Long.compare(priceA, priceB);
                if (cmp != 0) return cmp;
            } else if (priceA != null) {
                return -1;
            } else if (priceB != null) {
                return 1;
            }

            if (a.isNew() != b.isNew()) {
                return Boolean.compare(!a.isNew(), !b.isNew());
            }
            if (a.getDiscoveredAt() == null) return 1;
            if (b.getDiscoveredAt() == null) return -1;
            return b.getDiscoveredAt().compareTo(a.getDiscoveredAt());
        });

        result.setItems(uniqueAggregatedList);
        result.setNewCount(newItems.size());
        result.setTotalFound(uniqueAggregatedList.size());
        result.setExecutedAt(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
        result.setPortalCounts(portalCounts);
        result.setCityCounts(cityCounts);

        LOGGER.info("==================================================");
        LOGGER.info("Keresés befejeződött: {}", result.getExecutedAt());
        LOGGER.info("Összes találat: {} | Új: {}", result.getTotalFound(), result.getNewCount());
        LOGGER.info("Városok szerint: {}", result.getCityCounts());
        if (!result.getNewCitiesInitialized().isEmpty()) {
            LOGGER.info("Új város(ok) baseline mentve: {}", result.getNewCitiesInitialized());
        }
        LOGGER.info("==================================================");

        return result;
    }

    public PortalEngineRegistry getPortalRegistry() {
        return portalRegistry;
    }

    public ListingStorage getListingStorage() {
        return listingStorage;
    }

    public ConfigStorage getConfigStorage() {
        return configStorage;
    }

    public PropertyFilterEngine getFilterEngine() {
        return filterEngine;
    }

    public DeduplicationEngine getDedupEngine() {
        return dedupEngine;
    }

    public MarketAnalyticsEngine getAnalyticsEngine() {
        return analyticsEngine;
    }
}
