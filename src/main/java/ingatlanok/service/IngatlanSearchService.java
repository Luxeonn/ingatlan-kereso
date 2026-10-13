package ingatlanok.service;

import ingatlanok.common.AppLogger;
import ingatlanok.common.BrowserUtils;
import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.scraper.IngatlanScraper;
import ingatlanok.scraper.PortalScrapeResult;
import ingatlanok.scraper.ScraperRegistry;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class IngatlanSearchService {

    private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanSearchService.class);
    private static final IngatlanSearchService INSTANCE = new IngatlanSearchService();

    private IngatlanSearchService() {
    }

    public static IngatlanSearchService getInstance() {
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

        IngatlanRepository repository = IngatlanRepository.getInstance();
        SearchResult result = new SearchResult();

        List<IngatlanItem> allAggregatedItems = new ArrayList<>();
        Map<String, Integer> portalCounts = new LinkedHashMap<>();
        portalCounts.put("Duna House", 0);
        portalCounts.put("Otthon Centrum", 0);
        portalCounts.put("Jófogás Ingatlan", 0);
        portalCounts.put("Ingatlan.com", 0);
        portalCounts.put("Ingatlanbazár", 0);

        Map<String, Integer> cityCounts = new LinkedHashMap<>();

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

            boolean isFirstSearch = !repository.hasCityDatabase(currentCity);
            if (isFirstSearch) {
                result.getNewCitiesInitialized().add(currentCity);
                LOGGER.info("[Új Város] '{}' még nem szerepel az adatbázisban. Alapállapot (baseline snapshot) rögzítése...", currentCity);
            } else {
                LOGGER.info("[Ismert Város] '{}' adatbázisa létezik. Differenciál-keresés...", currentCity);
            }

            SearchCriteria cityCriteria = criteria.cloneForCity(currentCity);
            List<IngatlanItem> cityFoundItems = new ArrayList<>();

            List<IngatlanScraper> activeScrapers = ScraperRegistry.getInstance().getActiveScrapers(cityCriteria);
            for (IngatlanScraper scraper : activeScrapers) {
                try {
                    LOGGER.info("[{}/{}] {} lekérdezése ({})...", cityIndex, citiesToSearch.size(), scraper.getName(), currentCity);
                    PortalScrapeResult scrapeResult = scraper.scrape(cityCriteria);
                    result.getPortalResults().add(scrapeResult);
                    List<IngatlanItem> items = scrapeResult.getItems();
                    cityFoundItems.addAll(items);
                    portalCounts.put(scraper.getName(), portalCounts.getOrDefault(scraper.getName(), 0) + items.size());
                } catch (Exception e) {
                    LOGGER.error("Hiba a(z) {} keresésekor ({}): {}", scraper.getName(), currentCity, e.getMessage(), e);
                }
            }

            Map<String, IngatlanItem> uniqueCityMap = new LinkedHashMap<>();
            for (IngatlanItem item : cityFoundItems) {
                if (!cityCriteria.matches(item)) {
                    LOGGER.debug("[Kiszűrve] {} találat: {} (Ár: {}, Lokáció: {}, Link: {})",
                            item.getPortal(), item.getTitle(), item.getPrice(), item.getLocation(), item.getUrl());
                    continue;
                }

                if (item.getCity() == null || item.getCity().isBlank()) {
                    item.setCity(currentCity);
                }
                if (item.getLocation() == null || item.getLocation().isBlank()) {
                    item.setLocation(currentCity);
                }

                if (isFirstSearch) {
                    item.setNew(false);
                } else {
                    boolean isNew = repository.isNewUrl(currentCity, item.getUrl());
                    item.setNew(isNew);
                }

                uniqueCityMap.putIfAbsent(item.getUrl(), item);
            }

            List<IngatlanItem> finalCityItems = new ArrayList<>(uniqueCityMap.values());
            repository.saveCityItems(currentCity, finalCityItems);

            cityCounts.put(currentCity, finalCityItems.size());
            allAggregatedItems.addAll(finalCityItems);
        }

        Map<String, IngatlanItem> uniqueAggregatedMap = new LinkedHashMap<>();
        for (IngatlanItem item : allAggregatedItems) {
            uniqueAggregatedMap.putIfAbsent(item.getUrl(), item);
        }
        List<IngatlanItem> uniqueAggregatedList = new ArrayList<>(uniqueAggregatedMap.values());

        List<IngatlanItem> newItems = uniqueAggregatedList.stream()
                .filter(IngatlanItem::isNew)
                .collect(Collectors.toList());

        repository.saveLastCriteria(criteria);

        if (criteria.isAutoOpenBrowser() && !newItems.isEmpty()) {
            Set<String> urlsToOpen = newItems.stream().map(IngatlanItem::getUrl).collect(Collectors.toCollection(LinkedHashSet::new));
            BrowserUtils.openUrls(urlsToOpen);
        }

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
}
