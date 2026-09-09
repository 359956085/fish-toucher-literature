package com.fish.toucher.ui;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * 磁盘索引小说文档。源文件只扫描一次，堆内存仅保存稀疏索引和当前阅读窗口。
 *
 * @author fengshi
 */
final class IndexedNovelDocument implements AutoCloseable {

    static final long MAX_SOURCE_BYTES = 500L * 1024 * 1024;
    static final int MAX_LINE_BYTES = 1024 * 1024;
    static final int MAX_WINDOW_BYTES = 16 * 1024 * 1024;
    static final int MAX_WINDOW_LINES = 512;

    private static final long MAX_CACHE_BYTES = 1024L * 1024 * 1024;
    private static final int CHECKPOINT_LINES = 256;
    private static final long CHECKPOINT_BYTES = 4L * 1024 * 1024;
    private static final Path CACHE_DIR = Path.of(
            System.getProperty("java.io.tmpdir"),
            "fish-toucher-literature"
    );

    private final Path cachePath;
    private final NovelCacheSession cacheSession;
    private boolean closed;
    private int readers;
    private final int lineCount;
    private final int[] checkpointLines;
    private final long[] checkpointOffsets;

    private IndexedNovelDocument(NovelCacheSession cacheSession, int lineCount, SparseIndex index) {
        this.cacheSession = cacheSession;
        this.cachePath = cacheSession.cachePath();
        this.lineCount = lineCount;
        this.checkpointLines = index.lines();
        this.checkpointOffsets = index.offsets();
    }

    static IndexedNovelDocument load(Path source, BooleanSupplier cancelled)
            throws IOException, LoadException {
        if (source == null || !Files.isRegularFile(source) || !Files.isReadable(source)) {
            throw new LoadException(NovelReaderManager.LoadStatus.INVALID_FILE, "文件不存在或不可读");
        }
        long sourceBytes = Files.size(source);
        if (sourceBytes <= 0) {
            throw new LoadException(NovelReaderManager.LoadStatus.EMPTY, "文件为空");
        }
        if (sourceBytes > MAX_SOURCE_BYTES) {
            throw new LoadException(NovelReaderManager.LoadStatus.TOO_LARGE, "文件超过 500 MiB");
        }

        NovelCacheSession cacheSession = NovelCacheSession.create(CACHE_DIR, NovelCacheSession.CLEANER);
        Path cache = cacheSession.cachePath();
        boolean completed = false;
        try {
            CharsetInfo charsetInfo = detectCharset(source);
            SparseIndex index = new SparseIndex();
            index.add(0, 0);
            int lineCount = 0;
            int checkpointLineCount = 0;
            long checkpointByteCount = 0;
            long cacheBytes = 0;

            try (InputStream input = new BufferedInputStream(Files.newInputStream(source));
                 OutputStream output = new BufferedOutputStream(Files.newOutputStream(cache))) {
                input.skipNBytes(charsetInfo.bomBytes());
                try (LimitedLineReader reader = new LimitedLineReader(
                        new InputStreamReader(input, charsetInfo.charset())
                )) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (cancelled.getAsBoolean()) {
                            throw new CancellationException("加载已取消");
                        }
                        String trimmed = line.trim();
                        if (trimmed.isEmpty()) {
                            continue;
                        }
                        byte[] bytes = trimmed.getBytes(StandardCharsets.UTF_8);
                        if (bytes.length > MAX_LINE_BYTES) {
                            throw new LoadException(
                                    NovelReaderManager.LoadStatus.LINE_TOO_LONG,
                                    "存在超过 1 MiB 的单行"
                            );
                        }
                        if (cacheBytes + bytes.length + 1 > MAX_CACHE_BYTES) {
                            throw new LoadException(
                                    NovelReaderManager.LoadStatus.TOO_LARGE,
                                    "转换后的缓存超过 1 GiB"
                            );
                        }

                        output.write(bytes);
                        output.write('\n');
                        long written = bytes.length + 1L;
                        cacheBytes += written;
                        checkpointByteCount += written;
                        checkpointLineCount++;
                        lineCount++;

                        if (checkpointLineCount >= CHECKPOINT_LINES
                                || checkpointByteCount >= CHECKPOINT_BYTES) {
                            index.add(lineCount, cacheBytes);
                            checkpointLineCount = 0;
                            checkpointByteCount = 0;
                        }
                    }
                }
            }
            if (lineCount == 0) {
                throw new LoadException(NovelReaderManager.LoadStatus.EMPTY, "文件没有有效内容");
            }
            completed = true;
            return new IndexedNovelDocument(cacheSession, lineCount, index);
        } finally {
            if (!completed) {
                cacheSession.close();
            }
        }
    }

    int lineCount() {
        return lineCount;
    }

    LineWindow readWindow(int requestedLine) throws IOException {
        return readWithLease(() -> readWindowContents(requestedLine, 0));
    }

    LineWindow readPageWindow(int firstLine, int pageLines) throws IOException {
        int requiredLines = Math.max(1, Math.min(50, pageLines));
        return readWithLease(() -> readWindowContents(firstLine, requiredLines));
    }

    interface ReadOperation<T> {
        T read() throws IOException;
    }

    <T> T readWithLease(ReadOperation<T> operation) throws IOException {
        synchronized (this) {
            if (closed) throw new IOException("小说文档已关闭");
            readers++;
        }
        try {
            return operation.read();
        } finally {
            synchronized (this) {
                readers--;
                if (closed && readers == 0) cacheSession.close();
            }
        }
    }

    private LineWindow readWindowContents(int requestedLine, int requiredLines) throws IOException {
        int target = Math.max(0, Math.min(requestedLine, lineCount - 1));
        int checkpoint = findCheckpoint(target);
        int lineNumber = checkpointLines[checkpoint];
        int startLine = requiredLines > 0 ? target : Math.max(lineNumber, target - 128);
        List<String> lines = new ArrayList<>();
        int bytesRead = 0;

        try (FileChannel channel = FileChannel.open(cachePath, StandardOpenOption.READ)) {
            channel.position(checkpointOffsets[checkpoint]);
            try (BufferedReader reader = new BufferedReader(
                    Channels.newReader(channel, StandardCharsets.UTF_8)
            )) {
                String line;
                while (lineNumber < startLine && (line = reader.readLine()) != null) {
                    lineNumber++;
                }
                while (lines.size() < MAX_WINDOW_LINES
                        && lineNumber < lineCount
                        && (line = reader.readLine()) != null) {
                    int lineBytes = line.getBytes(StandardCharsets.UTF_8).length;
                    // 当前页必须完整；极长行页最多 50 行，其余预读仍受 16 MiB 限制。
                    if (lines.size() >= Math.max(1, requiredLines)
                            && bytesRead + lineBytes > MAX_WINDOW_BYTES) {
                        break;
                    }
                    lines.add(line);
                    bytesRead += lineBytes;
                    lineNumber++;
                }
            }
        }
        return new LineWindow(startLine, List.copyOf(lines));
    }

    private int findCheckpoint(int line) {
        int low = 0;
        int high = checkpointLines.length - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            if (checkpointLines[middle] <= line) {
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return Math.max(0, high);
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        if (readers == 0) cacheSession.close();
    }

    static void cleanupStaleCaches() {
        NovelCacheSession.CLEANER.execute(() ->
                NovelCacheSession.cleanup(CACHE_DIR, java.time.Clock.systemUTC()));
    }

    private static CharsetInfo detectCharset(Path source) throws IOException {
        byte[] sample;
        try (InputStream input = Files.newInputStream(source)) {
            sample = input.readNBytes(8192);
        }
        if (startsWith(sample, 0xEF, 0xBB, 0xBF)) {
            return new CharsetInfo(StandardCharsets.UTF_8, 3);
        }
        if (startsWith(sample, 0xFF, 0xFE)) {
            return new CharsetInfo(StandardCharsets.UTF_16LE, 2);
        }
        if (startsWith(sample, 0xFE, 0xFF)) {
            return new CharsetInfo(StandardCharsets.UTF_16BE, 2);
        }
        return new CharsetInfo(
                isValidUtf8(sample) ? StandardCharsets.UTF_8 : Charset.forName("GBK"),
                0
        );
    }

    private static boolean startsWith(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if ((bytes[index] & 0xFF) != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    static boolean isValidUtf8(byte[] data) {
        int index = 0;
        while (index < data.length) {
            int first = data[index] & 0xFF;
            int continuationCount;
            if (first <= 0x7F) {
                index++;
                continue;
            } else if (first >= 0xC2 && first <= 0xDF) {
                continuationCount = 1;
            } else if (first >= 0xE0 && first <= 0xEF) {
                continuationCount = 2;
            } else if (first >= 0xF0 && first <= 0xF4) {
                continuationCount = 3;
            } else {
                return false;
            }
            if (index + continuationCount >= data.length) {
                break;
            }
            for (int offset = 1; offset <= continuationCount; offset++) {
                int value = data[index + offset] & 0xFF;
                if (value < 0x80 || value > 0xBF) {
                    return false;
                }
            }
            index += continuationCount + 1;
        }
        return true;
    }

    record LineWindow(int startLine, List<String> lines) {
        boolean contains(int line) {
            return line >= startLine && line < startLine + lines.size();
        }

        String get(int line) {
            return lines.get(line - startLine);
        }
    }

    static final class LoadException extends Exception {
        private final NovelReaderManager.LoadStatus status;

        LoadException(NovelReaderManager.LoadStatus status, String message) {
            super(message);
            this.status = status;
        }

        NovelReaderManager.LoadStatus status() {
            return status;
        }
    }

    private record CharsetInfo(Charset charset, int bomBytes) {}

    private static final class SparseIndex {
        private int[] lines = new int[256];
        private long[] offsets = new long[256];
        private int size;

        void add(int line, long offset) {
            if (size > 0 && lines[size - 1] == line) {
                offsets[size - 1] = offset;
                return;
            }
            if (size == lines.length) {
                lines = Arrays.copyOf(lines, size * 2);
                offsets = Arrays.copyOf(offsets, size * 2);
            }
            lines[size] = line;
            offsets[size] = offset;
            size++;
        }

        int[] lines() {
            return Arrays.copyOf(lines, size);
        }

        long[] offsets() {
            return Arrays.copyOf(offsets, size);
        }
    }

    /**
     * 有界行读取器。先限制字符数，再限制 UTF-8 字节数，防止超长单行耗尽堆。
     */
    private static final class LimitedLineReader implements AutoCloseable {
        private final Reader reader;
        private final char[] buffer = new char[16 * 1024];
        private int position;
        private int limit;

        LimitedLineReader(Reader reader) {
            this.reader = reader;
        }

        String readLine() throws IOException, LoadException {
            StringBuilder result = new StringBuilder();
            boolean found = false;
            while (true) {
                int value = nextChar();
                if (value < 0) {
                    return found ? result.toString() : null;
                }
                found = true;
                if (value == '\n') {
                    if (!result.isEmpty() && result.charAt(result.length() - 1) == '\r') {
                        result.setLength(result.length() - 1);
                    }
                    return result.toString();
                }
                if (result.length() >= MAX_LINE_BYTES) {
                    throw new LoadException(
                            NovelReaderManager.LoadStatus.LINE_TOO_LONG,
                            "存在超过 1 MiB 的单行"
                    );
                }
                result.append((char) value);
            }
        }

        private int nextChar() throws IOException {
            if (position >= limit) {
                limit = reader.read(buffer);
                position = 0;
                if (limit < 0) {
                    return -1;
                }
            }
            return buffer[position++];
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}
