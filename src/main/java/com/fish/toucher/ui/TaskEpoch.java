package com.fish.toucher.ui;

/** 调用方与任务共用状态锁，使代次检查和状态修改成为同一个临界区。 */
final class TaskEpoch {
    private final Object owner;
    private long generation;
    private boolean closed;

    TaskEpoch(Object owner) {
        this.owner = owner;
    }

    void invalidate() {
        synchronized (owner) {
            generation++;
        }
    }

    Runnable guard(Runnable action) {
        synchronized (owner) {
            long expected = generation;
            return () -> {
                synchronized (owner) {
                    if (!closed && generation == expected) action.run();
                }
            };
        }
    }

    void close() {
        synchronized (owner) {
            closed = true;
            generation++;
        }
    }
}
