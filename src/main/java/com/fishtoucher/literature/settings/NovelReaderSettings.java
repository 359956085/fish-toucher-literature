package com.fishtoucher.literature.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@State(name = "NovelReaderSettings", storages = @Storage("NovelReaderSettings.xml"))
public class NovelReaderSettings implements PersistentStateComponent<NovelReaderSettings.State> {

    private static final Logger LOG = Logger.getInstance(NovelReaderSettings.class);

    public static class State {
        public int linesPerPage = 1;
        public int charsPerLine = 60; // 0 = 不限制，自动换行
        public String lastFilePath = "";
        public String fontFamily = "Microsoft YaHei";
        public int fontSize = 13;
        public boolean showInStatusBar = true;
        public String installedVersion = "";
        // file path -> line number (reading progress)
        public Map<String, Integer> readingProgress = new HashMap<>();
        // custom keyboard shortcuts (IntelliJ keystroke format)
        public String shortcutOpen = "ctrl shift alt M";
        public String shortcutNextPage = "alt shift RIGHT";
        public String shortcutPrevPage = "alt shift LEFT";
        public String shortcutToggle = "alt shift H";
    }

    private State myState = new State();

    public static NovelReaderSettings getInstance() {
        return ApplicationManager.getApplication().getService(NovelReaderSettings.class);
    }

    @Override
    public @Nullable State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        LOG.info("loadState: restoring settings - linesPerPage=" + state.linesPerPage
                + ", charsPerLine=" + state.charsPerLine
                + ", fontFamily=" + state.fontFamily
                + ", fontSize=" + state.fontSize
                + ", showInStatusBar=" + state.showInStatusBar
                + ", lastFilePath=" + state.lastFilePath
                + ", progressEntries=" + state.readingProgress.size());
        myState = state;
    }

    public int getLinesPerPage() {
        return myState.linesPerPage;
    }

    public void setLinesPerPage(int lines) {
        myState.linesPerPage = Math.max(1, Math.min(50, lines));
    }

    public String getLastFilePath() {
        return myState.lastFilePath;
    }

    public void setLastFilePath(String path) {
        myState.lastFilePath = path;
    }

    public int getReadingProgress(String filePath) {
        return myState.readingProgress.getOrDefault(filePath, 0);
    }

    public void setReadingProgress(String filePath, int lineNumber) {
        LOG.debug("setReadingProgress: " + filePath + " -> line " + lineNumber);
        myState.readingProgress.put(filePath, lineNumber);
    }

    public String getFontFamily() {
        return myState.fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        myState.fontFamily = fontFamily;
    }

    public int getFontSize() {
        return myState.fontSize;
    }

    public void setFontSize(int fontSize) {
        myState.fontSize = fontSize;
    }

    public boolean isShowInStatusBar() {
        return myState.showInStatusBar;
    }

    public void setShowInStatusBar(boolean show) {
        myState.showInStatusBar = show;
    }

    public int getCharsPerLine() {
        return myState.charsPerLine;
    }

    public void setCharsPerLine(int chars) {
        myState.charsPerLine = Math.max(0, Math.min(500, chars)); // 0=不限制
    }

    public String getShortcutOpen() { return myState.shortcutOpen; }
    public void setShortcutOpen(String s) { myState.shortcutOpen = s != null ? s : ""; }

    public String getShortcutNextPage() { return myState.shortcutNextPage; }
    public void setShortcutNextPage(String s) { myState.shortcutNextPage = s != null ? s : ""; }

    public String getShortcutPrevPage() { return myState.shortcutPrevPage; }
    public void setShortcutPrevPage(String s) { myState.shortcutPrevPage = s != null ? s : ""; }

    public String getShortcutToggle() { return myState.shortcutToggle; }
    public void setShortcutToggle(String s) { myState.shortcutToggle = s != null ? s : ""; }

    public String getInstalledVersion() { return myState.installedVersion; }
    public void setInstalledVersion(String v) { myState.installedVersion = v != null ? v : ""; }
}
