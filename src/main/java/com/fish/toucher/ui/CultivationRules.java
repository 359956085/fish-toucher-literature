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
    private static final int PERCENT_BASIS_POINTS = 100;
    private static final int[] SPIRIT_BASE_BREAKTHROUGH_CHANCE_BASIS_POINTS = {
            3_150, 2_900, 2_650, 2_400, 2_150
    };
    private static final int HUMAN_BREAKTHROUGH_FAILURE_BONUS_BASIS_POINTS = 1_600;
    private static final int SPIRIT_BREAKTHROUGH_FAILURE_BONUS_BASIS_POINTS = 500;

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
        if (isSpiritBreakthroughRealm(realmIndex)) {
            return (baseBreakthroughChanceBasisPoints(realmIndex) + PERCENT_BASIS_POINTS / 2) / PERCENT_BASIS_POINTS;
        }
        return Math.max(42, 72 - Math.max(0, realmIndex) * 4);
    }

    static int baseBreakthroughChanceBasisPoints(int realmIndex) {
        if (isSpiritBreakthroughRealm(realmIndex)) {
            return SPIRIT_BASE_BREAKTHROUGH_CHANCE_BASIS_POINTS[realmIndex - SPIRIT_START_REALM_INDEX];
        }
        return baseBreakthroughChance(realmIndex) * PERCENT_BASIS_POINTS;
    }

    static int breakthroughFailureBonusBasisPoints(int realmIndex) {
        return isSpiritBreakthroughRealm(realmIndex)
                ? SPIRIT_BREAKTHROUGH_FAILURE_BONUS_BASIS_POINTS
                : HUMAN_BREAKTHROUGH_FAILURE_BONUS_BASIS_POINTS;
    }

    static int finalBreakthroughChance(
            int additiveChance,
            int rebirthCount,
            int rebirthBonusPercent
    ) {
        return finalBreakthroughChanceBasisPoints(
                additiveChance * PERCENT_BASIS_POINTS,
                rebirthCount,
                rebirthBonusPercent
        );
    }

    static int finalBreakthroughChanceBasisPoints(
            int additiveChanceBasisPoints,
            int rebirthCount,
            int rebirthBonusPercent
    ) {
        long chanceBasisPoints = additiveChanceBasisPoints
                * (100L + Math.max(0, rebirthCount) * rebirthBonusPercent)
                / 100L;
        return (int) Math.min(96L, Math.max(0L, chanceBasisPoints / PERCENT_BASIS_POINTS));
    }

    private static boolean isSpiritBreakthroughRealm(int realmIndex) {
        return realmIndex >= SPIRIT_START_REALM_INDEX && realmIndex < realmCount() - 1;
    }
}
