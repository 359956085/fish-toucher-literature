package com.fish.toucher.ui;

import com.fish.toucher.settings.NovelReaderSettings;
import org.junit.jupiter.api.Test;
import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class HotSearchManagerTest {
    static final class ManualScheduler extends ScheduledThreadPoolExecutor {
        final List<Runnable> ticks = new ArrayList<>();
        final Deque<Runnable> ready = new ArrayDeque<>();
        ManualScheduler() { super(1); }
        @Override public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay,
                                                                long period, TimeUnit unit) {
            ticks.add(command);
            return super.schedule(() -> {}, 1, TimeUnit.DAYS);
        }
        @Override public void execute(Runnable command) { ready.add(command); }
        void drain() { while (!ready.isEmpty()) ready.remove().run(); }
    }

    static final class LateResponse extends CompletableFuture<HttpResponse<byte[]>> {
        boolean cancelRequested;
        @Override public boolean cancel(boolean interrupt) { cancelRequested = true; return false; }
    }

    static final class Transport implements HotSearchTransport {
        final List<HttpRequest> requests = new ArrayList<>();
        final List<LateResponse> results = new ArrayList<>();
        boolean closed;
        public CompletableFuture<HttpResponse<byte[]>> send(HttpRequest request) {
            requests.add(request);
            LateResponse response = new LateResponse(); results.add(response); return response;
        }
        public void close() { closed = true; }
        void respond(int index, String json) { results.get(index).complete(new Response(requests.get(index), json)); }
    }

    private record Response(HttpRequest request, String json) implements HttpResponse<byte[]> {
        public int statusCode() { return 200; }
        public Optional<HttpResponse<byte[]>> previousResponse() { return Optional.empty(); }
        public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (key, value) -> true); }
        public byte[] body() { return json.getBytes(StandardCharsets.UTF_8); }
        public Optional<SSLSession> sslSession() { return Optional.empty(); }
        public URI uri() { return request.uri(); }
        public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }

    private static String baidu(String word) {
        return "{\"data\":{\"cards\":[{\"content\":[{\"word\":\"" + word + "\",\"url\":\"https://example.com/\"}]}]}}";
    }

    @Test
    void 旧响应不能覆盖刷新结果且失败保留同源榜单() {
        NovelReaderSettings settings = new NovelReaderSettings();
        Transport transport = new Transport(); ManualScheduler scheduler = new ManualScheduler();
        HotSearchManager manager = new HotSearchManager(transport, () -> settings, () -> scheduler, Runnable::run);
        try {
            manager.start(); scheduler.ticks.get(0).run();
            manager.manualRefresh();
            assertTrue(transport.results.get(0).cancelRequested);
            transport.respond(1, baidu("最新榜单")); scheduler.drain();
            assertEquals("最新榜单", manager.getCurrentTitle());
            transport.respond(0, baidu("过期榜单")); scheduler.drain();
            assertEquals("最新榜单", manager.getCurrentTitle());
            manager.manualRefresh();
            transport.results.get(2).completeExceptionally(new java.io.IOException("模拟断网"));
            scheduler.drain();
            assertEquals("最新榜单", manager.getCurrentTitle());
        } finally { manager.dispose(); }
        assertTrue(transport.closed);
        assertTrue(scheduler.isShutdown());
    }

    @Test
    void 换源清空并冻结地区且停止重启拒绝旧任务() {
        NovelReaderSettings settings = new NovelReaderSettings();
        Transport transport = new Transport();
        List<ManualScheduler> schedulers = new ArrayList<>();
        HotSearchManager manager = new HotSearchManager(transport, () -> settings, () -> {
            ManualScheduler scheduler = new ManualScheduler(); schedulers.add(scheduler); return scheduler;
        }, Runnable::run);
        try {
            manager.start();
            ManualScheduler old = schedulers.get(0); old.ticks.get(0).run();
            transport.respond(0, baidu("原榜单")); old.drain();
            settings.setHotSearchSource("google"); settings.setGoogleTrendsGeo("JP");
            manager.switchSource();
            assertFalse(manager.hasContent());
            assertTrue(transport.requests.get(1).uri().toString().endsWith("geo=JP"));
            settings.setGoogleTrendsGeo("US");
            transport.respond(1, "<rss><channel><item><title>日本榜单</title><link>https://example.com</link></item></channel></rss>");
            old.drain();
            assertFalse(manager.hasContent());
            manager.stop(); manager.start();
            int requests = transport.requests.size();
            old.ticks.get(0).run();
            assertEquals(requests, transport.requests.size());
            ManualScheduler current = schedulers.get(1); current.ticks.get(0).run();
            manager.dispose();
            transport.respond(requests, baidu("关闭后的响应")); current.drain();
            assertFalse(manager.hasContent());
            manager.start();
            assertEquals(2, schedulers.size());
        } finally { manager.dispose(); }
    }
}
