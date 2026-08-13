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
        state.sectPrestigeBySectId = new HashMap<>(Map.of("qingyun_sword", -1L, "taiqing_dao", 20L));
        state.sectContributionBySectId = new HashMap<>(Map.of("qingyun_sword", -2L, "taiqing_dao", 30L));
        state.sectRankBySectId = new HashMap<>(Map.of("taiqing_dao", 99));
        state.activeSectTaskId = "";
        state.activeSectTaskElapsedMillis = -1L;

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
        assertEquals(20L, settings.getSectPrestige("taiqing_dao"));
        assertEquals(30L, settings.getSectContribution("taiqing_dao"));
        assertEquals(4, settings.getSectRankIndex("taiqing_dao"));
        assertEquals(0L, settings.getActiveSectTaskElapsedMillis());
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
}
