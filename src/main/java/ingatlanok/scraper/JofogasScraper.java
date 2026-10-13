package ingatlanok.scraper;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.IngatlanRepository;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Scraper implementáció a Jófogás Ingatlan (jofogas.hu) portálhoz.
 */
public class JofogasScraper extends BaseScraper implements IngatlanScraper {

    private static final String BASE_URL = "https://ingatlan.jofogas.hu";
    private static final Pattern ID_PATTERN = Pattern.compile("_(\\d+)\\.htm");

    @Override
    public String getName() {
        return "Jófogás Ingatlan";
    }

    @Override
    public void init() {
        // Init logic
    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 25;
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
        String typeWord = switch (rawType) {
            case "haz" -> "haz";
            case "garazs" -> "garazs";
            case "telek" -> "telek";
            case "nyaralo" -> "nyaralo";
            case "iroda" -> "iroda";
            case "osszes" -> "ingatlan";
            default -> "lakas";
        };

        String query = criteria.getCity() + " elado " + typeWord;
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        StringBuilder urlBuilder = new StringBuilder(BASE_URL)
                .append("/magyarorszag/ingatlan?q=").append(encodedQuery);

        if (criteria.getMinPriceMillion() != null) {
            urlBuilder.append("&min_price=").append(criteria.getMinPriceMillion() * 1_000_000L);
        }
        if (criteria.getMaxPriceMillion() != null) {
            urlBuilder.append("&max_price=").append(criteria.getMaxPriceMillion() * 1_000_000L);
        }

        String searchUrl = urlBuilder.toString();

        System.out.println("Jófogás Ingatlan keresés: " + searchUrl);

        Document document = getDocument(searchUrl);
        if (document == null) {
            System.err.println("Nem sikerült letölteni a Jófogás keresési oldalt: " + searchUrl);
            return items;
        }

        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        Set<String> processed = new HashSet<>();

        Elements links = document.select("a[href*=/ingatlan/], a[href*=/hajdu_bihar/], a[href*=_]");
        for (Element a : links) {
            String href = a.attr("href");
            if (href == null || href.isBlank() || !href.endsWith(".htm")) continue;

            String fullLink = href.startsWith("http") ? href : BASE_URL + (href.startsWith("/") ? "" : "/") + href;
            if (processed.contains(fullLink)) continue;
            processed.add(fullLink);

            String cardText = a.text().replaceAll("\\s+", " ").trim();
            if (cardText.length() < 10) continue; // Navigációs gombok kihagyása

            String id = PropertyParser.extractFirstGroup(fullLink, ID_PATTERN, UUID.randomUUID().toString().substring(0, 8));
            String price = PropertyParser.extractPrice(cardText);
            String details = PropertyParser.extractDetails(cardText);
            String title = cleanTitle(cardText, criteria.getCity(), id);
            boolean isNew = !IngatlanRepository.getInstance().containsUrl(fullLink);

            IngatlanItem item = new IngatlanItem(
                    id,
                    "Jófogás Ingatlan",
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

        System.out.println("jofogas.hu Találatok száma : " + items.size() + " (Új: " + items.stream().filter(IngatlanItem::isNew).count() + ")");
        return items;
    }

    private String cleanTitle(String text, String defaultCity, String id) {
        if (text != null) {
            int mFtIndex = text.indexOf("M Ft");
            if (mFtIndex > 0) {
                String prefix = text.substring(0, mFtIndex).replaceAll("\\d+[,.]?\\d*\\s*$", "").trim();
                if (prefix.length() > 5) {
                    return prefix;
                }
            }
        }
        return defaultCity + " - Jófogás ingatlan (" + id + ")";
    }
}
