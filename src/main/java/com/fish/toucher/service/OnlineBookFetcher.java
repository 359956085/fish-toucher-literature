package com.fish.toucher.service;

import com.fish.toucher.model.BookSource;
import com.fish.toucher.model.ChapterInfo;
import com.fish.toucher.model.SearchResult;
import com.intellij.openapi.diagnostic.Logger;
import com.jayway.jsonpath.JsonPath;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Core HTTP + parsing engine for online book sources.
 * Uses Java's HttpClient for requests and Jsoup/JsonPath for parsing.
 */
public class OnlineBookFetcher {

    private static final Logger LOG = Logger.getInstance(OnlineBookFetcher.class);
    private static final String DEFAULT_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /**
     * Search for books using the given source and keyword.
     */
    public List<SearchResult> search(BookSource source, String keyword) {
        List<SearchResult> results = new ArrayList<>();
        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            BookSource.SearchRule searchRule = source.getSearchRule();
            String url = searchRule.getUrl().replace("{{keyword}}", encodedKeyword);
            String method = searchRule.getMethod() != null ? searchRule.getMethod() : "GET";

            String body = fetch(url, method, source.getHeaders());
            String ruleType = searchRule.getRuleType() != null ? searchRule.getRuleType() : "html";

            if ("json".equalsIgnoreCase(ruleType)) {
                List<Map<String, Object>> items = JsonPath.read(body, searchRule.getList());
                for (Map<String, Object> item : items) {
                    SearchResult sr = new SearchResult();
                    sr.setName(extractJsonValue(item, searchRule.getName()));
                    sr.setAuthor(extractJsonValue(item, searchRule.getAuthor()));
                    sr.setBookUrl(resolveUrl(url, extractJsonValue(item, searchRule.getBookUrl())));
                    sr.setCoverUrl(extractJsonValue(item, searchRule.getCoverUrl()));
                    results.add(sr);
                }
            } else {
                Document doc = Jsoup.parse(body, url);
                Elements elements = doc.select(searchRule.getList());
                for (Element el : elements) {
                    SearchResult sr = new SearchResult();
                    sr.setName(extractHtml(el, searchRule.getName()));
                    sr.setAuthor(extractHtml(el, searchRule.getAuthor()));
                    sr.setBookUrl(resolveUrl(url, extractHtml(el, searchRule.getBookUrl())));
                    sr.setCoverUrl(extractHtml(el, searchRule.getCoverUrl()));
                    results.add(sr);
                }
            }
        } catch (Exception e) {
            LOG.warn("search failed for keyword '" + keyword + "': " + e.getMessage(), e);
        }
        return results;
    }

    /**
     * Fetch the chapter list for a book.
     */
    public List<ChapterInfo> fetchChapterList(BookSource source, String bookUrl) {
        List<ChapterInfo> chapters = new ArrayList<>();
        try {
            BookSource.ChapterRule chapterRule = source.getChapterRule();
            String url = chapterRule.getUrl().replace("{{bookUrl}}", bookUrl);
            String method = chapterRule.getMethod() != null ? chapterRule.getMethod() : "GET";

            String body = fetch(url, method, source.getHeaders());
            String ruleType = chapterRule.getRuleType() != null ? chapterRule.getRuleType() : "html";

            if ("json".equalsIgnoreCase(ruleType)) {
                List<Map<String, Object>> items = JsonPath.read(body, chapterRule.getList());
                for (int i = 0; i < items.size(); i++) {
                    Map<String, Object> item = items.get(i);
                    ChapterInfo ci = new ChapterInfo();
                    ci.setIndex(i);
                    ci.setTitle(extractJsonValue(item, chapterRule.getName()));
                    ci.setUrl(resolveUrl(url, extractJsonValue(item, chapterRule.getChapterUrl())));
                    chapters.add(ci);
                }
            } else {
                Document doc = Jsoup.parse(body, url);
                Elements elements = doc.select(chapterRule.getList());
                for (int i = 0; i < elements.size(); i++) {
                    Element el = elements.get(i);
                    ChapterInfo ci = new ChapterInfo();
                    ci.setIndex(i);
                    ci.setTitle(extractHtml(el, chapterRule.getName()));
                    ci.setUrl(resolveUrl(url, extractHtml(el, chapterRule.getChapterUrl())));
                    chapters.add(ci);
                }
            }
        } catch (Exception e) {
            LOG.warn("fetchChapterList failed for bookUrl '" + bookUrl + "': " + e.getMessage(), e);
        }
        return chapters;
    }

    /**
     * Fetch the content of a chapter, returning lines of text.
     */
    public List<String> fetchContent(BookSource source, String chapterUrl) {
        try {
            BookSource.ContentRule contentRule = source.getContentRule();
            String url = contentRule.getUrl().replace("{{chapterUrl}}", chapterUrl);
            String method = contentRule.getMethod() != null ? contentRule.getMethod() : "GET";

            String body = fetch(url, method, source.getHeaders());
            String ruleType = contentRule.getRuleType() != null ? contentRule.getRuleType() : "html";

            String text;
            if ("json".equalsIgnoreCase(ruleType)) {
                String raw = JsonPath.read(body, contentRule.getContent());
                text = Jsoup.parse(raw).text();
            } else {
                Document doc = Jsoup.parse(body, url);
                // Remove purify selectors (ads, scripts, etc.)
                if (contentRule.getPurify() != null) {
                    for (String purifySelector : contentRule.getPurify()) {
                        doc.select(purifySelector).remove();
                    }
                }
                Element contentEl = doc.selectFirst(contentRule.getContent());
                if (contentEl == null) {
                    LOG.warn("fetchContent: content selector matched nothing for url: " + url);
                    return Collections.emptyList();
                }
                // Convert <br> and <p> tags to newlines before extracting text
                contentEl.select("br").after("\\n");
                contentEl.select("p").before("\\n");
                text = contentEl.text().replace("\\n", "\n");
            }

            // Split by newlines, trim, filter empty
            List<String> lines = new ArrayList<>();
            for (String line : text.split("\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    lines.add(trimmed);
                }
            }
            return lines;
        } catch (Exception e) {
            LOG.warn("fetchContent failed for chapterUrl '" + chapterUrl + "': " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Perform an HTTP request and return the response body.
     */
    private String fetch(String url, String method, Map<String, String> headers) throws Exception {
        HttpClient client = buildHttpClient();

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", DEFAULT_UA);

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                reqBuilder.header(entry.getKey(), entry.getValue());
            }
        }

        if ("POST".equalsIgnoreCase(method)) {
            reqBuilder.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            reqBuilder.GET();
        }

        HttpRequest request = reqBuilder.build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        LOG.info("fetch: " + method + " " + url + " → " + response.statusCode());
        return response.body();
    }

    /**
     * Build HttpClient using JVM default ProxySelector.
     * IntelliJ registers its proxy settings as the JVM default ProxySelector at startup,
     * so this automatically respects IDEA's HTTP Proxy configuration.
     */
    private HttpClient buildHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL);
        try {
            ProxySelector proxySelector = ProxySelector.getDefault();
            if (proxySelector != null) {
                var proxies = proxySelector.select(URI.create("https://www.google.com"));
                LOG.info("buildHttpClient: ProxySelector=" + proxySelector.getClass().getName() + ", proxies=" + proxies);
                boolean hasDirect = proxies.size() == 1 && proxies.get(0).type() == Proxy.Type.DIRECT;
                if (!hasDirect) {
                    builder.proxy(proxySelector);
                }
            } else {
                LOG.info("buildHttpClient: ProxySelector is null, using direct connection");
            }
        } catch (Exception e) {
            LOG.warn("buildHttpClient: failed to get proxy: " + e.getMessage());
        }
        return builder.build();
    }

    /**
     * Extract a value from an HTML element using a rule string.
     * Rule format: "selector@attr" or "@attr" (use parent element directly).
     * Special attrs: @text → el.text(), @href → el.absUrl("href"), @src → el.absUrl("src").
     */
    private String extractHtml(Element parent, String rule) {
        if (rule == null || rule.isEmpty()) {
            return "";
        }

        String selector;
        String attr;

        int atIndex = rule.indexOf('@');
        if (atIndex < 0) {
            // No @ means treat the whole rule as a selector, default to text
            selector = rule;
            attr = "text";
        } else {
            selector = rule.substring(0, atIndex).trim();
            attr = rule.substring(atIndex + 1).trim();
        }

        Element target;
        if (selector.isEmpty()) {
            target = parent;
        } else {
            target = parent.selectFirst(selector);
        }

        if (target == null) {
            return "";
        }

        return switch (attr) {
            case "text" -> target.text();
            case "href" -> target.absUrl("href");
            case "src" -> target.absUrl("src");
            default -> target.attr(attr);
        };
    }

    /**
     * Extract a string value from a JSON map by key.
     */
    @SuppressWarnings("unchecked")
    private String extractJsonValue(Map item, String key) {
        if (item == null || key == null || key.isEmpty()) {
            return "";
        }
        Object val = item.get(key);
        return val != null ? val.toString() : "";
    }

    /**
     * Resolve a potentially relative URL against a base URL.
     */
    private String resolveUrl(String baseUrl, String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        // Already absolute
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        // Protocol-relative
        if (url.startsWith("//")) {
            try {
                URI base = URI.create(baseUrl);
                return base.getScheme() + ":" + url;
            } catch (Exception e) {
                return "https:" + url;
            }
        }
        // Path-relative
        try {
            URI base = URI.create(baseUrl);
            return base.resolve(url).toString();
        } catch (Exception e) {
            LOG.warn("resolveUrl: failed to resolve '" + url + "' against '" + baseUrl + "'");
            return url;
        }
    }
}
