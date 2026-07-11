package com.fish.toucher.ui;

/** Pure battle calculations shared by runtime code and deterministic tests. */
final class CultivationBattleEngine {

    private CultivationBattleEngine() {}

    static long damage(long attack, long defense, int attackPercent, int defensePercent) {
        long rawDamage = Math.max(0L, attack) * attackPercent / 100L
                - Math.max(0L, defense) * defensePercent / 100L;
        return Math.max(1L, rawDamage);
    }

    static long recovery(long maximum, int divisor) {
        return Math.max(1L, Math.max(0L, maximum) / Math.max(1, divisor));
    }
}
