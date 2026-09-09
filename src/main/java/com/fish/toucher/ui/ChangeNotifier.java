package com.fish.toucher.ui;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** 合并界面通知；移除的监听器及关闭后的通知不再执行。 */
final class ChangeNotifier implements AutoCloseable {
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean pending = new AtomicBoolean();
    private final Executor dispatcher;
    private final Consumer<RuntimeException> errorHandler;
    private volatile boolean closed;

    ChangeNotifier(Executor dispatcher, Consumer<RuntimeException> errorHandler) {
        this.dispatcher = dispatcher;
        this.errorHandler = errorHandler;
    }

    void add(Runnable listener) {
        if (!closed) {
            listeners.addIfAbsent(listener);
            if (closed) listeners.remove(listener);
        }
    }

    void remove(Runnable listener) {
        listeners.remove(listener);
    }

    void fire() {
        if (closed || !pending.compareAndSet(false, true)) return;
        try {
            dispatcher.execute(() -> {
                pending.set(false);
                for (Runnable listener : listeners) {
                    if (closed) return;
                    if (!listeners.contains(listener)) continue;
                    try {
                        listener.run();
                    } catch (RuntimeException exception) {
                        errorHandler.accept(exception);
                    }
                }
            });
        } catch (RuntimeException exception) {
            pending.set(false);
            if (!closed) throw exception;
        }
    }

    @Override
    public void close() {
        closed = true;
        listeners.clear();
    }
}
