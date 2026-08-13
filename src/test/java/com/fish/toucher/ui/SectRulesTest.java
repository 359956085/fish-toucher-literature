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
}
