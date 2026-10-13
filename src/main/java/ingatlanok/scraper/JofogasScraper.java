package ingatlanok.scraper;

import ingatlanok.common.PropertyParser;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
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

public class JofogasScraper extends BaseScraper implements IngatlanScraper {

    private static final String BASE_URL = "https://ingatlan.jofogas.hu";
    private static final Pattern ID_PATTERN = Pattern.compile("_(\\d+)\\.htm");
    private static final PortalSource SOURCE = PortalSource.of("Jófogás Ingatlan");

    @Override
    public String getPortalId() {
        return "jofogas";
    }

    @Override
    public String getName() {
        return "Jófogás Ingatlan";
    }

    @Override
    public void init() {

    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 25;
    }

    public String buildSearchUrl(SearchCriteria criteria) {
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

        return urlBuilder.toString();
    }

    @Override
    public PortalScrapeResult scrape(SearchCriteria criteria) {
        long startTime = System.currentTimeMillis();
        List<IngatlanItem> items = new ArrayList<>();
        if (criteria == null) {
            criteria = new SearchCriteria();
        }

        String searchUrl = buildSearchUrl(criteria);
        System.out.println("Jófogás Ingatlan keresés: " + searchUrl);

        try {
            Document document = getDocument(searchUrl);
            if (document == null) {
                long elapsed = System.currentTimeMillis() - startTime;
                System.err.println("Nem sikerült letölteni a Jófogás keresési oldalt: " + searchUrl);
                return PortalScrapeResult.failure(getPortalId(), getName(), "Nem sikerült letölteni az oldalt", elapsed);
            }

            String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
            Set<String> processed = new HashSet<>();

            Elements links = document.select("a[href*=/ingatlan/], a[href*=_]");
            for (Element a : links) {
                String href = a.attr("href");
                if (href == null || href.isBlank() || !href.endsWith(".htm")) continue;

                String fullLink = href.startsWith("http") ? href : BASE_URL + (href.startsWith("/") ? "" : "/") + href;
                if (processed.contains(fullLink)) continue;
                processed.add(fullLink);

                String cardText = a.text().replaceAll("\\s+", " ").trim();
                if (cardText.length() < 10) continue;

                String id = PropertyParser.extractFirstGroup(fullLink, ID_PATTERN, UUID.randomUUID().toString().substring(0, 8));
                PriceInfo priceInfo = PropertyParser.parsePrice(cardText);
                PropertyDetails detailsInfo = PropertyParser.parseDetails(cardText, priceInfo);
                String title = cleanTitle(cardText, criteria.getCity(), id);
                boolean isNew = !IngatlanRepository.getInstance().containsUrl(fullLink);

                IngatlanItem item = new IngatlanItem(
                        id,
                        SOURCE,
                        fullLink,
                        title,
                        priceInfo,
                        criteria.getCity(),
                        detailsInfo,
                        now,
                        isNew
                );
                item.setLocation(criteria.getCity());

                String imageUrl = PropertyParser.extractImageUrl(a);
                if (imageUrl == null && a.parent() != null) {
                    imageUrl = PropertyParser.extractImageUrl(a.parent());
                }
                item.setImageUrl(imageUrl);

                if (!criteria.matches(item)) {
                    continue;
                }

                items.add(item);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("jofogas.hu Találatok száma : " + items.size() + " (Új: " + items.stream().filter(IngatlanItem::isNew).count() + ")");
            return PortalScrapeResult.success(getPortalId(), getName(), items, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            System.err.println("Hiba a Jófogás keresésekor: " + e.getMessage());
            return PortalScrapeResult.failure(getPortalId(), getName(), e.getMessage(), elapsed);
        }
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
