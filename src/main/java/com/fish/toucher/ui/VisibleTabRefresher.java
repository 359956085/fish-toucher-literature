package com.fish.toucher.ui;

import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** 仅更新可见页签，隐藏期间合并变更，重新显示时补齐最新状态。 */
final class VisibleTabRefresher<K> implements AutoCloseable {
    private final Map<K, Runnable> refreshers = new LinkedHashMap<>();
    private final Set<K> dirty = new HashSet<>();
    private final BooleanSupplier showing;
    private final Supplier<K> selected;
    private boolean refreshing;
    private boolean closed;

    VisibleTabRefresher(BooleanSupplier showing, Supplier<K> selected) {
        this.showing = showing;
        this.selected = selected;
    }

    void register(K key, Runnable refresh) {
        refreshers.put(key, refresh);
        dirty.add(key);
    }

    void invalidate() {
        if (!closed) dirty.addAll(refreshers.keySet());
    }

    void refreshSelected() {
        if (closed || refreshing || !showing.getAsBoolean()) return;
        K key = selected.get();
        Runnable refresh = refreshers.get(key);
        if (refresh == null || !dirty.remove(key)) return;
        refreshing = true;
        try {
            refresh.run();
        } catch (RuntimeException exception) {
            dirty.add(key);
            throw exception;
        } finally {
            refreshing = false;
        }
    }

    @Override
    public void close() {
        closed = true;
        refreshers.clear();
        dirty.clear();
    }
}
