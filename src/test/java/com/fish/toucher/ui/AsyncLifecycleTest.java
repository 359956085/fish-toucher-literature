package com.fish.toucher.ui;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class AsyncLifecycleTest {
    static final class QueueExecutor implements Executor {
        final Deque<Runnable> tasks = new ArrayDeque<>();
        public void execute(Runnable task) { tasks.add(task); }
        void next() { tasks.remove().run(); }
        void drain() { while (!tasks.isEmpty()) next(); }
    }

    @Test
    void 读取只能运行一个且仅提交最后请求() {
        QueueExecutor worker = new QueueExecutor();
        QueueExecutor edt = new QueueExecutor();
        List<Integer> reads = new ArrayList<>();
        List<Integer> results = new ArrayList<>();
        var runner = new LatestTaskRunner<Integer, Integer>(worker, edt,
                value -> { reads.add(value); return value; },
                (request, value) -> results.add(value), (request, error) -> fail(error));
        runner.submit(1);
        runner.submit(1);
        assertEquals(1, worker.tasks.size());
        worker.next();
        for (int index = 2; index <= 100; index++) runner.submit(index);
        assertTrue(worker.tasks.isEmpty());
        edt.next();
        assertTrue(results.isEmpty());
        assertEquals(1, worker.tasks.size());
        worker.next();
        edt.next();
        assertEquals(List.of(1, 100), reads);
        assertEquals(List.of(100), results);
    }

    @Test
    void 缓存命中使旧读取失效且关闭清空待处理请求() {
        QueueExecutor worker = new QueueExecutor();
        QueueExecutor edt = new QueueExecutor();
        AtomicInteger calls = new AtomicInteger();
        var runner = new LatestTaskRunner<Integer, Integer>(worker, edt, value -> value,
                (request, value) -> calls.incrementAndGet(), (request, error) -> fail(error));
        runner.submit(1);
        worker.next();
        runner.invalidate();
        edt.next();
        assertEquals(0, calls.get());
        runner.submit(2);
        runner.submit(3);
        runner.close();
        worker.drain();
        edt.drain();
        runner.submit(4);
        assertTrue(worker.tasks.isEmpty());
        assertEquals(0, calls.get());
    }

    @Test
    void 读取失败后仍可继续处理最新请求() {
        QueueExecutor worker = new QueueExecutor();
        QueueExecutor edt = new QueueExecutor();
        AtomicInteger failures = new AtomicInteger();
        List<Integer> results = new ArrayList<>();
        var runner = new LatestTaskRunner<Integer, Integer>(worker, edt, value -> {
            if (value == 1) throw new java.io.IOException("模拟失败");
            return value;
        }, (request, value) -> results.add(value), (request, error) -> failures.incrementAndGet());
        runner.submit(1); worker.next(); edt.next();
        runner.submit(2); worker.next(); edt.next();
        assertEquals(1, failures.get());
        assertEquals(List.of(2), results);
    }

    @Test
    void 通知合并并隔离异常及已移除监听器() {
        QueueExecutor edt = new QueueExecutor();
        AtomicInteger errors = new AtomicInteger();
        AtomicInteger calls = new AtomicInteger();
        ChangeNotifier notifier = new ChangeNotifier(edt, error -> errors.incrementAndGet());
        Runnable removed = () -> fail("已移除监听器不应执行");
        notifier.add(() -> { notifier.remove(removed); throw new IllegalStateException("模拟失败"); });
        notifier.add(removed);
        notifier.add(calls::incrementAndGet);
        for (int index = 0; index < 100; index++) notifier.fire();
        assertEquals(1, edt.tasks.size());
        edt.next();
        assertEquals(1, errors.get());
        assertEquals(1, calls.get());
        notifier.fire();
        notifier.close();
        edt.drain();
        notifier.fire();
        assertEquals(1, calls.get());
        assertTrue(edt.tasks.isEmpty());
    }

    @Test
    void 停止重启及销毁后旧定时任务不可修改状态() {
        TaskEpoch epoch = new TaskEpoch(new Object());
        AtomicInteger ticks = new AtomicInteger();
        Runnable old = epoch.guard(ticks::incrementAndGet);
        old.run();
        epoch.invalidate();
        Runnable current = epoch.guard(ticks::incrementAndGet);
        old.run(); current.run();
        assertEquals(2, ticks.get());
        epoch.close();
        current.run(); old.run(); epoch.guard(ticks::incrementAndGet).run();
        assertEquals(2, ticks.get());
    }
}
