package ingatlanok.portal.ingatlancom;

import java.util.List;
import java.util.function.Function;

import org.jsoup.nodes.Document;

import ingatlanok.common.AppLogger;
import ingatlanok.common.HttpFetcher;
import ingatlanok.model.SearchCriteria;
import ingatlanok.model.raw.ExtractionMethod;
import ingatlanok.model.raw.RawExtractionResult;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.ExtractionStrategy;
import ingatlanok.portal.PortalSupport;
import ingatlanok.portal.sidecar.SidecarCliExecutor;

public class IngatlanComHtmlStrategy implements ExtractionStrategy {

  private static final AppLogger LOGGER = AppLogger.getLogger(IngatlanComHtmlStrategy.class);

  private final IngatlanComUrlBuilder urlBuilder;
  private final IngatlanComRawParser rawParser;
  private final Function<String, Document> documentFetcher;
  private final boolean defaultFetcher;

  public IngatlanComHtmlStrategy() {
    this(new IngatlanComUrlBuilder(), new IngatlanComRawParser(), null);
  }

  public IngatlanComHtmlStrategy(
      IngatlanComUrlBuilder urlBuilder,
      IngatlanComRawParser rawParser,
      Function<String, Document> documentFetcher) {
    this.urlBuilder = urlBuilder != null ? urlBuilder : new IngatlanComUrlBuilder();
    this.rawParser = rawParser != null ? rawParser : new IngatlanComRawParser();
    this.defaultFetcher = documentFetcher == null;
    this.documentFetcher = documentFetcher != null ? documentFetcher : HttpFetcher::fetchDocument;
  }

  @Override
  public ExtractionMethod method() {
    return ExtractionMethod.HTML;
  }

  @Override
  public RawExtractionResult extract(SearchCriteria criteria) {
    String searchUrl = urlBuilder.buildSearchUrl(criteria);
    if (defaultFetcher && SidecarCliExecutor.isAvailable()) {
      LOGGER.debug("Ingatlan.com: Sidecar elérhető volt, redundáns Jsoup lekérés kihagyása");
      return RawExtractionResult.botProtected(
          method(), searchUrl, "Cloudflare WAF / Sidecar fallback");
    }

    LOGGER.info("Ingatlan.com HTML stratégia indítása: {}", searchUrl);

    try {
      Document document = documentFetcher.apply(searchUrl);
      if (document == null) {
        LOGGER.warn("Ingatlan.com: Nem sikerült letölteni az oldalt -> WAF védelem feltételezve");
        return RawExtractionResult.botProtected(
            method(), searchUrl, "Nem sikerült letölteni az oldalt");
      }

      if (PortalSupport.isCommonBotChallenge(document)) {
        LOGGER.warn("Ingatlan.com: Cloudflare WAF bot-védelem detektálva");
        return RawExtractionResult.botProtected(
            method(), searchUrl, "Cloudflare WAF detektálva");
      }

      List<RawPropertyListing> listings = rawParser.parseRawListings(document);
      if (listings.isEmpty()) {
        LOGGER.warn(
            "Ingatlan.com: Nem sikerült közvetlen hirdetéseket kinyerni -> bot védelem feltételezve");
        return RawExtractionResult.botProtected(
            method(), searchUrl, "0 találat, bot-védelem aktív");
      }

      return RawExtractionResult.success(listings, method());
    } catch (Exception e) {
      LOGGER.error("Hiba az Ingatlan.com HTML stratégia futásakor: {}", e.getMessage(), e);
      return RawExtractionResult.failure(method(), searchUrl, e.getMessage(), 500);
    }
  }

  public IngatlanComUrlBuilder getUrlBuilder() {
    return urlBuilder;
  }

  public IngatlanComRawParser getRawParser() {
    return rawParser;
  }
}
