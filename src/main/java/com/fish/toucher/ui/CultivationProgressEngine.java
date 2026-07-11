package com.fish.toucher.ui;

/** Pure progression calculations. No IDE, persistence, clock, or random dependencies. */
final class CultivationProgressEngine {

    private CultivationProgressEngine() {}

    static ProgressGain settle(
            long elapsedMillis,
            long maximumCreditedMillis,
            long remainderUnits,
            long passiveQiPerMinute,
            long seclusionQiPerMinute,
            boolean includeSeclusion
    ) {
        long creditedMillis = Math.min(Math.max(0L, elapsedMillis), Math.max(0L, maximumCreditedMillis));
        long creditedSeconds = creditedMillis / 1_000L;
        long qiUnits = Math.max(0L, remainderUnits)
                + creditedSeconds * Math.max(0L, passiveQiPerMinute)
                + (includeSeclusion ? creditedSeconds * Math.max(0L, seclusionQiPerMinute) : 0L);
        return new ProgressGain(creditedMillis, qiUnits / 60L, qiUnits % 60L);
    }

    static long retainedQiAfterFailedBreakthrough(long requiredQi, boolean meridianPillActive) {
        return Math.max(0L, requiredQi) * (meridianPillActive ? 88L : 78L) / 100L;
    }

    static int travelReductionPercent(int realmIndex, int realmCount, int maximumReductionPercent) {
        int highestRealmIndex = Math.max(1, realmCount - 1);
        int clampedRealmIndex = Math.max(0, Math.min(highestRealmIndex, realmIndex));
        return Math.min(
                Math.max(0, maximumReductionPercent),
                (int) Math.round(clampedRealmIndex * Math.max(0, maximumReductionPercent)
                        / (double) highestRealmIndex)
        );
    }

    static long travelDurationMinutes(long baseMinutes, int reductionPercent) {
        long reducedMinutes = Math.round(Math.max(0L, baseMinutes)
                * (100.0 - Math.max(0, Math.min(100, reductionPercent))) / 100.0);
        return Math.max(1L, reducedMinutes);
    }

    record ProgressGain(long creditedMillis, long qiGain, long remainderUnits) {}
}
