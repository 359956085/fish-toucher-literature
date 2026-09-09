package com.fish.toucher.ui;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/** 热搜网络边界，支持替换传输实现验证取消与乱序响应。 */
interface HotSearchTransport extends AutoCloseable {
    CompletableFuture<HttpResponse<byte[]>> send(HttpRequest request);
    @Override
    void close();
}
