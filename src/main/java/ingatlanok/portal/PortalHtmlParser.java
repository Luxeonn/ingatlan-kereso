package ingatlanok.portal;

import java.util.List;
import java.util.Set;

import org.jsoup.nodes.Document;

import ingatlanok.model.IngatlanItem;
import ingatlanok.model.SearchCriteria;

/** Uniform contract for Jsoup HTML portal parsers with pagination and WAF detection hooks. */
public interface PortalHtmlParser {

  List<IngatlanItem> parseItems(
      Document document, SearchCriteria criteria, String executionTime, Set<String> processedUrls);

  default boolean hasNextPage(Document document, int currentPage) {
    return true;
  }

  default boolean isBotChallenge(Document document) {
    return PortalSupport.isCommonBotChallenge(document);
  }
}
