package com.fish.toucher.ui;

import com.fish.toucher.settings.NovelReaderSettings;

/**
 * 宗门规则只依赖配置快照，方便回归测试，也避免 UI 直接散落业务判断。
 * 作者：fengshi
 */
final class SectRules {

    private SectRules() {}

    static boolean isSectUnlocked(int realmIndex) {
        return realmIndex >= SectCatalog.UNLOCK_REALM_INDEX;
    }

    static int clampRank(int rankIndex) {
        return Math.max(0, Math.min(SectCatalog.ranks().size() - 1, rankIndex));
    }

    static SectCatalog.SectRankDefinition currentRank(NovelReaderSettings settings) {
        return SectCatalog.rank(settings.getCurrentSectRankIndex());
    }

    static SectCatalog.SectRankDefinition nextRank(NovelReaderSettings settings) {
        int nextRankIndex = settings.getCurrentSectRankIndex() + 1;
        if (nextRankIndex >= SectCatalog.ranks().size()) {
            return null;
        }
        return SectCatalog.rank(nextRankIndex);
    }

    static boolean canPromote(NovelReaderSettings settings) {
        SectCatalog.SectRankDefinition nextRank = nextRank(settings);
        if (nextRank == null || settings.getCultivationSectId().isEmpty()) {
            return false;
        }
        return settings.getCurrentSectPrestige() >= nextRank.prestigeRequired()
                && settings.getCultivationRealmIndex() >= nextRank.realmRequired();
    }

    static boolean isTaskUnlocked(NovelReaderSettings settings, SectCatalog.SectTaskDefinition task) {
        return task != null
                && !settings.getCultivationSectId().isEmpty()
                && settings.getCurrentSectRankIndex() >= task.minRankIndex();
    }

    static boolean isInheritanceUnlocked(NovelReaderSettings settings, SectCatalog.SectInheritanceDefinition inheritance) {
        if (inheritance == null || settings.getCultivationSectId().isEmpty()) {
            return false;
        }
        if (settings.getCurrentSectRankIndex() < inheritance.minRankIndex()) {
            return false;
        }
        String trialId = inheritance.requiredTrialId();
        return trialId == null || trialId.isEmpty() || settings.isSectTrialDefeated(trialId);
    }

    static boolean canPurchaseInheritance(NovelReaderSettings settings, SectCatalog.SectInheritanceDefinition inheritance) {
        return isInheritanceUnlocked(settings, inheritance)
                && !settings.isSectInheritanceLearned(inheritance.id())
                && settings.getCurrentSectContribution() >= inheritance.contributionCost();
    }

    static boolean isTrialUnlocked(NovelReaderSettings settings, SectCatalog.SectTrialDefinition trial) {
        if (trial == null || settings.getCultivationSectId().isEmpty()) {
            return false;
        }
        if (!trial.sectId().equals(settings.getCultivationSectId())) {
            return false;
        }
        int requiredRank = Math.max(0, trial.floor() - 1);
        return settings.getCurrentSectRankIndex() >= requiredRank;
    }

    static boolean isSecretRealmUnlocked(NovelReaderSettings settings, SectCatalog.SectSecretRealmDefinition secretRealm) {
        return secretRealm != null
                && !settings.getCultivationSectId().isEmpty()
                && secretRealm.sectId().equals(settings.getCultivationSectId())
                && settings.getCurrentSectRankIndex() >= secretRealm.minRankIndex();
    }

    static int currentSectBonus(NovelReaderSettings settings, SectCatalog.BonusType bonusType) {
        SectCatalog.SectDefinition sect = SectCatalog.sect(settings.getCultivationSectId());
        if (sect == null || sect.bonusType() != bonusType) {
            return 0;
        }
        return sect.bonusPercent();
    }
}
