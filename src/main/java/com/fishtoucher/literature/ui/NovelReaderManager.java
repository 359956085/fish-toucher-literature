package com.fishtoucher.literature.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.fishtoucher.literature.settings.NovelReaderSettings;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Singleton manager that holds the novel content and current reading position.
 */
public class NovelReaderManager {

    private static final Logger LOG = Logger.getInstance(NovelReaderManager.class);
    private static final NovelReaderManager INSTANCE = new NovelReaderManager();

    private final List<String> lines = new ArrayList<>();
    private String currentFilePath = "";
    private int currentLine = 0;
    private boolean visible = true;

    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

    public static NovelReaderManager getInstance() {
        return INSTANCE;
    }

    private NovelReaderManager() {}

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void fireChange() {
        for (Runnable r : listeners) {
            ApplicationManager.getApplication().invokeLater(r);
        }
    }

    /**
     * Load a novel file with auto charset detection (UTF-8 / GBK fallback).
     */
    public boolean loadFile(String filePath) {
        LOG.info("loadFile: attempting to load file: " + filePath);
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            LOG.warn("loadFile: file does not exist or is not a file: " + filePath);
            return false;
        }

        // 读取文件全部字节
        byte[] rawBytes;
        try (FileInputStream fis = new FileInputStream(file)) {
            rawBytes = fis.readAllBytes();
            LOG.info("loadFile: read " + rawBytes.length + " bytes from file");
        } catch (Exception e) {
            LOG.error("loadFile: failed to read file: " + filePath, e);
            return false;
        }

        if (rawBytes.length == 0) {
            LOG.warn("loadFile: file is empty: " + filePath);
            return false;
        }

        // 检测并跳过 BOM，确定编码
        int offset = 0;
        Charset charset;

        if (rawBytes.length >= 3
                && (rawBytes[0] & 0xFF) == 0xEF
                && (rawBytes[1] & 0xFF) == 0xBB
                && (rawBytes[2] & 0xFF) == 0xBF) {
            // UTF-8 BOM
            LOG.info("loadFile: detected UTF-8 BOM");
            charset = StandardCharsets.UTF_8;
            offset = 3;
        } else if (rawBytes.length >= 2
                && (rawBytes[0] & 0xFF) == 0xFF
                && (rawBytes[1] & 0xFF) == 0xFE) {
            // UTF-16 LE BOM
            LOG.info("loadFile: detected UTF-16 LE BOM");
            charset = StandardCharsets.UTF_16LE;
            offset = 2;
        } else if (rawBytes.length >= 2
                && (rawBytes[0] & 0xFF) == 0xFE
                && (rawBytes[1] & 0xFF) == 0xFF) {
            // UTF-16 BE BOM
            LOG.info("loadFile: detected UTF-16 BE BOM");
            charset = StandardCharsets.UTF_16BE;
            offset = 2;
        } else if (isValidUtf8(rawBytes)) {
            LOG.info("loadFile: detected encoding as UTF-8 (no BOM)");
            charset = StandardCharsets.UTF_8;
            offset = 0;
        } else {
            // fallback to GBK for Chinese novels
            try {
                charset = Charset.forName("GBK");
                LOG.info("loadFile: falling back to GBK encoding");
            } catch (Exception e) {
                LOG.warn("loadFile: GBK charset not available, falling back to UTF-8", e);
                charset = StandardCharsets.UTF_8;
            }
            offset = 0;
        }

        // 解码文本
        String content;
        try {
            content = new String(rawBytes, offset, rawBytes.length - offset, charset);
        } catch (Exception e) {
            LOG.warn("loadFile: decoding with " + charset + " failed, falling back to UTF-8", e);
            content = new String(rawBytes, offset, rawBytes.length - offset, StandardCharsets.UTF_8);
        }

        // 按行分割，过滤空行
        List<String> newLines = new ArrayList<>();
        int linesPerPage = NovelReaderSettings.getInstance().getCharsPerLine();
        for (String line : content.split("\\r?\\n")) {
            String trimmed = line.trim();

            if (!trimmed.isEmpty()) {
                int idx = 0;
                while (idx < trimmed.length()) {
                    int end = Math.min(idx + linesPerPage, trimmed.length());
                    newLines.add(trimmed.substring(idx, end));
                    idx = end;
                }
            }
        }

        if (newLines.isEmpty()) {
            LOG.warn("loadFile: no valid lines after parsing file: " + filePath);
            return false;
        }

        lines.clear();
        lines.addAll(newLines);
        currentFilePath = filePath;

        // Restore reading progress
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        settings.setLastFilePath(filePath);
        currentLine = settings.getReadingProgress(filePath);
        if (currentLine >= lines.size()) {
            currentLine = 0;
        }

        LOG.info("loadFile: successfully loaded " + lines.size() + " lines from " + filePath
                + ", restored progress to line " + currentLine);
        visible = true;
        fireChange();
        return true;
    }

    /**
     * Strict UTF-8 validation by checking byte sequences.
     * Much more reliable than String decode + check for replacement char.
     */
    private boolean isValidUtf8(byte[] data) {
        int len = Math.min(data.length, 8192); // check first 8KB
        int i = 0;
        boolean hasHighByte = false;
        while (i < len) {
            int b = data[i] & 0xFF;
            int expectedBytes;
            if (b <= 0x7F) {
                i++;
                continue;
            } else if (b >= 0xC2 && b <= 0xDF) {
                expectedBytes = 1;
            } else if (b >= 0xE0 && b <= 0xEF) {
                expectedBytes = 2;
            } else if (b >= 0xF0 && b <= 0xF4) {
                expectedBytes = 3;
            } else {
                return false; // invalid leading byte
            }
            hasHighByte = true;
            if (i + expectedBytes >= len) break; // not enough bytes to check
            for (int j = 1; j <= expectedBytes; j++) {
                int cb = data[i + j] & 0xFF;
                if (cb < 0x80 || cb > 0xBF) {
                    return false; // invalid continuation byte
                }
            }
            i += expectedBytes + 1;
        }
        // if all ASCII, still treat as UTF-8
        return true;
    }

    public void nextPage() {
        if (lines.isEmpty()) return;
        int linesPerPage = NovelReaderSettings.getInstance().getLinesPerPage();
        int prevLine = currentLine;
        currentLine = Math.min(currentLine + linesPerPage, lines.size() - 1);
        LOG.debug("nextPage: " + prevLine + " -> " + currentLine + " (linesPerPage=" + linesPerPage + ")");
        saveProgress();
        fireChange();
    }

    public void prevPage() {
        if (lines.isEmpty()) return;
        int linesPerPage = NovelReaderSettings.getInstance().getLinesPerPage();
        int prevLine = currentLine;
        currentLine = Math.max(currentLine - linesPerPage, 0);
        LOG.debug("prevPage: " + prevLine + " -> " + currentLine + " (linesPerPage=" + linesPerPage + ")");
        saveProgress();
        fireChange();
    }

    public void jumpToLine(int line) {
        if (lines.isEmpty()) return;
        int prevLine = currentLine;
        currentLine = Math.max(0, Math.min(line, lines.size() - 1));
        LOG.debug("jumpToLine: " + prevLine + " -> " + currentLine + " (requested=" + line + ")");
        saveProgress();
        fireChange();
    }

    public void jumpToPercent(int percent) {
        if (lines.isEmpty()) return;
        int prevLine = currentLine;
        currentLine = (int) ((long) percent * (lines.size() - 1) / 100);
        LOG.debug("jumpToPercent: " + prevLine + " -> " + currentLine + " (percent=" + percent + "%)");
        saveProgress();
        fireChange();
    }

    private void saveProgress() {
        if (!currentFilePath.isEmpty()) {
            NovelReaderSettings.getInstance().setReadingProgress(currentFilePath, currentLine);
        }
    }

    public List<String> getCurrentPageLines() {
        List<String> result = new ArrayList<>();
        if (lines.isEmpty()) return result;
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        int linesPerPage = settings.getLinesPerPage();
        int charsPerLine = settings.getCharsPerLine();
        int end = Math.min(currentLine + linesPerPage, lines.size());
        for (int i = currentLine; i < end; i++) {
            String line = lines.get(i);
            if (charsPerLine > 0 && line.length() > charsPerLine) {
                // 按指定字数截断，剩余内容不丢失（会在原始行中保留，翻页后继续显示）
                result.add(line.substring(0, charsPerLine));
            } else {
                result.add(line);
            }
        }
        return result;
    }

    /**
     * 获取当前页的显示行（考虑 charsPerLine 折行）。
     * 与 getCurrentPageLines 不同，此方法会将超长行拆分为多个显示行。
     */
    public List<String> getCurrentPageDisplayLines() {
        List<String> result = new ArrayList<>();
        if (lines.isEmpty()) return result;
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        int linesPerPage = settings.getLinesPerPage();
        int charsPerLine = settings.getCharsPerLine();
        int end = Math.min(currentLine + linesPerPage, lines.size());
        for (int i = currentLine; i < end; i++) {
            String line = lines.get(i);
            if (charsPerLine > 0 && line.length() > charsPerLine) {
                // 将长行拆分为多个显示行
                int pos = 0;
                while (pos < line.length()) {
                    int lineEnd = Math.min(pos + charsPerLine, line.length());
                    result.add(line.substring(pos, lineEnd));
                    pos = lineEnd;
                }
            } else {
                result.add(line);
            }
        }
        return result;
    }

    public String getCurrentPageText() {
        List<String> pageLines = getCurrentPageLines();
        if (pageLines.isEmpty()) return "[No novel loaded — Alt+Shift+N to open]";
        return String.join("  ", pageLines);
    }

    public String getStatusText() {
        if (lines.isEmpty()) return "";
        int percent = (int) ((long) currentLine * 100 / lines.size());
        return String.format("[%d/%d] %d%%", currentLine + 1, lines.size(), percent);
    }

    public boolean isVisible() { return visible; }
    public void toggleVisibility() { visible = !visible; LOG.info("toggleVisibility: visible=" + visible); fireChange(); }
    public boolean hasContent() { return !lines.isEmpty(); }
    public int getCurrentLine() { return currentLine; }
    public int getTotalLines() { return lines.size(); }
    public String getCurrentFilePath() { return currentFilePath; }
}
