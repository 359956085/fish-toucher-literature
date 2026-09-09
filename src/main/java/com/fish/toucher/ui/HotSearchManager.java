package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;

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
    private final ChangeNotifier changes;
    private final java.util.function.Supplier<NovelReaderSettings> settingsSupplier;
    private final java.util.function.Supplier<ScheduledExecutorService> schedulerFactory;
    private final AtomicLong requestVersion = new AtomicLong();
    private final TaskEpoch timerEpoch = new TaskEpoch(this);
    private final HotSearchTransport httpClient;

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
        this(createHttpClient(), NovelReaderSettings::getInstance,
                () -> Executors.newScheduledThreadPool(2, runnable -> {
                    Thread thread = new Thread(runnable, "HotSearchManager-pool");
                    thread.setDaemon(true);
                    return thread;
                }), task -> ApplicationManager.getApplication().invokeLater(task));
    }

    HotSearchManager(HotSearchTransport httpClient,
                     java.util.function.Supplier<NovelReaderSettings> settingsSupplier,
                     java.util.function.Supplier<ScheduledExecutorService> schedulerFactory,
                     Executor dispatcher) {
        this.httpClient = httpClient;
        this.settingsSupplier = settingsSupplier;
        this.schedulerFactory = schedulerFactory;
        changes = new ChangeNotifier(dispatcher, exception -> LOG.warn("界面变更监听器执行失败", exception));
    }

    private static HotSearchHttpClient createHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10));
        ProxySelector proxySelector = ProxySelector.getDefault();
        if (proxySelector != null) {
            builder.proxy(proxySelector);
        }
        return new HotSearchHttpClient(builder.build(),
                Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "HotSearch-timeout");
                    thread.setDaemon(true);
                    return thread;
                }), Duration.ofSeconds(15), MAX_RESPONSE_BYTES);
    }

    public static HotSearchManager getInstance() {
        return ApplicationManager.getApplication().getService(HotSearchManager.class);
    }

    public void addChangeListener(Runnable listener) {
        changes.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        changes.remove(listener);
    }

    private void fireChange() {
        changes.fire();
    }

    public synchronized void start() {
        if (running || disposed) return;
        running = true;
        timerEpoch.invalidate();
        scheduler = schedulerFactory.get();
        long refreshMinutes = Math.max(
                1,
                getSetting(() -> settingsSupplier.get().getRefreshIntervalMinutes(),
                        DEFAULT_REFRESH_MINUTES)
        );
        refreshTask = scheduler.scheduleAtFixedRate(
                timerEpoch.guard(this::submitFetch),
                0,
                refreshMinutes,
                TimeUnit.MINUTES
        );
    }

    public synchronized void stop() {
        timerEpoch.invalidate();
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
            if (disposed) return;
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
        if (!running || disposed || scheduler == null || scheduler.isShutdown()) return;
        long version = requestVersion.incrementAndGet();
        NovelReaderSettings settings = settingsSupplier.get();
        FetchContext context = new FetchContext(settings.getHotSearchSource(),
                safeXRegion(settings.getXTrendsRegion()), safeGoogleGeo(settings.getGoogleTrendsGeo()));
        cancel(activeFetch);
        try {
            CompletableFuture<HttpResponse<byte[]>> request = httpClient.send(buildRequest(context));
            activeFetch = request;
            request.whenComplete((response, error) -> {
                synchronized (HotSearchManager.this) {
                    if (version != requestVersion.get() || !running || disposed) return;
                    if (error != null) {
                        LOG.warn("热搜请求失败: " + context.source(), error);
                        return;
                    }
                    // 网络回调只投递解析任务，不占用界面线程。
                    scheduler.execute(() -> applyResponse(version, context, response));
                }
            });
        } catch (RuntimeException exception) {
            LOG.warn("启动热搜请求失败: " + context.source(), exception);
        }
    }

    private void applyResponse(long version, FetchContext context, HttpResponse<byte[]> response) {
        synchronized (this) {
            if (disposed || !running || version != requestVersion.get()) return;
        }
        try {
            List<HotSearchItem> parsed = HotSearchParser.parse(context.source(),
                    new String(response.body(), StandardCharsets.UTF_8));
            synchronized (this) {
                if (disposed || !running || version != requestVersion.get()
                        || !context.matches(settingsSupplier.get())) return;
                if (parsed.isEmpty()) return;
                items.clear();
                items.addAll(parsed);
                currentIndex = Math.min(currentIndex, items.size() - 1);
                currentSource = context.source();
                lastRefreshTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                startCarouselIfNeeded();
                fireChange();
            }
        } catch (RuntimeException exception) {
            LOG.warn("解析热搜响应失败: " + context.source(), exception);
        }
    }

    private record FetchContext(String source, String xRegion, String googleGeo) {
        boolean matches(NovelReaderSettings settings) {
            return source.equals(settings.getHotSearchSource())
                    && (!"x".equals(source) || xRegion.equals(safeXRegion(settings.getXTrendsRegion())))
                    && (!"google".equals(source) || googleGeo.equals(safeGoogleGeo(settings.getGoogleTrendsGeo())));
        }
    }

    private HttpRequest buildRequest(FetchContext context) {
        String source = context.source();
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
                String region = context.xRegion();
                request.uri(URI.create(region.isEmpty()
                        ? "https://trends24.in/"
                        : "https://trends24.in/" + region + "/"));
            }
            case "google" -> {
                String geo = context.googleGeo();
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
                getSetting(() -> settingsSupplier.get()
                                .getCarouselIntervalSeconds(),
                        DEFAULT_CAROUSEL_SECONDS)
        );
        carouselTask = scheduler.scheduleAtFixedRate(
                timerEpoch.guard(this::rotateCarousel),
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
    public synchronized void dispose() {
        disposed = true;
        timerEpoch.close();
        stop();
        httpClient.close();
        changes.close();
        synchronized (this) {
            items.clear();
        }
    }

    private static void cancel(Future<?> future) {
        if (future != null) {
            future.cancel(true);
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
