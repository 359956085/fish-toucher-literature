package com.fish.toucher.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CultivationBattleEngineTest {

    @Test
    void damageNeverDropsBelowOne() {
        assertEquals(40L, CultivationBattleEngine.damage(100L, 100L, 65, 25));
        assertEquals(1L, CultivationBattleEngine.damage(1L, 10_000L, 65, 25));
    }

    @Test
    void recoveryUsesExistingDivisorsAndMinimum() {
        assertEquals(4L, CultivationBattleEngine.recovery(1_000L, 250));
        assertEquals(1L, CultivationBattleEngine.recovery(0L, 250));
    }
}
