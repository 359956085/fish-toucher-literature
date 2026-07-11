package com.fish.toucher.ui;

import com.fish.toucher.settings.NovelReaderSettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 小说阅读应用服务。正文保存在磁盘缓存，内存只保留当前阅读窗口。
 *
 * @author fengshi
 */
@Service(Service.Level.APP)
public final class NovelReaderManager implements Disposable {

    private static final Logger LOG = Logger.getInstance(NovelReaderManager.class);

    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean notificationPending = new AtomicBoolean();
    private final AtomicLong loadGeneration = new AtomicLong();
    private final AtomicLong windowGeneration = new AtomicLong();
    private final AtomicBoolean cacheRecoveryPending = new AtomicBoolean();

    private IndexedNovelDocument document;
    private IndexedNovelDocument.LineWindow window;
    private String currentFilePath = "";
    private int currentLine;
    private boolean visible = true;
    private boolean loading;
    private boolean disposed;

    public NovelReaderManager() {
        CompletableFuture.runAsync(IndexedNovelDocument::cleanupStaleCaches);
    }

    public static NovelReaderManager getInstance() {
        return ApplicationManager.getApplication().getService(NovelReaderManager.class);
    }

    public void addChangeListener(Runnable listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void fireChange() {
        if (!notificationPending.compareAndSet(false, true)) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            notificationPending.set(false);
            if (disposed) {
                return;
            }
            for (Runnable listener : listeners) {
                try {
                    listener.run();
                } catch (RuntimeException exception) {
                    LOG.warn("fireChange: novel reader listener failed", exception);
                }
            }
        });
    }

    /**
     * 异步加载小说。新请求会使旧请求失效，完成回调始终在 EDT 执行。
     */
    public void loadFileAsync(
            @Nullable Project project,
            String filePath,
            @Nullable Consumer<LoadResult> completion
    ) {
        if (filePath == null || filePath.isBlank()) {
            complete(completion, new LoadResult(LoadStatus.INVALID_FILE, "文件路径为空"));
            return;
        }

        long generation = loadGeneration.incrementAndGet();
        synchronized (this) {
            if (disposed) {
                complete(completion, new LoadResult(LoadStatus.CANCELLED, "服务已关闭"));
                return;
            }
            loading = true;
        }
        fireChange();

        new Task.Backgroundable(project, "加载小说", true) {
            private IndexedNovelDocument loadedDocument;
            private IndexedNovelDocument.LineWindow loadedWindow;
            private int loadedLine;
            private LoadResult result;

            @Override
            public void run(ProgressIndicator indicator) {
                try {
                    loadedDocument = IndexedNovelDocument.load(
                            Path.of(filePath),
                            () -> indicator.isCanceled() || generation != loadGeneration.get()
                    );
                    if (generation != loadGeneration.get()) {
                        throw new CancellationException("加载请求已失效");
                    }
                    int savedLine = NovelReaderSettings.getInstance().getReadingProgress(filePath);
                    loadedLine = savedLine >= 0 && savedLine < loadedDocument.lineCount()
                            ? savedLine : 0;
                    loadedWindow = loadedDocument.readWindow(loadedLine);
                    result = new LoadResult(LoadStatus.SUCCESS, "");
                } catch (CancellationException exception) {
                    result = new LoadResult(LoadStatus.CANCELLED, "加载已取消");
                } catch (IndexedNovelDocument.LoadException exception) {
                    result = new LoadResult(exception.status(), exception.getMessage());
                } catch (Exception exception) {
                    LOG.warn("加载小说失败: " + filePath, exception);
                    result = new LoadResult(LoadStatus.IO_ERROR, "读取文件失败");
                }
            }

            @Override
            public void onSuccess() {
                finishLoad(
                        generation,
                        filePath,
                        loadedDocument,
                        loadedWindow,
                        loadedLine,
                        result,
                        completion
                );
            }

            @Override
            public void onCancel() {
                closeQuietly(loadedDocument);
                finishCancelled(generation, completion);
            }

            @Override
            public void onThrowable(Throwable error) {
                LOG.warn("小说后台任务异常: " + filePath, error);
                closeQuietly(loadedDocument);
                finishLoad(
                        generation,
                        filePath,
                        null,
                        null,
                        0,
                        new LoadResult(LoadStatus.IO_ERROR, "读取文件失败"),
                        completion
                );
            }
        }.queue();
    }


    public boolean loadMostRecentFileIfNeeded() {
        synchronized (this) {
            if (hasContent() || loading) {
                return false;
            }
        }
        List<String> paths = NovelReaderSettings.getInstance().getRecentFilePaths();
        if (paths.isEmpty()) {
            return false;
        }
        loadFileAsync(null, paths.get(0), result -> {
            if (!result.isSuccess() && result.status() != LoadStatus.CANCELLED) {
                LOG.warn("最近小说加载失败: " + result.message());
            }
        });
        return true;
    }

    private void finishLoad(
            long generation,
            String filePath,
            IndexedNovelDocument loadedDocument,
            IndexedNovelDocument.LineWindow loadedWindow,
            int loadedLine,
            LoadResult result,
            @Nullable Consumer<LoadResult> completion
    ) {
        LoadResult safeResult = result != null
                ? result : new LoadResult(LoadStatus.IO_ERROR, "读取文件失败");
        IndexedNovelDocument documentToClose = null;
        IndexedNovelDocument rejectedDocument = null;
        LoadResult completionResult = safeResult;
        boolean stateChanged = false;
        synchronized (this) {
            if (generation != loadGeneration.get() || disposed) {
                rejectedDocument = loadedDocument;
                completionResult = new LoadResult(LoadStatus.CANCELLED, "加载已取消");
            } else if (safeResult.isSuccess() && loadedDocument != null && loadedWindow != null) {
                loading = false;
                documentToClose = document;
                document = loadedDocument;
                window = loadedWindow;
                currentLine = loadedLine;
                currentFilePath = filePath;
                visible = true;
                stateChanged = true;

                NovelReaderSettings settings = NovelReaderSettings.getInstance();
                settings.setLastFilePath(filePath);
                settings.addRecentFilePath(filePath);
                LOG.info("小说加载完成: lines=" + document.lineCount());
            } else {
                loading = false;
                rejectedDocument = loadedDocument;
                stateChanged = true;
            }
        }
        closeQuietly(documentToClose);
        closeQuietly(rejectedDocument);
        if (stateChanged) {
            fireChange();
        }
        complete(completion, completionResult);
    }

    private void finishCancelled(long generation, @Nullable Consumer<LoadResult> completion) {
        boolean stateChanged = false;
        synchronized (this) {
            if (generation == loadGeneration.get() && !disposed) {
                loading = false;
                stateChanged = true;
            }
        }
        if (stateChanged) {
            fireChange();
        }
        complete(completion, new LoadResult(LoadStatus.CANCELLED, "加载已取消"));
    }

    private static void complete(@Nullable Consumer<LoadResult> completion, LoadResult result) {
        if (completion == null) return;
        Runnable callback = () -> completion.accept(result);
        if (ApplicationManager.getApplication().isDispatchThread()) {
            callback.run();
        } else {
            ApplicationManager.getApplication().invokeLater(callback);
        }
    }

    public synchronized void stealthNextPage() {
        moveToLine(Math.min(currentLine + 1, getTotalLines() - 1));
    }

    public synchronized void stealthPrevPage() {
        moveToLine(Math.max(currentLine - 1, 0));
    }

    public synchronized String getStealthText() {
        String line = getCachedLine(currentLine);
        if (line == null) {
            return loading ? "[加载中...]" : "[未加载小说]";
        }
        int maxChars = NovelReaderSettings.getInstance().getStealthCharsPerLine();
        int codePoints = line.codePointCount(0, line.length());
        if (codePoints > maxChars) {
            return line.substring(0, line.offsetByCodePoints(0, maxChars));
        }
        if (codePoints < maxChars) {
            return line + "\u3000".repeat(maxChars - codePoints);
        }
        return line;
    }

    public synchronized String getStealthStatusText() {
        return buildStatusText();
    }

    public synchronized int getStealthCurrentLine() {
        return currentLine;
    }

    public synchronized void normalNextPage() {
        int linesPerPage = NovelReaderSettings.getInstance().getNormalLinesPerPage();
        moveToLine(Math.min(currentLine + linesPerPage, getTotalLines() - 1));
    }

    public synchronized void normalPrevPage() {
        int linesPerPage = NovelReaderSettings.getInstance().getNormalLinesPerPage();
        moveToLine(Math.max(currentLine - linesPerPage, 0));
    }

    public synchronized void normalJumpToPercent(int percent) {
        if (document == null) {
            return;
        }
        int safePercent = Math.max(0, Math.min(100, percent));
        int target = (int) ((long) safePercent * (document.lineCount() - 1) / 100);
        moveToLine(target);
    }

    public synchronized List<String> getNormalPageDisplayLines() {
        List<String> result = new ArrayList<>();
        if (document == null || window == null) {
            return result;
        }
        int linesPerPage = NovelReaderSettings.getInstance().getNormalLinesPerPage();
        int charsPerLine = NovelReaderSettings.getInstance().getNormalCharsPerLine();
        int end = Math.min(currentLine + linesPerPage, document.lineCount());
        for (int lineNumber = currentLine; lineNumber < end; lineNumber++) {
            String line = getCachedLine(lineNumber);
            if (line == null) {
                queueWindowLoad(lineNumber, currentLine);
                break;
            }
            int codePointCount = line.codePointCount(0, line.length());
            if (codePointCount <= charsPerLine) {
                result.add(line);
                continue;
            }
            int position = 0;
            while (position < line.length()) {
                int remaining = line.codePointCount(position, line.length());
                int next = line.offsetByCodePoints(position, Math.min(charsPerLine, remaining));
                result.add(line.substring(position, next));
                position = next;
            }
        }
        return result;
    }

    public synchronized String getNormalStatusText() {
        return buildStatusText();
    }

    public synchronized int getNormalCurrentLine() {
        return currentLine;
    }

    private String buildStatusText() {
        if (document == null) {
            return "";
        }
        int percent = (int) ((long) (currentLine + 1) * 100 / document.lineCount());
        return String.format("[%d/%d] %d%%", currentLine + 1, document.lineCount(), percent);
    }

    private void moveToLine(int targetLine) {
        if (document == null || targetLine < 0) {
            return;
        }
        int target = Math.min(targetLine, document.lineCount() - 1);
        if (window != null && window.contains(target)) {
            currentLine = target;
            saveProgress();
            fireChange();
            return;
        }
        queueWindowLoad(target, target);
    }

    private void queueWindowLoad(int requestedLine, int targetLine) {
        IndexedNovelDocument activeDocument = document;
        if (activeDocument == null) {
            return;
        }
        long generation = windowGeneration.incrementAndGet();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                IndexedNovelDocument.LineWindow loadedWindow =
                        activeDocument.readWindow(requestedLine);
                ApplicationManager.getApplication().invokeLater(() -> {
                    synchronized (NovelReaderManager.this) {
                        if (generation != windowGeneration.get()
                                || activeDocument != document
                                || disposed) {
                            return;
                        }
                        window = loadedWindow;
                        currentLine = Math.max(
                                0,
                                Math.min(targetLine, document.lineCount() - 1)
                        );
                        saveProgress();
                    }
                    fireChange();
                });
            } catch (IOException exception) {
                LOG.warn("读取小说分页缓存失败", exception);
                recoverFromCacheFailure(activeDocument);
            }
        });
    }

    private void recoverFromCacheFailure(IndexedNovelDocument failedDocument) {
        String path;
        synchronized (this) {
            if (disposed || failedDocument != document || currentFilePath.isEmpty()) {
                return;
            }
            path = currentFilePath;
        }
        if (!cacheRecoveryPending.compareAndSet(false, true)) {
            return;
        }
        loadFileAsync(null, path, result -> {
            cacheRecoveryPending.set(false);
            if (!result.isSuccess() && result.status() != LoadStatus.CANCELLED) {
                LOG.warn("小说分页缓存重建失败: " + result.message());
            }
        });
    }

    private String getCachedLine(int line) {
        return window != null && window.contains(line) ? window.get(line) : null;
    }

    private void saveProgress() {
        if (!currentFilePath.isEmpty()) {
            NovelReaderSettings.getInstance().setReadingProgress(currentFilePath, currentLine);
        }
    }

    public synchronized boolean isVisible() {
        return visible;
    }

    public synchronized void toggleVisibility() {
        visible = !visible;
        fireChange();
    }

    public synchronized boolean hasContent() {
        return document != null && document.lineCount() > 0;
    }

    public synchronized boolean isLoading() {
        return loading;
    }

    public synchronized int getTotalLines() {
        return document == null ? 0 : document.lineCount();
    }

    public synchronized String getCurrentFilePath() {
        return currentFilePath;
    }

    public void nextPage() {
        stealthNextPage();
    }

    public void prevPage() {
        stealthPrevPage();
    }

    @Override
    public void dispose() {
        IndexedNovelDocument documentToClose;
        synchronized (this) {
            disposed = true;
            loading = false;
            loadGeneration.incrementAndGet();
            windowGeneration.incrementAndGet();
            documentToClose = document;
            document = null;
            window = null;
            listeners.clear();
        }
        closeQuietly(documentToClose);
        IndexedNovelDocument.shutdownSessionCache();
    }

    private static void closeQuietly(@Nullable IndexedNovelDocument value) {
        if (value != null) {
            value.close();
        }
    }

    public enum LoadStatus {
        SUCCESS,
        CANCELLED,
        INVALID_FILE,
        TOO_LARGE,
        EMPTY,
        LINE_TOO_LONG,
        IO_ERROR
    }

    public record LoadResult(LoadStatus status, String message) {
        public boolean isSuccess() {
            return status == LoadStatus.SUCCESS;
        }
    }
}
