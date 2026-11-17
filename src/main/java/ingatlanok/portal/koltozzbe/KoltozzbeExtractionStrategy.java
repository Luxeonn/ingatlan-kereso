package ingatlanok.portal.koltozzbe;

import java.util.List;
import java.util.function.Function;

import org.jsoup.nodes.Document;

import ingatlanok.common.HttpFetcher;
import ingatlanok.model.raw.RawPropertyListing;
import ingatlanok.portal.AbstractPaginatedHtmlStrategy;
import ingatlanok.portal.PaginationState;
import ingatlanok.portal.PaginationStopReason;
import ingatlanok.portal.PortalSupport;

public class KoltozzbeExtractionStrategy extends AbstractPaginatedHtmlStrategy {

  private final KoltozzbeRawParser koltozzbeRawParser;

  public KoltozzbeExtractionStrategy() {
    this(
        new KoltozzbeUrlBuilder(),
        new KoltozzbeRawParser(),
        HttpFetcher::fetchDocument,
        KoltozzbePortalEngine.DEFAULT_MAX_PAGES);
  }

  public KoltozzbeExtractionStrategy(
      KoltozzbeUrlBuilder urlBuilder,
      KoltozzbeRawParser rawParser,
      Function<String, Document> documentFetcher,
      int maxPages) {
    this(
        urlBuilder != null ? urlBuilder : new KoltozzbeUrlBuilder(),
        rawParser != null ? rawParser : new KoltozzbeRawParser(),
        documentFetcher,
        maxPages,
        true);
  }

  private KoltozzbeExtractionStrategy(
      KoltozzbeUrlBuilder urlBuilder,
      KoltozzbeRawParser rawParser,
      Function<String, Document> documentFetcher,
      int maxPages,
      boolean normalized) {
    super(
        urlBuilder,
        rawParser::parseHtmlListings,
        documentFetcher,
        maxPages,
        KoltozzbePortalEngine.DEFAULT_MAX_PAGES);
    this.koltozzbeRawParser = rawParser;
  }

  @Override
  protected String getPortalLogName() {
    return "Költözzbe";
  }

  @Override
  protected String getFallbackBaseUrl() {
    return KoltozzbeUrlBuilder.BASE_URL;
  }

  @Override
  protected PaginationStopReason processPageDocument(
      Document doc, int page, PaginationState state) {
    List<RawPropertyListing> pageHtml = koltozzbeRawParser.parseHtmlListings(doc);
    int addedHtml = addOrMergeListings(pageHtml, state);

    List<RawPropertyListing> pageJsonLd = koltozzbeRawParser.parseJsonLdListings(doc);
    int addedJsonLd = addOrMergeListings(pageJsonLd, state);

    boolean hasContent = !pageHtml.isEmpty() || !pageJsonLd.isEmpty();
    if (!hasContent) {
      return page == 1 ? PaginationStopReason.FIRST_PAGE_EMPTY : PaginationStopReason.EMPTY_PAGE;
    }
    if (addedHtml + addedJsonLd == 0) {
      return PaginationStopReason.REPEATED_PAGE_CONTENT;
    }
    if (!koltozzbeRawParser.hasNextPage(doc, page)) {
      return PaginationStopReason.NO_NEXT_PAGE;
    }
    return null;
  }

  private int addOrMergeListings(List<RawPropertyListing> candidates, PaginationState state) {
    if (candidates == null) {
      return 0;
    }
    List<RawPropertyListing> target = state.getAllListings();
    int added = 0;
    for (RawPropertyListing raw : candidates) {
      if (raw == null) {
        continue;
      }
      if (state.markProcessedListing(raw)) {
        target.add(raw);
        added++;
      } else {
        mergeIntoExistingListing(target, raw);
      }
    }
    return added;
  }

  private void mergeIntoExistingListing(
      List<RawPropertyListing> target, RawPropertyListing incoming) {
    String incomingUrl = PortalSupport.canonicalListingUrl(incoming.url());
    String incomingId = incoming.nativeId() != null ? incoming.nativeId().trim() : "";
    for (int i = 0; i < target.size(); i++) {
      RawPropertyListing existing = target.get(i);
      if (isSameListing(existing, incomingUrl, incomingId)) {
        target.set(i, mergeComplementaryFields(existing, incoming));
        return;
      }
    }
  }

  private boolean isSameListing(
      RawPropertyListing existing, String incomingUrl, String incomingId) {
    if (existing == null) {
      return false;
    }
    String existingUrl = PortalSupport.canonicalListingUrl(existing.url());
    if (!incomingUrl.isEmpty() && incomingUrl.equals(existingUrl)) {
      return true;
    }
    String existingId = existing.nativeId() != null ? existing.nativeId().trim() : "";
    return !incomingId.isEmpty() && incomingId.equals(existingId);
  }

  private RawPropertyListing mergeComplementaryFields(
      RawPropertyListing existing, RawPropertyListing incoming) {
    return RawPropertyListing.builder()
        .portalId(firstNonBlank(existing.portalId(), incoming.portalId()))
        .nativeId(firstNonBlank(existing.nativeId(), incoming.nativeId()))
        .url(firstNonBlank(existing.url(), incoming.url()))
        .title(firstNonBlank(existing.title(), incoming.title()))
        .priceText(firstNonBlank(existing.priceText(), incoming.priceText()))
        .priceHuf(existing.priceHuf() != null ? existing.priceHuf() : incoming.priceHuf())
        .detailsText(firstNonBlank(existing.detailsText(), incoming.detailsText()))
        .areaSquareMeters(
            existing.areaSquareMeters() != null
                ? existing.areaSquareMeters()
                : incoming.areaSquareMeters())
        .roomCount(existing.roomCount() != null ? existing.roomCount() : incoming.roomCount())
        .locationText(firstNonBlank(existing.locationText(), incoming.locationText()))
        .imageUrl(firstNonBlank(existing.imageUrl(), incoming.imageUrl()))
        .descriptionText(firstNonBlank(existing.descriptionText(), incoming.descriptionText()))
        .method(existing.method() != null ? existing.method() : incoming.method())
        .extra(existing.extra() != null ? existing.extra() : incoming.extra())
        .build();
  }

  private String firstNonBlank(String primary, String fallback) {
    return (primary != null && !primary.isBlank()) ? primary : fallback;
  }
}
