package ingatlanok.portal;

import java.util.List;

import org.jsoup.nodes.Document;

import ingatlanok.model.raw.RawPropertyListing;

/** Uniform parser contract for extracting raw property listings from a Jsoup {@link Document}. */
@FunctionalInterface
public interface HtmlListingParser {

  List<RawPropertyListing> parseRawListings(Document document);
}
