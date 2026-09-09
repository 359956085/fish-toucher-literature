package com.fish.toucher.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.Flow;

/** 对完整正文执行大小和时间限制，并将取消传播到底层网络订阅。 */
final class HotSearchHttpClient implements HotSearchTransport {
    private final HttpClient client;
    private final ScheduledExecutorService timer;
    private final Duration timeout;
    private final int maxBytes;
    private final Set<CompletableFuture<HttpResponse<byte[]>>> requests = ConcurrentHashMap.newKeySet();
    private boolean closed;

    HotSearchHttpClient(HttpClient client, ScheduledExecutorService timer, Duration timeout, int maxBytes) {
        this.client = client;
        this.timer = timer;
        this.timeout = timeout;
        this.maxBytes = maxBytes;
    }

    public synchronized CompletableFuture<HttpResponse<byte[]>> send(HttpRequest request) {
        if (closed) return CompletableFuture.failedFuture(new IOException("热搜客户端已关闭"));
        BoundedSubscriber body = new BoundedSubscriber(maxBytes);
        CompletableFuture<HttpResponse<byte[]>> result = new CompletableFuture<>();
        CompletableFuture<HttpResponse<byte[]>> network = client.sendAsync(request, info -> {
            if (info.statusCode() != 200) body.fail(new IOException("热搜请求返回 HTTP " + info.statusCode()));
            return body;
        });
        requests.add(result);
        ScheduledFuture<?> deadline = timer.schedule(() -> {
            result.completeExceptionally(new HttpTimeoutException("热搜完整正文接收超时"));
        }, timeout.toNanos(), TimeUnit.NANOSECONDS);
        result.whenComplete((response, error) -> {
            deadline.cancel(false);
            if (error != null) {
                body.fail(error);
                network.cancel(true);
            }
            requests.remove(result);
        });
        network.whenComplete((response, error) -> {
            if (error != null) result.completeExceptionally(error);
            else result.complete(response);
        });
        return result;
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        for (var request : requests) request.cancel(true);
        timer.shutdownNow();
        client.shutdownNow();
    }

    static final class BoundedSubscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final int maxBytes;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> body = new CompletableFuture<>();
        private Flow.Subscription subscription;

        BoundedSubscriber(int maxBytes) {
            this.maxBytes = maxBytes;
        }

        @Override
        public CompletionStage<byte[]> getBody() {
            return body;
        }

        @Override
        public synchronized void onSubscribe(Flow.Subscription value) {
            if (subscription != null || body.isDone()) {
                value.cancel();
                return;
            }
            subscription = value;
            value.request(1);
        }

        @Override
        public synchronized void onNext(List<ByteBuffer> buffers) {
            if (body.isDone()) return;
            for (ByteBuffer buffer : buffers) {
                int count = buffer.remaining();
                if (count > maxBytes - bytes.size()) {
                    fail(new IOException("热搜响应超过大小限制"));
                    return;
                }
                byte[] chunk = new byte[count];
                buffer.get(chunk);
                bytes.writeBytes(chunk);
            }
            subscription.request(1);
        }

        @Override
        public void onError(Throwable error) {
            fail(error);
        }

        @Override
        public synchronized void onComplete() {
            if (!body.isDone()) body.complete(bytes.toByteArray());
        }

        synchronized void fail(Throwable error) {
            if (body.completeExceptionally(error) && subscription != null) subscription.cancel();
        }
    }
}
