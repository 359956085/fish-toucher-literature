package com.fish.toucher.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CultivationRulesTest {

    @Test
    void 应限制境界并返回突破需求() {
        assertEquals(9, CultivationRules.realmCount());
        assertEquals(0, CultivationRules.clampRealm(-1));
        assertEquals(8, CultivationRules.clampRealm(99));
        assertEquals(8_000L, CultivationRules.requiredQi(0));
        assertEquals(0L, CultivationRules.requiredQi(8));
        assertTrue(CultivationRules.isMaxRealm(8));
    }

    @Test
    void 突破概率应保持上限和转生加成() {
        assertEquals(72, CultivationRules.baseBreakthroughChance(0));
        assertEquals(42, CultivationRules.baseBreakthroughChance(20));
        assertEquals(96, CultivationRules.finalBreakthroughChance(90, 1, 15));
        assertEquals(50, CultivationRules.finalBreakthroughChance(50, 0, 15));
    }
}
