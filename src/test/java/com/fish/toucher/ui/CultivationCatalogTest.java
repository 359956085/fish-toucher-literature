package com.fish.toucher.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CultivationCatalogTest {

    @Test
    void preservesExistingDefinitionIdsAndBalanceValues() {
        assertEquals(
                List.of(
                        "basic_breathing",
                        "evergreen_method",
                        "stone_gathering",
                        "mystic_orthodox",
                        "sword_heart",
                        "golden_body"
                ),
                CultivationCatalog.TECHNIQUES.stream()
                        .map(IdleCultivationManager.TechniqueDefinition::id)
                        .toList()
        );
        assertEquals(4, CultivationCatalog.TRAVEL_LOCATIONS.size());
        assertEquals(10, CultivationCatalog.CULTIVATORS.size());
        assertEquals(5, CultivationCatalog.SPELLS.size());
        assertEquals(5, CultivationCatalog.ARTIFACTS.size());
        assertEquals(35_000L, CultivationCatalog.CULTIVATOR_BY_ID.get("taixu_xuanheng").maxHealth());
        assertEquals(32_000L, CultivationCatalog.TRAVEL_BY_ID.get("cloud_dream_secret").baseQiReward());
    }
}
