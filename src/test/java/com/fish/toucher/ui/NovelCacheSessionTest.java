package com.fish.toucher.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;

class NovelCacheSessionTest {
    @TempDir Path root;
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-09T00:00:00Z"), ZoneOffset.UTC);

    private void age(Path path) throws Exception {
        Files.setLastModifiedTime(path, FileTime.from(clock.instant().minusSeconds(172800)));
    }

    @Test
    void 超过一天的活跃会话不可被另一个实例清理() throws Exception {
        var cleaner = new AsyncLifecycleTest.QueueExecutor();
        NovelCacheSession first = NovelCacheSession.create(root, cleaner);
        NovelCacheSession second = NovelCacheSession.create(root, cleaner);
        Files.writeString(first.cachePath(), "第一实例");
        Files.writeString(second.cachePath(), "第二实例");
        age(first.cachePath().getParent());
        age(second.cachePath().getParent());
        NovelCacheSession.cleanup(root, clock);
        assertEquals("第一实例", Files.readString(first.cachePath()));
        assertEquals("第二实例", Files.readString(second.cachePath()));
        first.close(); first.close();
        assertEquals(1, cleaner.tasks.size());
        assertTrue(Files.exists(first.cachePath()));
        cleaner.drain();
        assertFalse(Files.exists(first.cachePath().getParent()));
        assertTrue(Files.exists(second.cachePath()));
        second.close(); cleaner.drain();
    }

    @Test
    void 仅回收过期且无锁的新格式缓存() throws Exception {
        Path old = Files.createDirectory(root.resolve("session-v1-abandoned"));
        Files.writeString(old.resolve("novel.utf8"), "异常退出遗留");
        try (FileChannel channel = FileChannel.open(old.resolve("owner.lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            try (var lock = channel.lock()) {
                age(old);
                NovelCacheSession.cleanup(root, clock);
                assertTrue(Files.exists(old.resolve("novel.utf8")));
            }
        }
        Path young = Files.createDirectory(root.resolve("session-v1-young"));
        Files.writeString(young.resolve("owner.lock"), "");
        Files.writeString(young.resolve("novel.utf8"), "近期缓存");
        Files.setLastModifiedTime(young, FileTime.from(clock.instant()));
        Path legacy = Files.writeString(root.resolve("novel-legacy.utf8"), "旧格式缓存");
        age(legacy);
        NovelCacheSession.cleanup(root, clock);
        assertFalse(Files.exists(old));
        assertTrue(Files.exists(young.resolve("novel.utf8")));
        assertTrue(Files.exists(legacy));
    }

    @Test
    void 文档关闭后拒绝新读取且可重复关闭() throws Exception {
        Path source = Files.writeString(root.resolve("book.txt"), "小说正文\n下一行");
        IndexedNovelDocument document = IndexedNovelDocument.load(source, () -> false);
        assertEquals("小说正文", document.readWindow(0).get(0));
        document.close();
        document.close();
        assertThrows(java.io.IOException.class, () -> document.readWindow(0));
    }

    @Test
    void 关闭不会中断已进入的读取但立即禁止新读取() throws Exception {
        Path source = Files.writeString(root.resolve("reading.txt"), "正文");
        IndexedNovelDocument document = IndexedNovelDocument.load(source, () -> false);
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var worker = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var read = worker.submit(() -> document.readWithLease(() -> {
                entered.countDown();
                try {
                    if (!release.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new java.io.IOException("等待超时");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new java.io.IOException(exception);
                }
                return "读取完成";
            }));
            assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS));
            document.close();
            assertFalse(read.isDone());
            assertThrows(java.io.IOException.class, () -> document.readWindow(0));
            release.countDown();
            assertEquals("读取完成", read.get(5, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            release.countDown();
            document.close();
            worker.shutdownNow();
        }
    }
}
