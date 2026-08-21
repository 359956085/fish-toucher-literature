package com.fish.toucher.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 飞升后自建宗门的静态定义。
 * 这里不放运行时状态，避免宗门成长数据和规则混在一起。
 *
 * @author fengshi
 */
final class AscendedSectCatalog {

    static final String GATHERING_ARRAY_ID = "gathering_array";
    static final String ALCHEMY_HALL_ID = "alchemy_hall";
    static final String REFINING_PAVILION_ID = "refining_pavilion";
    static final String SCRIPTURE_LIBRARY_ID = "scripture_library";

    private static final List<TierDefinition> TIERS = List.of(
            new TierDefinition(0, "草创山门", 30, 55, 65, 6, 2, 0L, 0L, 0),
            new TierDefinition(1, "一方小宗", 40, 65, 75, 8, 3, 10_000L, 80L, 9),
            new TierDefinition(2, "灵域宗门", 50, 75, 85, 10, 4, 28_000L, 180L, 10),
            new TierDefinition(3, "名门大宗", 60, 82, 92, 12, 5, 70_000L, 420L, 11),
            new TierDefinition(4, "灵界上宗", 70, 90, 98, 14, 5, 160_000L, 900L, 12),
            new TierDefinition(5, "修仙圣地", 80, 96, 100, 16, 5, 360_000L, 1_800L, 14)
    );

    private static final List<BuildingDefinition> BUILDINGS = List.of(
            new BuildingDefinition(GATHERING_ARRAY_ID, "聚灵阵", Specialty.GATHERING, "提高本尊修炼速度和离线修炼上限。", 2_400L, 18L),
            new BuildingDefinition(ALCHEMY_HALL_ID, "丹鼎殿", Specialty.ALCHEMY, "产出本尊可用丹药，丹道弟子提高效率。", 2_800L, 22L),
            new BuildingDefinition(REFINING_PAVILION_ID, "炼器阁", Specialty.REFINING, "提高本尊法宝攻击、防御与真气效果。", 3_200L, 26L),
            new BuildingDefinition(SCRIPTURE_LIBRARY_ID, "藏经阁", Specialty.SCRIPTURE, "收录已获功法传承，强化功法与法术效果。", 3_600L, 30L)
    );

    private static final Map<String, BuildingDefinition> BUILDING_BY_ID = indexBuildings();

    private AscendedSectCatalog() {}

    static List<TierDefinition> tiers() { return TIERS; }
    static List<BuildingDefinition> buildings() { return BUILDINGS; }
    static BuildingDefinition building(String id) { return BUILDING_BY_ID.get(id); }

    static TierDefinition tier(int tierIndex) {
        return TIERS.get(Math.max(0, Math.min(TIERS.size() - 1, tierIndex)));
    }

    static TierDefinition nextTier(int tierIndex) {
        int nextIndex = tierIndex + 1;
        return nextIndex >= TIERS.size() ? null : TIERS.get(nextIndex);
    }

    private static Map<String, BuildingDefinition> indexBuildings() {
        Map<String, BuildingDefinition> result = new LinkedHashMap<>();
        for (BuildingDefinition building : BUILDINGS) {
            result.put(building.id(), building);
        }
        return result;
    }

    enum Specialty {
        GATHERING("聚灵"),
        ALCHEMY("丹道"),
        REFINING("炼器"),
        SCRIPTURE("藏经");

        private final String label;

        Specialty(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    record TierDefinition(int index, String name, int commonAptitudeMin, int commonAptitudeMax,
                          int maxAptitude, int discipleLimit, int buildingLevelLimit,
                          long promotionStoneCost, long promotionMaterialCost,
                          int requiredRealmIndex) {}

    record BuildingDefinition(String id, String name, Specialty specialty, String description,
                              long baseStoneCost, long baseMaterialCost) {}
}
