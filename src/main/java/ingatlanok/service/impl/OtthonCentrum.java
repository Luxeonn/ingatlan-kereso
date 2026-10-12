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
 * Scraper implementáció az Otthon Centrum (oc.hu) portálhoz.
 */
public class OtthonCentrum extends IngatlanStart implements Ingatlan {

    private static final String BASE_URL = "https://www.oc.hu";
    private static final Pattern ID_PATTERN = Pattern.compile("/ingatlanok/([A-Za-z0-9]+)");
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d+[,.]?\\d*\\s*M\\s*Ft|\\d[\\d\\s]*\\s*Ft)");
    private static final Pattern ROOM_SIZE_PATTERN = Pattern.compile("(\\d+[,.]?\\d*\\s*m²[^\\n]*\\d+\\s*szoba|\\d+[,.]?\\d*\\s*m²)");

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
        String jelleg = "haz".equalsIgnoreCase(criteria.getPropertyType()) ? "haz" : "lakas";
        String cityParam = criteria.getCity().toLowerCase().contains("debrecen") ? "debrecen=on" : "q=" + criteria.getCity();
        String searchUrl = BASE_URL + "/ingatlanok/kereses?realestate%5Bertekesites%5D=elado&realestate%5Bjelleg%5D=" + jelleg + "&" + cityParam;

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
                String id = extractId(fullLink);
                String price = extractPrice(cardText);
                String details = extractDetails(cardText);
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
        if (text != null && text.contains("szoba")) {
            // Vegyük a szoba utáni részt ha van
            int idx = text.indexOf("szoba");
            String after = text.substring(idx + 5).trim();
            if (after.length() > 5) {
                // levágjuk a túl hosszú leírást 60 karakterre
                int end = Math.min(after.length(), 65);
                return after.substring(0, end).trim() + "...";
            }
        }
        return defaultCity + " - Eladó ingatlan (" + id + ")";
    }
}
