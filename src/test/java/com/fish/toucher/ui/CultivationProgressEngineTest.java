package com.fish.toucher.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CultivationProgressEngineTest {

    @Test
    void settlesPassiveAndSeclusionWithRemainder() {
        CultivationProgressEngine.ProgressGain gain = CultivationProgressEngine.settle(
                90_000L, 8 * 60 * 60 * 1_000L, 30L, 4L, 24L, true
        );

        assertEquals(90_000L, gain.creditedMillis());
        assertEquals(42L, gain.qiGain());
        assertEquals(30L, gain.remainderUnits());
    }

    @Test
    void capsOfflineTimeAndCanPauseSeclusion() {
        CultivationProgressEngine.ProgressGain gain = CultivationProgressEngine.settle(
                12 * 60 * 60 * 1_000L, 8 * 60 * 60 * 1_000L, 0L, 4L, 24L, false
        );

        assertEquals(8 * 60 * 60 * 1_000L, gain.creditedMillis());
        assertEquals(1_920L, gain.qiGain());
        assertEquals(0L, gain.remainderUnits());
    }

    @Test
    void preservesBreakthroughAndTravelRules() {
        assertEquals(7_800L, CultivationProgressEngine.retainedQiAfterFailedBreakthrough(10_000L, false));
        assertEquals(8_800L, CultivationProgressEngine.retainedQiAfterFailedBreakthrough(10_000L, true));
        assertEquals(25, CultivationProgressEngine.travelReductionPercent(4, 9, 50));
        assertEquals(90L, CultivationProgressEngine.travelDurationMinutes(120L, 25));
    }
}
