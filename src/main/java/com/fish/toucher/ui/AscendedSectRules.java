package com.fish.toucher.ui;

import com.fish.toucher.settings.NovelReaderSettings;

import java.util.List;

/**
 * 飞升后自建宗门规则。
 * 规则层只依赖设置快照，方便测试，UI 不直接拼业务判断。
 *
 * @author fengshi
 */
final class AscendedSectRules {

    private AscendedSectRules() {}

    static boolean canCreateOwnSect(NovelReaderSettings settings) {
        return settings != null && settings.isCultivationAscended() && !settings.isOwnSectCreated();
    }

    static int buildingLevelLimit(NovelReaderSettings settings) {
        return AscendedSectCatalog.tier(settings.getOwnSectTierIndex()).buildingLevelLimit();
    }

    static int discipleLimit(NovelReaderSettings settings) {
        return AscendedSectCatalog.tier(settings.getOwnSectTierIndex()).discipleLimit();
    }

    static boolean canUpgradeBuilding(NovelReaderSettings settings, AscendedSectCatalog.BuildingDefinition building) {
        if (settings == null || building == null || !settings.isCultivationAscended() || !settings.isOwnSectCreated()) {
            return false;
        }
        int level = settings.getOwnSectBuildingLevel(building.id());
        if (level >= buildingLevelLimit(settings)) {
            return false;
        }
        return settings.getCultivationSpiritStones() >= buildingStoneCost(settings, building)
                && settings.getOwnSectMaterials() >= buildingMaterialCost(settings, building);
    }

    static long buildingStoneCost(NovelReaderSettings settings, AscendedSectCatalog.BuildingDefinition building) {
        int nextLevel = settings.getOwnSectBuildingLevel(building.id()) + 1;
        return building.baseStoneCost() * nextLevel * nextLevel;
    }

    static long buildingMaterialCost(NovelReaderSettings settings, AscendedSectCatalog.BuildingDefinition building) {
        int nextLevel = settings.getOwnSectBuildingLevel(building.id()) + 1;
        return building.baseMaterialCost() * nextLevel * nextLevel;
    }

    static int buildingSlotCount(int level) {
        if (level >= 7) {
            return 3;
        }
        if (level >= 3) {
            return 2;
        }
        return level > 0 ? 1 : 0;
    }

    static int assignedDiscipleCount(NovelReaderSettings settings, String buildingId) {
        int count = 0;
        for (NovelReaderSettings.OwnSectDiscipleState disciple : settings.getOwnSectDisciples()) {
            if (buildingId.equals(disciple.assignedBuildingId)) {
                count++;
            }
        }
        return count;
    }

    static boolean canAssignDisciple(NovelReaderSettings settings,
                                     NovelReaderSettings.OwnSectDiscipleState disciple,
                                     AscendedSectCatalog.BuildingDefinition building) {
        if (settings == null || disciple == null || building == null) {
            return false;
        }
        int level = settings.getOwnSectBuildingLevel(building.id());
        return level > 0
                && building.specialty().name().equals(disciple.specialty)
                && assignedDiscipleCount(settings, building.id()) < buildingSlotCount(level);
    }

    static int aptitudeAmplifiedPercent(NovelReaderSettings settings, String buildingId, int basePercent) {
        int aptitudeSum = 0;
        for (NovelReaderSettings.OwnSectDiscipleState disciple : settings.getOwnSectDisciples()) {
            if (buildingId.equals(disciple.assignedBuildingId)) {
                aptitudeSum += Math.max(1, Math.min(100, disciple.aptitude));
            }
        }
        double multiplier = 1.0 + 0.2 * aptitudeSum / 100.0;
        return (int) Math.round(basePercent * multiplier);
    }

    static int gatheringQiBonusPercent(NovelReaderSettings settings) {
        int level = settings.getOwnSectBuildingLevel(AscendedSectCatalog.GATHERING_ARRAY_ID);
        int base = switch (Math.max(0, Math.min(10, level))) {
            case 0 -> 0;
            case 1 -> 5;
            case 2 -> 10;
            case 3 -> 16;
            case 4 -> 23;
            case 5 -> 31;
            case 6 -> 39;
            case 7 -> 48;
            case 8 -> 57;
            case 9 -> 66;
            default -> 75;
        };
        return aptitudeAmplifiedPercent(settings, AscendedSectCatalog.GATHERING_ARRAY_ID, base);
    }

    static int alchemyBonusPercent(NovelReaderSettings settings) {
        int level = settings.getOwnSectBuildingLevel(AscendedSectCatalog.ALCHEMY_HALL_ID);
        return aptitudeAmplifiedPercent(settings, AscendedSectCatalog.ALCHEMY_HALL_ID, level * 8);
    }

    static int refiningBonusPercent(NovelReaderSettings settings) {
        int level = settings.getOwnSectBuildingLevel(AscendedSectCatalog.REFINING_PAVILION_ID);
        return aptitudeAmplifiedPercent(settings, AscendedSectCatalog.REFINING_PAVILION_ID, level * 5);
    }

    static int scriptureBonusPercent(NovelReaderSettings settings) {
        int level = settings.getOwnSectBuildingLevel(AscendedSectCatalog.SCRIPTURE_LIBRARY_ID);
        int collectionCount = settings.getUnlockedTechniqueIds().size()
                + settings.getUnlockedSpellIds().size()
                + settings.getUnlockedArtifactIds().size()
                + settings.getSectSecretRealmResolvedNodeIds().size();
        return aptitudeAmplifiedPercent(settings, AscendedSectCatalog.SCRIPTURE_LIBRARY_ID, level * 4 + collectionCount);
    }

    static boolean canPromote(NovelReaderSettings settings) {
        AscendedSectCatalog.TierDefinition nextTier = AscendedSectCatalog.nextTier(settings.getOwnSectTierIndex());
        if (nextTier == null) {
            return false;
        }
        return settings.getCultivationRealmIndex() >= nextTier.requiredRealmIndex()
                && settings.getCultivationSpiritStones() >= nextTier.promotionStoneCost()
                && settings.getOwnSectMaterials() >= nextTier.promotionMaterialCost()
                && totalBuildingLevel(settings) >= nextTier.index() * 4;
    }

    static int totalBuildingLevel(NovelReaderSettings settings) {
        int total = 0;
        for (AscendedSectCatalog.BuildingDefinition building : AscendedSectCatalog.buildings()) {
            total += settings.getOwnSectBuildingLevel(building.id());
        }
        return total;
    }

    static List<AscendedSectCatalog.Specialty> specialties() {
        return List.of(AscendedSectCatalog.Specialty.values());
    }
}
