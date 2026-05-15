package com.fish.toucher.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@State(name = "NovelReaderSettings", storages = @Storage("NovelReaderSettings.xml"))
public class NovelReaderSettings implements PersistentStateComponent<NovelReaderSettings.State> {

    private static final Logger LOG = Logger.getInstance(NovelReaderSettings.class);
    private static final int MAX_RECENT_FILE_PATHS = 10;
    public static final String MODE_NOVEL = "novel";
    public static final String MODE_HOT_SEARCH = "hotsearch";
    public static final String MODE_CULTIVATION = "cultivation";
    public static final String LANGUAGE_AUTO = "auto";

    public static class State {
        // --- Stealth mode (status bar): fixed 1 line ---
        public int stealthCharsPerLine = 60;

        // --- Normal mode (tool window): configurable multi-line ---
        public int normalLinesPerPage = 5;
        public int normalCharsPerLine = 60;

        // --- Plugin mode: "novel", "hotsearch", or "cultivation" ---
        public String pluginMode = MODE_NOVEL;
        // --- UI language: "auto" follows IDEA; otherwise one of the bundled language codes ---
        public String uiLanguage = LANGUAGE_AUTO;
        // --- Hot search source: "baidu", "toutiao", "zhihu" ---
        public String hotSearchSource = "baidu";
        // --- Hot search timing (seconds/minutes) ---
        public int carouselIntervalSeconds = 10;
        public int refreshIntervalMinutes = 15;
        // --- X trends region slug (e.g. "united-states", "japan", "" for worldwide) ---
        public String xTrendsRegion = "";
        // --- Google Trends geo code (e.g. "US", "JP", "CN") ---
        public String googleTrendsGeo = "US";

        // --- Shared settings ---
        public String lastFilePath = "";
        public List<String> recentFilePaths = new ArrayList<>();
        public String fontFamily = "Microsoft YaHei";
        public int fontSize = 13;
        public boolean showInStatusBar = true;
        public String installedVersion = "";

        // unified reading progress (file path -> line number)
        public Map<String, Integer> readingProgress = new HashMap<>();
        // legacy fields kept for migration from dual-progress versions
        public Map<String, Integer> stealthReadingProgress = new HashMap<>();
        public Map<String, Integer> normalReadingProgress = new HashMap<>();

        // custom keyboard shortcuts (IntelliJ keystroke format)
        public String shortcutOpen = "ctrl shift alt M";
        public String shortcutNextPage = "alt shift RIGHT";
        public String shortcutPrevPage = "alt shift LEFT";
        public String shortcutToggle = "alt shift H";

        // --- Idle cultivation mode ---
        public int cultivationRealmIndex = 0;
        public long cultivationQi = 0L;
        public long cultivationSpiritStones = 0L;
        public long cultivationQiRemainderSeconds = 0L;
        public long cultivationSpiritStoneRemainderSeconds = 0L;
        public int cultivationBreakthroughFailures = 0;
        public long cultivationLastUpdateMillis = 0L;
        public long cultivationLastMeditationMillis = 0L;
        public int cultivationRebirthCount = 0;
        public String equippedTechniqueId = "basic_breathing";
        public List<String> unlockedTechniqueIds = new ArrayList<>();
        public Map<String, Integer> pillInventory = new HashMap<>();
        public boolean breakthroughPillActive = false;
        public boolean meridianPillActive = false;
        public String activeTravelLocationId = "";
        public long travelStartMillis = 0L;
        public long travelEndMillis = 0L;
        public long activeTravelElapsedMillis = 0L;
        public Map<String, Integer> abodeFacilityLevels = new HashMap<>();
        public Map<String, Long> abodeLastClaimMillis = new HashMap<>();
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
        if (state.readingProgress == null) {
            state.readingProgress = new HashMap<>();
        }
        if (state.stealthReadingProgress == null) {
            state.stealthReadingProgress = new HashMap<>();
        }
        if (state.normalReadingProgress == null) {
            state.normalReadingProgress = new HashMap<>();
        }
        if (state.recentFilePaths == null) {
            state.recentFilePaths = new ArrayList<>();
        }
        state.pluginMode = normalizePluginMode(state.pluginMode);
        state.uiLanguage = normalizeUiLanguage(state.uiLanguage);
        state.cultivationRealmIndex = Math.max(0, state.cultivationRealmIndex);
        state.cultivationQi = Math.max(0L, state.cultivationQi);
        state.cultivationSpiritStones = Math.max(0L, state.cultivationSpiritStones);
        state.cultivationQiRemainderSeconds = Math.max(0L, state.cultivationQiRemainderSeconds);
        state.cultivationSpiritStoneRemainderSeconds = Math.max(0L, state.cultivationSpiritStoneRemainderSeconds);
        state.cultivationBreakthroughFailures = Math.max(0, state.cultivationBreakthroughFailures);
        state.cultivationLastUpdateMillis = Math.max(0L, state.cultivationLastUpdateMillis);
        state.cultivationLastMeditationMillis = Math.max(0L, state.cultivationLastMeditationMillis);
        state.cultivationRebirthCount = Math.max(0, state.cultivationRebirthCount);
        state.activeTravelElapsedMillis = Math.max(0L, state.activeTravelElapsedMillis);
        normalizeCultivationState(state);
        // Migrate legacy dual-progress maps into unified readingProgress
        if (!state.stealthReadingProgress.isEmpty() || !state.normalReadingProgress.isEmpty()) {
            // Merge: take the greater progress (further reading position) for each file
            for (Map.Entry<String, Integer> entry : state.stealthReadingProgress.entrySet()) {
                state.readingProgress.merge(entry.getKey(), entry.getValue(), Math::max);
            }
            for (Map.Entry<String, Integer> entry : state.normalReadingProgress.entrySet()) {
                state.readingProgress.merge(entry.getKey(), entry.getValue(), Math::max);
            }
            state.stealthReadingProgress.clear();
            state.normalReadingProgress.clear();
        }
        if (state.recentFilePaths.isEmpty() && state.lastFilePath != null && !state.lastFilePath.isEmpty()) {
            addRecentFilePath(state.lastFilePath);
        } else {
            normalizeRecentFilePaths();
        }
    }

    // --- Stealth mode ---
    public int getStealthCharsPerLine() { return myState.stealthCharsPerLine; }
    public void setStealthCharsPerLine(int chars) { myState.stealthCharsPerLine = Math.max(10, Math.min(500, chars)); }

    // --- Normal mode ---
    public int getNormalLinesPerPage() { return myState.normalLinesPerPage; }
    public void setNormalLinesPerPage(int lines) { myState.normalLinesPerPage = Math.max(1, Math.min(50, lines)); }

    public int getNormalCharsPerLine() { return myState.normalCharsPerLine; }
    public void setNormalCharsPerLine(int chars) { myState.normalCharsPerLine = Math.max(10, Math.min(500, chars)); }

    // --- Unified reading progress ---
    public int getReadingProgress(String filePath) {
        return myState.readingProgress.getOrDefault(filePath, 0);
    }
    public void setReadingProgress(String filePath, int lineNumber) {
        myState.readingProgress.put(filePath, lineNumber);
    }

    // --- Shared ---
    public String getLastFilePath() { return myState.lastFilePath; }
    public void setLastFilePath(String path) { myState.lastFilePath = path; }

    public List<String> getRecentFilePaths() {
        normalizeRecentFilePaths();
        return Collections.unmodifiableList(new ArrayList<>(myState.recentFilePaths));
    }

    public void addRecentFilePath(String path) {
        if (path == null || path.isEmpty()) return;
        normalizeRecentFilePaths();
        myState.recentFilePaths.remove(path);
        myState.recentFilePaths.add(0, path);
        trimRecentFilePaths();
    }

    public void removeRecentFilePath(String path) {
        if (path == null || path.isEmpty() || myState.recentFilePaths == null) return;
        myState.recentFilePaths.remove(path);
    }

    private void normalizeRecentFilePaths() {
        if (myState.recentFilePaths == null) {
            myState.recentFilePaths = new ArrayList<>();
            return;
        }
        List<String> normalized = new ArrayList<>();
        for (String path : myState.recentFilePaths) {
            if (path != null && !path.isEmpty() && !normalized.contains(path)) {
                normalized.add(path);
            }
        }
        myState.recentFilePaths = normalized;
        trimRecentFilePaths();
    }

    private void trimRecentFilePaths() {
        while (myState.recentFilePaths.size() > MAX_RECENT_FILE_PATHS) {
            myState.recentFilePaths.remove(myState.recentFilePaths.size() - 1);
        }
    }

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

    // --- Plugin mode ---
    public String getPluginMode() { return normalizePluginMode(myState.pluginMode); }
    public void setPluginMode(String mode) { myState.pluginMode = normalizePluginMode(mode); }
    public boolean isNovelMode() { return MODE_NOVEL.equals(myState.pluginMode); }
    public boolean isHotSearchMode() { return MODE_HOT_SEARCH.equals(myState.pluginMode); }
    public boolean isCultivationMode() { return MODE_CULTIVATION.equals(myState.pluginMode); }

    public String getUiLanguage() { return normalizeUiLanguage(myState.uiLanguage); }
    public void setUiLanguage(String language) { myState.uiLanguage = normalizeUiLanguage(language); }

    private static String normalizePluginMode(String mode) {
        if (MODE_HOT_SEARCH.equals(mode) || MODE_CULTIVATION.equals(mode) || MODE_NOVEL.equals(mode)) {
            return mode;
        }
        return MODE_NOVEL;
    }

    private static String normalizeUiLanguage(String language) {
        if (LANGUAGE_AUTO.equals(language)
                || "en".equals(language)
                || "zh".equals(language)
                || "de".equals(language)
                || "fr".equals(language)
                || "it".equals(language)
                || "ja".equals(language)
                || "ko".equals(language)
                || "ru".equals(language)) {
            return language;
        }
        return LANGUAGE_AUTO;
    }

    private static void normalizeCultivationState(State state) {
        if (state.unlockedTechniqueIds == null) {
            state.unlockedTechniqueIds = new ArrayList<>();
        }
        if (!state.unlockedTechniqueIds.contains("basic_breathing")) {
            state.unlockedTechniqueIds.add(0, "basic_breathing");
        }
        if (state.equippedTechniqueId == null || state.equippedTechniqueId.isEmpty()
                || !state.unlockedTechniqueIds.contains(state.equippedTechniqueId)) {
            state.equippedTechniqueId = "basic_breathing";
        }
        if (state.pillInventory == null) {
            state.pillInventory = new HashMap<>();
        }
        state.pillInventory.entrySet().removeIf(entry -> entry.getKey() == null || entry.getKey().isEmpty() || entry.getValue() == null || entry.getValue() <= 0);
        if (state.activeTravelLocationId == null) {
            state.activeTravelLocationId = "";
        }
        state.travelStartMillis = Math.max(0L, state.travelStartMillis);
        state.travelEndMillis = Math.max(0L, state.travelEndMillis);
        state.activeTravelElapsedMillis = Math.max(0L, state.activeTravelElapsedMillis);
        if (state.activeTravelLocationId.isEmpty()) {
            state.travelStartMillis = 0L;
            state.travelEndMillis = 0L;
            state.activeTravelElapsedMillis = 0L;
        } else if (state.activeTravelElapsedMillis <= 0L && state.travelStartMillis > 0L && state.travelEndMillis > state.travelStartMillis) {
            long durationMillis = state.travelEndMillis - state.travelStartMillis;
            long elapsedMillis = Math.max(0L, System.currentTimeMillis() - state.travelStartMillis);
            state.activeTravelElapsedMillis = Math.min(durationMillis, elapsedMillis);
            state.travelStartMillis = 0L;
            state.travelEndMillis = 0L;
        }
        if (state.abodeFacilityLevels == null) {
            state.abodeFacilityLevels = new HashMap<>();
        }
        Map<String, Integer> normalizedFacilityLevels = new HashMap<>();
        for (Map.Entry<String, Integer> entry : state.abodeFacilityLevels.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isEmpty() && entry.getValue() != null && entry.getValue() > 0) {
                normalizedFacilityLevels.put(entry.getKey(), entry.getValue());
            }
        }
        state.abodeFacilityLevels = normalizedFacilityLevels;
        if (state.abodeLastClaimMillis == null) {
            state.abodeLastClaimMillis = new HashMap<>();
        }
        Map<String, Long> normalizedClaimMillis = new HashMap<>();
        for (Map.Entry<String, Long> entry : state.abodeLastClaimMillis.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isEmpty() && entry.getValue() != null && entry.getValue() > 0L) {
                normalizedClaimMillis.put(entry.getKey(), entry.getValue());
            }
        }
        state.abodeLastClaimMillis = normalizedClaimMillis;
    }

    // --- Hot search source ---
    public String getHotSearchSource() { return myState.hotSearchSource; }
    public void setHotSearchSource(String source) { myState.hotSearchSource = (source != null && !source.isEmpty()) ? source : "baidu"; }

    // --- Hot search timing ---
    public int getCarouselIntervalSeconds() { return myState.carouselIntervalSeconds; }
    public void setCarouselIntervalSeconds(int s) { myState.carouselIntervalSeconds = Math.max(3, Math.min(120, s)); }

    public int getRefreshIntervalMinutes() { return myState.refreshIntervalMinutes; }
    public void setRefreshIntervalMinutes(int m) { myState.refreshIntervalMinutes = Math.max(1, Math.min(120, m)); }

    // --- X trends region ---
    public String getXTrendsRegion() { return myState.xTrendsRegion; }
    public void setXTrendsRegion(String region) { myState.xTrendsRegion = region != null ? region : ""; }

    // --- Google Trends geo ---
    public String getGoogleTrendsGeo() { return myState.googleTrendsGeo; }
    public void setGoogleTrendsGeo(String geo) { myState.googleTrendsGeo = (geo != null && !geo.isEmpty()) ? geo : "US"; }

    // --- Idle cultivation ---
    public int getCultivationRealmIndex() { return Math.max(0, myState.cultivationRealmIndex); }
    public void setCultivationRealmIndex(int realmIndex) { myState.cultivationRealmIndex = Math.max(0, realmIndex); }

    public long getCultivationQi() { return Math.max(0L, myState.cultivationQi); }
    public void setCultivationQi(long qi) { myState.cultivationQi = Math.max(0L, qi); }

    public long getCultivationSpiritStones() { return Math.max(0L, myState.cultivationSpiritStones); }
    public void setCultivationSpiritStones(long spiritStones) { myState.cultivationSpiritStones = Math.max(0L, spiritStones); }

    public long getCultivationQiRemainderSeconds() { return Math.max(0L, myState.cultivationQiRemainderSeconds); }
    public void setCultivationQiRemainderSeconds(long remainderSeconds) { myState.cultivationQiRemainderSeconds = Math.max(0L, remainderSeconds); }

    public long getCultivationSpiritStoneRemainderSeconds() { return Math.max(0L, myState.cultivationSpiritStoneRemainderSeconds); }
    public void setCultivationSpiritStoneRemainderSeconds(long remainderSeconds) { myState.cultivationSpiritStoneRemainderSeconds = Math.max(0L, remainderSeconds); }

    public int getCultivationBreakthroughFailures() { return Math.max(0, myState.cultivationBreakthroughFailures); }
    public void setCultivationBreakthroughFailures(int failures) { myState.cultivationBreakthroughFailures = Math.max(0, failures); }

    public long getCultivationLastUpdateMillis() { return Math.max(0L, myState.cultivationLastUpdateMillis); }
    public void setCultivationLastUpdateMillis(long lastUpdateMillis) { myState.cultivationLastUpdateMillis = Math.max(0L, lastUpdateMillis); }

    public long getCultivationLastMeditationMillis() { return Math.max(0L, myState.cultivationLastMeditationMillis); }
    public void setCultivationLastMeditationMillis(long lastMeditationMillis) { myState.cultivationLastMeditationMillis = Math.max(0L, lastMeditationMillis); }

    public int getCultivationRebirthCount() { return Math.max(0, myState.cultivationRebirthCount); }
    public void setCultivationRebirthCount(int rebirthCount) { myState.cultivationRebirthCount = Math.max(0, rebirthCount); }

    public String getEquippedTechniqueId() {
        normalizeCultivationState(myState);
        return myState.equippedTechniqueId;
    }

    public void setEquippedTechniqueId(String techniqueId) {
        normalizeCultivationState(myState);
        if (techniqueId != null && myState.unlockedTechniqueIds.contains(techniqueId)) {
            myState.equippedTechniqueId = techniqueId;
        }
    }

    public List<String> getUnlockedTechniqueIds() {
        normalizeCultivationState(myState);
        return Collections.unmodifiableList(new ArrayList<>(myState.unlockedTechniqueIds));
    }

    public boolean isTechniqueUnlocked(String techniqueId) {
        normalizeCultivationState(myState);
        return techniqueId != null && myState.unlockedTechniqueIds.contains(techniqueId);
    }

    public boolean unlockTechnique(String techniqueId) {
        normalizeCultivationState(myState);
        if (techniqueId == null || techniqueId.isEmpty() || myState.unlockedTechniqueIds.contains(techniqueId)) {
            return false;
        }
        myState.unlockedTechniqueIds.add(techniqueId);
        return true;
    }

    public void resetUnlockedTechniquesForRebirth(String retainedTechniqueId) {
        List<String> rebirthTechniques = new ArrayList<>();
        rebirthTechniques.add("basic_breathing");
        if (retainedTechniqueId != null && !retainedTechniqueId.isEmpty()
                && !"basic_breathing".equals(retainedTechniqueId)) {
            rebirthTechniques.add(retainedTechniqueId);
        }
        myState.unlockedTechniqueIds = rebirthTechniques;
        myState.equippedTechniqueId = rebirthTechniques.contains(retainedTechniqueId)
                ? retainedTechniqueId
                : "basic_breathing";
        normalizeCultivationState(myState);
    }

    public Map<String, Integer> getPillInventory() {
        normalizeCultivationState(myState);
        return Collections.unmodifiableMap(new HashMap<>(myState.pillInventory));
    }

    public int getPillCount(String pillId) {
        normalizeCultivationState(myState);
        return myState.pillInventory.getOrDefault(pillId, 0);
    }

    public void addPill(String pillId, int count) {
        if (pillId == null || pillId.isEmpty() || count <= 0) return;
        normalizeCultivationState(myState);
        myState.pillInventory.merge(pillId, count, Integer::sum);
    }

    public void clearPillInventory() {
        myState.pillInventory = new HashMap<>();
    }

    public boolean consumePill(String pillId) {
        normalizeCultivationState(myState);
        int current = myState.pillInventory.getOrDefault(pillId, 0);
        if (current <= 0) {
            return false;
        }
        if (current == 1) {
            myState.pillInventory.remove(pillId);
        } else {
            myState.pillInventory.put(pillId, current - 1);
        }
        return true;
    }

    public boolean isBreakthroughPillActive() { return myState.breakthroughPillActive; }
    public void setBreakthroughPillActive(boolean active) { myState.breakthroughPillActive = active; }

    public boolean isMeridianPillActive() { return myState.meridianPillActive; }
    public void setMeridianPillActive(boolean active) { myState.meridianPillActive = active; }

    public String getActiveTravelLocationId() { return myState.activeTravelLocationId != null ? myState.activeTravelLocationId : ""; }
    public void setActiveTravelLocationId(String locationId) { myState.activeTravelLocationId = locationId != null ? locationId : ""; }

    public long getTravelStartMillis() { return Math.max(0L, myState.travelStartMillis); }
    public void setTravelStartMillis(long travelStartMillis) { myState.travelStartMillis = Math.max(0L, travelStartMillis); }

    public long getTravelEndMillis() { return Math.max(0L, myState.travelEndMillis); }
    public void setTravelEndMillis(long travelEndMillis) { myState.travelEndMillis = Math.max(0L, travelEndMillis); }

    public long getActiveTravelElapsedMillis() { return Math.max(0L, myState.activeTravelElapsedMillis); }
    public void setActiveTravelElapsedMillis(long elapsedMillis) { myState.activeTravelElapsedMillis = Math.max(0L, elapsedMillis); }

    public void clearTravel() {
        myState.activeTravelLocationId = "";
        myState.travelStartMillis = 0L;
        myState.travelEndMillis = 0L;
        myState.activeTravelElapsedMillis = 0L;
    }

    public Map<String, Integer> getAbodeFacilityLevels() {
        normalizeCultivationState(myState);
        return Collections.unmodifiableMap(new HashMap<>(myState.abodeFacilityLevels));
    }

    public int getAbodeFacilityLevel(String facilityId) {
        normalizeCultivationState(myState);
        return myState.abodeFacilityLevels.getOrDefault(facilityId, 0);
    }

    public void setAbodeFacilityLevel(String facilityId, int level) {
        if (facilityId == null || facilityId.isEmpty()) return;
        normalizeCultivationState(myState);
        if (level <= 0) {
            myState.abodeFacilityLevels.remove(facilityId);
        } else {
            myState.abodeFacilityLevels.put(facilityId, level);
        }
    }

    public void clearAbodeState() {
        myState.abodeFacilityLevels = new HashMap<>();
        myState.abodeLastClaimMillis = new HashMap<>();
    }

    public long getAbodeLastClaimMillis(String facilityId) {
        normalizeCultivationState(myState);
        return myState.abodeLastClaimMillis.getOrDefault(facilityId, 0L);
    }

    public void setAbodeLastClaimMillis(String facilityId, long millis) {
        if (facilityId == null || facilityId.isEmpty()) return;
        normalizeCultivationState(myState);
        if (millis <= 0L) {
            myState.abodeLastClaimMillis.remove(facilityId);
        } else {
            myState.abodeLastClaimMillis.put(facilityId, millis);
        }
    }
}
