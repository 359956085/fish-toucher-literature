package com.fish.toucher.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CultivationRulesTest {

    @Test
    void 应限制境界并返回突破需求() {
        assertEquals(15, CultivationRules.realmCount());
        assertEquals(0, CultivationRules.clampRealm(-1));
        assertEquals(14, CultivationRules.clampRealm(99));
        assertEquals(8_000L, CultivationRules.requiredQi(0));
        assertEquals(380_000L, CultivationRules.requiredQi(8));
        assertEquals(0L, CultivationRules.requiredQi(14));
        assertTrue(CultivationRules.isHumanMaxRealm(8));
        assertFalse(CultivationRules.isCurrentPhaseMaxRealm(9, true));
        assertTrue(CultivationRules.isMaxRealm(14));
    }

    @Test
    void 突破概率应保持上限和转生加成() {
        assertEquals(72, CultivationRules.baseBreakthroughChance(0));
        assertEquals(42, CultivationRules.baseBreakthroughChance(20));
        assertEquals(96, CultivationRules.finalBreakthroughChance(90, 1, 15));
        assertEquals(50, CultivationRules.finalBreakthroughChance(50, 0, 15));
    }
}
