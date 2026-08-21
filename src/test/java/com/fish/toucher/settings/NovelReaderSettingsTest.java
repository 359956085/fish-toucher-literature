package com.fish.toucher.settings;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
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
        state.pendingAlchemyPills = new HashMap<>();
        state.pendingAlchemyPills.put("qi_pill", 2);
        state.pendingAlchemyPills.put("unknown_pill", 3);
        state.pendingAlchemyPills.put("", 4);
        state.pendingAlchemyPills.put("spirit_pill", -1);
        state.activeSectTaskId = "";
        state.activeSectTaskElapsedMillis = -1L;
        state.pendingSectEvents = new ArrayList<>(List.of(
                new NovelReaderSettings.SectPendingEventState("a", "sparring", "qingyun_sword", -1L),
                new NovelReaderSettings.SectPendingEventState("a", "elder_lecture", "qingyun_sword", 1L),
                new NovelReaderSettings.SectPendingEventState("", "back_mountain", "qingyun_sword", 1L),
                new NovelReaderSettings.SectPendingEventState("b", "", "qingyun_sword", 1L),
                new NovelReaderSettings.SectPendingEventState("c", "back_mountain", "", 1L),
                new NovelReaderSettings.SectPendingEventState("d", "back_mountain", "qingyun_sword", 1L),
                new NovelReaderSettings.SectPendingEventState("e", "junior_help", "qingyun_sword", 1L),
                new NovelReaderSettings.SectPendingEventState("f", "inheritance_fragment", "qingyun_sword", 1L)
        ));

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
        assertEquals(2, settings.getPendingAlchemyPillCount());
        assertEquals(2, settings.getPendingAlchemyPills().get("qi_pill"));
        assertFalse(settings.getPendingAlchemyPills().containsKey("unknown_pill"));
        assertEquals(0L, settings.getActiveSectTaskElapsedMillis());
        assertEquals(3, settings.getPendingSectEvents().size());
        assertEquals(0L, settings.getPendingSectEvents().get(0).createdMillis);
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
    void 修仙下拉框选择默认空且应清洗空值() {
        NovelReaderSettings settings = new NovelReaderSettings();

        assertEquals("", settings.getSelectedCultivationPillId());
        assertEquals("", settings.getSelectedTravelLocationId());
        assertEquals("", settings.getSelectedCultivatorId());
        assertEquals("", settings.getSelectedSectPreviewId());

        settings.setSelectedCultivationPillId(" qi_pill ");
        settings.setSelectedTravelLocationId(" forest_edge ");
        assertEquals("qi_pill", settings.getSelectedCultivationPillId());
        assertEquals("forest_edge", settings.getSelectedTravelLocationId());

        NovelReaderSettings.State state = new NovelReaderSettings.State();
        state.selectedCultivationPillId = null;
        state.selectedTravelLocationId = null;
        state.selectedCultivatorId = null;
        state.selectedSectPreviewId = null;
        state.selectedSectTaskId = null;
        state.selectedSectSecretRealmId = null;
        state.selectedSectInheritanceId = null;
        state.selectedSectTrialId = null;

        settings.loadState(state);

        assertEquals("", settings.getSelectedCultivationPillId());
        assertEquals("", settings.getSelectedTravelLocationId());
        assertEquals("", settings.getSelectedCultivatorId());
        assertEquals("", settings.getSelectedSectPreviewId());
        assertEquals("", settings.getSelectedSectTaskId());
        assertEquals("", settings.getSelectedSectSecretRealmId());
        assertEquals("", settings.getSelectedSectInheritanceId());
        assertEquals("", settings.getSelectedSectTrialId());
    }

    @Test
    void 宗门事件队列限制最多三条且不允许重复实例() {
        NovelReaderSettings settings = new NovelReaderSettings();

        assertTrue(settings.addPendingSectEvent("a", "sparring", "qingyun_sword", 1L));
        assertFalse(settings.addPendingSectEvent("a", "elder_lecture", "qingyun_sword", 2L));
        assertTrue(settings.addPendingSectEvent("b", "elder_lecture", "qingyun_sword", 2L));
        assertTrue(settings.addPendingSectEvent("c", "back_mountain", "qingyun_sword", 3L));
        assertFalse(settings.addPendingSectEvent("d", "junior_help", "qingyun_sword", 4L));

        assertEquals(3, settings.getPendingSectEvents().size());
        assertTrue(settings.removePendingSectEvent("b"));
        assertEquals(2, settings.getPendingSectEvents().size());
    }

    @Test
    void 丹房待领取丹药应累加并随洞府状态清空() {
        NovelReaderSettings settings = new NovelReaderSettings();

        settings.addPendingAlchemyPills(new HashMap<>(Map.of("qi_pill", 1)));
        settings.addPendingAlchemyPills(new HashMap<>(Map.of("qi_pill", 2, "", 3, "spirit_pill", -1)));

        assertEquals(3, settings.getPendingAlchemyPillCount());
        assertEquals(3, settings.getPendingAlchemyPills().get("qi_pill"));

        settings.clearAbodeState();

        assertEquals(0, settings.getPendingAlchemyPillCount());
        assertTrue(settings.getPendingAlchemyPills().isEmpty());
    }

    @Test
    void 自建宗门建筑等级应限制到五级() {
        NovelReaderSettings settings = new NovelReaderSettings();
        settings.createOwnSect("太虚宗");
        settings.setOwnSectBuildingLevel("gathering_array", 10);

        assertEquals(5, settings.getOwnSectBuildingLevel("gathering_array"));

        NovelReaderSettings.State state = new NovelReaderSettings.State();
        state.cultivationAscended = true;
        state.ownSectCreated = true;
        state.ownSectName = "太虚宗";
        state.ownSectBuildingLevels = new HashMap<>(Map.of("gathering_array", 9, "unknown", 3));

        settings.loadState(state);

        assertEquals(5, settings.getOwnSectBuildingLevel("gathering_array"));
        assertFalse(settings.getOwnSectBuildingLevels().containsKey("unknown"));
    }
}
