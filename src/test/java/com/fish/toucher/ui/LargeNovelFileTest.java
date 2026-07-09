package com.fish.toucher.ui;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@Tag("large-file")
class LargeNovelFileTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void 应在五百一十二MiB堆内加载五百MiB小说() throws Exception {
        Path source = temporaryDirectory.resolve("500-mib.txt");
        byte[] block = new byte[1024 * 1024];
        Arrays.fill(block, (byte) 'a');
        for (int index = 127; index < block.length; index += 128) {
            block[index] = '\n';
        }
        try (OutputStream output = Files.newOutputStream(source)) {
            for (int index = 0; index < 500; index++) {
                output.write(block);
            }
        }

        assertEquals(IndexedNovelDocument.MAX_SOURCE_BYTES, Files.size(source));
        try (IndexedNovelDocument document =
                     IndexedNovelDocument.load(source, () -> false)) {
            assertTrue(document.lineCount() > 4_000_000);
            int lastLine = document.lineCount() - 1;
            IndexedNovelDocument.LineWindow window = document.readWindow(lastLine);
            assertTrue(window.contains(lastLine));
        }
    }
}
