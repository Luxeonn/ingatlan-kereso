package ingatlanok.service;

import ingatlanok.helper.IngatlanHelper;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.SearchResult;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.service.impl.DunaHause;
import ingatlanok.service.impl.IngatlanBazar;
import ingatlanok.service.impl.IngatlanCom;
import ingatlanok.service.impl.JofogasIngatlan;
import ingatlanok.service.impl.OtthonCentrum;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Központi keresési szolgáltatás, amely vezérli az egyes portálok lekérdezését és aggregálja az eredményeket.
 */
public class IngatlanSearchService {

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

        System.out.println("==================================================");
        System.out.println("Keresés indítása: Város=" + criteria.getCity() + ", Jelleg=" + criteria.getPropertyType());
        System.out.println("Aktív portálok: " + criteria.getPortals());
        System.out.println("==================================================");

        List<IngatlanItem> allFoundItems = new ArrayList<>();
        Map<String, Integer> portalCounts = new HashMap<>();

        // 1. Duna House
        if (criteria.isPortalEnabled("dh")) {
            try {
                System.out.println("\n[1/4] Duna House lekérdezése...");
                List<IngatlanItem> dhItems = new DunaHause().search(criteria);
                allFoundItems.addAll(dhItems);
                portalCounts.put("Duna House", dhItems.size());
            } catch (Exception e) {
                System.err.println("Hiba a Duna House keresésekor: " + e.getMessage());
                portalCounts.put("Duna House", 0);
            }
        }

        // 2. Otthon Centrum
        if (criteria.isPortalEnabled("oc")) {
            try {
                System.out.println("\n[2/4] Otthon Centrum lekérdezése...");
                List<IngatlanItem> ocItems = new OtthonCentrum().search(criteria);
                allFoundItems.addAll(ocItems);
                portalCounts.put("Otthon Centrum", ocItems.size());
            } catch (Exception e) {
                System.err.println("Hiba az Otthon Centrum keresésekor: " + e.getMessage());
                portalCounts.put("Otthon Centrum", 0);
            }
        }

        // 3. Jófogás Ingatlan
        if (criteria.isPortalEnabled("jofogas")) {
            try {
                System.out.println("\n[3/4] Jófogás Ingatlan lekérdezése...");
                List<IngatlanItem> jfItems = new JofogasIngatlan().search(criteria);
                allFoundItems.addAll(jfItems);
                portalCounts.put("Jófogás Ingatlan", jfItems.size());
            } catch (Exception e) {
                System.err.println("Hiba a Jófogás keresésekor: " + e.getMessage());
                portalCounts.put("Jófogás Ingatlan", 0);
            }
        }

        // 4. Ingatlan.com
        if (criteria.isPortalEnabled("ingatlancom")) {
            try {
                System.out.println("\n[4/4] Ingatlan.com lekérdezése...");
                List<IngatlanItem> icItems = new IngatlanCom().search(criteria);
                allFoundItems.addAll(icItems);
                portalCounts.put("Ingatlan.com", icItems.size());
            } catch (Exception e) {
                System.err.println("Hiba az Ingatlan.com keresésekor: " + e.getMessage());
                portalCounts.put("Ingatlan.com", 0);
            }
        }

        // Duplikációk szűrése az aktuális találati listán belül (egyedi URL alapján)
        Map<String, IngatlanItem> uniqueFound = new LinkedHashMap<>();
        for (IngatlanItem item : allFoundItems) {
            uniqueFound.putIfAbsent(item.getUrl(), item);
        }
        List<IngatlanItem> uniqueList = new ArrayList<>(uniqueFound.values());

        // Számoljuk az újakat
        List<IngatlanItem> newItems = uniqueList.stream()
                .filter(IngatlanItem::isNew)
                .collect(Collectors.toList());

        // Mentés a JSON adatbázisba
        IngatlanRepository.getInstance().saveAll(uniqueList);
        IngatlanRepository.getInstance().saveLastCriteria(criteria);

        // Opcionális automatikus böngészőfül nyitás ha kérte a felhasználó
        if (criteria.isAutoOpenBrowser() && !newItems.isEmpty()) {
            Set<String> urlsToOpen = newItems.stream().map(IngatlanItem::getUrl).collect(Collectors.toCollection(LinkedHashSet::new));
            IngatlanHelper.openUrl(urlsToOpen);
        }

        SearchResult result = new SearchResult();
        // A legfrissebbek elöl
        uniqueList.sort((a, b) -> Boolean.compare(!a.isNew(), !b.isNew()));
        result.setItems(uniqueList);
        result.setNewCount(newItems.size());
        result.setTotalFound(uniqueList.size());
        result.setExecutedAt(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
        result.setPortalCounts(portalCounts);

        System.out.println("\n==================================================");
        System.out.println("Keresés befejeződött: " + result.getExecutedAt());
        System.out.println("Összes találat: " + result.getTotalFound() + " | Új: " + result.getNewCount());
        System.out.println("==================================================");

        return result;
    }
}
