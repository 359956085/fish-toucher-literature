package com.fish.toucher.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.fish.toucher.ui.IdleCultivationManager.AbodeFacilityDefinition;
import static com.fish.toucher.ui.IdleCultivationManager.ArtifactDefinition;
import static com.fish.toucher.ui.IdleCultivationManager.BattleSpellType;
import static com.fish.toucher.ui.IdleCultivationManager.CultivatorDefinition;
import static com.fish.toucher.ui.IdleCultivationManager.PillDefinition;
import static com.fish.toucher.ui.IdleCultivationManager.SpellDefinition;
import static com.fish.toucher.ui.IdleCultivationManager.TechniqueDefinition;
import static com.fish.toucher.ui.IdleCultivationManager.TravelLocationDefinition;

/** Immutable cultivation definitions, kept separate from runtime coordination. */
final class CultivationCatalog {

    static final List<TechniqueDefinition> TECHNIQUES = List.of(
            new TechniqueDefinition("basic_breathing", "cultivation.technique.basic.name", "cultivation.technique.basic.desc", 0, 0, 0, 0, 0, 0),
            new TechniqueDefinition("evergreen_method", "cultivation.technique.evergreen.name", "cultivation.technique.evergreen.desc", 18, 0, 0, 0, 0, 0),
            new TechniqueDefinition("stone_gathering", "cultivation.technique.stone.name", "cultivation.technique.stone.desc", 0, 25, 0, 0, 0, 0),
            new TechniqueDefinition("mystic_orthodox", "cultivation.technique.mystic.name", "cultivation.technique.mystic.desc", 12, 12, 6, 0, 0, 0),
            new TechniqueDefinition("sword_heart", "cultivation.technique.swordHeart.name", "cultivation.technique.swordHeart.desc", 8, 0, 0, 16, 0, 8),
            new TechniqueDefinition("golden_body", "cultivation.technique.goldenBody.name", "cultivation.technique.goldenBody.desc", 0, 0, 4, 0, 18, 12)
    );
    static final Map<String, TechniqueDefinition> TECHNIQUE_BY_ID = index(TECHNIQUES, TechniqueDefinition::id);

    static final List<PillDefinition> PILLS = List.of(
            new PillDefinition("qi_pill", "cultivation.pill.qi.name", "cultivation.pill.qi.desc"),
            new PillDefinition("spirit_pill", "cultivation.pill.spirit.name", "cultivation.pill.spirit.desc"),
            new PillDefinition("breakthrough_pill", "cultivation.pill.breakthrough.name", "cultivation.pill.breakthrough.desc"),
            new PillDefinition("meridian_pill", "cultivation.pill.meridian.name", "cultivation.pill.meridian.desc")
    );
    static final Map<String, PillDefinition> PILL_BY_ID = index(PILLS, PillDefinition::id);

    static final List<SpellDefinition> SPELLS = List.of(
            new SpellDefinition("fire_sword", "cultivation.spell.fireSword.name", "cultivation.spell.fireSword.desc", 90, 6, BattleSpellType.DAMAGE, 100),
            new SpellDefinition("palm_thunder", "cultivation.spell.palmThunder.name", "cultivation.spell.palmThunder.desc", 140, 10, BattleSpellType.DAMAGE, 160),
            new SpellDefinition("frost_bind", "cultivation.spell.frostBind.name", "cultivation.spell.frostBind.desc", 120, 12, BattleSpellType.FROST, 70),
            new SpellDefinition("greenwood_heal", "cultivation.spell.greenwoodHeal.name", "cultivation.spell.greenwoodHeal.desc", 110, 14, BattleSpellType.HEAL, 16),
            new SpellDefinition("golden_light", "cultivation.spell.goldenLight.name", "cultivation.spell.goldenLight.desc", 100, 16, BattleSpellType.SHIELD, 45)
    );
    static final Map<String, SpellDefinition> SPELL_BY_ID = index(SPELLS, SpellDefinition::id);

    static final List<ArtifactDefinition> ARTIFACTS = List.of(
            new ArtifactDefinition("green_sword", "cultivation.artifact.greenSword.name", "cultivation.artifact.greenSword.desc", 14, 0, 0, 0),
            new ArtifactDefinition("turtle_shield", "cultivation.artifact.turtleShield.name", "cultivation.artifact.turtleShield.desc", 0, 14, 0, 0),
            new ArtifactDefinition("spirit_jade", "cultivation.artifact.spiritJade.name", "cultivation.artifact.spiritJade.desc", 0, 0, 16, 6),
            new ArtifactDefinition("wind_thunder_boots", "cultivation.artifact.windThunderBoots.name", "cultivation.artifact.windThunderBoots.desc", 6, 6, 0, 4),
            new ArtifactDefinition("taixu_cauldron", "cultivation.artifact.taixuCauldron.name", "cultivation.artifact.taixuCauldron.desc", 8, 8, 8, 10)
    );
    static final Map<String, ArtifactDefinition> ARTIFACT_BY_ID = index(ARTIFACTS, ArtifactDefinition::id);

    static final List<TravelLocationDefinition> TRAVEL_LOCATIONS = List.of(
            new TravelLocationDefinition("bamboo_forest", "cultivation.travel.bamboo.name", "cultivation.travel.bamboo.desc", 30, 0, 1_500, 45, 45, 8),
            new TravelLocationDefinition("abandoned_alchemy_room", "cultivation.travel.alchemy.name", "cultivation.travel.alchemy.desc", 60, 0, 4_200, 120, 70, 12),
            new TravelLocationDefinition("spirit_mine", "cultivation.travel.mine.name", "cultivation.travel.mine.desc", 120, 1, 14_000, 420, 40, 16),
            new TravelLocationDefinition("cloud_dream_secret", "cultivation.travel.secret.name", "cultivation.travel.secret.desc", 240, 2, 32_000, 900, 65, 30)
    );
    static final Map<String, TravelLocationDefinition> TRAVEL_BY_ID = index(TRAVEL_LOCATIONS, TravelLocationDefinition::id);

    static final List<CultivatorDefinition> CULTIVATORS = List.of(
            new CultivatorDefinition("outer_sword_lin", "cultivation.cultivator.outerSwordLin.name", 1_000L, 100L, 70L, 100L, 600L, "fire_sword", "", ""),
            new CultivatorDefinition("herbalist_chen", "cultivation.cultivator.herbalistChen.name", 1_700L, 170L, 120L, 160L, 900L, "", "", "green_sword"),
            new CultivatorDefinition("talisman_zhao", "cultivation.cultivator.talismanZhao.name", 2_600L, 260L, 190L, 250L, 1_200L, "palm_thunder", "evergreen_method", ""),
            new CultivatorDefinition("cold_shen", "cultivation.cultivator.coldShen.name", 4_200L, 390L, 300L, 380L, 1_600L, "", "", "turtle_shield"),
            new CultivatorDefinition("golden_core_han", "cultivation.cultivator.goldenCoreHan.name", 6_500L, 550L, 420L, 600L, 2_100L, "frost_bind", "stone_gathering", ""),
            new CultivatorDefinition("armor_bai", "cultivation.cultivator.armorBai.name", 15_000L, 1_350L, 1_000L, 850L, 2_700L, "", "", "spirit_jade"),
            new CultivatorDefinition("thunder_luo", "cultivation.cultivator.thunderLuo.name", 18_000L, 1_550L, 1_200L, 1_100L, 3_400L, "greenwood_heal", "mystic_orthodox", ""),
            new CultivatorDefinition("illusion_su", "cultivation.cultivator.illusionSu.name", 22_000L, 1_800L, 1_450L, 1_350L, 4_200L, "", "sword_heart", "wind_thunder_boots"),
            new CultivatorDefinition("mahayana_gu", "cultivation.cultivator.mahayanaGu.name", 28_000L, 2_200L, 1_750L, 1_600L, 5_100L, "golden_light", "golden_body", ""),
            new CultivatorDefinition("taixu_xuanheng", "cultivation.cultivator.taixuXuanheng.name", 35_000L, 2_700L, 2_150L, 1_900L, 6_200L, "", "", "taixu_cauldron")
    );
    static final Map<String, CultivatorDefinition> CULTIVATOR_BY_ID = index(CULTIVATORS, CultivatorDefinition::id);

    static final List<AbodeFacilityDefinition> ABODE_FACILITIES = List.of(
            new AbodeFacilityDefinition("spirit_gathering_array", "cultivation.abode.spiritGathering.name", "cultivation.abode.spiritGathering.desc", 180),
            new AbodeFacilityDefinition("spirit_vein", "cultivation.abode.spiritVein.name", "cultivation.abode.spiritVein.desc", 220),
            new AbodeFacilityDefinition("alchemy_room", "cultivation.abode.alchemyRoom.name", "cultivation.abode.alchemyRoom.desc", 260),
            new AbodeFacilityDefinition("insight_room", "cultivation.abode.insightRoom.name", "cultivation.abode.insightRoom.desc", 300)
    );
    static final Map<String, AbodeFacilityDefinition> ABODE_BY_ID = index(ABODE_FACILITIES, AbodeFacilityDefinition::id);

    private CultivationCatalog() {}

    private static <T> Map<String, T> index(List<T> values, java.util.function.Function<T, String> id) {
        Map<String, T> result = new LinkedHashMap<>();
        for (T value : values) {
            result.put(id.apply(value), value);
        }
        return Map.copyOf(result);
    }
}
