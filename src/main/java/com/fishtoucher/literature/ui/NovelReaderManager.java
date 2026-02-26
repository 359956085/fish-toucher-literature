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
 * Both stealth (status bar) and normal (tool window) modes share a unified reading position.
 */
public class NovelReaderManager {

    private static final Logger LOG = Logger.getInstance(NovelReaderManager.class);
    private static final NovelReaderManager INSTANCE = new NovelReaderManager();

    /** Raw lines from file. */
    private final List<String> rawLines = new ArrayList<>();
    private String currentFilePath = "";
    private boolean visible = true;

    // Unified reading position shared by both modes
    private int currentLine = 0;

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

    // ========== File loading ==========

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

        // Detect charset
        int offset = 0;
        Charset charset;

        if (rawBytes.length >= 3
                && (rawBytes[0] & 0xFF) == 0xEF
                && (rawBytes[1] & 0xFF) == 0xBB
                && (rawBytes[2] & 0xFF) == 0xBF) {
            charset = StandardCharsets.UTF_8;
            offset = 3;
        } else if (rawBytes.length >= 2
                && (rawBytes[0] & 0xFF) == 0xFF
                && (rawBytes[1] & 0xFF) == 0xFE) {
            charset = StandardCharsets.UTF_16LE;
            offset = 2;
        } else if (rawBytes.length >= 2
                && (rawBytes[0] & 0xFF) == 0xFE
                && (rawBytes[1] & 0xFF) == 0xFF) {
            charset = StandardCharsets.UTF_16BE;
            offset = 2;
        } else if (isValidUtf8(rawBytes)) {
            charset = StandardCharsets.UTF_8;
        } else {
            try {
                charset = Charset.forName("GBK");
            } catch (Exception e) {
                charset = StandardCharsets.UTF_8;
            }
        }

        String content;
        try {
            content = new String(rawBytes, offset, rawBytes.length - offset, charset);
        } catch (Exception e) {
            content = new String(rawBytes, offset, rawBytes.length - offset, StandardCharsets.UTF_8);
        }

        // Split into lines, filter empty
        List<String> newLines = new ArrayList<>();
        for (String line : content.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                newLines.add(trimmed);
            }
        }

        if (newLines.isEmpty()) {
            LOG.warn("loadFile: no valid lines after parsing file: " + filePath);
            return false;
        }

        rawLines.clear();
        rawLines.addAll(newLines);
        currentFilePath = filePath;

        // Restore unified reading progress
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        settings.setLastFilePath(filePath);

        currentLine = settings.getReadingProgress(filePath);
        if (currentLine >= rawLines.size()) currentLine = 0;

        LOG.info("loadFile: loaded " + rawLines.size() + " lines, position@" + currentLine);
        visible = true;
        fireChange();
        return true;
    }

    // ========== Stealth mode (status bar): 1 line at a time ==========

    public void stealthNextPage() {
        if (rawLines.isEmpty()) return;
        currentLine = Math.min(currentLine + 1, rawLines.size() - 1);
        saveProgress();
        fireChange();
    }

    public void stealthPrevPage() {
        if (rawLines.isEmpty()) return;
        currentLine = Math.max(currentLine - 1, 0);
        saveProgress();
        fireChange();
    }

    public String getStealthText() {
        if (rawLines.isEmpty()) return "[No novel loaded]";
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        int maxChars = settings.getStealthCharsPerLine();
        String line = rawLines.get(currentLine);
        if (maxChars > 0) {
            if (line.length() > maxChars) {
                line = line.substring(0, maxChars);
            } else if (line.length() < maxChars) {
                // Pad with ideographic spaces to fixed width for stable left-aligned display
                line = line + "\u3000".repeat(maxChars - line.length());
            }
        }
        return line;
    }

    public String getStealthStatusText() {
        if (rawLines.isEmpty()) return "";
        int percent = (int) ((long) currentLine * 100 / rawLines.size());
        return String.format("[%d/%d] %d%%", currentLine + 1, rawLines.size(), percent);
    }

    public int getStealthCurrentLine() { return currentLine; }

    // ========== Normal mode (tool window): multi-line ==========

    public void normalNextPage() {
        if (rawLines.isEmpty()) return;
        int linesPerPage = NovelReaderSettings.getInstance().getNormalLinesPerPage();
        currentLine = Math.min(currentLine + linesPerPage, rawLines.size() - 1);
        saveProgress();
        fireChange();
    }

    public void normalPrevPage() {
        if (rawLines.isEmpty()) return;
        int linesPerPage = NovelReaderSettings.getInstance().getNormalLinesPerPage();
        currentLine = Math.max(currentLine - linesPerPage, 0);
        saveProgress();
        fireChange();
    }

    public void normalJumpToPercent(int percent) {
        if (rawLines.isEmpty()) return;
        currentLine = (int) ((long) percent * (rawLines.size() - 1) / 100);
        saveProgress();
        fireChange();
    }

    /**
     * Get display lines for normal mode tool window (respects normalCharsPerLine wrapping).
     */
    public List<String> getNormalPageDisplayLines() {
        List<String> result = new ArrayList<>();
        if (rawLines.isEmpty()) return result;
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        int linesPerPage = settings.getNormalLinesPerPage();
        int charsPerLine = settings.getNormalCharsPerLine();
        int end = Math.min(currentLine + linesPerPage, rawLines.size());
        for (int i = currentLine; i < end; i++) {
            String line = rawLines.get(i);
            if (charsPerLine > 0 && line.length() > charsPerLine) {
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

    public String getNormalStatusText() {
        if (rawLines.isEmpty()) return "";
        int percent = (int) ((long) currentLine * 100 / rawLines.size());
        return String.format("[%d/%d] %d%%", currentLine + 1, rawLines.size(), percent);
    }

    public int getNormalCurrentLine() { return currentLine; }

    // ========== Progress persistence ==========

    private void saveProgress() {
        if (!currentFilePath.isEmpty()) {
            NovelReaderSettings.getInstance().setReadingProgress(currentFilePath, currentLine);
        }
    }

    // ========== Shared ==========

    public boolean isVisible() { return visible; }
    public void toggleVisibility() { visible = !visible; LOG.info("toggleVisibility: visible=" + visible); fireChange(); }
    public boolean hasContent() { return !rawLines.isEmpty(); }
    public int getTotalLines() { return rawLines.size(); }
    public String getCurrentFilePath() { return currentFilePath; }

    // ========== Shortcut actions ==========

    /** Next page: advances by 1 line (used by keyboard shortcuts). */
    public void nextPage() {
        stealthNextPage();
    }

    /** Prev page: retreats by 1 line (used by keyboard shortcuts). */
    public void prevPage() {
        stealthPrevPage();
    }

    // ========== Charset detection ==========

    private boolean isValidUtf8(byte[] data) {
        int len = Math.min(data.length, 8192);
        int i = 0;
        while (i < len) {
            int b = data[i] & 0xFF;
            int expectedBytes;
            if (b <= 0x7F) { i++; continue; }
            else if (b >= 0xC2 && b <= 0xDF) expectedBytes = 1;
            else if (b >= 0xE0 && b <= 0xEF) expectedBytes = 2;
            else if (b >= 0xF0 && b <= 0xF4) expectedBytes = 3;
            else return false;
            if (i + expectedBytes >= len) break;
            for (int j = 1; j <= expectedBytes; j++) {
                int cb = data[i + j] & 0xFF;
                if (cb < 0x80 || cb > 0xBF) return false;
            }
            i += expectedBytes + 1;
        }
        return true;
    }
}
