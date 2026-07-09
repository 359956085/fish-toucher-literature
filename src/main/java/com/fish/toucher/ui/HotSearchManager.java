package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;

import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 热搜轮播应用服务。
 *
 * @author fengshi
 */
@Service(Service.Level.APP)
public final class HotSearchManager implements Disposable {

    private static final Logger LOG = Logger.getInstance(HotSearchManager.class);
    private static final long DEFAULT_REFRESH_MINUTES = 15;
    private static final long DEFAULT_CAROUSEL_SECONDS = 10;
    private static final int MAX_RESPONSE_BYTES = 5 * 1024 * 1024;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final String KUAISHOU_BODY =
            "{\"operationName\":\"visionHotRank\",\"variables\":{\"page\":\"1\"},"
                    + "\"query\":\"query visionHotRank($page: String) "
                    + "{ visionHotRank(page: $page) { result items "
                    + "{ rank name hotValue } } }\"}";

    public static final String[] SOURCE_VALUES = {
            "baidu", "toutiao", "zhihu", "douyin", "kuaishou", "x", "google"
    };

    private final List<HotSearchItem> items = new ArrayList<>();
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean notificationPending = new AtomicBoolean();
    private final AtomicLong requestVersion = new AtomicLong();
    private final HttpClient httpClient;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> refreshTask;
    private ScheduledFuture<?> carouselTask;
    private Future<?> activeFetch;
    private int currentIndex;
    private String lastRefreshTime = "";
    private String currentSource = "";
    private volatile boolean running;
    private volatile boolean disposed;

    public HotSearchManager() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10));
        ProxySelector proxySelector = ProxySelector.getDefault();
        if (proxySelector != null) {
            builder.proxy(proxySelector);
        }
        httpClient = builder.build();
    }

    public static HotSearchManager getInstance() {
        return ApplicationManager.getApplication().getService(HotSearchManager.class);
    }

    public void addChangeListener(Runnable listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void fireChange() {
        if (!notificationPending.compareAndSet(false, true)) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            notificationPending.set(false);
            if (disposed) return;
            for (Runnable listener : listeners) {
                listener.run();
            }
        });
    }

    public synchronized void start() {
        if (running || disposed) return;
        running = true;
        scheduler = Executors.newScheduledThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "HotSearchManager-pool");
            thread.setDaemon(true);
            return thread;
        });
        long refreshMinutes = Math.max(
                1,
                getSetting(() -> NovelReaderSettings.getInstance().getRefreshIntervalMinutes(),
                        DEFAULT_REFRESH_MINUTES)
        );
        refreshTask = scheduler.scheduleAtFixedRate(
                this::submitFetch,
                0,
                refreshMinutes,
                TimeUnit.MINUTES
        );
    }

    public synchronized void stop() {
        if (!running && scheduler == null) return;
        running = false;
        requestVersion.incrementAndGet();
        cancel(activeFetch);
        cancel(refreshTask);
        cancel(carouselTask);
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        activeFetch = null;
        refreshTask = null;
        carouselTask = null;
        scheduler = null;
    }

    public boolean isRunning() {
        return running;
    }

    public void switchSource() {
        synchronized (this) {
            items.clear();
            currentIndex = 0;
            lastRefreshTime = "";
            requestVersion.incrementAndGet();
            cancel(activeFetch);
        }
        fireChange();
        submitFetch();
    }

    public void applyTimingChanges() {
        if (running) {
            stop();
            start();
        }
    }

    public void manualRefresh() {
        submitFetch();
    }

    private synchronized void submitFetch() {
        if (!running || scheduler == null || scheduler.isShutdown()) {
            return;
        }
        long version = requestVersion.incrementAndGet();
        String source = NovelReaderSettings.getInstance().getHotSearchSource();
        cancel(activeFetch);
        activeFetch = scheduler.submit(() -> fetch(version, source));
    }

    private void fetch(long version, String source) {
        try {
            HttpResponse<InputStream> response = httpClient.send(
                    buildRequest(source),
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            if (response.statusCode() != 200) {
                closeQuietly(response.body());
                LOG.warn("热搜请求返回 HTTP " + response.statusCode() + ": " + source);
                return;
            }

            String body;
            try (InputStream input = response.body()) {
                byte[] bytes = input.readNBytes(MAX_RESPONSE_BYTES + 1);
                if (bytes.length > MAX_RESPONSE_BYTES) {
                    LOG.warn("热搜响应超过 5 MiB: " + source);
                    return;
                }
                body = new String(bytes, StandardCharsets.UTF_8);
            }

            List<HotSearchItem> parsed = HotSearchParser.parse(source, body);
            if (version != requestVersion.get()
                    || !running
                    || !source.equals(NovelReaderSettings.getInstance().getHotSearchSource())) {
                return;
            }

            synchronized (this) {
                if (version != requestVersion.get() || !running) return;
                if (!parsed.isEmpty()) {
                    items.clear();
                    items.addAll(parsed);
                    currentIndex = Math.min(currentIndex, items.size() - 1);
                    currentSource = source;
                }
                lastRefreshTime = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            }
            if (!parsed.isEmpty()) {
                startCarouselIfNeeded();
                fireChange();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            if (version == requestVersion.get() && running) {
                LOG.warn("热搜请求失败: " + source, exception);
            }
        }
    }

    private HttpRequest buildRequest(String source) {
        if ("kuaishou".equals(source)) {
            return HttpRequest.newBuilder(URI.create("https://www.kuaishou.com/graphql"))
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "https://www.kuaishou.com/")
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(KUAISHOU_BODY))
                    .build();
        }

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(15))
                .GET();
        switch (source) {
            case "toutiao" -> request.uri(URI.create(
                    "https://www.toutiao.com/hot-event/hot-board/?origin=toutiao_pc"));
            case "zhihu" -> request.uri(URI.create(
                    "https://api.zhihu.com/topstory/hot-lists/total?limit=50"));
            case "douyin" -> request.uri(URI.create(
                            "https://www.iesdouyin.com/web/api/v2/hotsearch/billboard/word/"))
                    .header("Referer", "https://www.douyin.com/");
            case "x" -> {
                String region = safeXRegion(NovelReaderSettings.getInstance().getXTrendsRegion());
                request.uri(URI.create(region.isEmpty()
                        ? "https://trends24.in/"
                        : "https://trends24.in/" + region + "/"));
            }
            case "google" -> {
                String geo = safeGoogleGeo(
                        NovelReaderSettings.getInstance().getGoogleTrendsGeo());
                request.uri(URI.create(
                        "https://trends.google.com/trending/rss?geo=" + geo));
            }
            default -> request.uri(URI.create(
                    "https://top.baidu.com/api/board?platform=wise&tab=realtime"));
        }
        return request.build();
    }

    private static String safeXRegion(String value) {
        return value != null && value.matches("[a-z-]{0,64}") ? value : "";
    }

    private static String safeGoogleGeo(String value) {
        return value != null && value.matches("[A-Z]{2}") ? value : "US";
    }

    private synchronized void startCarouselIfNeeded() {
        if (carouselTask != null || scheduler == null || !running) return;
        long seconds = Math.max(
                3,
                getSetting(() -> NovelReaderSettings.getInstance()
                                .getCarouselIntervalSeconds(),
                        DEFAULT_CAROUSEL_SECONDS)
        );
        carouselTask = scheduler.scheduleAtFixedRate(
                this::rotateCarousel,
                seconds,
                seconds,
                TimeUnit.SECONDS
        );
    }

    private void rotateCarousel() {
        synchronized (this) {
            if (items.isEmpty() || !running) return;
            currentIndex = (currentIndex + 1) % items.size();
        }
        fireChange();
    }

    public synchronized String getCurrentTitle() {
        return items.isEmpty()
                ? "[" + FishToucherBundle.message("hotSearch.loading") + "]"
                : items.get(currentIndex).word();
    }

    public synchronized String getCurrentStatusText() {
        return items.isEmpty() ? "" : String.format("[%d/%d]", currentIndex + 1, items.size());
    }

    public synchronized String getCurrentUrl() {
        return items.isEmpty() ? "" : items.get(currentIndex).url();
    }

    public synchronized List<HotSearchItem> getAllItems() {
        return List.copyOf(items);
    }

    public synchronized int getCurrentIndex() {
        return currentIndex;
    }

    public synchronized String getLastRefreshTime() {
        return lastRefreshTime;
    }

    public synchronized String getCurrentSource() {
        return currentSource;
    }

    public synchronized boolean hasContent() {
        return !items.isEmpty();
    }

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
        for (int index = 0; index < SOURCE_VALUES.length; index++) {
            if (SOURCE_VALUES[index].equals(value)) return labels[index];
        }
        return labels[0];
    }

    @Override
    public void dispose() {
        disposed = true;
        stop();
        listeners.clear();
        synchronized (this) {
            items.clear();
        }
    }

    private static void cancel(Future<?> future) {
        if (future != null) {
            future.cancel(true);
        }
    }

    private static void closeQuietly(InputStream input) {
        try {
            input.close();
        } catch (Exception ignored) {
            // 响应关闭失败不覆盖原始 HTTP 状态。
        }
    }

    private static long getSetting(Callable<Integer> getter, long fallback) {
        try {
            return getter.call();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public record HotSearchItem(int rank, String word, String hotTag, String url) {}
}
