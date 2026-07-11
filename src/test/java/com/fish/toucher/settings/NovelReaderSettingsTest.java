package com.fish.toucher.settings;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NovelReaderSettingsTest {

    @Test
    void 加载状态时应修复非法边界值() {
        NovelReaderSettings.State state = new NovelReaderSettings.State();
        state.fontSize = -1;
        state.carouselIntervalSeconds = 0;
        state.refreshIntervalMinutes = 0;
        state.hotSearchSource = "unknown";
        state.xTrendsRegion = "../escape";
        state.googleTrendsGeo = "BAD";
        state.cultivationRealmIndex = 999;
        state.readingProgress = new HashMap<>(Map.of("book.txt", -10));

        NovelReaderSettings settings = new NovelReaderSettings();
        settings.loadState(state);

        assertEquals(8, settings.getFontSize());
        assertEquals(3, settings.getCarouselIntervalSeconds());
        assertEquals(1, settings.getRefreshIntervalMinutes());
        assertEquals("baidu", settings.getHotSearchSource());
        assertEquals("", settings.getXTrendsRegion());
        assertEquals("US", settings.getGoogleTrendsGeo());
        assertEquals(8, settings.getCultivationRealmIndex());
        assertEquals(0, settings.getReadingProgress("book.txt"));
    }

    @Test
    void Setter应执行与加载相同的校验() {
        NovelReaderSettings settings = new NovelReaderSettings();
        settings.setFontSize(100);
        settings.setHotSearchSource("invalid");
        settings.setXTrendsRegion("a/b");
        settings.setGoogleTrendsGeo("xx");
        settings.setReadingProgress("book.txt", -2);

        assertEquals(30, settings.getFontSize());
        assertEquals("baidu", settings.getHotSearchSource());
        assertEquals("", settings.getXTrendsRegion());
        assertEquals("US", settings.getGoogleTrendsGeo());
        assertEquals(0, settings.getReadingProgress("book.txt"));
    }

    @Test
    void persistentStateIsDeepCopy() {
        NovelReaderSettings settings = new NovelReaderSettings();
        settings.unlockTechnique("evergreen_method");
        settings.addPill("qi_pill", 2);

        NovelReaderSettings.State snapshot = settings.getState();
        assertNotNull(snapshot);
        snapshot.unlockedTechniqueIds.clear();
        snapshot.pillInventory.clear();

        assertTrue(settings.isTechniqueUnlocked("evergreen_method"));
        assertEquals(2, settings.getPillCount("qi_pill"));
    }
}
