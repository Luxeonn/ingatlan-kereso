package ingatlanok.scraper;

/**
 * Portál scraping művelet kimeneti státusza a hibák és fallback állapotok izolálására.
 */
public enum PortalScrapeStatus {
    SUCCESS,
    EMPTY,
    BOT_PROTECTED,
    SPA_FALLBACK,
    FAILED
}
