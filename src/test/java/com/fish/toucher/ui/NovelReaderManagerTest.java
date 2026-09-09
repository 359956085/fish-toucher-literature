package com.fish.toucher.ui;

import com.fish.toucher.settings.NovelReaderSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class NovelReaderManagerTest {
    @TempDir Path directory;
    final AsyncLifecycleTest.QueueExecutor worker = new AsyncLifecycleTest.QueueExecutor();
    final AsyncLifecycleTest.QueueExecutor edt = new AsyncLifecycleTest.QueueExecutor();
    final NovelReaderSettings settings = new NovelReaderSettings();

    private NovelReaderManager create() throws Exception {
        NovelReaderManager manager = new NovelReaderManager(() -> settings, worker, edt);
        install(manager, "first.txt");
        edt.drain();
        return manager;
    }

    private void install(NovelReaderManager manager, String name) throws Exception {
        Path path = directory.resolve(name);
        Files.writeString(path, IntStream.range(0, 2000).mapToObj(i -> "第" + i + "行")
                .collect(Collectors.joining("\n")));
        IndexedNovelDocument document = IndexedNovelDocument.load(path, () -> false);
        manager.finishLoad(0, path.toString(), document, document.readWindow(0), 0,
                new NovelReaderManager.LoadResult(NovelReaderManager.LoadStatus.SUCCESS, ""), null);
    }

    @Test
    void 跳远后回到缓存位置不能被旧结果覆盖() throws Exception {
        NovelReaderManager manager = create();
        try {
            manager.normalJumpToPercent(90);
            worker.next();
            manager.normalJumpToPercent(0);
            manager.stealthNextPage();
            edt.drain();
            assertEquals(1, manager.getNormalCurrentLine());
            assertTrue(worker.tasks.isEmpty());
        } finally { manager.dispose(); }
    }

    @Test
    void 连续跨缓存翻页累计最新目标且合并读取() throws Exception {
        NovelReaderManager manager = create();
        try {
            for (int index = 0; index < 600; index++) manager.stealthNextPage();
            assertEquals(1, worker.tasks.size());
            worker.next(); edt.drain();
            assertEquals(1, worker.tasks.size());
            worker.next(); edt.drain();
            assertEquals(600, manager.getNormalCurrentLine());
            assertEquals(600, settings.getReadingProgress(manager.getCurrentFilePath()));
        } finally { manager.dispose(); }
    }

    @Test
    void 换书与关闭都拒绝旧分页回调() throws Exception {
        NovelReaderManager manager = create();
        manager.normalJumpToPercent(90);
        worker.next();
        install(manager, "second.txt");
        edt.drain();
        assertEquals(0, manager.getNormalCurrentLine());
        assertTrue(manager.getCurrentFilePath().endsWith("second.txt"));
        manager.normalJumpToPercent(50);
        worker.next();
        manager.dispose();
        edt.drain();
        assertFalse(manager.hasContent());
    }

    @Test
    void 当前页跨窗口边界时应一次补齐而不反复加载() throws Exception {
        NovelReaderManager manager = create();
        try {
            settings.setNormalLinesPerPage(50);
            for (int index = 0; index < 500; index++) manager.stealthNextPage();
            manager.getNormalPageDisplayLines();
            worker.next(); edt.drain();
            assertEquals(50, manager.getNormalPageDisplayLines().size());
            assertTrue(worker.tasks.isEmpty());
            assertEquals("第500行", manager.getNormalPageDisplayLines().get(0));
        } finally { manager.dispose(); }
    }

    @Test
    void 无效路径完成回调应投递到界面线程一次() {
        NovelReaderManager manager = new NovelReaderManager(() -> settings, worker, edt);
        java.util.List<NovelReaderManager.LoadStatus> results = new java.util.ArrayList<>();
        manager.loadFileAsync(null, "", result -> results.add(result.status()));
        assertTrue(results.isEmpty());
        edt.drain();
        assertEquals(java.util.List.of(NovelReaderManager.LoadStatus.INVALID_FILE), results);
        manager.dispose();
    }
}
