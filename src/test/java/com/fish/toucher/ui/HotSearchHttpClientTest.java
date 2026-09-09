package com.fish.toucher.ui;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class HotSearchHttpClientTest {
    private HotSearchHttpClient client(Duration timeout, int maxBytes) {
        return new HotSearchHttpClient(HttpClient.newHttpClient(),
                Executors.newSingleThreadScheduledExecutor(), timeout, maxBytes);
    }

    private HttpRequest request(HttpServer server, String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path)).build();
    }

    @Test
    void 正文停滞也必须超时且取消不等待正文完成() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        CountDownLatch headers = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        server.createContext("/stall", exchange -> {
            try {
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().write('x');
                exchange.getResponseBody().flush();
                headers.countDown();
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally { exchange.close(); }
        });
        server.start();
        try (HotSearchHttpClient client = client(Duration.ofMillis(500), 1024)) {
            var result = client.send(request(server, "/stall"));
            assertTrue(headers.await(3, TimeUnit.SECONDS));
            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> result.get(3, TimeUnit.SECONDS));
            assertInstanceOf(HttpTimeoutException.class, failure.getCause());
            var cancelled = client.send(request(server, "/stall"));
            assertTrue(cancelled.cancel(true));
            assertTrue(cancelled.isCancelled());
        } finally { release.countDown(); server.stop(0); }
    }

    @Test
    void 完整响应允许上限但拒绝超限和非成功状态() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", exchange -> {
            try (exchange) { exchange.sendResponseHeaders(200, 4); exchange.getResponseBody().write(new byte[4]); }
        });
        server.createContext("/large", exchange -> {
            try (exchange) { exchange.sendResponseHeaders(200, 5); exchange.getResponseBody().write(new byte[5]); }
        });
        server.createContext("/error", exchange -> {
            try (exchange) { exchange.sendResponseHeaders(503, -1); }
        });
        server.start();
        try (HotSearchHttpClient client = client(Duration.ofSeconds(3), 4)) {
            assertEquals(4, client.send(request(server, "/ok")).get(5, TimeUnit.SECONDS).body().length);
            assertThrows(ExecutionException.class, () -> client.send(request(server, "/large")).get(5, TimeUnit.SECONDS));
            assertThrows(ExecutionException.class, () -> client.send(request(server, "/error")).get(5, TimeUnit.SECONDS));
            client.close();
            assertThrows(ExecutionException.class, () -> client.send(request(server, "/ok")).get());
        } finally { server.stop(0); }
    }

    @Test
    void 超限和提前取消必须取消底层订阅() {
        AtomicBoolean cancelled = new AtomicBoolean();
        Flow.Subscription subscription = new Flow.Subscription() {
            public void request(long count) {}
            public void cancel() { cancelled.set(true); }
        };
        var body = new HotSearchHttpClient.BoundedSubscriber(4);
        body.onSubscribe(subscription);
        body.onNext(List.of(ByteBuffer.wrap(new byte[3])));
        body.onNext(List.of(ByteBuffer.wrap(new byte[2])));
        assertTrue(cancelled.get());
        assertTrue(body.getBody().toCompletableFuture().isCompletedExceptionally());
        cancelled.set(false);
        var early = new HotSearchHttpClient.BoundedSubscriber(4);
        early.fail(new CancellationException());
        early.onSubscribe(subscription);
        assertTrue(cancelled.get());
    }
}
