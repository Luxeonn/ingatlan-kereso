package ingatlanok.scraper;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.IngatlanRepository;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Scraper implementáció az Otthon Centrum (oc.hu) portálhoz.
 */
public class OtthonCentrumScraper extends BaseScraper implements IngatlanScraper {

    private static final String BASE_URL = "https://www.oc.hu";
    private static final Pattern ID_PATTERN = Pattern.compile("/ingatlanok/([A-Za-z0-9]+)");

    @Override
    public String getName() {
        return "Otthon Centrum";
    }

    @Override
    public void init() {
        // Init logic
    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 12;
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
        if (criteria == null) {
            criteria = new SearchCriteria();
        }

        String rawType = criteria.getPropertyType() != null ? criteria.getPropertyType().toLowerCase().trim() : "lakas";
        String jellegParam = switch (rawType) {
            case "haz" -> "2";
            case "nyaralo" -> "3";
            case "telek" -> "4";
            case "garazs" -> "5";
            case "iroda" -> "6";
            case "osszes" -> "";
            default -> "1";
        };

        String cityParam = criteria.getCity().toLowerCase().contains("debrecen") ? "debrecen=on" : "q=" + criteria.getCity();
        StringBuilder urlBuilder = new StringBuilder(BASE_URL)
                .append("/ingatlanok/kereses?realestate%5Bertekesites%5D=elado");

        if (!jellegParam.isEmpty()) {
            urlBuilder.append("&realestate%5Bjelleg%5D=").append(jellegParam);
        }
        if (criteria.getMinPriceMillion() != null) {
            urlBuilder.append("&realestate%5Bar%5D%5Bmin%5D=").append(criteria.getMinPriceMillion());
        }
        if (criteria.getMaxPriceMillion() != null) {
            urlBuilder.append("&realestate%5Bar%5D%5Bmax%5D=").append(criteria.getMaxPriceMillion());
        }
        urlBuilder.append("&").append(cityParam);

        String searchUrl = urlBuilder.toString();

        System.out.println("Otthon Centrum keresés: " + searchUrl);

        Document document = getDocument(searchUrl);
        if (document == null) {
            System.err.println("Nem sikerült letölteni az Otthon Centrum keresési oldalt: " + searchUrl);
            return items;
        }

        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        Set<String> processed = new HashSet<>();

        Elements links = document.select("a[href*=/ingatlanok/]");
        for (Element a : links) {
            String href = a.attr("href");
            if (href == null || href.isBlank()) continue;

            if (href.matches(".*/ingatlanok/[A-Za-z0-9]+$")) {
                String fullLink = href.startsWith("http") ? href : BASE_URL + (href.startsWith("/") ? "" : "/") + href;
                if (processed.contains(fullLink)) continue;
                processed.add(fullLink);

                String cardText = a.text().replaceAll("\\s+", " ").trim();
                String id = PropertyParser.extractFirstGroup(fullLink, ID_PATTERN, UUID.randomUUID().toString().substring(0, 8));
                String price = PropertyParser.extractPrice(cardText);
                String details = PropertyParser.extractDetails(cardText);
                String title = cleanTitle(cardText, criteria.getCity(), id);
                boolean isNew = !IngatlanRepository.getInstance().containsUrl(fullLink);

                IngatlanItem item = new IngatlanItem(
                        id,
                        "Otthon Centrum",
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

        System.out.println("oc.hu Találatok száma : " + items.size() + " (Új: " + items.stream().filter(IngatlanItem::isNew).count() + ")");
        return items;
    }

    private String cleanTitle(String text, String defaultCity, String id) {
        if (text != null && text.contains("szoba")) {
            int idx = text.indexOf("szoba");
            String after = text.substring(idx + 5).trim();
            if (after.length() > 5) {
                int end = Math.min(after.length(), 65);
                return after.substring(0, end).trim() + "...";
            }
        }
        return defaultCity + " - Eladó ingatlan (" + id + ")";
    }
}
