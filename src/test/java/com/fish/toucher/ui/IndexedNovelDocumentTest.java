package com.fish.toucher.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IndexedNovelDocumentTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void 应识别常用编码并保持空行过滤语义() throws Exception {
        assertContent("第一行\n\n 第二行 \n".getBytes(StandardCharsets.UTF_8));
        assertContent(withBom("第一行\n\n 第二行 \n", StandardCharsets.UTF_8));
        assertContent(withBom("第一行\n\n 第二行 \n", StandardCharsets.UTF_16LE));
        assertContent(withBom("第一行\n\n 第二行 \n", StandardCharsets.UTF_16BE));
        assertContent("第一行\n\n 第二行 \n".getBytes(Charset.forName("GBK")));
    }

    @Test
    void 应通过稀疏索引随机读取窗口() throws Exception {
        Path source = temporaryDirectory.resolve("many-lines.txt");
        StringBuilder content = new StringBuilder();
        for (int index = 0; index < 2_000; index++) {
            content.append("第").append(index).append("行\n");
        }
        Files.writeString(source, content, StandardCharsets.UTF_8);

        try (IndexedNovelDocument document =
                     IndexedNovelDocument.load(source, () -> false)) {
            IndexedNovelDocument.LineWindow window = document.readWindow(1_700);
            assertEquals(2_000, document.lineCount());
            assertTrue(window.contains(1_700));
            assertEquals("第1700行", window.get(1_700));
            assertTrue(window.lines().size() <= IndexedNovelDocument.MAX_WINDOW_LINES);
        }
    }

    @Test
    void 应拒绝超过五百MiB的源文件() throws Exception {
        Path source = temporaryDirectory.resolve("too-large.txt");
        try (RandomAccessFile file = new RandomAccessFile(source.toFile(), "rw")) {
            file.setLength(IndexedNovelDocument.MAX_SOURCE_BYTES + 1);
        }

        IndexedNovelDocument.LoadException exception = assertThrows(
                IndexedNovelDocument.LoadException.class,
                () -> IndexedNovelDocument.load(source, () -> false)
        );
        assertEquals(NovelReaderManager.LoadStatus.TOO_LARGE, exception.status());
    }

    @Test
    void 应拒绝超长单行() throws Exception {
        Path source = temporaryDirectory.resolve("long-line.txt");
        Files.writeString(
                source,
                "a".repeat(IndexedNovelDocument.MAX_LINE_BYTES + 1),
                StandardCharsets.UTF_8
        );

        IndexedNovelDocument.LoadException exception = assertThrows(
                IndexedNovelDocument.LoadException.class,
                () -> IndexedNovelDocument.load(source, () -> false)
        );
        assertEquals(NovelReaderManager.LoadStatus.LINE_TOO_LONG, exception.status());
    }

    private void assertContent(byte[] bytes) throws Exception {
        Path source = Files.createTempFile(temporaryDirectory, "encoding-", ".txt");
        Files.write(source, bytes);
        try (IndexedNovelDocument document =
                     IndexedNovelDocument.load(source, () -> false)) {
            IndexedNovelDocument.LineWindow window = document.readWindow(0);
            assertEquals(2, document.lineCount());
            assertEquals(List.of("第一行", "第二行"), window.lines());
        }
    }

    private static byte[] withBom(String text, Charset charset) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (StandardCharsets.UTF_8.equals(charset)) {
            output.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        } else if (StandardCharsets.UTF_16LE.equals(charset)) {
            output.write(new byte[]{(byte) 0xFF, (byte) 0xFE});
        } else {
            output.write(new byte[]{(byte) 0xFE, (byte) 0xFF});
        }
        output.write(text.getBytes(charset));
        return output.toByteArray();
    }
}
