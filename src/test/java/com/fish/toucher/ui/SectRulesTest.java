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

        settings.addCurrentSectPrestige(400L);
        assertFalse(SectRules.canPromote(settings));

        settings.setCultivationRealmIndex(2);
        assertTrue(SectRules.canPromote(settings));
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
}
