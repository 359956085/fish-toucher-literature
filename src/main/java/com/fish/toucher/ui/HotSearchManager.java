package com.fish.toucher.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;

import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Singleton manager for hot search carousel.
 * Supports multiple sources: Baidu, Toutiao, Zhihu, Douyin, Kuaishou.
 */
public class HotSearchManager {

    private static final Logger LOG = Logger.getInstance(HotSearchManager.class);
    private static final HotSearchManager INSTANCE = new HotSearchManager();

    // defaults; actual values read from settings
    private static final long DEFAULT_REFRESH_INTERVAL_MINUTES = 15;
    private static final long DEFAULT_CAROUSEL_INTERVAL_SECONDS = 10;

    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    private final List<HotSearchItem> items = new ArrayList<>();
    private int currentIndex = 0;
    private String lastRefreshTime = "";
    private String currentSource = "";
    private boolean running = false;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> refreshTask;
    private ScheduledFuture<?> carouselTask;

    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

    public static HotSearchManager getInstance() {
        return INSTANCE;
    }

    private HotSearchManager() {}

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void fireChange() {
        for (Runnable r : listeners) {
            ApplicationManager.getApplication().invokeLater(r);
        }
    }

    // ========== Lifecycle ==========

    public synchronized void start() {
        if (running) return;
        running = true;
        LOG.info("start: starting hot search manager");
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "HotSearchManager-pool");
            t.setDaemon(true);
            return t;
        });

        long refreshMin = DEFAULT_REFRESH_INTERVAL_MINUTES;
        try { refreshMin = NovelReaderSettings.getInstance().getRefreshIntervalMinutes(); } catch (Exception ignored) {}
        refreshTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                fetchHotSearch();
            } catch (Exception e) {
                LOG.error("start: uncaught exception in fetchHotSearch", e);
            }
        }, 0, refreshMin, TimeUnit.MINUTES);
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        LOG.info("stop: stopping hot search manager");
        if (refreshTask != null) refreshTask.cancel(false);
        if (carouselTask != null) carouselTask.cancel(false);
        if (scheduler != null) scheduler.shutdownNow();
        scheduler = null;
        refreshTask = null;
        carouselTask = null;
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Called when the user changes the hot search source in settings.
     * Clears current data and triggers an immediate refresh.
     */
    public void switchSource() {
        synchronized (this) {
            items.clear();
            currentIndex = 0;
            lastRefreshTime = "";
        }
        fireChange();
        if (scheduler != null && running) {
            scheduler.submit(this::fetchHotSearch);
        }
    }

    /**
     * Called when timing settings change. Restarts scheduler with new intervals.
     */
    public void applyTimingChanges() {
        if (running) {
            stop();
            start();
        }
    }

    // ========== Data fetch ==========

    private void fetchHotSearch() {
        String source = NovelReaderSettings.getInstance().getHotSearchSource();
        LOG.info("fetchHotSearch: fetching from source: " + source);
        try {
            HttpClient client = buildHttpClient();

            HttpRequest request;
            if ("kuaishou".equals(source)) {
                String graphql = "{\"operationName\":\"visionHotRank\",\"variables\":{\"page\":\"1\"},"
                        + "\"query\":\"query visionHotRank($page: String) { visionHotRank(page: $page) "
                        + "{ result items { rank name hotValue } } }\"}";
                request = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.kuaishou.com/graphql"))
                        .header("User-Agent", UA)
                        .header("Referer", "https://www.kuaishou.com/")
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(graphql))
                        .build();
            } else {
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .header("User-Agent", UA)
                        .timeout(Duration.ofSeconds(15))
                        .GET();

                switch (source) {
                    case "toutiao" -> reqBuilder.uri(URI.create("https://www.toutiao.com/hot-event/hot-board/?origin=toutiao_pc"));
                    case "zhihu" -> reqBuilder.uri(URI.create("https://api.zhihu.com/topstory/hot-lists/total?limit=50"));
                    case "douyin" -> reqBuilder.uri(URI.create("https://www.iesdouyin.com/web/api/v2/hotsearch/billboard/word/"))
                            .header("Referer", "https://www.douyin.com/");
                    case "x" -> {
                        String region = getXRegion();
                        String xUrl = region.isEmpty() ? "https://trends24.in/" : "https://trends24.in/" + region + "/";
                        reqBuilder.uri(URI.create(xUrl));
                    }
                    case "google" -> {
                        String geo = getGoogleTrendsGeo();
                        reqBuilder.uri(URI.create("https://trends.google.com/trending/rss?geo=" + geo));
                    }
                    default -> reqBuilder.uri(URI.create("https://top.baidu.com/api/board?platform=wise&tab=realtime"));
                }
                request = reqBuilder.build();
            }

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                List<HotSearchItem> newItems = switch (source) {
                    case "toutiao" -> parseToutiao(response.body());
                    case "zhihu" -> parseZhihu(response.body());
                    case "douyin" -> parseDouyin(response.body());
                    case "kuaishou" -> parseKuaishou(response.body());
                    case "x" -> parseX(response.body());
                    case "google" -> parseGoogleTrends(response.body());
                    default -> parseBaidu(response.body());
                };
                if (!newItems.isEmpty()) {
                    synchronized (this) {
                        items.clear();
                        items.addAll(newItems);
                        currentSource = source;
                        if (currentIndex >= items.size()) {
                            currentIndex = 0;
                        }
                    }
                    startCarouselIfNeeded();
                    fireChange();
                }
                lastRefreshTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                LOG.info("fetchHotSearch: successfully fetched " + items.size() + " items from " + source);
            } else {
                LOG.warn("fetchHotSearch: HTTP " + response.statusCode() + " from " + source);
            }
        } catch (Exception e) {
            LOG.warn("fetchHotSearch: failed to fetch from " + source + ": " + e.getMessage());
        }
    }

    // ========== Parsers ==========

    private List<HotSearchItem> parseBaidu(String json) {
        List<HotSearchItem> newItems = new ArrayList<>();
        Pattern wordPattern = Pattern.compile("\"word\"\\s*:\\s*\"([^\"]+)\"");
        Pattern urlPattern = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
        Pattern indexPattern = Pattern.compile("\"index\"\\s*:\\s*(\\d+)");
        Pattern hotTagPattern = Pattern.compile("\"hotTag\"\\s*:\\s*\"(\\d+)\"");

        String[] parts = json.split("\\{\"isTop\"");
        int rank = 0;
        for (int i = 1; i < parts.length; i++) {
            String part = "{\"isTop\"" + parts[i];
            Matcher wm = wordPattern.matcher(part);
            if (wm.find()) {
                String word = unescapeJson(wm.group(1));
                int index = rank++;
                Matcher im = indexPattern.matcher(part);
                if (im.find()) {
                    try { index = Integer.parseInt(im.group(1)); } catch (NumberFormatException ignored) {}
                }
                String hotTag = "";
                Matcher hm = hotTagPattern.matcher(part);
                if (hm.find()) hotTag = hm.group(1);
                String url = "";
                Matcher um = urlPattern.matcher(part);
                if (um.find()) {
                    url = unescapeJson(um.group(1)).replace("m.baidu.com", "www.baidu.com");
                }
                newItems.add(new HotSearchItem(index, word, hotTag, url));
            }
        }
        return newItems;
    }

    private List<HotSearchItem> parseToutiao(String json) {
        List<HotSearchItem> newItems = new ArrayList<>();
        // Response: {"data":[{"Title":"...", "Url":"...", "HotValue":"N", "ClusterIdStr":"..."}]}
        Pattern titlePattern = Pattern.compile("\"Title\"\\s*:\\s*\"([^\"]+)\"");
        Pattern urlPattern = Pattern.compile("\"Url\"\\s*:\\s*\"([^\"]+)\"");
        Pattern hotValuePattern = Pattern.compile("\"HotValue\"\\s*:\\s*\"(\\d+)\"");

        String[] parts = json.split("\\{\"ClusterId\"");
        int rank = 0;
        for (int i = 1; i < parts.length; i++) {
            String part = "{\"ClusterId\"" + parts[i];
            Matcher tm = titlePattern.matcher(part);
            if (tm.find()) {
                String title = unescapeJson(tm.group(1));
                String hotTag = "";
                Matcher hm = hotValuePattern.matcher(part);
                if (hm.find()) hotTag = hm.group(1);
                String url = "";
                Matcher um = urlPattern.matcher(part);
                if (um.find()) url = unescapeJson(um.group(1));
                newItems.add(new HotSearchItem(rank++, title, hotTag, url));
            }
        }
        return newItems;
    }

    private List<HotSearchItem> parseZhihu(String json) {
        List<HotSearchItem> newItems = new ArrayList<>();
        // Response: {"data":[{"target":{"id":N,"title":"...","url":"..."}}]}
        Pattern titlePattern = Pattern.compile("\"title\"\\s*:\\s*\"([^\"]+)\"");
        Pattern idPattern = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

        String[] parts = json.split("\"hot_list_feed\"");
        int rank = 0;
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            // Find the target section
            int targetIdx = part.indexOf("\"target\"");
            if (targetIdx < 0) continue;
            String targetPart = part.substring(targetIdx);
            Matcher tm = titlePattern.matcher(targetPart);
            if (tm.find()) {
                String title = unescapeJson(tm.group(1));
                long questionId = 0;
                Matcher im = idPattern.matcher(targetPart);
                if (im.find()) {
                    try { questionId = Long.parseLong(im.group(1)); } catch (NumberFormatException ignored) {}
                }
                String url = questionId > 0 ? "https://www.zhihu.com/question/" + questionId : "";
                newItems.add(new HotSearchItem(rank++, title, "", url));
            }
        }
        return newItems;
    }

    private List<HotSearchItem> parseDouyin(String json) {
        List<HotSearchItem> newItems = new ArrayList<>();
        // Response: {"word_list":[{"word":"...","hot_value":N,"label":N}]}
        Pattern wordPattern = Pattern.compile("\"word\"\\s*:\\s*\"([^\"]+)\"");
        Pattern hotValuePattern = Pattern.compile("\"hot_value\"\\s*:\\s*(\\d+)");

        String[] parts = json.split("\\{\"word\"\\s*:");
        int rank = 0;
        for (int i = 1; i < parts.length; i++) {
            String part = "{\"word\":" + parts[i];
            Matcher wm = wordPattern.matcher(part);
            if (wm.find()) {
                String word = unescapeJson(wm.group(1));
                String hotTag = "";
                Matcher hm = hotValuePattern.matcher(part);
                if (hm.find()) hotTag = hm.group(1);
                String url = "https://www.douyin.com/search/" + word;
                newItems.add(new HotSearchItem(rank++, word, hotTag, url));
            }
        }
        return newItems;
    }

    private List<HotSearchItem> parseKuaishou(String json) {
        List<HotSearchItem> newItems = new ArrayList<>();
        // Response: {"data":{"visionHotRank":{"items":[{"rank":N,"name":"...","hotValue":"N万"}]}}}
        Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
        Pattern rankPattern = Pattern.compile("\"rank\"\\s*:\\s*(\\d+)");
        Pattern hotValuePattern = Pattern.compile("\"hotValue\"\\s*:\\s*\"([^\"]+)\"");

        String[] parts = json.split("\\{\"rank\"");
        for (int i = 1; i < parts.length; i++) {
            String part = "{\"rank\"" + parts[i];
            Matcher nm = namePattern.matcher(part);
            if (nm.find()) {
                String name = unescapeJson(nm.group(1));
                int rank = i - 1;
                Matcher rm = rankPattern.matcher(part);
                if (rm.find()) {
                    try { rank = Integer.parseInt(rm.group(1)); } catch (NumberFormatException ignored) {}
                }
                String hotTag = "";
                Matcher hm = hotValuePattern.matcher(part);
                if (hm.find()) hotTag = hm.group(1);
                String url = "https://www.kuaishou.com/search/video?searchKey=" + name;
                newItems.add(new HotSearchItem(rank, name, hotTag, url));
            }
        }
        return newItems;
    }

    private List<HotSearchItem> parseX(String html) {
        List<HotSearchItem> newItems = new ArrayList<>();
        Pattern pattern = Pattern.compile("trend-name[^<]*<a\\s+href=\"([^\"]+)\"[^>]*>([^<]+)</a>");
        Matcher m = pattern.matcher(html);
        int rank = 0;
        while (m.find() && rank < 50) {
            String url = m.group(1);
            String name = m.group(2).trim();
            if (!name.isEmpty()) {
                newItems.add(new HotSearchItem(rank++, name, "", url));
            }
        }
        return newItems;
    }

    /**
     * Get X trends region slug from plugin settings.
     * Empty string means worldwide (default).
     */
    private String getXRegion() {
        try {
            String region = NovelReaderSettings.getInstance().getXTrendsRegion();
            if (region != null && !region.isEmpty()) return region;
        } catch (Exception ignored) {}
        return "";
    }

    private List<HotSearchItem> parseGoogleTrends(String xml) {
        List<HotSearchItem> newItems = new ArrayList<>();
        // RSS format: <item><title>...</title>...</item>
        Pattern itemPattern = Pattern.compile("<item>([\\s\\S]*?)</item>");
        Pattern titlePattern = Pattern.compile("<title>([^<]+)</title>");
        Pattern trafficPattern = Pattern.compile("<ht:approx_traffic>([^<]+)</ht:approx_traffic>");
        Matcher im = itemPattern.matcher(xml);
        int rank = 0;
        while (im.find() && rank < 50) {
            String item = im.group(1);
            Matcher tm = titlePattern.matcher(item);
            if (tm.find()) {
                String title = tm.group(1).replace("&amp;", "&").replace("&apos;", "'").replace("&quot;", "\"");
                String hotTag = "";
                Matcher hm = trafficPattern.matcher(item);
                if (hm.find()) hotTag = hm.group(1);
                String url = "https://www.google.com/search?q=" + title.replace(" ", "+");
                newItems.add(new HotSearchItem(rank++, title, hotTag, url));
            }
        }
        return newItems;
    }

    /**
     * Get Google Trends geo code from plugin settings.
     * Default is "US".
     */
    private String getGoogleTrendsGeo() {
        try {
            String geo = NovelReaderSettings.getInstance().getGoogleTrendsGeo();
            if (geo != null && !geo.isEmpty()) return geo;
        } catch (Exception ignored) {}
        return "US";
    }

    /**
     * Build HttpClient using JVM default ProxySelector.
     * IntelliJ registers its proxy settings as the JVM default ProxySelector at startup,
     * so this automatically respects IDEA's HTTP Proxy configuration.
     */
    private HttpClient buildHttpClient() {
        ProxySelector proxySelector = ProxySelector.getDefault();
        if (proxySelector != null) {
            try {
                var proxies = proxySelector.select(URI.create("https://www.google.com"));
                LOG.info("buildHttpClient: ProxySelector=" + proxySelector.getClass().getName() + ", proxies=" + proxies);
            } catch (Exception e) {
                LOG.info("buildHttpClient: ProxySelector=" + proxySelector.getClass().getName() + ", failed to query: " + e.getMessage());
            }
        } else {
            LOG.info("buildHttpClient: no ProxySelector, using direct connection");
        }
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10));
        if (proxySelector != null) {
            builder.proxy(proxySelector);
        }
        return builder.build();
    }

    private String unescapeJson(String s) {
        return s.replace("\\u0026", "&")
                .replace("\\\"", "\"")
                .replace("\\/", "/")
                .replace("\\n", "\n");
    }

    // ========== Manual refresh ==========

    public void manualRefresh() {
        if (scheduler != null && running) {
            scheduler.submit(this::fetchHotSearch);
        }
    }

    // ========== Carousel ==========

    private synchronized void startCarouselIfNeeded() {
        if (carouselTask == null && scheduler != null && running) {
            long carouselSec = DEFAULT_CAROUSEL_INTERVAL_SECONDS;
            try { carouselSec = NovelReaderSettings.getInstance().getCarouselIntervalSeconds(); } catch (Exception ignored) {}
            LOG.info("startCarouselIfNeeded: starting carousel, interval=" + carouselSec + "s");
            carouselTask = scheduler.scheduleAtFixedRate(() -> {
                try {
                    rotateCarousel();
                } catch (Exception e) {
                    LOG.error("carousel: uncaught exception", e);
                }
            }, carouselSec, carouselSec, TimeUnit.SECONDS);
        }
    }

    private void rotateCarousel() {
        synchronized (this) {
            if (items.isEmpty()) return;
            currentIndex = (currentIndex + 1) % items.size();
        }
        fireChange();
    }

    // ========== Getters for UI ==========

    public synchronized String getCurrentTitle() {
        if (items.isEmpty()) return "[" + FishToucherBundle.message("hotSearch.loading") + "]";
        return items.get(currentIndex).word();
    }

    public synchronized String getCurrentStatusText() {
        if (items.isEmpty()) return "";
        return String.format("[%d/%d]", currentIndex + 1, items.size());
    }

    public synchronized String getCurrentUrl() {
        if (items.isEmpty()) return "";
        return items.get(currentIndex).url();
    }

    public synchronized List<HotSearchItem> getAllItems() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    public synchronized int getCurrentIndex() {
        return currentIndex;
    }

    public String getLastRefreshTime() {
        return lastRefreshTime;
    }

    public String getCurrentSource() {
        return currentSource;
    }

    public synchronized boolean hasContent() {
        return !items.isEmpty();
    }

    // ========== Source labels ==========

    public static final String[] SOURCE_VALUES = {"baidu", "toutiao", "zhihu", "douyin", "kuaishou", "x", "google"};

    public static String[] getSourceLabels() {
        return new String[]{
                FishToucherBundle.message("hotSearch.source.baidu"),
                FishToucherBundle.message("hotSearch.source.toutiao"),
                FishToucherBundle.message("hotSearch.source.zhihu"),
                FishToucherBundle.message("hotSearch.source.douyin"),
                FishToucherBundle.message("hotSearch.source.kuaishou"),
                FishToucherBundle.message("hotSearch.source.x"),
                FishToucherBundle.message("hotSearch.source.google")
        };
    }

    public static String getSourceLabel(String value) {
        String[] labels = getSourceLabels();
        for (int i = 0; i < SOURCE_VALUES.length; i++) {
            if (SOURCE_VALUES[i].equals(value)) return labels[i];
        }
        return labels[0];
    }

    // ========== Data model ==========

    public record HotSearchItem(int rank, String word, String hotTag, String url) {}
}
