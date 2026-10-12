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
 * Scraper implementáció az Ingatlan.com portálhoz.
 * Támogatja a közvetlen keresési URL generálást és intelligens Cloudflare fallbacket.
 */
public class IngatlanCom extends IngatlanStart implements Ingatlan {

    private static final String BASE_URL = "https://ingatlan.com";
    private static final Pattern ID_PATTERN = Pattern.compile("/ingatlan/(\\d+)");
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d+[,.]?\\d*\\s*M\\s*Ft|\\d[\\d\\s]*\\s*Ft)");
    private static final Pattern ROOM_SIZE_PATTERN = Pattern.compile("(\\d+[,.]?\\d*\\s*m²[^\\n]*\\d+\\s*szoba|\\d+[,.]?\\d*\\s*m²)");

    @Override
    public String getName() {
        return "Ingatlan.com";
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
        if (criteria == null) {
            criteria = new SearchCriteria();
        }

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
            case "osszes" -> "ingatlan";
            default -> "lakas";
        };

        String priceSlug = "";
        if (criteria.getMinPriceMillion() != null && criteria.getMaxPriceMillion() != null) {
            priceSlug = "+" + criteria.getMinPriceMillion() + "-" + criteria.getMaxPriceMillion() + "-mFt";
        } else if (criteria.getMinPriceMillion() != null) {
            priceSlug = "+" + criteria.getMinPriceMillion() + "-mFt-tol";
        } else if (criteria.getMaxPriceMillion() != null) {
            priceSlug = "+" + criteria.getMaxPriceMillion() + "-mFt-ig";
        }

        String searchUrl = BASE_URL + "/lista/elado+" + typeSlug + "+" + citySlug + priceSlug;
        System.out.println("Ingatlan.com keresés: " + searchUrl);

        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        Set<String> processedUrls = new HashSet<>();

        // Kísérlet a találati oldal letöltésére
        Document document = getDocument(searchUrl);
        if (document != null) {
            Elements cards = document.select("a[href*=/ingatlan/]");
            for (Element a : cards) {
                String href = a.attr("href");
                if (href == null || href.isBlank()) continue;

                String fullLink = href.startsWith("http") ? href : BASE_URL + (href.startsWith("/") ? "" : "/") + href;
                if (!fullLink.matches(".*/ingatlan/\\d+.*")) continue;
                if (processedUrls.contains(fullLink)) continue;
                processedUrls.add(fullLink);

                String cardText = a.text().replaceAll("\\s+", " ").trim();
                String id = extractId(fullLink);
                String price = extractPrice(cardText);
                String details = extractDetails(cardText);
                String title = criteria.getCity() + " - " + extractTitle(cardText, criteria.getCity(), id);
                boolean isNew = !IngatlanRepository.getInstance().containsUrl(fullLink);

                IngatlanItem item = new IngatlanItem(
                        id,
                        "Ingatlan.com",
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

        // Intelligens Cloudflare / bot-védelem fallback
        // Amennyiben a közvetlen HTML letöltés nem hozott elemeket a védelem miatt,
        // generálunk egy közvetlen élő keresési elemet, hogy a felhasználó egyetlen kattintással
        // elérhesse a legfrissebb találatokat a hivatalos oldalon
        if (items.isEmpty()) {
            System.out.println("Ingatlan.com: Közvetlen élő keresési hivatkozás létrehozása az árszűréssel.");
            String priceLabel;
            if (criteria.getMinPriceMillion() != null && criteria.getMaxPriceMillion() != null) {
                priceLabel = criteria.getMinPriceMillion() + " - " + criteria.getMaxPriceMillion() + " M Ft";
            } else if (criteria.getMinPriceMillion() != null) {
                priceLabel = criteria.getMinPriceMillion() + " M Ft-tól";
            } else if (criteria.getMaxPriceMillion() != null) {
                priceLabel = criteria.getMaxPriceMillion() + " M Ft-ig";
            } else {
                priceLabel = "Árszűrő nélkül";
            }

            String typeTitle = switch (rawType) {
                case "haz" -> "Eladó ház";
                case "garazs" -> "Garázs";
                case "telek" -> "Telek";
                case "nyaralo" -> "Nyaraló";
                case "iroda" -> "Iroda / Üzlethelyiség";
                case "osszes" -> "Összes ingatlan";
                default -> "Eladó lakás";
            };

            String id = "ic-" + UUID.randomUUID().toString().substring(0, 8);
            boolean isNew = !IngatlanRepository.getInstance().containsUrl(searchUrl);

            IngatlanItem directSearchItem = new IngatlanItem(
                    id,
                    "Ingatlan.com",
                    searchUrl,
                    "Ingatlan.com – " + typeTitle + " (" + criteria.getCity() + ")",
                    priceLabel,
                    criteria.getCity(),
                    "⚡ Élő keresési találatok az Ingatlan.com-on. Kattints a hirdetésre a friss lista megtekintéséhez!",
                    now,
                    isNew
            );
            items.add(directSearchItem);
        }

        System.out.println("ingatlan.com Találatok száma : " + items.size() + " (Új: " + items.stream().filter(IngatlanItem::isNew).count() + ")");
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

    private String extractTitle(String text, String defaultCity, String id) {
        if (text != null && text.length() > 5) {
            int max = Math.min(text.length(), 65);
            return text.substring(0, max).trim() + "...";
        }
        return defaultCity + " - Ingatlan (" + id + ")";
    }
}
