package com.fish.toucher.ui;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

/** 同时仅执行一次读取，后续请求只保留最新项，重复请求自动合并。 */
final class LatestTaskRunner<Q, R> implements AutoCloseable {
    interface Reader<Q, R> {
        R read(Q request) throws Exception;
    }

    private record Request<Q>(Q value, long generation) {}

    private final Executor worker;
    private final Executor dispatcher;
    private final Reader<Q, R> reader;
    private final BiConsumer<Q, R> completion;
    private final BiConsumer<Q, Exception> failure;
    private Request<Q> active;
    private Request<Q> pending;
    private long generation;
    private boolean closed;

    LatestTaskRunner(Executor worker, Executor dispatcher, Reader<Q, R> reader,
                     BiConsumer<Q, R> completion, BiConsumer<Q, Exception> failure) {
        this.worker = worker;
        this.dispatcher = dispatcher;
        this.reader = reader;
        this.completion = completion;
        this.failure = failure;
    }

    synchronized void submit(Q value) {
        if (closed) return;
        if (pending != null && Objects.equals(pending.value(), value)) return;
        if (pending == null && active != null && active.generation() == generation
                && Objects.equals(active.value(), value)) return;
        pending = new Request<>(value, ++generation);
        startNext();
    }

    synchronized void invalidate() {
        generation++;
        pending = null;
    }

    private synchronized void startNext() {
        if (closed || active != null || pending == null) return;
        Request<Q> request = pending;
        active = request;
        pending = null;
        try {
            worker.execute(() -> read(request));
        } catch (RuntimeException exception) {
            active = null;
            throw exception;
        }
    }

    private void read(Request<Q> request) {
        R result = null;
        Exception error = null;
        try {
            result = reader.read(request.value());
        } catch (Exception exception) {
            error = exception;
        }
        R value = result;
        Exception problem = error;
        try {
            dispatcher.execute(() -> {
                try {
                    if (isCurrent(request)) {
                        if (problem == null) completion.accept(request.value(), value);
                        else failure.accept(request.value(), problem);
                    }
                } finally {
                    finished();
                }
            });
        } catch (RuntimeException exception) {
            finished();
        }
    }

    private synchronized boolean isCurrent(Request<Q> request) {
        return !closed && generation == request.generation();
    }

    private synchronized void finished() {
        active = null;
        startNext();
    }

    @Override
    public synchronized void close() {
        closed = true;
        invalidate();
    }
}
