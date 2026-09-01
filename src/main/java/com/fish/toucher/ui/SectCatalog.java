package com.fish.toucher.ui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 宗门静态定义集中放在这里，避免继续把玩法数据堆进 IdleCultivationManager。
 * 作者：fengshi
 */
final class SectCatalog {

    static final int UNLOCK_REALM_INDEX = 1;

    private static final List<SectDefinition> SECTS = List.of(
            new SectDefinition("qingyun_sword", "青云剑宗", "攻击 / 剑修", "攻击 +5%", BonusType.ATTACK, 5),
            new SectDefinition("danxia_valley", "丹霞谷", "丹药 / 发育", "丹药奖励 +10%", BonusType.PILL, 10),
            new SectDefinition("xuanwu_gate", "玄武门", "防御 / 渡劫", "防御 +6%", BonusType.DEFENSE, 6),
            new SectDefinition("tianji_pavilion", "天机阁", "游历 / 推演", "游历耗时 -5%", BonusType.TRAVEL_DURATION, 5),
            new SectDefinition("taiqing_dao", "太清道宗", "修炼 / 突破", "修为收益 +5%", BonusType.QI, 5)
    );

    private static final List<SectRankDefinition> RANKS = List.of(
            new SectRankDefinition(0, "外门弟子", 0L, 1),
            new SectRankDefinition(1, "内门弟子", 100L, 2),
            new SectRankDefinition(2, "真传弟子", 375L, 3),
            new SectRankDefinition(3, "执事", 1_000L, 4),
            new SectRankDefinition(4, "长老", 2_500L, 6)
    );

    private static final List<SectTaskDefinition> TASKS = List.of(
            new SectTaskDefinition("sort_library", "整理经阁", 30, 30L, 10L, 0, 500L, "少量灵石/修为"),
            new SectTaskDefinition("guard_herb_garden", "看守药园", 60, 60L, 20L, 0, 1_200L, "小概率丹药"),
            new SectTaskDefinition("clear_beasts", "清理妖兽", 120, 120L, 35L, 1, 2_600L, "灵石 + 战斗资源"),
            new SectTaskDefinition("escort_herbs", "护送灵药", 240, 240L, 70L, 1, 5_200L, "丹药/材料倾向"),
            new SectTaskDefinition("guard_spirit_vein", "镇守灵脉", 360, 340L, 95L, 2, 8_800L, "高额灵石"),
            new SectTaskDefinition("explore_secret", "探查秘境", 480, 450L, 120L, 2, 13_500L, "高价值随机奖励")
    );

    private static final List<SectInheritanceDefinition> INHERITANCES = List.of(
            new SectInheritanceDefinition("qingyun_basic", "青云吐纳法", "青云剑宗", "青云基础功法，偏向攻击。", InheritanceType.TECHNIQUE, "qingyun_sword_method", 200L, 0, ""),
            new SectInheritanceDefinition("qingyun_spell", "赤炎剑诀", "青云剑宗", "剑修入门法术。", InheritanceType.SPELL, "fire_sword", 600L, 1, ""),
            new SectInheritanceDefinition("qingyun_core", "青锋剑", "青云剑宗", "镇宗剑器，需要最终试炼认可。", InheritanceType.ARTIFACT, "green_sword", 6_000L, 4, "qingyun_sword:5"),

            new SectInheritanceDefinition("danxia_basic", "百草诀", "丹霞谷", "丹药与资源成长传承。", InheritanceType.TECHNIQUE, "danxia_herb_method", 200L, 0, ""),
            new SectInheritanceDefinition("danxia_spell", "青木回春", "丹霞谷", "丹霞谷疗伤法术。", InheritanceType.SPELL, "greenwood_heal", 600L, 1, ""),
            new SectInheritanceDefinition("danxia_core", "太上丹经", "丹霞谷", "丹道镇宗传承。", InheritanceType.PILL, "breakthrough_pill", 6_000L, 4, "danxia_valley:5"),

            new SectInheritanceDefinition("xuanwu_basic", "玄武吐息", "玄武门", "防御与护体传承。", InheritanceType.TECHNIQUE, "xuanwu_guard_method", 200L, 0, ""),
            new SectInheritanceDefinition("xuanwu_spell", "金光护体", "玄武门", "护盾法术。", InheritanceType.SPELL, "golden_light", 600L, 1, ""),
            new SectInheritanceDefinition("xuanwu_core", "玄龟盾", "玄武门", "镇宗护体法宝。", InheritanceType.ARTIFACT, "turtle_shield", 6_000L, 4, "xuanwu_gate:5"),

            new SectInheritanceDefinition("tianji_basic", "观星术", "天机阁", "游历收益传承。", InheritanceType.TECHNIQUE, "tianji_star_method", 200L, 0, ""),
            new SectInheritanceDefinition("tianji_spell", "寒霜封脉", "天机阁", "控场法术。", InheritanceType.SPELL, "frost_bind", 600L, 1, ""),
            new SectInheritanceDefinition("tianji_core", "风雷靴", "天机阁", "镇宗机缘法宝。", InheritanceType.ARTIFACT, "wind_thunder_boots", 6_000L, 4, "tianji_pavilion:5"),

            new SectInheritanceDefinition("taiqing_basic", "清心诀", "太清道宗", "稳健修炼传承。", InheritanceType.TECHNIQUE, "taiqing_clear_method", 200L, 0, ""),
            new SectInheritanceDefinition("taiqing_spell", "掌心雷", "太清道宗", "雷法传承。", InheritanceType.SPELL, "palm_thunder", 600L, 1, ""),
            new SectInheritanceDefinition("taiqing_core", "太虚鼎", "太清道宗", "镇宗综合法宝。", InheritanceType.ARTIFACT, "taixu_cauldron", 6_000L, 4, "taiqing_dao:5")
    );

    private static final List<SectTrialDefinition> TRIALS = List.of(
            trial("qingyun_sword", 1, "青云外门师兄", 1_200L, 130L, 80L, 120L, 200L),
            trial("qingyun_sword", 2, "青云内门剑修", 2_200L, 210L, 140L, 180L, 450L),
            trial("qingyun_sword", 3, "青云真传首席", 4_400L, 390L, 250L, 320L, 900L),
            trial("qingyun_sword", 4, "护宗剑长老", 9_500L, 820L, 560L, 680L, 1_800L),
            trial("qingyun_sword", 5, "青云祖师残影", 18_000L, 1_450L, 950L, 1_250L, 3_200L),
            trial("danxia_valley", 1, "丹霞药童", 1_300L, 110L, 90L, 160L, 200L),
            trial("danxia_valley", 2, "丹霞内门丹师", 2_500L, 190L, 160L, 240L, 450L),
            trial("danxia_valley", 3, "丹霞真传炉主", 4_800L, 340L, 290L, 420L, 900L),
            trial("danxia_valley", 4, "丹霞护法长老", 10_000L, 720L, 650L, 760L, 1_800L),
            trial("danxia_valley", 5, "丹霞丹祖残影", 19_000L, 1_300L, 1_080L, 1_400L, 3_200L),
            trial("xuanwu_gate", 1, "玄武外门武修", 1_500L, 100L, 120L, 100L, 200L),
            trial("xuanwu_gate", 2, "玄武内门铁卫", 2_900L, 175L, 220L, 170L, 450L),
            trial("xuanwu_gate", 3, "玄武真传盾修", 5_400L, 310L, 420L, 300L, 900L),
            trial("xuanwu_gate", 4, "玄武护宗长老", 11_000L, 650L, 880L, 610L, 1_800L),
            trial("xuanwu_gate", 5, "玄武祖师残影", 21_000L, 1_150L, 1_520L, 1_050L, 3_200L),
            trial("tianji_pavilion", 1, "天机外门卜者", 1_150L, 120L, 75L, 150L, 200L),
            trial("tianji_pavilion", 2, "天机内门术士", 2_150L, 205L, 135L, 260L, 450L),
            trial("tianji_pavilion", 3, "天机真传阵师", 4_200L, 370L, 240L, 440L, 900L),
            trial("tianji_pavilion", 4, "天机护阁长老", 9_000L, 760L, 520L, 820L, 1_800L),
            trial("tianji_pavilion", 5, "天机阁主残影", 17_500L, 1_360L, 900L, 1_520L, 3_200L),
            trial("taiqing_dao", 1, "太清外门道人", 1_250L, 115L, 95L, 170L, 200L),
            trial("taiqing_dao", 2, "太清内门道士", 2_400L, 195L, 170L, 280L, 450L),
            trial("taiqing_dao", 3, "太清真传道子", 4_600L, 350L, 300L, 480L, 900L),
            trial("taiqing_dao", 4, "太清执法长老", 9_800L, 730L, 670L, 900L, 1_800L),
            trial("taiqing_dao", 5, "太清祖师残影", 18_800L, 1_320L, 1_120L, 1_650L, 3_200L)
    );

    private static final List<SectEventDefinition> EVENTS = List.of(
            event("sparring", "同门切磋", "同门邀你切磋，胜负不伤和气。",
                    option("accept", "接受", "按当前战力即时判定，胜利获得威望和灵石。"),
                    option("decline", "婉拒", "不获得奖励。")),
            event("elder_lecture", "长老授课", "长老临时开坛讲法，适合补足修行细节。",
                    option("listen", "听讲", "获得一笔修为。"),
                    option("skip", "继续闭关", "不获得奖励。")),
            event("back_mountain", "后山异动", "后山灵气异常，可能有小机缘，也可能空手而归。",
                    option("inspect", "前往查看", "随机获得灵石、丹药或修为。"),
                    option("ignore", "忽略", "不获得奖励。")),
            event("junior_help", "同门求助", "同门修行受阻，希望借一枚聚气丹渡过关口。",
                    option("give_qi_pill", "赠送聚气丹", "消耗 1 枚聚气丹，获得威望和贡献。"),
                    option("refuse", "婉拒", "不获得奖励。")),
            event("inheritance_fragment", "传承残卷", "你偶然得到一页残卷，可自行参悟，也可上交宗门。",
                    option("study", "自行参悟", "获得修为。"),
                    option("submit", "上交宗门", "获得宗门贡献。"))
    );

    private static final List<SectSecretRealmDefinition> SECRET_REALMS = List.of(
            secretRealm("qingyun_secret_realm", "qingyun_sword", "青云秘境", "青云剑宗封存的剑道秘境，适合攻击 Build。",
                    secretNode("qingyun_enter", SecretRealmNodeType.ENTRY, "进入青云秘境", "山门令牌亮起，云海中现出一条剑气石阶。", 0L, 0L, 0L, 0L),
                    secretNode("qingyun_puppet", SecretRealmNodeType.BATTLE, "第 1 层剑傀战斗", "剑傀持残剑守路，考验基础攻防。", 7_800L, 760L, 500L, 420L),
                    secretNode("qingyun_tablet", SecretRealmNodeType.CHOICE, "第 2 层残碑", "残碑留有剑痕，可参悟片刻，也可保存状态直接前进。", 0L, 0L, 0L, 0L,
                            secretOption("study", "参悟", "获得修为后前进。"),
                            secretOption("advance", "前进", "不额外停留，直接前进。")),
                    secretNode("qingyun_chest", SecretRealmNodeType.CHEST, "第 3 层剑匣宝箱", "旧剑匣尚有灵光，开启后获得秘境资源。", 0L, 0L, 0L, 0L),
                    secretNode("qingyun_boss", SecretRealmNodeType.BOSS, "第 4 层剑魂 BOSS", "青云剑魂镇守终点，只认可足够完整的战斗 Build。", 13_500L, 1_180L, 760L, 780L)),
            secretRealm("danxia_secret_realm", "danxia_valley", "丹霞秘境", "丹霞谷药火秘境，资源收益高但守关火灵危险。",
                    secretNode("danxia_enter", SecretRealmNodeType.ENTRY, "进入丹霞秘境", "丹雾散开，药香与火气从石门内涌出。", 0L, 0L, 0L, 0L),
                    secretNode("danxia_guard", SecretRealmNodeType.BATTLE, "第 1 层药园火灵", "火灵盘踞药园，先稳住身法才能深入。", 8_200L, 690L, 570L, 560L),
                    secretNode("danxia_cauldron", SecretRealmNodeType.CHOICE, "第 2 层旧丹炉", "旧丹炉余温未散，可参悟丹诀，也可继续深入。", 0L, 0L, 0L, 0L,
                            secretOption("study", "参悟", "获得修为后前进。"),
                            secretOption("advance", "前进", "不额外停留，直接前进。")),
                    secretNode("danxia_chest", SecretRealmNodeType.CHEST, "第 3 层药柜宝箱", "药柜中封着几份尚可使用的灵材。", 0L, 0L, 0L, 0L),
                    secretNode("danxia_boss", SecretRealmNodeType.BOSS, "第 4 层丹火 BOSS", "丹火凝形，攻势不急却极耗法力。", 14_200L, 1_060L, 850L, 960L)),
            secretRealm("xuanwu_secret_realm", "xuanwu_gate", "玄武秘境", "玄武门护山秘境，偏重防御与持久战。",
                    secretNode("xuanwu_enter", SecretRealmNodeType.ENTRY, "进入玄武秘境", "黑水石门开启，沉重灵压落在肩头。", 0L, 0L, 0L, 0L),
                    secretNode("xuanwu_guard", SecretRealmNodeType.BATTLE, "第 1 层玄甲卫", "玄甲卫以盾阵拦路，硬碰硬难以取巧。", 9_000L, 640L, 760L, 440L),
                    secretNode("xuanwu_stele", SecretRealmNodeType.CHOICE, "第 2 层龟甲碑", "龟甲碑记录护体法门，可参悟，也可继续前进。", 0L, 0L, 0L, 0L,
                            secretOption("study", "参悟", "获得修为后前进。"),
                            secretOption("advance", "前进", "不额外停留，直接前进。")),
                    secretNode("xuanwu_chest", SecretRealmNodeType.CHEST, "第 3 层水府宝箱", "水府宝箱被玄光包裹，开启后获得秘境资源。", 0L, 0L, 0L, 0L),
                    secretNode("xuanwu_boss", SecretRealmNodeType.BOSS, "第 4 层玄武 BOSS", "玄武残影厚重如山，需要足够攻防才能破局。", 15_800L, 980L, 1_150L, 760L)),
            secretRealm("tianji_secret_realm", "tianji_pavilion", "天机秘境", "天机阁星盘秘境，重视法力与综合属性。",
                    secretNode("tianji_enter", SecretRealmNodeType.ENTRY, "进入天机秘境", "星盘转动，脚下阵纹铺成通路。", 0L, 0L, 0L, 0L),
                    secretNode("tianji_guard", SecretRealmNodeType.BATTLE, "第 1 层星盘守卫", "守卫借阵法变换方位，考验综合战力。", 7_600L, 740L, 480L, 680L),
                    secretNode("tianji_scroll", SecretRealmNodeType.CHOICE, "第 2 层星图残卷", "残卷上星象未灭，可参悟，也可继续追踪机缘。", 0L, 0L, 0L, 0L,
                            secretOption("study", "参悟", "获得修为后前进。"),
                            secretOption("advance", "前进", "不额外停留，直接前进。")),
                    secretNode("tianji_chest", SecretRealmNodeType.CHEST, "第 3 层星匣宝箱", "星匣内有秘境凝结的灵物。", 0L, 0L, 0L, 0L),
                    secretNode("tianji_boss", SecretRealmNodeType.BOSS, "第 4 层星魂 BOSS", "星魂借天机阵压制来者，法力不足会迅速失势。", 13_000L, 1_130L, 720L, 1_050L)),
            secretRealm("taiqing_secret_realm", "taiqing_dao", "太清秘境", "太清道宗清气秘境，收益均衡，BOSS 要求全面 Build。",
                    secretNode("taiqing_enter", SecretRealmNodeType.ENTRY, "进入太清秘境", "清气化桥，秘境深处传来钟声。", 0L, 0L, 0L, 0L),
                    secretNode("taiqing_guard", SecretRealmNodeType.BATTLE, "第 1 层清气道兵", "道兵以清气化刃，攻守平衡。", 8_000L, 710L, 610L, 620L),
                    secretNode("taiqing_wall", SecretRealmNodeType.CHOICE, "第 2 层道纹石壁", "石壁上道纹流转，可参悟，也可继续前进。", 0L, 0L, 0L, 0L,
                            secretOption("study", "参悟", "获得修为后前进。"),
                            secretOption("advance", "前进", "不额外停留，直接前进。")),
                    secretNode("taiqing_chest", SecretRealmNodeType.CHEST, "第 3 层清光宝箱", "宝箱内清光未散，可得秘境资源。", 0L, 0L, 0L, 0L),
                    secretNode("taiqing_boss", SecretRealmNodeType.BOSS, "第 4 层太清 BOSS", "太清残影攻防法俱全，偏科 Build 难以通过。", 14_600L, 1_100L, 900L, 980L))
    );

    private static final Map<String, SectDefinition> SECT_BY_ID = index(SECTS);
    private static final Map<String, SectTaskDefinition> TASK_BY_ID = index(TASKS);
    private static final Map<String, SectInheritanceDefinition> INHERITANCE_BY_ID = index(INHERITANCES);
    private static final Map<String, SectTrialDefinition> TRIAL_BY_ID = index(TRIALS);
    private static final Map<String, SectEventDefinition> EVENT_BY_ID = index(EVENTS);
    private static final Map<String, SectSecretRealmDefinition> SECRET_REALM_BY_ID = index(SECRET_REALMS);

    private SectCatalog() {}

    static List<SectDefinition> sects() { return SECTS; }
    static List<SectRankDefinition> ranks() { return RANKS; }
    static List<SectTaskDefinition> tasks() { return TASKS; }
    static List<SectInheritanceDefinition> inheritances() { return INHERITANCES; }
    static List<SectTrialDefinition> trials() { return TRIALS; }
    static List<SectEventDefinition> events() { return EVENTS; }
    static List<SectSecretRealmDefinition> secretRealms() { return SECRET_REALMS; }
    static SectDefinition sect(String id) { return SECT_BY_ID.get(id); }
    static SectTaskDefinition task(String id) { return TASK_BY_ID.get(id); }
    static SectInheritanceDefinition inheritance(String id) { return INHERITANCE_BY_ID.get(id); }
    static SectTrialDefinition trial(String id) { return TRIAL_BY_ID.get(id); }
    static SectEventDefinition event(String id) { return EVENT_BY_ID.get(id); }
    static SectSecretRealmDefinition secretRealm(String id) { return SECRET_REALM_BY_ID.get(id); }
    static SectRankDefinition rank(int rankIndex) { return RANKS.get(Math.max(0, Math.min(RANKS.size() - 1, rankIndex))); }

    private static SectSecretRealmDefinition secretRealm(String id, String sectId, String name, String description,
                                                        SectSecretRealmNodeDefinition... nodes) {
        return new SectSecretRealmDefinition(id, sectId, name, description, 3, List.of(nodes));
    }

    private static SectSecretRealmNodeDefinition secretNode(String id, SecretRealmNodeType type, String title,
                                                           String description, long maxHealth, long attack,
                                                           long defense, long mana,
                                                           SectSecretRealmOptionDefinition... options) {
        return new SectSecretRealmNodeDefinition(id, type, title, description, maxHealth, attack, defense, mana, List.of(options));
    }

    private static SectSecretRealmOptionDefinition secretOption(String id, String label, String description) {
        return new SectSecretRealmOptionDefinition(id, label, description);
    }

    private static SectEventDefinition event(String id, String title, String description, SectEventOptionDefinition... options) {
        return new SectEventDefinition(id, title, description, List.of(options));
    }

    private static SectEventOptionDefinition option(String id, String label, String description) {
        return new SectEventOptionDefinition(id, label, description);
    }

    private static SectTrialDefinition trial(String sectId, int floor, String enemyName,
                                             long maxHealth, long attack, long defense, long mana, long stoneReward) {
        return new SectTrialDefinition(sectId + ":" + floor, sectId, floor, enemyName, maxHealth, attack, defense, mana, stoneReward);
    }

    private static <T extends Identified> Map<String, T> index(List<T> values) {
        Map<String, T> result = new LinkedHashMap<>();
        for (T value : values) {
            result.put(value.id(), value);
        }
        return Collections.unmodifiableMap(result);
    }

    interface Identified {
        String id();
    }

    enum BonusType {
        ATTACK, DEFENSE, QI, PILL, TRAVEL_DURATION
    }

    enum InheritanceType {
        TECHNIQUE, SPELL, ARTIFACT, PILL
    }

    enum SecretRealmNodeType {
        ENTRY, BATTLE, CHOICE, CHEST, BOSS
    }

    record SectDefinition(String id, String name, String style, String bonusText,
                          BonusType bonusType, int bonusPercent) implements Identified {}

    record SectRankDefinition(int rankIndex, String name, long prestigeRequired,
                              int realmRequired) {}

    record SectTaskDefinition(String id, String name, int durationMinutes,
                              long contributionReward, long prestigeReward,
                              int minRankIndex, long qiReward, String extraText) implements Identified {}

    record SectInheritanceDefinition(String id, String name, String sectName, String description,
                                     InheritanceType type, String rewardId, long contributionCost,
                                     int minRankIndex, String requiredTrialId) implements Identified {}

    record SectTrialDefinition(String id, String sectId, int floor, String enemyName,
                               long maxHealth, long attack, long defense,
                               long mana, long stoneReward) implements Identified {}

    record SectEventDefinition(String id, String title, String description,
                               List<SectEventOptionDefinition> options) implements Identified {}

    record SectEventOptionDefinition(String id, String label, String description) implements Identified {}

    record SectSecretRealmDefinition(String id, String sectId, String name, String description,
                                     int minRankIndex, List<SectSecretRealmNodeDefinition> nodes) implements Identified {}

    record SectSecretRealmNodeDefinition(String id, SecretRealmNodeType type, String title, String description,
                                         long maxHealth, long attack, long defense, long mana,
                                         List<SectSecretRealmOptionDefinition> options) implements Identified {}

    record SectSecretRealmOptionDefinition(String id, String label, String description) implements Identified {}
}
