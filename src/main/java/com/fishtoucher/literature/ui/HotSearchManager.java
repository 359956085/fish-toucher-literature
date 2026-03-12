package com.fishtoucher.literature.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;

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
 * Singleton manager for Baidu hot search carousel.
 * Fetches hot search data periodically and rotates titles in the status bar.
 */
public class HotSearchManager {

    private static final Logger LOG = Logger.getInstance(HotSearchManager.class);
    private static final HotSearchManager INSTANCE = new HotSearchManager();

    private static final String API_URL = "https://top.baidu.com/api/board?platform=wise&tab=realtime";
    private static final long REFRESH_INTERVAL_MINUTES = 15;
    private static final long CAROUSEL_INTERVAL_SECONDS = 10;

    private final List<HotSearchItem> items = new ArrayList<>();
    private int currentIndex = 0;
    private String lastRefreshTime = "";
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

        // Initial fetch immediately, then every 15 minutes
        refreshTask = scheduler.scheduleAtFixedRate(
                this::fetchHotSearch, 0, REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES);

        // Carousel rotation every 10 seconds
        carouselTask = scheduler.scheduleAtFixedRate(
                this::rotateCarousel, CAROUSEL_INTERVAL_SECONDS, CAROUSEL_INTERVAL_SECONDS, TimeUnit.SECONDS);
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

    // ========== Data fetch ==========

    private void fetchHotSearch() {
        LOG.info("fetchHotSearch: fetching Baidu hot search data");
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                parseResponse(response.body());
                lastRefreshTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                LOG.info("fetchHotSearch: successfully fetched " + items.size() + " items");
            } else {
                LOG.warn("fetchHotSearch: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            LOG.warn("fetchHotSearch: failed to fetch data: " + e.getMessage());
        }
    }

    /**
     * Parse the JSON response. We use simple regex/string parsing to avoid
     * adding a JSON library dependency (IntelliJ platform bundles Gson but
     * we keep it minimal with manual parsing).
     */
    private void parseResponse(String json) {
        List<HotSearchItem> newItems = new ArrayList<>();
        // The API returns items in data.cards[0].content[0].content array
        // Each item starts with {"isTop":... and contains "word":"title", "index":N, "hotTag":"N"
        Pattern wordPattern = Pattern.compile("\"word\"\\s*:\\s*\"([^\"]+)\"");
        Pattern urlPattern = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
        Pattern indexPattern = Pattern.compile("\"index\"\\s*:\\s*(\\d+)");
        Pattern hotTagPattern = Pattern.compile("\"hotTag\"\\s*:\\s*\"(\\d+)\"");

        // Split by item boundaries - each item starts with {"isTop"
        String[] parts = json.split("\\{\"isTop\"");
        int rank = 0;
        for (int i = 1; i < parts.length; i++) {
            String part = "{\"isTop\"" + parts[i];
            Matcher wm = wordPattern.matcher(part);
            if (wm.find()) {
                String word = wm.group(1)
                        .replace("\\u0026", "&")
                        .replace("\\\"", "\"")
                        .replace("\\/", "/");
                int index = rank++;
                Matcher im = indexPattern.matcher(part);
                if (im.find()) {
                    try { index = Integer.parseInt(im.group(1)); } catch (NumberFormatException ignored) {}
                }
                String hotTag = "";
                Matcher hm = hotTagPattern.matcher(part);
                if (hm.find()) {
                    hotTag = hm.group(1);
                }
                String url = "";
                Matcher um = urlPattern.matcher(part);
                if (um.find()) {
                    url = um.group(1)
                            .replace("\\u0026", "&")
                            .replace("\\/", "/");
                }
                newItems.add(new HotSearchItem(index, word, hotTag, url));
            }
        }

        if (!newItems.isEmpty()) {
            synchronized (this) {
                items.clear();
                items.addAll(newItems);
                if (currentIndex >= items.size()) {
                    currentIndex = 0;
                }
            }
            fireChange();
        }
    }

    public void manualRefresh() {
        if (scheduler != null && running) {
            scheduler.submit(this::fetchHotSearch);
        }
    }

    // ========== Carousel ==========

    private void rotateCarousel() {
        synchronized (this) {
            if (items.isEmpty()) return;
            currentIndex = (currentIndex + 1) % items.size();
        }
        fireChange();
    }

    // ========== Getters for UI ==========

    public synchronized String getCurrentTitle() {
        if (items.isEmpty()) return "[Loading hot search...]";
        HotSearchItem item = items.get(currentIndex);
        return item.word();
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

    public boolean hasContent() {
        return !items.isEmpty();
    }

    // ========== Data model ==========

    public record HotSearchItem(int rank, String word, String hotTag, String url) {}
}
