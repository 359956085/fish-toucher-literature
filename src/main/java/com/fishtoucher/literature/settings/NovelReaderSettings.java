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
        // --- Stealth mode (status bar): fixed 1 line ---
        public int stealthCharsPerLine = 60;

        // --- Normal mode (tool window): configurable multi-line ---
        public int normalLinesPerPage = 5;
        public int normalCharsPerLine = 60;

        // --- Shared settings ---
        public String lastFilePath = "";
        public String fontFamily = "Microsoft YaHei";
        public int fontSize = 13;
        public boolean showInStatusBar = true;
        public String installedVersion = "";

        // reading progress per mode (file path -> line number)
        public Map<String, Integer> stealthReadingProgress = new HashMap<>();
        public Map<String, Integer> normalReadingProgress = new HashMap<>();
        // legacy field kept for migration
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
        LOG.info("loadState: restoring settings"
                + ", stealthCharsPerLine=" + state.stealthCharsPerLine
                + ", normalLinesPerPage=" + state.normalLinesPerPage
                + ", normalCharsPerLine=" + state.normalCharsPerLine
                + ", fontFamily=" + state.fontFamily
                + ", fontSize=" + state.fontSize
                + ", showInStatusBar=" + state.showInStatusBar
                + ", lastFilePath=" + state.lastFilePath);
        myState = state;
        // Migrate legacy readingProgress to both modes if new maps are empty
        if (!state.readingProgress.isEmpty()) {
            if (state.stealthReadingProgress.isEmpty()) {
                state.stealthReadingProgress.putAll(state.readingProgress);
            }
            if (state.normalReadingProgress.isEmpty()) {
                state.normalReadingProgress.putAll(state.readingProgress);
            }
            state.readingProgress.clear();
        }
    }

    // --- Stealth mode ---
    public int getStealthCharsPerLine() { return myState.stealthCharsPerLine; }
    public void setStealthCharsPerLine(int chars) { myState.stealthCharsPerLine = Math.max(10, Math.min(500, chars)); }

    public int getStealthReadingProgress(String filePath) {
        return myState.stealthReadingProgress.getOrDefault(filePath, 0);
    }
    public void setStealthReadingProgress(String filePath, int lineNumber) {
        myState.stealthReadingProgress.put(filePath, lineNumber);
    }

    // --- Normal mode ---
    public int getNormalLinesPerPage() { return myState.normalLinesPerPage; }
    public void setNormalLinesPerPage(int lines) { myState.normalLinesPerPage = Math.max(1, Math.min(50, lines)); }

    public int getNormalCharsPerLine() { return myState.normalCharsPerLine; }
    public void setNormalCharsPerLine(int chars) { myState.normalCharsPerLine = Math.max(10, Math.min(500, chars)); }

    public int getNormalReadingProgress(String filePath) {
        return myState.normalReadingProgress.getOrDefault(filePath, 0);
    }
    public void setNormalReadingProgress(String filePath, int lineNumber) {
        myState.normalReadingProgress.put(filePath, lineNumber);
    }

    // --- Shared ---
    public String getLastFilePath() { return myState.lastFilePath; }
    public void setLastFilePath(String path) { myState.lastFilePath = path; }

    public String getFontFamily() { return myState.fontFamily; }
    public void setFontFamily(String fontFamily) { myState.fontFamily = fontFamily; }

    public int getFontSize() { return myState.fontSize; }
    public void setFontSize(int fontSize) { myState.fontSize = fontSize; }

    public boolean isShowInStatusBar() { return myState.showInStatusBar; }
    public void setShowInStatusBar(boolean show) { myState.showInStatusBar = show; }

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
