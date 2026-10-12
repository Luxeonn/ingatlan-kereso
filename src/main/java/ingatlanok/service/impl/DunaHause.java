package ingatlanok.service.impl;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.IngatlanRepository;
import ingatlanok.service.Ingatlan;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Scraper implementáció a Duna House (dh.hu) portálhoz.
 */
public class DunaHause extends IngatlanStart implements Ingatlan {

    private static final String BASE_URL = "https://dh.hu";
    private static final Pattern ID_PATTERN = Pattern.compile("/ingatlan/([A-Za-z0-9]+)");
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d[\\d\\s]*\\s*Ft)");
    private static final Pattern ROOM_SIZE_PATTERN = Pattern.compile("(\\d+\\s*szoba[^\\d]*\\d+\\s*m²|\\d+\\s*m²)");

    @Override
    public String getName() {
        return "Duna House";
    }

    @Override
    public void init() {
        // Init logic
    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 20;
    }

    @Override
    public Set<String> getUrls() {
        return search(new SearchCriteria()).stream()
                .filter(IngatlanItem::isNew)
                .map(IngatlanItem::getUrl)
                .collect(Collectors.toSet());
    }

    @Override
    public List<IngatlanItem> search(SearchCriteria criteria) {
        List<IngatlanItem> items = new ArrayList<>();
        String citySlug = criteria.getCity().toLowerCase().trim()
                .replace("é", "e").replace("á", "a").replace("ó", "o")
                .replace("ő", "o").replace("ö", "o").replace("ú", "u")
                .replace("ű", "u").replace("ü", "u").replace("í", "i")
                .replaceAll("[^a-z0-9]", "-");

        String rawType = criteria.getPropertyType() != null ? criteria.getPropertyType().toLowerCase().trim() : "lakas";
        String typeSlug = switch (rawType) {
            case "haz" -> "haz";
            case "garazs" -> "garazs";
            case "telek" -> "telek";
            case "nyaralo" -> "nyaralo";
            case "iroda" -> "iroda";
            case "osszes" -> "lakas-haz";
            default -> "lakas";
        };

        String priceSlug = "";
        if (criteria.getMinPriceMillion() != null && criteria.getMaxPriceMillion() != null) {
            priceSlug = "/" + criteria.getMinPriceMillion() + "-" + criteria.getMaxPriceMillion() + "-mFt";
        } else if (criteria.getMinPriceMillion() != null) {
            priceSlug = "/" + criteria.getMinPriceMillion() + "-mFt-tol";
        } else if (criteria.getMaxPriceMillion() != null) {
            priceSlug = "/" + criteria.getMaxPriceMillion() + "-mFt-ig";
        }

        String searchRoute = "/elado-ingatlan/" + typeSlug + "/" + (citySlug.equals("debrecen") ? "hajdu-bihar/debrecen" : citySlug) + priceSlug;
        String searchUrl = BASE_URL + searchRoute;

        System.out.println("Duna House keresés: " + searchUrl);

        int maxPages = 2;
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        Set<String> processedUrls = new HashSet<>();

        for (int i = 1; i <= maxPages; i++) {
            String pageUrl = (i == 1) ? searchUrl : searchUrl + "/oldal-" + i;
            Document document = getDocument(pageUrl);
            if (document == null) {
                System.err.println("Nem sikerült letölteni a Duna House oldalt: " + pageUrl);
                continue;
            }

            Elements anchorLinks = document.select("a[href*=/ingatlan/]");
            for (Element a : anchorLinks) {
                String href = a.attr("href");
                if (href == null || href.isBlank()) continue;

                String fullLink = href.startsWith("http") ? href : BASE_URL + (href.startsWith("/") ? "" : "/") + href;
                if (!fullLink.matches(".*/ingatlan/[A-Za-z0-9]+/.*") && !fullLink.matches(".*/ingatlan/[A-Za-z0-9]+$")) {
                    continue;
                }
                if (processedUrls.contains(fullLink)) {
                    continue;
                }
                processedUrls.add(fullLink);

                String cardText = a.text().replaceAll("\\s+", " ").trim();
                String id = extractId(fullLink);
                String price = extractPrice(cardText);
                String details = extractDetails(cardText);
                String title = cleanTitle(cardText, criteria.getCity(), id);
                boolean isNew = !IngatlanRepository.getInstance().containsUrl(fullLink);

                IngatlanItem item = new IngatlanItem(
                        id,
                        "Duna House",
                        fullLink,
                        title,
                        price,
                        criteria.getCity(),
                        details,
                        now,
                        isNew
                );
                items.add(item);
            }
        }

        System.out.println("dh.hu Találatok száma : " + items.size() + " (Új: " + items.stream().filter(IngatlanItem::isNew).count() + ")");
        return items;
    }

    private String extractId(String url) {
        Matcher m = ID_PATTERN.matcher(url);
        return m.find() ? m.group(1) : UUID.randomUUID().toString().substring(0, 8);
    }

    private String extractPrice(String text) {
        Matcher m = PRICE_PATTERN.matcher(text);
        return m.find() ? m.group(1).trim() : "Ár a hirdetésben";
    }

    private String extractDetails(String text) {
        Matcher m = ROOM_SIZE_PATTERN.matcher(text);
        return m.find() ? m.group(1).trim() : "";
    }

    private String cleanTitle(String text, String defaultCity, String id) {
        if (text != null && text.contains("|")) {
            String[] parts = text.split("\\|");
            if (parts.length > 0 && !parts[0].isBlank()) {
                return parts[0].replace("Új ", "").trim();
            }
        }
        return defaultCity + " - Eladó ingatlan (" + id + ")";
    }
}
