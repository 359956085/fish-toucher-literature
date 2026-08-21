package com.fish.toucher.ui;

import com.fish.toucher.settings.NovelReaderSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SectRulesTest {

    @Test
    void 筑基后开放宗门并限制职位范围() {
        assertFalse(SectRules.isSectUnlocked(0));
        assertTrue(SectRules.isSectUnlocked(1));
        assertEquals(0, SectRules.clampRank(-1));
        assertEquals(4, SectRules.clampRank(99));
    }

    @Test
    void 晋升需要威望和境界双门槛() {
        NovelReaderSettings settings = new NovelReaderSettings();
        settings.setCultivationRealmIndex(1);
        settings.setCultivationSectId("qingyun_sword");
        settings.setCurrentSectRankIndex(0);

        assertFalse(SectRules.canPromote(settings));

        settings.addCurrentSectPrestige(100L);
        assertFalse(SectRules.canPromote(settings));

        settings.setCultivationRealmIndex(2);
        assertTrue(SectRules.canPromote(settings));
    }

    @Test
    void 宗门职位晋升威望门槛应为旧值四分之一() {
        assertEquals(100L, SectCatalog.rank(1).prestigeRequired());
        assertEquals(375L, SectCatalog.rank(2).prestigeRequired());
        assertEquals(1_000L, SectCatalog.rank(3).prestigeRequired());
        assertEquals(2_500L, SectCatalog.rank(4).prestigeRequired());
    }

    @Test
    void 贡献消费不影响威望() {
        NovelReaderSettings settings = new NovelReaderSettings();
        settings.setCultivationSectId("qingyun_sword");
        settings.addCurrentSectPrestige(500L);
        settings.addCurrentSectContribution(300L);

        assertTrue(settings.spendCurrentSectContribution(200L));
        assertEquals(100L, settings.getCurrentSectContribution());
        assertEquals(500L, settings.getCurrentSectPrestige());
    }

    @Test
    void 传承需要职位贡献和试炼条件() {
        NovelReaderSettings settings = new NovelReaderSettings();
        settings.setCultivationRealmIndex(8);
        settings.setCultivationSectId("qingyun_sword");
        settings.setCurrentSectRankIndex(4);
        settings.addCurrentSectContribution(6_000L);
        SectCatalog.SectInheritanceDefinition core = SectCatalog.inheritance("qingyun_core");

        assertFalse(SectRules.canPurchaseInheritance(settings, core));

        settings.markSectTrialDefeated("qingyun_sword:5");
        assertTrue(SectRules.canPurchaseInheritance(settings, core));
    }

    @Test
    void 宗门随机事件定义应具备可处理选项() {
        assertEquals(5, SectCatalog.events().size());
        for (SectCatalog.SectEventDefinition event : SectCatalog.events()) {
            assertNotNull(SectCatalog.event(event.id()));
            assertFalse(event.options().isEmpty());
            for (SectCatalog.SectEventOptionDefinition option : event.options()) {
                assertFalse(option.id().isEmpty());
                assertFalse(option.label().isEmpty());
            }
        }
    }

    @Test
    void 宗门基础功法应使用独立功法避免弱于游历功法() {
        assertEquals("qingyun_sword_method", SectCatalog.inheritance("qingyun_basic").rewardId());
        assertEquals("danxia_herb_method", SectCatalog.inheritance("danxia_basic").rewardId());
        assertEquals("xuanwu_guard_method", SectCatalog.inheritance("xuanwu_basic").rewardId());
        assertEquals("tianji_star_method", SectCatalog.inheritance("tianji_basic").rewardId());
        assertEquals("taiqing_clear_method", SectCatalog.inheritance("taiqing_basic").rewardId());
    }

    @Test
    void 宗门秘境需要当前宗门执事职位() {
        NovelReaderSettings settings = new NovelReaderSettings();
        SectCatalog.SectSecretRealmDefinition qingyun = SectCatalog.secretRealm("qingyun_secret_realm");
        SectCatalog.SectSecretRealmDefinition danxia = SectCatalog.secretRealm("danxia_secret_realm");

        assertFalse(SectRules.isSecretRealmUnlocked(settings, qingyun));

        settings.setCultivationSectId("qingyun_sword");
        settings.setCurrentSectRankIndex(2);
        assertFalse(SectRules.isSecretRealmUnlocked(settings, qingyun));

        settings.setCurrentSectRankIndex(3);
        assertTrue(SectRules.isSecretRealmUnlocked(settings, qingyun));
        assertFalse(SectRules.isSecretRealmUnlocked(settings, danxia));
    }

    @Test
    void 每个宗门应配置一个文字节点秘境() {
        assertEquals(SectCatalog.sects().size(), SectCatalog.secretRealms().size());
        for (SectCatalog.SectSecretRealmDefinition secretRealm : SectCatalog.secretRealms()) {
            assertNotNull(SectCatalog.sect(secretRealm.sectId()));
            assertEquals(3, secretRealm.minRankIndex());
            assertTrue(secretRealm.nodes().size() >= 3);
            assertTrue(secretRealm.nodes().size() <= 5);
            assertEquals(SectCatalog.SecretRealmNodeType.ENTRY, secretRealm.nodes().get(0).type());
            assertEquals(SectCatalog.SecretRealmNodeType.BOSS, secretRealm.nodes().get(secretRealm.nodes().size() - 1).type());
        }
    }

    @Test
    void 宗门任务耗时应复用境界和天机阁减免() {
        IdleCultivationManager manager = new IdleCultivationManager();
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        SectCatalog.SectTaskDefinition task = SectCatalog.task("sort_library");

        settings.setCultivationRealmIndex(0);
        settings.setCultivationSectId("qingyun_sword");
        assertEquals(30L, manager.getSectTaskDurationMinutes(task, settings));

        settings.setCultivationRealmIndex(8);
        assertEquals(15L, manager.getSectTaskDurationMinutes(task, settings));

        settings.setCultivationRealmIndex(4);
        settings.setCultivationSectId("qingyun_sword");
        assertEquals(23L, manager.getSectTaskDurationMinutes(task, settings));

        settings.setCultivationSectId("tianji_pavilion");
        assertEquals(21L, manager.getSectTaskDurationMinutes(task, settings));

        settings.setCultivationRealmIndex(8);
        assertEquals(15L, manager.getSectTaskDurationMinutes(task, settings));
    }

    @Test
    void 飞升宗门建筑应按弟子资质统一放大() {
        NovelReaderSettings settings = new NovelReaderSettings();
        settings.setCultivationAscended(true);
        settings.createOwnSect("太虚宗");
        settings.setOwnSectBuildingLevel(AscendedSectCatalog.GATHERING_ARRAY_ID, 3);
        settings.addOwnSectDisciple(new NovelReaderSettings.OwnSectDiscipleState(
                "a",
                "陆离",
                AscendedSectCatalog.Specialty.GATHERING.name(),
                80,
                AscendedSectCatalog.GATHERING_ARRAY_ID
        ));
        settings.addOwnSectDisciple(new NovelReaderSettings.OwnSectDiscipleState(
                "b",
                "苏玄",
                AscendedSectCatalog.Specialty.GATHERING.name(),
                70,
                AscendedSectCatalog.GATHERING_ARRAY_ID
        ));

        assertEquals(21, AscendedSectRules.gatheringQiBonusPercent(settings));
        assertEquals(2, AscendedSectRules.assignedDiscipleCount(settings, AscendedSectCatalog.GATHERING_ARRAY_ID));
    }

    @Test
    void 飞升宗门建筑最高等级应收敛到五级() {
        assertEquals(2, AscendedSectCatalog.tier(0).buildingLevelLimit());
        assertEquals(3, AscendedSectCatalog.tier(1).buildingLevelLimit());
        assertEquals(4, AscendedSectCatalog.tier(2).buildingLevelLimit());
        assertEquals(5, AscendedSectCatalog.tier(3).buildingLevelLimit());
        assertEquals(5, AscendedSectCatalog.tier(4).buildingLevelLimit());
        assertEquals(5, AscendedSectCatalog.tier(5).buildingLevelLimit());

        assertEquals(0, AscendedSectRules.buildingSlotCount(0));
        assertEquals(1, AscendedSectRules.buildingSlotCount(2));
        assertEquals(2, AscendedSectRules.buildingSlotCount(4));
        assertEquals(3, AscendedSectRules.buildingSlotCount(5));
    }
}
