package com.fish.toucher.ui;

/**
 * 修仙模式纯数值规则。无 IDE、线程或持久化依赖，便于独立回归测试。
 *
 * @author fengshi
 */
final class CultivationRules {

    static final int HUMAN_MAX_REALM_INDEX = 8;
    static final int SPIRIT_START_REALM_INDEX = 9;

    private static final long[] REQUIRED_QI = {
            8_000L, 18_000L, 34_000L, 58_000L,
            90_000L, 130_000L, 180_000L, 245_000L,
            380_000L, 9_000_000L, 14_000_000L, 19_000_000L,
            24_000_000L, 30_000_000L
    };

    private CultivationRules() {}

    static int realmCount() {
        return REQUIRED_QI.length + 1;
    }

    static int clampRealm(int realmIndex) {
        return Math.max(0, Math.min(realmCount() - 1, realmIndex));
    }

    static boolean isMaxRealm(int realmIndex) {
        return realmIndex >= realmCount() - 1;
    }

    static boolean isHumanMaxRealm(int realmIndex) {
        return realmIndex >= HUMAN_MAX_REALM_INDEX;
    }

    static boolean isCurrentPhaseMaxRealm(int realmIndex, boolean ascended) {
        return ascended ? isMaxRealm(realmIndex) : isHumanMaxRealm(realmIndex);
    }

    static long requiredQi(int realmIndex) {
        if (isMaxRealm(realmIndex)) {
            return 0L;
        }
        return REQUIRED_QI[Math.min(REQUIRED_QI.length - 1, Math.max(0, realmIndex))];
    }

    static int baseBreakthroughChance(int realmIndex) {
        return Math.max(42, 72 - Math.max(0, realmIndex) * 4);
    }

    static int finalBreakthroughChance(
            int additiveChance,
            int rebirthCount,
            int rebirthBonusPercent
    ) {
        long chance = additiveChance
                * (100L + Math.max(0, rebirthCount) * rebirthBonusPercent)
                / 100L;
        return (int) Math.min(96L, Math.max(0L, chance));
    }
}
