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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@State(name = "NovelReaderSettings", storages = @Storage("NovelReaderSettings.xml"))
public class NovelReaderSettings implements PersistentStateComponent<NovelReaderSettings.State> {

    private static final Logger LOG = Logger.getInstance(NovelReaderSettings.class);
    private static final int MAX_RECENT_FILE_PATHS = 10;
    private static final int MAX_CULTIVATION_REALM_INDEX = 14;
    private static final int MAX_PENDING_SECT_EVENTS = 3;
    private static final int MAX_OWN_SECT_DISCIPLES = 16;
    private static final int MAX_OWN_SECT_CANDIDATES = 3;
    private static final int MAX_OWN_SECT_NAME_LENGTH = 16;
    private static final int MAX_OWN_SECT_BUILDING_LEVEL = 5;
    private static final Set<String> HOT_SEARCH_SOURCES = Set.of(
            "baidu", "toutiao", "zhihu", "douyin", "kuaishou", "x", "google"
    );
    private static final Set<String> X_REGIONS = Set.of(
            "", "united-states", "japan", "korea", "russia", "france",
            "germany", "italy", "spain", "brazil", "india", "indonesia",
            "thailand", "vietnam", "saudi-arabia", "portugal"
    );
    private static final Set<String> GOOGLE_GEOS = Set.of(
            "US", "JP", "KR", "RU", "FR", "DE", "IT", "ES", "BR",
            "IN", "ID", "TH", "VN", "SA", "PT", "GB", "AU", "CA", "MX"
    );
    private static final Set<String> CULTIVATION_PILL_IDS = Set.of(
            "qi_pill", "spirit_pill", "breakthrough_pill", "meridian_pill"
    );
    private static final Set<String> OWN_SECT_BUILDING_IDS = Set.of(
            "gathering_array", "alchemy_hall", "refining_pavilion", "scripture_library"
    );
    private static final Set<String> OWN_SECT_SPECIALTIES = Set.of(
            "GATHERING", "ALCHEMY", "REFINING", "SCRIPTURE"
    );
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
        public boolean cultivationAscended = false;
        public int ascensionRebirthCount = 0;
        public long ascensionMillis = 0L;
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
        public Map<String, Integer> pendingAlchemyPills = new HashMap<>();
        public List<String> unlockedSpellIds = new ArrayList<>();
        public List<String> equippedSpellIds = new ArrayList<>();
        public List<String> unlockedArtifactIds = new ArrayList<>();
        public List<String> equippedArtifactIds = new ArrayList<>();
        public List<String> defeatedCultivatorIds = new ArrayList<>();
        public String selectedCultivationPillId = "";
        public String selectedTravelLocationId = "";
        public String selectedCultivatorId = "";
        public String selectedSectPreviewId = "";
        public String selectedSectTaskId = "";
        public String selectedSectSecretRealmId = "";
        public String selectedSectInheritanceId = "";
        public String selectedSectTrialId = "";

        // --- Sect gameplay ---
        public String cultivationSectId = "";
        public Map<String, Long> sectPrestigeBySectId = new HashMap<>();
        public Map<String, Long> sectContributionBySectId = new HashMap<>();
        public Map<String, Integer> sectRankBySectId = new HashMap<>();
        public String activeSectTaskId = "";
        public long sectTaskStartMillis = 0L;
        public long sectTaskEndMillis = 0L;
        public long activeSectTaskElapsedMillis = 0L;
        public List<String> defeatedSectTrialIds = new ArrayList<>();
        public List<String> learnedSectInheritanceIds = new ArrayList<>();
        public List<String> graduatedSectIds = new ArrayList<>();
        public List<SectPendingEventState> pendingSectEvents = new ArrayList<>();
        public String activeSectSecretRealmId = "";
        public int sectSecretRealmNodeIndex = 0;
        public long sectSecretRealmStartedMillis = 0L;
        public long sectSecretRealmCooldownUntilMillis = 0L;
        public List<String> sectSecretRealmResolvedNodeIds = new ArrayList<>();

        // --- Ascended own sect gameplay ---
        public boolean ownSectCreated = false;
        public String ownSectName = "";
        public int ownSectTierIndex = 0;
        public long ownSectMaterials = 0L;
        public Map<String, Integer> ownSectBuildingLevels = new HashMap<>();
        public List<OwnSectDiscipleState> ownSectDisciples = new ArrayList<>();
        public List<OwnSectDiscipleState> ownSectRecruitmentCandidates = new ArrayList<>();
        public String ownSectRecruitmentSpecialty = "";
        public long ownSectRecruitmentStartMillis = 0L;
        public long ownSectRecruitmentElapsedMillis = 0L;
    }

    public static class SectPendingEventState {
        public String instanceId = "";
        public String eventId = "";
        public String sectId = "";
        public long createdMillis = 0L;

        public SectPendingEventState() {}

        public SectPendingEventState(String instanceId, String eventId, String sectId, long createdMillis) {
            this.instanceId = instanceId != null ? instanceId : "";
            this.eventId = eventId != null ? eventId : "";
            this.sectId = sectId != null ? sectId : "";
            this.createdMillis = Math.max(0L, createdMillis);
        }
    }

    public static class OwnSectDiscipleState {
        public String id = "";
        public String name = "";
        public String specialty = "";
        public int aptitude = 1;
        public String assignedBuildingId = "";

        public OwnSectDiscipleState() {}

        public OwnSectDiscipleState(String id, String name, String specialty, int aptitude, String assignedBuildingId) {
            this.id = id != null ? id : "";
            this.name = name != null ? name : "";
            this.specialty = specialty != null ? specialty : "";
            this.aptitude = aptitude;
            this.assignedBuildingId = assignedBuildingId != null ? assignedBuildingId : "";
        }
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
        state.stealthCharsPerLine = clamp(state.stealthCharsPerLine, 10, 500);
        state.normalLinesPerPage = clamp(state.normalLinesPerPage, 1, 50);
        state.normalCharsPerLine = clamp(state.normalCharsPerLine, 10, 500);
        state.fontSize = clamp(state.fontSize, 8, 30);
        state.fontFamily = state.fontFamily == null || state.fontFamily.isBlank()
                ? "Microsoft YaHei" : state.fontFamily;
        state.hotSearchSource = normalizeHotSearchSource(state.hotSearchSource);
        state.carouselIntervalSeconds = clamp(state.carouselIntervalSeconds, 3, 120);
        state.refreshIntervalMinutes = clamp(state.refreshIntervalMinutes, 1, 120);
        state.xTrendsRegion = normalizeXRegion(state.xTrendsRegion);
        state.googleTrendsGeo = normalizeGoogleGeo(state.googleTrendsGeo);
        normalizeReadingProgress(state.readingProgress);
        normalizeReadingProgress(state.stealthReadingProgress);
        normalizeReadingProgress(state.normalReadingProgress);
        state.cultivationRealmIndex = clamp(
                state.cultivationRealmIndex,
                0,
                MAX_CULTIVATION_REALM_INDEX
        );
        state.cultivationQi = Math.max(0L, state.cultivationQi);
        state.cultivationSpiritStones = Math.max(0L, state.cultivationSpiritStones);
        state.cultivationQiRemainderSeconds = Math.max(0L, state.cultivationQiRemainderSeconds);
        state.cultivationSpiritStoneRemainderSeconds = Math.max(0L, state.cultivationSpiritStoneRemainderSeconds);
        state.cultivationBreakthroughFailures = Math.max(0, state.cultivationBreakthroughFailures);
        state.cultivationLastUpdateMillis = Math.max(0L, state.cultivationLastUpdateMillis);
        state.cultivationLastMeditationMillis = Math.max(0L, state.cultivationLastMeditationMillis);
        state.cultivationRebirthCount = Math.max(0, state.cultivationRebirthCount);
        state.ascensionRebirthCount = Math.max(0, state.ascensionRebirthCount);
        state.ascensionMillis = Math.max(0L, state.ascensionMillis);
        state.activeTravelElapsedMillis = Math.max(0L, state.activeTravelElapsedMillis);
        normalizeCultivationUiSelections(state);
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
        if (filePath == null || filePath.isBlank()) return;
        myState.readingProgress.put(filePath, Math.max(0, lineNumber));
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
    public void setFontFamily(String fontFamily) {
        myState.fontFamily = fontFamily == null || fontFamily.isBlank()
                ? "Microsoft YaHei" : fontFamily;
    }

    public int getFontSize() { return myState.fontSize; }
    public void setFontSize(int fontSize) { myState.fontSize = clamp(fontSize, 8, 30); }

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

    // --- Idle cultivation UI selections ---
    public String getSelectedCultivationPillId() { return normalizeUiSelectionId(myState.selectedCultivationPillId); }
    public void setSelectedCultivationPillId(String id) { myState.selectedCultivationPillId = normalizeUiSelectionId(id); }

    public String getSelectedTravelLocationId() { return normalizeUiSelectionId(myState.selectedTravelLocationId); }
    public void setSelectedTravelLocationId(String id) { myState.selectedTravelLocationId = normalizeUiSelectionId(id); }

    public String getSelectedCultivatorId() { return normalizeUiSelectionId(myState.selectedCultivatorId); }
    public void setSelectedCultivatorId(String id) { myState.selectedCultivatorId = normalizeUiSelectionId(id); }

    public String getSelectedSectPreviewId() { return normalizeUiSelectionId(myState.selectedSectPreviewId); }
    public void setSelectedSectPreviewId(String id) { myState.selectedSectPreviewId = normalizeUiSelectionId(id); }

    public String getSelectedSectTaskId() { return normalizeUiSelectionId(myState.selectedSectTaskId); }
    public void setSelectedSectTaskId(String id) { myState.selectedSectTaskId = normalizeUiSelectionId(id); }

    public String getSelectedSectSecretRealmId() { return normalizeUiSelectionId(myState.selectedSectSecretRealmId); }
    public void setSelectedSectSecretRealmId(String id) { myState.selectedSectSecretRealmId = normalizeUiSelectionId(id); }

    public String getSelectedSectInheritanceId() { return normalizeUiSelectionId(myState.selectedSectInheritanceId); }
    public void setSelectedSectInheritanceId(String id) { myState.selectedSectInheritanceId = normalizeUiSelectionId(id); }

    public String getSelectedSectTrialId() { return normalizeUiSelectionId(myState.selectedSectTrialId); }
    public void setSelectedSectTrialId(String id) { myState.selectedSectTrialId = normalizeUiSelectionId(id); }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static String normalizeHotSearchSource(String source) {
        return source != null && HOT_SEARCH_SOURCES.contains(source) ? source : "baidu";
    }

    private static String normalizeXRegion(String region) {
        return region != null && X_REGIONS.contains(region) ? region : "";
    }

    private static String normalizeGoogleGeo(String geo) {
        return geo != null && GOOGLE_GEOS.contains(geo) ? geo : "US";
    }

    private static void normalizeReadingProgress(Map<String, Integer> progress) {
        progress.entrySet().removeIf(entry ->
                entry.getKey() == null
                        || entry.getKey().isBlank()
                        || entry.getValue() == null
                        || entry.getValue() < 0
        );
    }

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

    private static void normalizeCultivationUiSelections(State state) {
        state.selectedCultivationPillId = normalizeUiSelectionId(state.selectedCultivationPillId);
        state.selectedTravelLocationId = normalizeUiSelectionId(state.selectedTravelLocationId);
        state.selectedCultivatorId = normalizeUiSelectionId(state.selectedCultivatorId);
        state.selectedSectPreviewId = normalizeUiSelectionId(state.selectedSectPreviewId);
        state.selectedSectTaskId = normalizeUiSelectionId(state.selectedSectTaskId);
        state.selectedSectSecretRealmId = normalizeUiSelectionId(state.selectedSectSecretRealmId);
        state.selectedSectInheritanceId = normalizeUiSelectionId(state.selectedSectInheritanceId);
        state.selectedSectTrialId = normalizeUiSelectionId(state.selectedSectTrialId);
    }

    private static String normalizeUiSelectionId(String id) {
        return id == null ? "" : id.trim();
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
        state.pendingAlchemyPills = normalizeIntegerMap(state.pendingAlchemyPills, 1, Integer.MAX_VALUE, CULTIVATION_PILL_IDS);
        state.unlockedSpellIds = normalizeStringList(state.unlockedSpellIds);
        state.equippedSpellIds = normalizeStringList(state.equippedSpellIds);
        state.equippedSpellIds.removeIf(spellId -> !state.unlockedSpellIds.contains(spellId));
        while (state.equippedSpellIds.size() > 3) {
            state.equippedSpellIds.remove(state.equippedSpellIds.size() - 1);
        }
        state.unlockedArtifactIds = normalizeStringList(state.unlockedArtifactIds);
        state.equippedArtifactIds = normalizeStringList(state.equippedArtifactIds);
        state.equippedArtifactIds.removeIf(artifactId -> !state.unlockedArtifactIds.contains(artifactId));
        while (state.equippedArtifactIds.size() > 2) {
            state.equippedArtifactIds.remove(state.equippedArtifactIds.size() - 1);
        }
        state.defeatedCultivatorIds = normalizeStringList(state.defeatedCultivatorIds);
        state.cultivationSectId = state.cultivationSectId == null ? "" : state.cultivationSectId;
        state.sectPrestigeBySectId = normalizeLongMap(state.sectPrestigeBySectId);
        state.sectContributionBySectId = normalizeLongMap(state.sectContributionBySectId);
        state.sectRankBySectId = normalizeIntegerMap(state.sectRankBySectId, 0, 4);
        state.activeSectTaskId = state.activeSectTaskId == null ? "" : state.activeSectTaskId;
        state.sectTaskStartMillis = Math.max(0L, state.sectTaskStartMillis);
        state.sectTaskEndMillis = Math.max(0L, state.sectTaskEndMillis);
        state.activeSectTaskElapsedMillis = Math.max(0L, state.activeSectTaskElapsedMillis);
        if (state.activeSectTaskId.isEmpty()) {
            state.sectTaskStartMillis = 0L;
            state.sectTaskEndMillis = 0L;
            state.activeSectTaskElapsedMillis = 0L;
        }
        state.defeatedSectTrialIds = normalizeStringList(state.defeatedSectTrialIds);
        state.learnedSectInheritanceIds = normalizeStringList(state.learnedSectInheritanceIds);
        state.graduatedSectIds = normalizeStringList(state.graduatedSectIds);
        state.pendingSectEvents = normalizeSectPendingEvents(state.pendingSectEvents);
        state.activeSectSecretRealmId = state.activeSectSecretRealmId == null ? "" : state.activeSectSecretRealmId;
        state.sectSecretRealmNodeIndex = Math.max(0, state.sectSecretRealmNodeIndex);
        state.sectSecretRealmStartedMillis = Math.max(0L, state.sectSecretRealmStartedMillis);
        state.sectSecretRealmCooldownUntilMillis = Math.max(0L, state.sectSecretRealmCooldownUntilMillis);
        state.sectSecretRealmResolvedNodeIds = normalizeStringList(state.sectSecretRealmResolvedNodeIds);
        if (state.activeSectSecretRealmId.isEmpty()) {
            state.sectSecretRealmNodeIndex = 0;
            state.sectSecretRealmStartedMillis = 0L;
            state.sectSecretRealmResolvedNodeIds.clear();
        }
        normalizeOwnSectState(state);
    }

    private static void normalizeOwnSectState(State state) {
        state.ownSectName = normalizeOwnSectName(state.ownSectName);
        if (!state.cultivationAscended) {
            state.cultivationRealmIndex = Math.min(state.cultivationRealmIndex, 8);
            state.ascensionRebirthCount = 0;
            state.ascensionMillis = 0L;
            state.ownSectCreated = false;
            state.ownSectName = "";
        }
        state.ownSectTierIndex = clamp(state.ownSectTierIndex, 0, 5);
        state.ownSectMaterials = Math.max(0L, state.ownSectMaterials);
        state.ownSectBuildingLevels = normalizeIntegerMap(state.ownSectBuildingLevels, 0, MAX_OWN_SECT_BUILDING_LEVEL, OWN_SECT_BUILDING_IDS);
        state.ownSectDisciples = normalizeOwnSectDisciples(state.ownSectDisciples, MAX_OWN_SECT_DISCIPLES);
        state.ownSectRecruitmentCandidates = normalizeOwnSectDisciples(state.ownSectRecruitmentCandidates, MAX_OWN_SECT_CANDIDATES);
        state.ownSectRecruitmentSpecialty = normalizeOwnSectSpecialty(state.ownSectRecruitmentSpecialty);
        state.ownSectRecruitmentStartMillis = Math.max(0L, state.ownSectRecruitmentStartMillis);
        state.ownSectRecruitmentElapsedMillis = Math.max(0L, state.ownSectRecruitmentElapsedMillis);
        if (state.ownSectRecruitmentSpecialty.isEmpty()) {
            state.ownSectRecruitmentStartMillis = 0L;
            state.ownSectRecruitmentElapsedMillis = 0L;
        }
        if (!state.ownSectCreated) {
            state.ownSectName = "";
            state.ownSectTierIndex = 0;
            state.ownSectMaterials = 0L;
            state.ownSectBuildingLevels = new HashMap<>();
            state.ownSectDisciples = new ArrayList<>();
            state.ownSectRecruitmentCandidates = new ArrayList<>();
            state.ownSectRecruitmentSpecialty = "";
            state.ownSectRecruitmentStartMillis = 0L;
            state.ownSectRecruitmentElapsedMillis = 0L;
        }
    }

    private static String normalizeOwnSectName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.length() > MAX_OWN_SECT_NAME_LENGTH) {
            normalized = normalized.substring(0, MAX_OWN_SECT_NAME_LENGTH);
        }
        return normalized;
    }

    private static String normalizeOwnSectSpecialty(String specialty) {
        String normalized = specialty == null ? "" : specialty.trim();
        return OWN_SECT_SPECIALTIES.contains(normalized) ? normalized : "";
    }

    private static List<OwnSectDiscipleState> normalizeOwnSectDisciples(List<OwnSectDiscipleState> values, int limit) {
        List<OwnSectDiscipleState> normalized = new ArrayList<>();
        if (values == null) {
            return normalized;
        }
        Set<String> ids = new LinkedHashSet<>();
        for (OwnSectDiscipleState value : values) {
            if (value == null || normalized.size() >= limit) {
                continue;
            }
            String id = value.id != null ? value.id : "";
            String name = normalizeOwnSectName(value.name);
            String specialty = value.specialty != null ? value.specialty : "";
            String buildingId = value.assignedBuildingId != null ? value.assignedBuildingId : "";
            if (id.isEmpty() || name.isEmpty() || !OWN_SECT_SPECIALTIES.contains(specialty) || !ids.add(id)) {
                continue;
            }
            if (!OWN_SECT_BUILDING_IDS.contains(buildingId)) {
                buildingId = "";
            }
            normalized.add(new OwnSectDiscipleState(id, name, specialty, clamp(value.aptitude, 1, 100), buildingId));
        }
        return normalized;
    }

    private static List<SectPendingEventState> normalizeSectPendingEvents(List<SectPendingEventState> values) {
        List<SectPendingEventState> normalized = new ArrayList<>();
        if (values == null) {
            return normalized;
        }
        Set<String> instanceIds = new LinkedHashSet<>();
        for (SectPendingEventState value : values) {
            if (value == null) {
                continue;
            }
            String instanceId = value.instanceId != null ? value.instanceId : "";
            String eventId = value.eventId != null ? value.eventId : "";
            String sectId = value.sectId != null ? value.sectId : "";
            if (instanceId.isEmpty() || eventId.isEmpty() || sectId.isEmpty() || !instanceIds.add(instanceId)) {
                continue;
            }
            normalized.add(new SectPendingEventState(instanceId, eventId, sectId, Math.max(0L, value.createdMillis)));
            if (normalized.size() >= MAX_PENDING_SECT_EVENTS) {
                break;
            }
        }
        return normalized;
    }

    private static Map<String, Long> normalizeLongMap(Map<String, Long> values) {
        Map<String, Long> normalized = new HashMap<>();
        if (values == null) {
            return normalized;
        }
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isEmpty() && entry.getValue() != null && entry.getValue() >= 0L) {
                normalized.put(entry.getKey(), entry.getValue());
            }
        }
        return normalized;
    }

    private static Map<String, Integer> normalizeIntegerMap(Map<String, Integer> values, int minimum, int maximum) {
        Map<String, Integer> normalized = new HashMap<>();
        if (values == null) {
            return normalized;
        }
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isEmpty() && entry.getValue() != null) {
                normalized.put(entry.getKey(), clamp(entry.getValue(), minimum, maximum));
            }
        }
        return normalized;
    }

    private static Map<String, Integer> normalizeIntegerMap(
            Map<String, Integer> values,
            int minimum,
            int maximum,
            Set<String> allowedKeys
    ) {
        Map<String, Integer> normalized = new HashMap<>();
        if (values == null) {
            return normalized;
        }
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (entry.getKey() != null
                    && allowedKeys.contains(entry.getKey())
                    && entry.getValue() != null
                    && entry.getValue() >= minimum) {
                normalized.put(entry.getKey(), clamp(entry.getValue(), minimum, maximum));
            }
        }
        return normalized;
    }

    private static List<String> normalizeStringList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                normalized.add(value);
            }
        }
        return new ArrayList<>(normalized);
    }

    // --- Hot search source ---
    public String getHotSearchSource() { return normalizeHotSearchSource(myState.hotSearchSource); }
    public void setHotSearchSource(String source) { myState.hotSearchSource = normalizeHotSearchSource(source); }

    // --- Hot search timing ---
    public int getCarouselIntervalSeconds() { return myState.carouselIntervalSeconds; }
    public void setCarouselIntervalSeconds(int s) { myState.carouselIntervalSeconds = Math.max(3, Math.min(120, s)); }

    public int getRefreshIntervalMinutes() { return myState.refreshIntervalMinutes; }
    public void setRefreshIntervalMinutes(int m) { myState.refreshIntervalMinutes = Math.max(1, Math.min(120, m)); }

    // --- X trends region ---
    public String getXTrendsRegion() { return normalizeXRegion(myState.xTrendsRegion); }
    public void setXTrendsRegion(String region) { myState.xTrendsRegion = normalizeXRegion(region); }

    // --- Google Trends geo ---
    public String getGoogleTrendsGeo() { return normalizeGoogleGeo(myState.googleTrendsGeo); }
    public void setGoogleTrendsGeo(String geo) { myState.googleTrendsGeo = normalizeGoogleGeo(geo); }

    // --- Idle cultivation ---
    public int getCultivationRealmIndex() {
        int maximumRealmIndex = myState.cultivationAscended ? MAX_CULTIVATION_REALM_INDEX : 8;
        return clamp(myState.cultivationRealmIndex, 0, maximumRealmIndex);
    }
    public void setCultivationRealmIndex(int realmIndex) {
        int maximumRealmIndex = myState.cultivationAscended ? MAX_CULTIVATION_REALM_INDEX : 8;
        myState.cultivationRealmIndex = clamp(
                realmIndex,
                0,
                maximumRealmIndex
        );
    }

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

    public boolean isCultivationAscended() { return myState.cultivationAscended; }
    public void setCultivationAscended(boolean ascended) { myState.cultivationAscended = ascended; normalizeCultivationState(myState); }
    public int getAscensionRebirthCount() { return Math.max(0, myState.ascensionRebirthCount); }
    public void setAscensionRebirthCount(int rebirthCount) { myState.ascensionRebirthCount = Math.max(0, rebirthCount); }
    public long getAscensionMillis() { return Math.max(0L, myState.ascensionMillis); }
    public void setAscensionMillis(long millis) { myState.ascensionMillis = Math.max(0L, millis); }

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
        myState.pendingAlchemyPills = new HashMap<>();
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

    public Map<String, Integer> getPendingAlchemyPills() {
        normalizeCultivationState(myState);
        return Collections.unmodifiableMap(new HashMap<>(myState.pendingAlchemyPills));
    }

    public int getPendingAlchemyPillCount() {
        normalizeCultivationState(myState);
        int total = 0;
        for (Integer count : myState.pendingAlchemyPills.values()) {
            if (count != null && count > 0) {
                total += count;
            }
        }
        return total;
    }

    public void addPendingAlchemyPills(Map<String, Integer> pills) {
        if (pills == null || pills.isEmpty()) return;
        normalizeCultivationState(myState);
        for (Map.Entry<String, Integer> entry : pills.entrySet()) {
            if (entry.getKey() != null
                    && CULTIVATION_PILL_IDS.contains(entry.getKey())
                    && entry.getValue() != null
                    && entry.getValue() > 0) {
                myState.pendingAlchemyPills.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
    }

    public void clearPendingAlchemyPills() {
        myState.pendingAlchemyPills = new HashMap<>();
    }

    public List<String> getUnlockedSpellIds() {
        normalizeCultivationState(myState);
        return Collections.unmodifiableList(new ArrayList<>(myState.unlockedSpellIds));
    }

    public boolean isSpellUnlocked(String spellId) {
        normalizeCultivationState(myState);
        return spellId != null && myState.unlockedSpellIds.contains(spellId);
    }

    public boolean unlockSpell(String spellId) {
        normalizeCultivationState(myState);
        if (spellId == null || spellId.isEmpty() || myState.unlockedSpellIds.contains(spellId)) {
            return false;
        }
        myState.unlockedSpellIds.add(spellId);
        return true;
    }

    public List<String> getEquippedSpellIds() {
        normalizeCultivationState(myState);
        return Collections.unmodifiableList(new ArrayList<>(myState.equippedSpellIds));
    }

    public void setEquippedSpellIds(List<String> spellIds) {
        normalizeCultivationState(myState);
        List<String> nextSpellIds = new ArrayList<>();
        if (spellIds != null) {
            for (String spellId : spellIds) {
                if (spellId != null
                        && myState.unlockedSpellIds.contains(spellId)
                        && !nextSpellIds.contains(spellId)
                        && nextSpellIds.size() < 3) {
                    nextSpellIds.add(spellId);
                }
            }
        }
        myState.equippedSpellIds = nextSpellIds;
    }

    public List<String> getUnlockedArtifactIds() {
        normalizeCultivationState(myState);
        return Collections.unmodifiableList(new ArrayList<>(myState.unlockedArtifactIds));
    }

    public boolean isArtifactUnlocked(String artifactId) {
        normalizeCultivationState(myState);
        return artifactId != null && myState.unlockedArtifactIds.contains(artifactId);
    }

    public boolean unlockArtifact(String artifactId) {
        normalizeCultivationState(myState);
        if (artifactId == null || artifactId.isEmpty() || myState.unlockedArtifactIds.contains(artifactId)) {
            return false;
        }
        myState.unlockedArtifactIds.add(artifactId);
        if (myState.equippedArtifactIds.size() < 2) {
            myState.equippedArtifactIds.add(artifactId);
        }
        return true;
    }

    public List<String> getEquippedArtifactIds() {
        normalizeCultivationState(myState);
        return Collections.unmodifiableList(new ArrayList<>(myState.equippedArtifactIds));
    }

    public void setEquippedArtifactIds(List<String> artifactIds) {
        normalizeCultivationState(myState);
        List<String> nextArtifactIds = new ArrayList<>();
        if (artifactIds != null) {
            for (String artifactId : artifactIds) {
                if (artifactId != null
                        && myState.unlockedArtifactIds.contains(artifactId)
                        && !nextArtifactIds.contains(artifactId)
                        && nextArtifactIds.size() < 2) {
                    nextArtifactIds.add(artifactId);
                }
            }
        }
        myState.equippedArtifactIds = nextArtifactIds;
    }

    public List<String> getDefeatedCultivatorIds() {
        normalizeCultivationState(myState);
        return Collections.unmodifiableList(new ArrayList<>(myState.defeatedCultivatorIds));
    }

    public boolean isCultivatorDefeated(String cultivatorId) {
        normalizeCultivationState(myState);
        return cultivatorId != null && myState.defeatedCultivatorIds.contains(cultivatorId);
    }

    public boolean markCultivatorDefeated(String cultivatorId) {
        normalizeCultivationState(myState);
        if (cultivatorId == null || cultivatorId.isEmpty() || myState.defeatedCultivatorIds.contains(cultivatorId)) {
            return false;
        }
        myState.defeatedCultivatorIds.add(cultivatorId);
        return true;
    }

    public String getCultivationSectId() {
        normalizeCultivationState(myState);
        return myState.cultivationSectId != null ? myState.cultivationSectId : "";
    }

    public void setCultivationSectId(String sectId) {
        normalizeCultivationState(myState);
        myState.cultivationSectId = sectId != null ? sectId : "";
    }

    public long getCurrentSectPrestige() {
        return getSectPrestige(getCultivationSectId());
    }

    public long getSectPrestige(String sectId) {
        normalizeCultivationState(myState);
        return myState.sectPrestigeBySectId.getOrDefault(sectId, 0L);
    }

    public void addCurrentSectPrestige(long amount) {
        String sectId = getCultivationSectId();
        if (!sectId.isEmpty() && amount > 0L) {
            myState.sectPrestigeBySectId.merge(sectId, amount, Long::sum);
        }
    }

    public long getCurrentSectContribution() {
        return getSectContribution(getCultivationSectId());
    }

    public long getSectContribution(String sectId) {
        normalizeCultivationState(myState);
        return myState.sectContributionBySectId.getOrDefault(sectId, 0L);
    }

    public void addCurrentSectContribution(long amount) {
        String sectId = getCultivationSectId();
        if (!sectId.isEmpty() && amount > 0L) {
            myState.sectContributionBySectId.merge(sectId, amount, Long::sum);
        }
    }

    public boolean spendCurrentSectContribution(long amount) {
        if (amount < 0L) return false;
        String sectId = getCultivationSectId();
        long current = getSectContribution(sectId);
        if (sectId.isEmpty() || current < amount) {
            return false;
        }
        myState.sectContributionBySectId.put(sectId, current - amount);
        return true;
    }

    public void clearCurrentSectContribution() {
        String sectId = getCultivationSectId();
        if (!sectId.isEmpty()) {
            myState.sectContributionBySectId.put(sectId, 0L);
        }
    }

    public int getCurrentSectRankIndex() {
        return getSectRankIndex(getCultivationSectId());
    }

    public int getSectRankIndex(String sectId) {
        normalizeCultivationState(myState);
        return myState.sectRankBySectId.getOrDefault(sectId, 0);
    }

    public void setCurrentSectRankIndex(int rankIndex) {
        String sectId = getCultivationSectId();
        if (!sectId.isEmpty()) {
            myState.sectRankBySectId.put(sectId, clamp(rankIndex, 0, 4));
        }
    }

    public String getActiveSectTaskId() { return myState.activeSectTaskId != null ? myState.activeSectTaskId : ""; }
    public void setActiveSectTaskId(String taskId) { myState.activeSectTaskId = taskId != null ? taskId : ""; }
    public long getSectTaskStartMillis() { return Math.max(0L, myState.sectTaskStartMillis); }
    public void setSectTaskStartMillis(long millis) { myState.sectTaskStartMillis = Math.max(0L, millis); }
    public long getSectTaskEndMillis() { return Math.max(0L, myState.sectTaskEndMillis); }
    public void setSectTaskEndMillis(long millis) { myState.sectTaskEndMillis = Math.max(0L, millis); }
    public long getActiveSectTaskElapsedMillis() { return Math.max(0L, myState.activeSectTaskElapsedMillis); }
    public void setActiveSectTaskElapsedMillis(long millis) { myState.activeSectTaskElapsedMillis = Math.max(0L, millis); }

    public void clearSectTask() {
        myState.activeSectTaskId = "";
        myState.sectTaskStartMillis = 0L;
        myState.sectTaskEndMillis = 0L;
        myState.activeSectTaskElapsedMillis = 0L;
    }

    public boolean isSectTrialDefeated(String trialId) {
        normalizeCultivationState(myState);
        return trialId != null && myState.defeatedSectTrialIds.contains(trialId);
    }

    public boolean markSectTrialDefeated(String trialId) {
        normalizeCultivationState(myState);
        if (trialId == null || trialId.isEmpty() || myState.defeatedSectTrialIds.contains(trialId)) {
            return false;
        }
        myState.defeatedSectTrialIds.add(trialId);
        return true;
    }

    public boolean isSectInheritanceLearned(String inheritanceId) {
        normalizeCultivationState(myState);
        return inheritanceId != null && myState.learnedSectInheritanceIds.contains(inheritanceId);
    }

    public boolean markSectInheritanceLearned(String inheritanceId) {
        normalizeCultivationState(myState);
        if (inheritanceId == null || inheritanceId.isEmpty() || myState.learnedSectInheritanceIds.contains(inheritanceId)) {
            return false;
        }
        myState.learnedSectInheritanceIds.add(inheritanceId);
        return true;
    }

    public boolean isSectGraduated(String sectId) {
        normalizeCultivationState(myState);
        return sectId != null && myState.graduatedSectIds.contains(sectId);
    }

    public boolean markSectGraduated(String sectId) {
        normalizeCultivationState(myState);
        if (sectId == null || sectId.isEmpty() || myState.graduatedSectIds.contains(sectId)) {
            return false;
        }
        myState.graduatedSectIds.add(sectId);
        return true;
    }

    public List<SectPendingEventState> getPendingSectEvents() {
        normalizeCultivationState(myState);
        List<SectPendingEventState> snapshot = new ArrayList<>();
        for (SectPendingEventState event : myState.pendingSectEvents) {
            snapshot.add(new SectPendingEventState(event.instanceId, event.eventId, event.sectId, event.createdMillis));
        }
        return Collections.unmodifiableList(snapshot);
    }

    public boolean addPendingSectEvent(String instanceId, String eventId, String sectId, long createdMillis) {
        normalizeCultivationState(myState);
        if (myState.pendingSectEvents.size() >= MAX_PENDING_SECT_EVENTS) {
            return false;
        }
        SectPendingEventState eventState = new SectPendingEventState(instanceId, eventId, sectId, createdMillis);
        if (eventState.instanceId.isEmpty() || eventState.eventId.isEmpty() || eventState.sectId.isEmpty()) {
            return false;
        }
        for (SectPendingEventState existing : myState.pendingSectEvents) {
            if (eventState.instanceId.equals(existing.instanceId)) {
                return false;
            }
        }
        myState.pendingSectEvents.add(eventState);
        return true;
    }

    public boolean removePendingSectEvent(String instanceId) {
        normalizeCultivationState(myState);
        if (instanceId == null || instanceId.isEmpty()) {
            return false;
        }
        return myState.pendingSectEvents.removeIf(event -> instanceId.equals(event.instanceId));
    }

    public String getActiveSectSecretRealmId() {
        normalizeCultivationState(myState);
        return myState.activeSectSecretRealmId != null ? myState.activeSectSecretRealmId : "";
    }

    public void setActiveSectSecretRealmId(String secretRealmId) {
        normalizeCultivationState(myState);
        myState.activeSectSecretRealmId = secretRealmId != null ? secretRealmId : "";
    }

    public int getSectSecretRealmNodeIndex() {
        normalizeCultivationState(myState);
        return Math.max(0, myState.sectSecretRealmNodeIndex);
    }

    public void setSectSecretRealmNodeIndex(int nodeIndex) {
        normalizeCultivationState(myState);
        myState.sectSecretRealmNodeIndex = Math.max(0, nodeIndex);
    }

    public long getSectSecretRealmStartedMillis() {
        normalizeCultivationState(myState);
        return Math.max(0L, myState.sectSecretRealmStartedMillis);
    }

    public void setSectSecretRealmStartedMillis(long millis) {
        normalizeCultivationState(myState);
        myState.sectSecretRealmStartedMillis = Math.max(0L, millis);
    }

    public long getSectSecretRealmCooldownUntilMillis() {
        normalizeCultivationState(myState);
        return Math.max(0L, myState.sectSecretRealmCooldownUntilMillis);
    }

    public void setSectSecretRealmCooldownUntilMillis(long millis) {
        normalizeCultivationState(myState);
        myState.sectSecretRealmCooldownUntilMillis = Math.max(0L, millis);
    }

    public List<String> getSectSecretRealmResolvedNodeIds() {
        normalizeCultivationState(myState);
        return Collections.unmodifiableList(new ArrayList<>(myState.sectSecretRealmResolvedNodeIds));
    }

    public void markSectSecretRealmNodeResolved(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) return;
        normalizeCultivationState(myState);
        if (!myState.sectSecretRealmResolvedNodeIds.contains(nodeId)) {
            myState.sectSecretRealmResolvedNodeIds.add(nodeId);
        }
    }

    public void clearSectSecretRealmProgress() {
        myState.activeSectSecretRealmId = "";
        myState.sectSecretRealmNodeIndex = 0;
        myState.sectSecretRealmStartedMillis = 0L;
        myState.sectSecretRealmResolvedNodeIds = new ArrayList<>();
    }

    public boolean isOwnSectCreated() {
        normalizeCultivationState(myState);
        return myState.ownSectCreated;
    }

    public void createOwnSect(String name) {
        normalizeCultivationState(myState);
        myState.ownSectCreated = true;
        myState.ownSectName = normalizeOwnSectName(name);
        if (myState.ownSectName.isEmpty()) {
            myState.ownSectName = "太虚宗";
        }
        if (myState.ownSectBuildingLevels.isEmpty()) {
            myState.ownSectBuildingLevels = new HashMap<>();
        }
    }

    public String getOwnSectName() {
        normalizeCultivationState(myState);
        return myState.ownSectName;
    }

    public int getOwnSectTierIndex() {
        normalizeCultivationState(myState);
        return myState.ownSectTierIndex;
    }

    public void setOwnSectTierIndex(int tierIndex) {
        myState.ownSectTierIndex = clamp(tierIndex, 0, 5);
    }

    public long getOwnSectMaterials() {
        normalizeCultivationState(myState);
        return Math.max(0L, myState.ownSectMaterials);
    }

    public void addOwnSectMaterials(long amount) {
        if (amount > 0L) {
            myState.ownSectMaterials = Math.max(0L, myState.ownSectMaterials) + amount;
        }
    }

    public boolean spendOwnSectMaterials(long amount) {
        if (amount < 0L || getOwnSectMaterials() < amount) {
            return false;
        }
        myState.ownSectMaterials -= amount;
        return true;
    }

    public Map<String, Integer> getOwnSectBuildingLevels() {
        normalizeCultivationState(myState);
        return Collections.unmodifiableMap(new HashMap<>(myState.ownSectBuildingLevels));
    }

    public int getOwnSectBuildingLevel(String buildingId) {
        normalizeCultivationState(myState);
        return myState.ownSectBuildingLevels.getOrDefault(buildingId, 0);
    }

    public void setOwnSectBuildingLevel(String buildingId, int level) {
        if (buildingId == null || !OWN_SECT_BUILDING_IDS.contains(buildingId)) return;
        normalizeCultivationState(myState);
        if (level <= 0) {
            myState.ownSectBuildingLevels.remove(buildingId);
        } else {
            myState.ownSectBuildingLevels.put(buildingId, clamp(level, 0, MAX_OWN_SECT_BUILDING_LEVEL));
        }
    }

    public List<OwnSectDiscipleState> getOwnSectDisciples() {
        normalizeCultivationState(myState);
        List<OwnSectDiscipleState> snapshot = new ArrayList<>();
        for (OwnSectDiscipleState disciple : myState.ownSectDisciples) {
            snapshot.add(copyOwnSectDisciple(disciple));
        }
        return Collections.unmodifiableList(snapshot);
    }

    public List<OwnSectDiscipleState> getOwnSectRecruitmentCandidates() {
        normalizeCultivationState(myState);
        List<OwnSectDiscipleState> snapshot = new ArrayList<>();
        for (OwnSectDiscipleState disciple : myState.ownSectRecruitmentCandidates) {
            snapshot.add(copyOwnSectDisciple(disciple));
        }
        return Collections.unmodifiableList(snapshot);
    }

    public void setOwnSectRecruitmentCandidates(List<OwnSectDiscipleState> candidates) {
        myState.ownSectRecruitmentCandidates = normalizeOwnSectDisciples(candidates, MAX_OWN_SECT_CANDIDATES);
    }

    public String getOwnSectRecruitmentSpecialty() {
        normalizeCultivationState(myState);
        return myState.ownSectRecruitmentSpecialty;
    }

    public long getOwnSectRecruitmentStartMillis() {
        normalizeCultivationState(myState);
        return Math.max(0L, myState.ownSectRecruitmentStartMillis);
    }

    public long getOwnSectRecruitmentElapsedMillis() {
        normalizeCultivationState(myState);
        return Math.max(0L, myState.ownSectRecruitmentElapsedMillis);
    }

    public void startOwnSectRecruitment(String specialty, long now) {
        normalizeCultivationState(myState);
        myState.ownSectRecruitmentSpecialty = normalizeOwnSectSpecialty(specialty);
        myState.ownSectRecruitmentStartMillis = myState.ownSectRecruitmentSpecialty.isEmpty() ? 0L : Math.max(0L, now);
        myState.ownSectRecruitmentElapsedMillis = 0L;
    }

    public void setOwnSectRecruitmentElapsedMillis(long elapsedMillis) {
        normalizeCultivationState(myState);
        myState.ownSectRecruitmentElapsedMillis = Math.max(0L, elapsedMillis);
    }

    public void clearOwnSectRecruitment() {
        myState.ownSectRecruitmentSpecialty = "";
        myState.ownSectRecruitmentStartMillis = 0L;
        myState.ownSectRecruitmentElapsedMillis = 0L;
        myState.ownSectRecruitmentCandidates = new ArrayList<>();
    }

    public boolean addOwnSectDisciple(OwnSectDiscipleState disciple) {
        normalizeCultivationState(myState);
        if (disciple == null || myState.ownSectDisciples.size() >= MAX_OWN_SECT_DISCIPLES) {
            return false;
        }
        OwnSectDiscipleState normalized = normalizeOwnSectDisciples(List.of(disciple), 1).stream().findFirst().orElse(null);
        if (normalized == null) {
            return false;
        }
        for (OwnSectDiscipleState existing : myState.ownSectDisciples) {
            if (existing.id.equals(normalized.id)) {
                return false;
            }
        }
        myState.ownSectDisciples.add(normalized);
        return true;
    }

    public boolean removeOwnSectRecruitmentCandidate(String candidateId) {
        normalizeCultivationState(myState);
        return candidateId != null && myState.ownSectRecruitmentCandidates.removeIf(candidate -> candidateId.equals(candidate.id));
    }

    public boolean assignOwnSectDisciple(String discipleId, String buildingId) {
        if (discipleId == null || !OWN_SECT_BUILDING_IDS.contains(buildingId)) {
            return false;
        }
        normalizeCultivationState(myState);
        for (OwnSectDiscipleState disciple : myState.ownSectDisciples) {
            if (discipleId.equals(disciple.id)) {
                disciple.assignedBuildingId = buildingId;
                return true;
            }
        }
        return false;
    }

    public boolean unassignOwnSectDisciple(String discipleId) {
        if (discipleId == null) {
            return false;
        }
        normalizeCultivationState(myState);
        for (OwnSectDiscipleState disciple : myState.ownSectDisciples) {
            if (discipleId.equals(disciple.id)) {
                disciple.assignedBuildingId = "";
                return true;
            }
        }
        return false;
    }

    public boolean dismissOwnSectDisciple(String discipleId) {
        normalizeCultivationState(myState);
        return discipleId != null && myState.ownSectDisciples.removeIf(disciple -> discipleId.equals(disciple.id));
    }

    private static OwnSectDiscipleState copyOwnSectDisciple(OwnSectDiscipleState disciple) {
        return new OwnSectDiscipleState(
                disciple.id,
                disciple.name,
                disciple.specialty,
                disciple.aptitude,
                disciple.assignedBuildingId
        );
    }
}
