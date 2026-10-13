package ingatlanok.scraper;

import ingatlanok.common.AppLogger;
import ingatlanok.common.PropertyParser;
import ingatlanok.common.SlugUtils;
import ingatlanok.model.IngatlanItem;
import ingatlanok.model.PortalSource;
import ingatlanok.model.PriceInfo;
import ingatlanok.model.PropertyDetails;
import ingatlanok.model.SearchCriteria;
import ingatlanok.persistence.IngatlanRepository;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

public class IngatlanComScraper extends BaseScraper implements IngatlanScraper {

    private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanComScraper.class);
    private static final String BASE_URL = "https://ingatlan.com";
    private static final Pattern ID_PATTERN = Pattern.compile("/ingatlan/(\\d+)");
    private static final PortalSource SOURCE = PortalSource.of("Ingatlan.com");

    @Override
    public String getPortalId() {
        return "ingatlancom";
    }

    @Override
    public String getName() {
        return "Ingatlan.com";
    }

    @Override
    public void init() {

    }

    @Override
    public int getCardNumber(Element resultPageSize) {
        return 20;
    }

    public String buildSearchUrl(SearchCriteria criteria) {
        if (criteria == null) {
            criteria = new SearchCriteria();
        }

        String citySlug = SlugUtils.toSlug(criteria.getCity());
        if (citySlug.isEmpty()) {
            citySlug = "debrecen";
        }

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

        return BASE_URL + "/lista/elado+" + typeSlug + "+" + citySlug + priceSlug;
    }

    @Override
    public PortalScrapeResult scrape(SearchCriteria criteria) {
        long startTime = System.currentTimeMillis();
        List<IngatlanItem> items = new ArrayList<>();
        if (criteria == null) {
            criteria = new SearchCriteria();
        }

        String searchUrl = buildSearchUrl(criteria);
        LOGGER.info("Ingatlan.com keresés indítása: {}", searchUrl);

        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        Set<String> processedUrls = new HashSet<>();

        if (SidecarCliExecutor.isAvailable()) {
            LOGGER.info("Ingatlan.com: Sidecar modul detektálva, lekérdezés indítása...");
            try {
                Optional<SidecarResponse> sidecarResp = SidecarCliExecutor.execute(getPortalId(), criteria);
                if (sidecarResp.isPresent() && sidecarResp.get().isSuccess()) {
                    SidecarResponse resp = sidecarResp.get();
                    for (SidecarResponse.SidecarItem sidecarItem : resp.getItems()) {
                        if (sidecarItem.getUrl() == null || sidecarItem.getUrl().isBlank()) continue;
                        if (!processedUrls.add(sidecarItem.getUrl())) continue;

                        boolean isNew = !IngatlanRepository.getInstance().containsUrl(sidecarItem.getUrl());
                        IngatlanItem item = sidecarItem.toIngatlanItem(SOURCE, criteria.getCity(), now, isNew);

                        if (criteria.matches(item)) {
                            items.add(item);
                        }
                    }

                    long elapsed = System.currentTimeMillis() - startTime;
                    if (!items.isEmpty()) {
                        long newCount = items.stream().filter(IngatlanItem::isNew).count();
                        LOGGER.info("ingatlan.com (Sidecar) Találatok száma: {} (Új: {}) [{} ms]", items.size(), newCount, elapsed);
                        return PortalScrapeResult.success(getPortalId(), getName(), items, elapsed);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Ingatlan.com: Hiba a sidecar végrehajtás során: {}", e.getMessage(), e);
            }
        }

        try {
            Document document = getDocument(searchUrl);
            if (document != null) {
                Elements cards = document.select("a[href*=/ingatlan/]");
                LOGGER.debug("Ingatlan.com kártya elemek: {} db", cards.size());
                for (Element a : cards) {
                    String href = a.attr("href");
                    if (href == null || href.isBlank()) continue;

                    String fullLink = href.startsWith("http") ? href : BASE_URL + (href.startsWith("/") ? "" : "/") + href;
                    if (!fullLink.matches(".*/ingatlan/\\d+.*")) continue;
                    if (processedUrls.contains(fullLink)) continue;
                    processedUrls.add(fullLink);

                    String cardText = a.text().replaceAll("\\s+", " ").trim();
                    String id = PropertyParser.extractFirstGroup(fullLink, ID_PATTERN, UUID.randomUUID().toString().substring(0, 8));
                    PriceInfo priceInfo = PropertyParser.parsePrice(cardText);
                    PropertyDetails detailsInfo = PropertyParser.parseDetails(cardText, priceInfo);
                    String title = criteria.getCity() + " - " + extractTitle(cardText, criteria.getCity(), id);
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

                    if (!criteria.matches(item)) {
                        LOGGER.debug("Ingatlan.com tétel kiszűrve: {} (Ár: {})", item.getUrl(), item.getPrice());
                        continue;
                    }

                    items.add(item);
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            if (items.isEmpty()) {
                LOGGER.warn("Ingatlan.com: Nem sikerült közvetlen hirdetéseket kinyerni (bot-védelem aktív) [{} ms] -> fallback: {}", elapsed, searchUrl);
                return PortalScrapeResult.botProtected(getPortalId(), getName(), searchUrl, elapsed);
            }

            long newCount = items.stream().filter(IngatlanItem::isNew).count();
            LOGGER.info("ingatlan.com Találatok száma: {} (Új: {}) [{} ms]", items.size(), newCount, elapsed);
            return PortalScrapeResult.success(getPortalId(), getName(), items, elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.error("Hiba az Ingatlan.com keresésekor [{} ms]: {}", elapsed, e.getMessage(), e);
            return PortalScrapeResult.failure(getPortalId(), getName(), e.getMessage(), elapsed);
        }
    }

    private String extractTitle(String text, String defaultCity, String id) {
        if (text != null && text.length() > 5) {
            int max = Math.min(text.length(), 65);
            return text.substring(0, max).trim() + "...";
        }
        return defaultCity + " - Ingatlan (" + id + ")";
    }
}
