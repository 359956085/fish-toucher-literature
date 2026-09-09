package com.fish.toucher.ui;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 每份文档独占一个带锁会话目录，后台回收时跳过仍被任何进程占用的目录。 */
final class NovelCacheSession implements AutoCloseable {
    static final Executor CLEANER = new ThreadPoolExecutor(0, 1, 1, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(), runnable -> {
        Thread thread = new Thread(runnable, "NovelCache-cleaner");
        thread.setDaemon(true);
        thread.setContextClassLoader(ClassLoader.getPlatformClassLoader());
        return thread;
    });
    private static final String PREFIX = "session-v1-";
    private final Path directory;
    private final FileChannel channel;
    private final FileLock lock;
    private final Executor cleaner;
    private boolean closed;

    private NovelCacheSession(Path directory, FileChannel channel, FileLock lock, Executor cleaner) {
        this.directory = directory;
        this.channel = channel;
        this.lock = lock;
        this.cleaner = cleaner;
    }

    static NovelCacheSession create(Path root, Executor cleaner) throws IOException {
        Files.createDirectories(root);
        Path directory = Files.createDirectory(root.resolve(PREFIX + UUID.randomUUID()));
        FileChannel channel = null;
        try {
            channel = FileChannel.open(directory.resolve("owner.lock"),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            FileLock lock = channel.lock();
            return new NovelCacheSession(directory, channel, lock, cleaner);
        } catch (IOException | RuntimeException exception) {
            if (channel != null) channel.close();
            Files.deleteIfExists(directory.resolve("owner.lock"));
            Files.deleteIfExists(directory);
            throw exception;
        }
    }

    Path cachePath() {
        return directory.resolve("novel.utf8");
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        cleaner.execute(() -> {
            try {
                Files.deleteIfExists(cachePath());
            } catch (IOException ignored) {
                // 占用或磁盘故障留下的缓存交给后续清理。
            } finally {
                try {
                    lock.release();
                } catch (IOException ignored) {
                    // 关闭通道仍会释放文件锁。
                }
                try {
                    channel.close();
                } catch (IOException ignored) {
                    // 不阻止其余会话继续清理。
                }
            }
            removeEmptySession(directory);
        });
    }

    static void cleanup(Path root, Clock clock) {
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) return;
        long threshold = clock.millis() - Duration.ofDays(1).toMillis();
        try (var directories = Files.list(root)) {
            directories.filter(path -> path.getFileName().toString().startsWith(PREFIX))
                    .filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                    .forEach(path -> cleanupSession(path, threshold));
        } catch (IOException ignored) {
            // 清理失败不影响阅读。
        }
    }

    private static void cleanupSession(Path directory, long threshold) {
        Path owner = directory.resolve("owner.lock");
        try {
            if (Files.getLastModifiedTime(directory).toMillis() >= threshold
                    || !Files.isRegularFile(owner, LinkOption.NOFOLLOW_LINKS)) return;
            try (FileChannel channel = FileChannel.open(owner, StandardOpenOption.WRITE);
                 FileLock lock = channel.tryLock()) {
                if (lock == null) return;
                Files.deleteIfExists(directory.resolve("novel.utf8"));
            }
            removeEmptySession(directory);
        } catch (IOException | OverlappingFileLockException ignored) {
            // 当前实例或其他进程仍持有锁时必须跳过。
        }
    }

    private static void removeEmptySession(Path directory) {
        try {
            if (Files.exists(directory.resolve("novel.utf8"))) return;
            Files.deleteIfExists(directory.resolve("owner.lock"));
            Files.deleteIfExists(directory);
        } catch (IOException ignored) {
            // 不递归删除未知文件，避免扩大清理范围。
        }
    }
}
