package ingatlanok.scraper;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import ingatlanok.common.HttpFetcher;

public abstract class BaseScraper {

  public abstract void init();

  public abstract int getCardNumber(Element resultPageSize);

  public Document getUrlContent(int page, String url) {
    String fullUrl =
        url.contains("?")
            ? url + "&page=" + page
            : url + (url.endsWith("/") ? "" : "/") + "oldal-" + page;
    return getDocument(fullUrl);
  }

  public Document getDocument(String targetUrl) {
    return HttpFetcher.fetchDocument(targetUrl);
  }

  public String getJson(String targetUrl) {
    return HttpFetcher.fetchJson(targetUrl);
  }

  public String getJson(String targetUrl, int timeoutMs) {
    return HttpFetcher.fetchJson(targetUrl, timeoutMs);
  }
}
