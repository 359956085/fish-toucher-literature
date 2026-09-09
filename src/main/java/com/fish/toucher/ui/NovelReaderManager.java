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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
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

    private final ChangeNotifier changes;
    private final AtomicLong loadGeneration = new AtomicLong();
    private final AtomicLong windowGeneration = new AtomicLong();
    private final LatestTaskRunner<WindowRequest, IndexedNovelDocument.LineWindow> windowReader;
    private final java.util.function.Supplier<NovelReaderSettings> settingsSupplier;
    private final java.util.concurrent.Executor dispatcher;

    private IndexedNovelDocument document;
    private IndexedNovelDocument.LineWindow window;
    private String currentFilePath = "";
    private int currentLine;
    private int requestedLine;
    private boolean visible = true;
    private boolean loading;
    private volatile boolean disposed;

    public NovelReaderManager() {
        this(NovelReaderSettings::getInstance,
                task -> ApplicationManager.getApplication().executeOnPooledThread(task),
                task -> ApplicationManager.getApplication().invokeLater(task));
        IndexedNovelDocument.cleanupStaleCaches();
    }

    NovelReaderManager(java.util.function.Supplier<NovelReaderSettings> settingsSupplier,
                       java.util.concurrent.Executor worker, java.util.concurrent.Executor dispatcher) {
        this.settingsSupplier = settingsSupplier;
        this.dispatcher = dispatcher;
        changes = new ChangeNotifier(dispatcher, exception -> LOG.warn("界面变更监听器执行失败", exception));
        windowReader = new LatestTaskRunner<>(worker, dispatcher,
                request -> request.document().readPageWindow(request.targetLine(), request.pageLines()),
                this::finishWindowLoad, this::windowLoadFailed);
    }

    public static NovelReaderManager getInstance() {
        return ApplicationManager.getApplication().getService(NovelReaderManager.class);
    }

    public void addChangeListener(Runnable listener) {
        changes.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        changes.remove(listener);
    }

    private void fireChange() {
        changes.fire();
    }

    /**
     * 异步加载小说。新请求会使旧请求失效，完成回调始终在 EDT 执行。
     */
    public void loadFileAsync(
            @Nullable Project project,
            String filePath,
            @Nullable Consumer<LoadResult> completion
    ) {
        AtomicBoolean completed = new AtomicBoolean();
        Consumer<LoadResult> callback = result -> {
            if (completed.compareAndSet(false, true)) complete(completion, result);
        };
        if (filePath == null || filePath.isBlank()) {
            callback.accept(new LoadResult(LoadStatus.INVALID_FILE, "文件路径为空"));
            return;
        }

        long generation;
        synchronized (this) {
            if (disposed) {
                callback.accept(new LoadResult(LoadStatus.CANCELLED, "服务已关闭"));
                return;
            }
            generation = loadGeneration.incrementAndGet();
            invalidateWindowLoads();
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
                    int savedLine = settingsSupplier.get().getReadingProgress(filePath);
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
                        callback
                );
            }

            @Override
            public void onCancel() {
                closeQuietly(loadedDocument);
                finishCancelled(generation, callback);
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
                        callback
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
        List<String> paths = settingsSupplier.get().getRecentFilePaths();
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

    void finishLoad(
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
        synchronized (this) {
            if (generation != loadGeneration.get() || disposed) {
                closeQuietly(loadedDocument);
                complete(completion, new LoadResult(LoadStatus.CANCELLED, "加载已取消"));
                return;
            }
            loading = false;
            if (safeResult.isSuccess() && loadedDocument != null && loadedWindow != null) {
                invalidateWindowLoads();
                closeQuietly(document);
                document = loadedDocument;
                window = loadedWindow;
                currentLine = loadedLine;
                requestedLine = loadedLine;
                currentFilePath = filePath;
                visible = true;

                NovelReaderSettings settings = settingsSupplier.get();
                settings.setLastFilePath(filePath);
                settings.addRecentFilePath(filePath);
                LOG.info("小说加载完成: lines=" + document.lineCount());
            } else {
                closeQuietly(loadedDocument);
            }
        }
        fireChange();
        complete(completion, safeResult);
    }

    private void finishCancelled(long generation, @Nullable Consumer<LoadResult> completion) {
        synchronized (this) {
            if (generation == loadGeneration.get() && !disposed) {
                loading = false;
                invalidateWindowLoads();
                fireChange();
            }
        }
        complete(completion, new LoadResult(LoadStatus.CANCELLED, "加载已取消"));
    }

    private void complete(@Nullable Consumer<LoadResult> completion, LoadResult result) {
        if (completion != null) {
            dispatcher.execute(() -> completion.accept(result));
        }
    }

    public synchronized void stealthNextPage() {
        moveToLine(Math.min(requestedLine + 1, getTotalLines() - 1));
    }

    public synchronized void stealthPrevPage() {
        moveToLine(Math.max(requestedLine - 1, 0));
    }

    public synchronized String getStealthText() {
        String line = getCachedLine(currentLine);
        if (line == null) {
            return loading ? "[加载中...]" : "[未加载小说]";
        }
        int maxChars = settingsSupplier.get().getStealthCharsPerLine();
        if (line.length() > maxChars) {
            return line.substring(0, maxChars);
        }
        if (line.length() < maxChars) {
            return line + "\u3000".repeat(maxChars - line.length());
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
        int linesPerPage = settingsSupplier.get().getNormalLinesPerPage();
        moveToLine(Math.min(requestedLine + linesPerPage, getTotalLines() - 1));
    }

    public synchronized void normalPrevPage() {
        int linesPerPage = settingsSupplier.get().getNormalLinesPerPage();
        moveToLine(Math.max(requestedLine - linesPerPage, 0));
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
        int linesPerPage = settingsSupplier.get().getNormalLinesPerPage();
        int charsPerLine = settingsSupplier.get().getNormalCharsPerLine();
        int end = Math.min(currentLine + linesPerPage, document.lineCount());
        for (int lineNumber = currentLine; lineNumber < end; lineNumber++) {
            String line = getCachedLine(lineNumber);
            if (line == null) {
                if (requestedLine == currentLine) queueWindowLoad(currentLine);
                break;
            }
            if (line.length() <= charsPerLine) {
                result.add(line);
                continue;
            }
            for (int position = 0; position < line.length(); position += charsPerLine) {
                result.add(line.substring(position, Math.min(position + charsPerLine, line.length())));
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
        int percent = (int) ((long) currentLine * 100 / document.lineCount());
        return String.format("[%d/%d] %d%%", currentLine + 1, document.lineCount(), percent);
    }

    private void moveToLine(int targetLine) {
        if (disposed || document == null || targetLine < 0) {
            return;
        }
        int target = Math.min(targetLine, document.lineCount() - 1);
        windowGeneration.incrementAndGet();
        windowReader.invalidate();
        requestedLine = target;
        if (window != null && window.contains(target)) {
            currentLine = target;
            saveProgress();
            fireChange();
            return;
        }
        queueWindowLoad(target);
    }

    private void invalidateWindowLoads() {
        windowGeneration.incrementAndGet();
        windowReader.invalidate();
        requestedLine = currentLine;
    }

    private void queueWindowLoad(int targetLine) {
        if (disposed || document == null) return;
        windowReader.submit(new WindowRequest(document, targetLine,
                settingsSupplier.get().getNormalLinesPerPage(), windowGeneration.get()));
    }

    private synchronized void finishWindowLoad(WindowRequest request,
                                              IndexedNovelDocument.LineWindow loadedWindow) {
        if (disposed || request.generation() != windowGeneration.get()
                || request.document() != document) return;
        window = loadedWindow;
        currentLine = request.targetLine();
        requestedLine = currentLine;
        saveProgress();
        fireChange();
    }

    private synchronized void windowLoadFailed(WindowRequest request, Exception exception) {
        if (disposed || request.generation() != windowGeneration.get()
                || request.document() != document) return;
        requestedLine = currentLine;
        LOG.warn("读取小说分页缓存失败", exception);
    }

    private record WindowRequest(IndexedNovelDocument document, int targetLine,
                                 int pageLines, long generation) {}

    private String getCachedLine(int line) {
        return window != null && window.contains(line) ? window.get(line) : null;
    }

    private void saveProgress() {
        if (!currentFilePath.isEmpty()) {
            settingsSupplier.get().setReadingProgress(currentFilePath, currentLine);
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
    public synchronized void dispose() {
        disposed = true;
        loadGeneration.incrementAndGet();
        windowGeneration.incrementAndGet();
        windowReader.close();
        closeQuietly(document);
        document = null;
        window = null;
        changes.close();
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
