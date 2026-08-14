package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service(Service.Level.APP)
public final class IdleCultivationManager implements Disposable {

    public static final String BASIC_TECHNIQUE_ID = "basic_breathing";
    private static final String QI_PILL_ID = "qi_pill";
    private static final String SPIRIT_PILL_ID = "spirit_pill";
    private static final String BREAKTHROUGH_PILL_ID = "breakthrough_pill";
    private static final String MERIDIAN_PILL_ID = "meridian_pill";
    private static final String SPIRIT_GATHERING_ARRAY_ID = "spirit_gathering_array";
    private static final String SPIRIT_VEIN_ID = "spirit_vein";
    private static final String ALCHEMY_ROOM_ID = "alchemy_room";
    private static final String INSIGHT_ROOM_ID = "insight_room";
    private static final String EVERGREEN_TECHNIQUE_ID = "evergreen_method";
    private static final String STONE_GATHERING_TECHNIQUE_ID = "stone_gathering";
    private static final String MYSTIC_ORTHODOX_TECHNIQUE_ID = "mystic_orthodox";
    private static final String SWORD_HEART_TECHNIQUE_ID = "sword_heart";
    private static final String GOLDEN_BODY_TECHNIQUE_ID = "golden_body";
    private static final String QINGYUN_SECT_TECHNIQUE_ID = "qingyun_sword_method";
    private static final String DANXIA_SECT_TECHNIQUE_ID = "danxia_herb_method";
    private static final String XUANWU_SECT_TECHNIQUE_ID = "xuanwu_guard_method";
    private static final String TIANJI_SECT_TECHNIQUE_ID = "tianji_star_method";
    private static final String TAIQING_SECT_TECHNIQUE_ID = "taiqing_clear_method";
    private static final String FIRE_SWORD_SPELL_ID = "fire_sword";
    private static final String PALM_THUNDER_SPELL_ID = "palm_thunder";
    private static final String FROST_BIND_SPELL_ID = "frost_bind";
    private static final String GREENWOOD_HEAL_SPELL_ID = "greenwood_heal";
    private static final String GOLDEN_LIGHT_SPELL_ID = "golden_light";
    private static final String GREEN_SWORD_ARTIFACT_ID = "green_sword";
    private static final String TURTLE_SHIELD_ARTIFACT_ID = "turtle_shield";
    private static final String SPIRIT_JADE_ARTIFACT_ID = "spirit_jade";
    private static final String WIND_THUNDER_BOOTS_ARTIFACT_ID = "wind_thunder_boots";
    private static final String TAIXU_CAULDRON_ARTIFACT_ID = "taixu_cauldron";

    private static final Logger LOG = Logger.getInstance(IdleCultivationManager.class);

    private static final int OFFLINE_CAP_HOURS = 8;
    public static final int MAX_ABODE_LEVEL = 5;
    private static final int[] SPIRIT_GATHERING_BONUS_PERCENT = {0, 10, 22, 36, 52, 70};
    private static final long[] SPIRIT_GATHERING_UPGRADE_COST = {160L, 420L, 900L, 1_650L, 2_700L};
    private static final long OFFLINE_CAP_MILLIS = TimeUnit.HOURS.toMillis(OFFLINE_CAP_HOURS);
    private static final long TICK_SECONDS = 10;
    private static final long SECLUSION_ONLINE_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(5);
    private static final long MEDITATION_COOLDOWN_MILLIS = TimeUnit.MINUTES.toMillis(10);
    private static final int REBIRTH_QI_BONUS_PERCENT = 25;
    private static final int REBIRTH_BREAKTHROUGH_BONUS_PERCENT = 15;
    private static final int REBIRTH_ATTACK_BONUS_PERCENT = 18;
    private static final int REBIRTH_DEFENSE_BONUS_PERCENT = 16;
    private static final int REBIRTH_MANA_BONUS_PERCENT = 20;
    // 悟道室只影响突破概率，翻倍后每级提供 6% 加成。
    private static final int INSIGHT_ROOM_BREAKTHROUGH_BONUS_PER_LEVEL = 6;
    private static final int MAX_TRAVEL_DURATION_REDUCTION_PERCENT = 50;
    private static final int MAX_EQUIPPED_SPELL_COUNT = 3;
    private static final int MAX_EQUIPPED_ARTIFACT_COUNT = 2;
    private static final int BATTLE_LOG_LIMIT = 80;
    private static final long BATTLE_TICK_SECONDS = 1;
    private static final int BATTLE_HEALTH_RECOVERY_DIVISOR = 250;
    private static final int BATTLE_MANA_RECOVERY_DIVISOR = 180;
    private static final long SPIRIT_VEIN_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(1);
    private static final long ALCHEMY_ROOM_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(3);
    private static final int MAX_PENDING_SECT_EVENTS = 3;
    private static final int ABODE_UNLOCK_REALM_INDEX = 2;
    private static final long SECT_SECRET_REALM_COOLDOWN_MILLIS = TimeUnit.MINUTES.toMillis(30);

    private static final List<TechniqueDefinition> TECHNIQUES = List.of(
            new TechniqueDefinition(BASIC_TECHNIQUE_ID, "cultivation.technique.basic.name", "cultivation.technique.basic.desc", 0, 0, 0, 0, 0, 0),
            new TechniqueDefinition(EVERGREEN_TECHNIQUE_ID, "cultivation.technique.evergreen.name", "cultivation.technique.evergreen.desc", 18, 0, 0, 0, 0, 0),
            new TechniqueDefinition(STONE_GATHERING_TECHNIQUE_ID, "cultivation.technique.stone.name", "cultivation.technique.stone.desc", 0, 25, 0, 0, 0, 0),
            new TechniqueDefinition(MYSTIC_ORTHODOX_TECHNIQUE_ID, "cultivation.technique.mystic.name", "cultivation.technique.mystic.desc", 12, 12, 6, 0, 0, 0),
            new TechniqueDefinition(SWORD_HEART_TECHNIQUE_ID, "cultivation.technique.swordHeart.name", "cultivation.technique.swordHeart.desc", 8, 0, 0, 16, 0, 8),
            new TechniqueDefinition(GOLDEN_BODY_TECHNIQUE_ID, "cultivation.technique.goldenBody.name", "cultivation.technique.goldenBody.desc", 0, 0, 4, 0, 18, 12),
            new TechniqueDefinition(QINGYUN_SECT_TECHNIQUE_ID, "cultivation.technique.qingyunSect.name", "cultivation.technique.qingyunSect.desc", 10, 0, 0, 22, 0, 10),
            new TechniqueDefinition(DANXIA_SECT_TECHNIQUE_ID, "cultivation.technique.danxiaSect.name", "cultivation.technique.danxiaSect.desc", 24, 6, 0, 0, 0, 0),
            new TechniqueDefinition(XUANWU_SECT_TECHNIQUE_ID, "cultivation.technique.xuanwuSect.name", "cultivation.technique.xuanwuSect.desc", 0, 0, 6, 0, 24, 16),
            new TechniqueDefinition(TIANJI_SECT_TECHNIQUE_ID, "cultivation.technique.tianjiSect.name", "cultivation.technique.tianjiSect.desc", 6, 32, 0, 0, 0, 0),
            new TechniqueDefinition(TAIQING_SECT_TECHNIQUE_ID, "cultivation.technique.taiqingSect.name", "cultivation.technique.taiqingSect.desc", 18, 16, 8, 0, 0, 0)
    );
    private static final Map<String, TechniqueDefinition> TECHNIQUE_BY_ID = indexTechniques();

    private static final List<PillDefinition> PILLS = List.of(
            new PillDefinition(QI_PILL_ID, "cultivation.pill.qi.name", "cultivation.pill.qi.desc"),
            new PillDefinition(SPIRIT_PILL_ID, "cultivation.pill.spirit.name", "cultivation.pill.spirit.desc"),
            new PillDefinition(BREAKTHROUGH_PILL_ID, "cultivation.pill.breakthrough.name", "cultivation.pill.breakthrough.desc"),
            new PillDefinition(MERIDIAN_PILL_ID, "cultivation.pill.meridian.name", "cultivation.pill.meridian.desc")
    );
    private static final Map<String, PillDefinition> PILL_BY_ID = indexPills();

    private static final List<SpellDefinition> SPELLS = List.of(
            new SpellDefinition(FIRE_SWORD_SPELL_ID, "cultivation.spell.fireSword.name", "cultivation.spell.fireSword.desc", 90, 6, BattleSpellType.DAMAGE, 100),
            new SpellDefinition(PALM_THUNDER_SPELL_ID, "cultivation.spell.palmThunder.name", "cultivation.spell.palmThunder.desc", 140, 10, BattleSpellType.DAMAGE, 160),
            new SpellDefinition(FROST_BIND_SPELL_ID, "cultivation.spell.frostBind.name", "cultivation.spell.frostBind.desc", 120, 12, BattleSpellType.FROST, 70),
            new SpellDefinition(GREENWOOD_HEAL_SPELL_ID, "cultivation.spell.greenwoodHeal.name", "cultivation.spell.greenwoodHeal.desc", 110, 14, BattleSpellType.HEAL, 16),
            new SpellDefinition(GOLDEN_LIGHT_SPELL_ID, "cultivation.spell.goldenLight.name", "cultivation.spell.goldenLight.desc", 100, 16, BattleSpellType.SHIELD, 45)
    );
    private static final Map<String, SpellDefinition> SPELL_BY_ID = indexSpells();

    private static final List<ArtifactDefinition> ARTIFACTS = List.of(
            new ArtifactDefinition(GREEN_SWORD_ARTIFACT_ID, "cultivation.artifact.greenSword.name", "cultivation.artifact.greenSword.desc", 14, 0, 0, 0),
            new ArtifactDefinition(TURTLE_SHIELD_ARTIFACT_ID, "cultivation.artifact.turtleShield.name", "cultivation.artifact.turtleShield.desc", 0, 14, 0, 0),
            new ArtifactDefinition(SPIRIT_JADE_ARTIFACT_ID, "cultivation.artifact.spiritJade.name", "cultivation.artifact.spiritJade.desc", 0, 0, 16, 6),
            new ArtifactDefinition(WIND_THUNDER_BOOTS_ARTIFACT_ID, "cultivation.artifact.windThunderBoots.name", "cultivation.artifact.windThunderBoots.desc", 6, 6, 0, 4),
            new ArtifactDefinition(TAIXU_CAULDRON_ARTIFACT_ID, "cultivation.artifact.taixuCauldron.name", "cultivation.artifact.taixuCauldron.desc", 8, 8, 8, 10)
    );
    private static final Map<String, ArtifactDefinition> ARTIFACT_BY_ID = indexArtifacts();

    private static final List<TravelLocationDefinition> TRAVEL_LOCATIONS = List.of(
            new TravelLocationDefinition("bamboo_forest", "cultivation.travel.bamboo.name", "cultivation.travel.bamboo.desc", 30, 0, 1_500, 45, 45, 8),
            new TravelLocationDefinition("abandoned_alchemy_room", "cultivation.travel.alchemy.name", "cultivation.travel.alchemy.desc", 60, 0, 4_200, 120, 70, 12),
            new TravelLocationDefinition("spirit_mine", "cultivation.travel.mine.name", "cultivation.travel.mine.desc", 120, 1, 14_000, 420, 40, 16),
            new TravelLocationDefinition("cloud_dream_secret", "cultivation.travel.secret.name", "cultivation.travel.secret.desc", 240, 2, 32_000, 900, 65, 30)
    );
    private static final Map<String, TravelLocationDefinition> TRAVEL_BY_ID = indexTravelLocations();

    private static final List<CultivatorDefinition> CULTIVATORS = List.of(
            new CultivatorDefinition("outer_sword_lin", "cultivation.cultivator.outerSwordLin.name", 1_000L, 100L, 70L, 100L, 600L, FIRE_SWORD_SPELL_ID, "", ""),
            new CultivatorDefinition("herbalist_chen", "cultivation.cultivator.herbalistChen.name", 1_700L, 170L, 120L, 160L, 900L, "", "", GREEN_SWORD_ARTIFACT_ID),
            new CultivatorDefinition("talisman_zhao", "cultivation.cultivator.talismanZhao.name", 2_600L, 260L, 190L, 250L, 1_200L, PALM_THUNDER_SPELL_ID, EVERGREEN_TECHNIQUE_ID, ""),
            new CultivatorDefinition("cold_shen", "cultivation.cultivator.coldShen.name", 4_200L, 390L, 300L, 380L, 1_600L, "", "", TURTLE_SHIELD_ARTIFACT_ID),
            new CultivatorDefinition("golden_core_han", "cultivation.cultivator.goldenCoreHan.name", 6_500L, 550L, 420L, 600L, 2_100L, FROST_BIND_SPELL_ID, STONE_GATHERING_TECHNIQUE_ID, ""),
            new CultivatorDefinition("armor_bai", "cultivation.cultivator.armorBai.name", 15_000L, 1_350L, 1_000L, 850L, 2_700L, "", "", SPIRIT_JADE_ARTIFACT_ID),
            new CultivatorDefinition("thunder_luo", "cultivation.cultivator.thunderLuo.name", 18_000L, 1_550L, 1_200L, 1_100L, 3_400L, GREENWOOD_HEAL_SPELL_ID, MYSTIC_ORTHODOX_TECHNIQUE_ID, ""),
            new CultivatorDefinition("illusion_su", "cultivation.cultivator.illusionSu.name", 22_000L, 1_800L, 1_450L, 1_350L, 4_200L, "", SWORD_HEART_TECHNIQUE_ID, WIND_THUNDER_BOOTS_ARTIFACT_ID),
            new CultivatorDefinition("mahayana_gu", "cultivation.cultivator.mahayanaGu.name", 28_000L, 2_200L, 1_750L, 1_600L, 5_100L, GOLDEN_LIGHT_SPELL_ID, GOLDEN_BODY_TECHNIQUE_ID, ""),
            new CultivatorDefinition("taixu_xuanheng", "cultivation.cultivator.taixuXuanheng.name", 35_000L, 2_700L, 2_150L, 1_900L, 6_200L, "", "", TAIXU_CAULDRON_ARTIFACT_ID)
    );
    private static final Map<String, CultivatorDefinition> CULTIVATOR_BY_ID = indexCultivators();

    private static final List<AbodeFacilityDefinition> ABODE_FACILITIES = List.of(
            new AbodeFacilityDefinition(SPIRIT_GATHERING_ARRAY_ID, "cultivation.abode.spiritGathering.name", "cultivation.abode.spiritGathering.desc", 180),
            new AbodeFacilityDefinition(SPIRIT_VEIN_ID, "cultivation.abode.spiritVein.name", "cultivation.abode.spiritVein.desc", 220),
            new AbodeFacilityDefinition(ALCHEMY_ROOM_ID, "cultivation.abode.alchemyRoom.name", "cultivation.abode.alchemyRoom.desc", 260),
            new AbodeFacilityDefinition(INSIGHT_ROOM_ID, "cultivation.abode.insightRoom.name", "cultivation.abode.insightRoom.desc", 300)
    );
    private static final Map<String, AbodeFacilityDefinition> ABODE_BY_ID = indexAbodeFacilities();

    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean notificationPending = new AtomicBoolean();
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> tickTask;
    private ScheduledFuture<?> battleTask;
    private boolean running;
    private String lastMessage;
    private BattleState battleState;
    private Random sectEventRandom;
    private Random sectSecretRealmRandom;

    public static IdleCultivationManager getInstance() {
        return ApplicationManager.getApplication().getService(IdleCultivationManager.class);
    }

    public IdleCultivationManager() {}

    void setSectEventRandomForTest(Random random) {
        this.sectEventRandom = random;
    }

    void setSectSecretRealmRandomForTest(Random random) {
        this.sectSecretRealmRandom = random;
    }

    public void addChangeListener(Runnable listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void fireChange() {
        if (!notificationPending.compareAndSet(false, true)) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            notificationPending.set(false);
            for (Runnable listener : listeners) {
                listener.run();
            }
        });
    }

    public synchronized void start() {
        ensureCultivationDefaults();
        if (running) {
            settleProgress(false);
            return;
        }
        running = true;
        LOG.info("start: starting idle cultivation manager");
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "IdleCultivationManager-pool");
            thread.setDaemon(true);
            return thread;
        });
        settleProgress(true);
        tickTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                settleProgress(false);
            } catch (Exception e) {
                LOG.warn("tick: failed to settle cultivation progress: " + e.getMessage());
            }
        }, TICK_SECONDS, TICK_SECONDS, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }
        settleProgress(false);
        running = false;
        LOG.info("stop: stopping idle cultivation manager");
        if (tickTask != null) {
            tickTask.cancel(false);
        }
        if (battleTask != null) {
            battleTask.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        tickTask = null;
        battleTask = null;
        battleState = null;
        scheduler = null;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public synchronized void meditateOnce() {
        settleProgress(false);
        long now = System.currentTimeMillis();
        if (!canMeditate(now)) {
            lastMessage = FishToucherBundle.message("cultivation.status.meditationCooldown", getMeditationRemainingText(now));
            fireChange();
            return;
        }
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        int realmIndex = settings.getCultivationRealmIndex();
        long qiGain = getManualQiGain(realmIndex);
        long actualGain = addCultivationQi(settings, qiGain);
        settings.setCultivationLastMeditationMillis(now);
        lastMessage = FishToucherBundle.message("cultivation.status.meditate", actualGain);
        fireChange();
    }

    public synchronized void receiveKoiBlessing() {
        settleProgress(false);
        long qiGain = 1L;
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        long actualGain = addCultivationQi(settings, qiGain);
        lastMessage = FishToucherBundle.message("cultivation.status.koiBlessing", actualGain);
        fireChange();
    }

    public synchronized void tryBreakthrough() {
        settleProgress(false);
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        int realmIndex = settings.getCultivationRealmIndex();
        if (isMaxRealm(realmIndex)) {
            lastMessage = FishToucherBundle.message("cultivation.status.maxRealm");
            fireChange();
            return;
        }

        long requiredQi = getRequiredQi(realmIndex);
        long currentQi = settings.getCultivationQi();
        if (currentQi < requiredQi) {
            lastMessage = FishToucherBundle.message("cultivation.status.needMore", requiredQi - currentQi);
            fireChange();
            return;
        }

        int successChance = getBreakthroughChance(realmIndex, settings.getCultivationBreakthroughFailures());
        boolean usedBreakthroughPill = settings.isBreakthroughPillActive();
        boolean usedMeridianPill = settings.isMeridianPillActive();
        boolean success = ThreadLocalRandom.current().nextInt(100) < successChance;
        settings.setBreakthroughPillActive(false);
        settings.setMeridianPillActive(false);

        if (success) {
            int nextRealm = realmIndex + 1;
            settings.setCultivationRealmIndex(nextRealm);
            settings.setCultivationQi(0L);
            settings.setCultivationBreakthroughFailures(0);
            lastMessage = FishToucherBundle.message("cultivation.status.breakthroughSuccess", getRealmName(nextRealm));
        } else {
            int retainPercent = usedMeridianPill ? 88 : 78;
            settings.setCultivationBreakthroughFailures(settings.getCultivationBreakthroughFailures() + 1);
            settings.setCultivationQi(requiredQi * retainPercent / 100L);
            lastMessage = usedMeridianPill
                    ? FishToucherBundle.message("cultivation.status.breakthroughProtected")
                    : FishToucherBundle.message("cultivation.status.breakthroughFailed");
        }

        if (usedBreakthroughPill && !success) {
            lastMessage = lastMessage + " " + FishToucherBundle.message("cultivation.status.buffConsumed");
        }
        fireChange();
    }

    public synchronized void settleProgress(boolean showOfflineMessage) {
        ensureCultivationDefaults();
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        long now = System.currentTimeMillis();
        long lastUpdate = settings.getCultivationLastUpdateMillis();
        if (lastUpdate <= 0L || lastUpdate > now) {
            settings.setCultivationLastUpdateMillis(now);
            return;
        }

        long elapsedMillis = now - lastUpdate;
        if (elapsedMillis < 1_000L) {
            return;
        }

        long creditedMillis = Math.min(elapsedMillis, OFFLINE_CAP_MILLIS);
        int realmIndex = settings.getCultivationRealmIndex();
        boolean offlineCatchUp = showOfflineMessage || elapsedMillis > SECLUSION_ONLINE_WINDOW_MILLIS;
        long passiveSeconds = creditedMillis / 1_000L;
        long seclusionSeconds = offlineCatchUp || isSeclusionPaused(settings) ? 0L : creditedMillis / 1_000L;
        long qiUnits = settings.getCultivationQiRemainderSeconds()
                + passiveSeconds * getPassiveQiPerMinute(realmIndex)
                + seclusionSeconds * getSeclusionQiPerMinute(realmIndex);
        long qiGain = qiUnits / 60L;
        settings.setCultivationQiRemainderSeconds(qiUnits % 60L);
        settings.setCultivationSpiritStoneRemainderSeconds(0L);
        boolean travelProgressed = advanceActiveTravel(settings, creditedMillis);
        boolean sectTaskProgressed = advanceActiveSectTask(settings, creditedMillis);

        long actualQiGain = 0L;
        if (qiGain > 0L) {
            actualQiGain = addCultivationQi(settings, qiGain);
        }
        settings.setCultivationLastUpdateMillis(now);

        if (actualQiGain > 0L || travelProgressed || sectTaskProgressed) {
            if (showOfflineMessage && elapsedMillis > TICK_SECONDS * 1_000L) {
                lastMessage = FishToucherBundle.message(
                        "cultivation.status.offline",
                        actualQiGain,
                        formatDuration(creditedMillis)
                );
            }
            fireChange();
        }
    }

    public synchronized boolean equipTechnique(String techniqueId) {
        ensureCultivationDefaults();
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        TechniqueDefinition technique = getTechnique(techniqueId);
        if (technique == null || !settings.isTechniqueUnlocked(techniqueId)) {
            lastMessage = FishToucherBundle.message("cultivation.status.techniqueLocked");
            fireChange();
            return false;
        }
        settings.setEquippedTechniqueId(techniqueId);
        lastMessage = FishToucherBundle.message("cultivation.status.techniqueEquipped", technique.name());
        fireChange();
        return true;
    }

    public synchronized boolean usePill(String pillId) {
        settleProgress(false);
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        PillDefinition pill = getPill(pillId);
        if (pill == null || settings.getPillCount(pillId) <= 0) {
            lastMessage = FishToucherBundle.message("cultivation.status.noPill");
            fireChange();
            return false;
        }

        int realmIndex = settings.getCultivationRealmIndex();
        switch (pillId) {
            case QI_PILL_ID -> {
                long gain = applyQiBonus(Math.max(300L, getRequiredQi(realmIndex) / 45L));
                long availableGain = getAvailableCultivationQiGain(settings, gain);
                if (availableGain <= 0L) {
                    lastMessage = FishToucherBundle.message(isMaxRealm(realmIndex)
                            ? "cultivation.status.maxRealm"
                            : "cultivation.status.ready");
                    fireChange();
                    return false;
                }
                if (!settings.consumePill(pillId)) return false;
                long actualGain = addCultivationQi(settings, gain);
                lastMessage = FishToucherBundle.message("cultivation.status.usedQiPill", actualGain);
            }
            case SPIRIT_PILL_ID -> {
                if (!settings.consumePill(pillId)) return false;
                long gain = 120L + Math.max(0, realmIndex) * 70L;
                settings.setCultivationSpiritStones(settings.getCultivationSpiritStones() + gain);
                lastMessage = FishToucherBundle.message("cultivation.status.usedSpiritPill", gain);
            }
            case BREAKTHROUGH_PILL_ID -> {
                if (settings.isBreakthroughPillActive()) {
                    lastMessage = FishToucherBundle.message("cultivation.status.buffAlreadyActive");
                    fireChange();
                    return false;
                }
                if (!settings.consumePill(pillId)) return false;
                settings.setBreakthroughPillActive(true);
                lastMessage = FishToucherBundle.message("cultivation.status.usedBreakthroughPill");
            }
            case MERIDIAN_PILL_ID -> {
                if (settings.isMeridianPillActive()) {
                    lastMessage = FishToucherBundle.message("cultivation.status.buffAlreadyActive");
                    fireChange();
                    return false;
                }
                if (!settings.consumePill(pillId)) return false;
                settings.setMeridianPillActive(true);
                lastMessage = FishToucherBundle.message("cultivation.status.usedMeridianPill");
            }
            default -> {
                return false;
            }
        }
        fireChange();
        return true;
    }

    public synchronized boolean startTravel(String locationId) {
        settleProgress(false);
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        TravelLocationDefinition location = getTravelLocation(locationId);
        if (location == null) {
            lastMessage = FishToucherBundle.message("cultivation.status.travelUnknown");
            fireChange();
            return false;
        }
        if (hasActiveBattle()) {
            lastMessage = FishToucherBundle.message("cultivation.status.travelBlockedByChallenge");
            fireChange();
            return false;
        }
        if (hasActiveTravel()) {
            lastMessage = FishToucherBundle.message("cultivation.status.travelBusy");
            fireChange();
            return false;
        }
        if (hasActiveSectSecretRealm()) {
            lastMessage = FishToucherBundle.message("cultivation.sect.secretRealmBusy");
            fireChange();
            return false;
        }
        if (!isTravelUnlocked(location)) {
            lastMessage = FishToucherBundle.message("cultivation.status.travelLocked", getRealmName(location.minRealmIndex()));
            fireChange();
            return false;
        }

        long now = System.currentTimeMillis();
        settings.setActiveTravelLocationId(location.id());
        settings.setTravelStartMillis(now);
        settings.setTravelEndMillis(0L);
        settings.setActiveTravelElapsedMillis(0L);
        lastMessage = FishToucherBundle.message("cultivation.status.travelStarted", location.name(), getTravelDurationMinutes(location));
        fireChange();
        return true;
    }

    public synchronized TravelReward claimTravelReward() {
        settleProgress(false);
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        TravelLocationDefinition location = getActiveTravelLocation();
        if (location == null) {
            lastMessage = FishToucherBundle.message("cultivation.status.travelNone");
            fireChange();
            return TravelReward.empty();
        }
        if (!isTravelReady()) {
            lastMessage = FishToucherBundle.message("cultivation.status.travelNotReady", getTravelRemainingText());
            fireChange();
            return TravelReward.empty();
        }

        int realmIndex = settings.getCultivationRealmIndex();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        long qiGain = applyQiBonus(Math.round(location.baseQiReward() * (1.0 + realmIndex * 0.04)));
        long stoneGain = applyStoneBonus(Math.round(location.baseStoneReward() * (1.0 + realmIndex * 0.08)));
        String pillId = "";
        int pillCount = 0;
        if (random.nextInt(100) < location.pillChance()) {
            pillId = choosePillForLocation(location.id());
            pillCount = random.nextInt(100) < 18 ? 2 : 1;
            settings.addPill(pillId, pillCount);
        }

        String techniqueId = "";
        boolean duplicateTechnique = false;
        if (random.nextInt(100) < location.techniqueChance()) {
            techniqueId = chooseTechniqueForTravel();
            if (!techniqueId.isEmpty()) {
                boolean unlocked = settings.unlockTechnique(techniqueId);
                duplicateTechnique = !unlocked;
                if (duplicateTechnique) {
                    stoneGain += applyStoneBonus(120L + realmIndex * 40L);
                }
            }
        }

        String spellId = "";
        boolean duplicateSpell = false;
        if (random.nextInt(100) < getTravelSpellChance(location.id())) {
            spellId = chooseSpellForTravel();
            if (!spellId.isEmpty()) {
                boolean unlocked = settings.unlockSpell(spellId);
                duplicateSpell = !unlocked;
                if (duplicateSpell) {
                    stoneGain += applyStoneBonus(160L + realmIndex * 50L);
                }
            }
        }

        qiGain = addCultivationQi(settings, qiGain);
        settings.setCultivationSpiritStones(settings.getCultivationSpiritStones() + stoneGain);
        settings.clearTravel();
        TravelReward reward = new TravelReward(qiGain, stoneGain, pillId, pillCount, techniqueId, duplicateTechnique, spellId, duplicateSpell);
        lastMessage = reward.summary();
        fireChange();
        return reward;
    }

    public synchronized boolean canRebirth() {
        return isMaxRealm(NovelReaderSettings.getInstance().getCultivationRealmIndex());
    }

    public synchronized boolean rebirth(String ignoredTechniqueId) {
        settleProgress(false);
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        if (!canRebirth()) {
            lastMessage = FishToucherBundle.message("cultivation.status.rebirthUnavailable");
            fireChange();
            return false;
        }

        int nextRebirthCount = settings.getCultivationRebirthCount() + 1;
        long now = System.currentTimeMillis();
        settings.setCultivationRebirthCount(nextRebirthCount);
        settings.setCultivationRealmIndex(0);
        settings.setCultivationQi(0L);
        settings.setCultivationSpiritStones(0L);
        settings.setCultivationQiRemainderSeconds(0L);
        settings.setCultivationSpiritStoneRemainderSeconds(0L);
        settings.setCultivationBreakthroughFailures(0);
        settings.setCultivationLastUpdateMillis(now);
        settings.setCultivationLastMeditationMillis(0L);
        settings.setBreakthroughPillActive(false);
        settings.setMeridianPillActive(false);
        settings.clearTravel();
        settings.clearSectSecretRealmProgress();
        settings.clearPillInventory();
        settings.clearAbodeState();
        ensureCultivationDefaults();

        lastMessage = FishToucherBundle.message(
                "cultivation.status.rebirthSuccess",
                nextRebirthCount
        );
        fireChange();
        return true;
    }

    public synchronized CombatStats getCombatStats() {
        return calculateCombatStats();
    }

    public synchronized long getHealthRecoveryPerSecond() {
        CombatStats stats = hasActiveBattle() ? battleState.playerStats : calculateCombatStats();
        return calculateBattleHealthRecovery(stats);
    }

    public synchronized long getManaRecoveryPerSecond() {
        CombatStats stats = hasActiveBattle() ? battleState.playerStats : calculateCombatStats();
        return calculateBattleManaRecovery(stats);
    }

    public List<SpellDefinition> getSpellDefinitions() {
        return SPELLS;
    }

    public SpellDefinition getSpell(String id) {
        return SPELL_BY_ID.get(id);
    }

    public synchronized List<SpellDefinition> getEquippedSpellDefinitions() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        return settings.getEquippedSpellIds().stream()
                .map(this::getSpell)
                .filter(spell -> spell != null)
                .toList();
    }

    public synchronized boolean equipSpells(List<String> spellIds) {
        ensureCultivationDefaults();
        List<String> validSpellIds = new ArrayList<>();
        if (spellIds != null) {
            NovelReaderSettings settings = NovelReaderSettings.getInstance();
            for (String spellId : spellIds) {
                if (spellId != null
                        && SPELL_BY_ID.containsKey(spellId)
                        && settings.isSpellUnlocked(spellId)
                        && !validSpellIds.contains(spellId)
                        && validSpellIds.size() < MAX_EQUIPPED_SPELL_COUNT) {
                    validSpellIds.add(spellId);
                }
            }
        }
        NovelReaderSettings.getInstance().setEquippedSpellIds(validSpellIds);
        lastMessage = FishToucherBundle.message("cultivation.status.spellsEquipped", validSpellIds.size(), MAX_EQUIPPED_SPELL_COUNT);
        fireChange();
        return true;
    }

    public List<ArtifactDefinition> getArtifactDefinitions() {
        return ARTIFACTS;
    }

    public ArtifactDefinition getArtifact(String id) {
        return ARTIFACT_BY_ID.get(id);
    }

    public synchronized List<ArtifactDefinition> getEquippedArtifactDefinitions() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        return settings.getEquippedArtifactIds().stream()
                .map(this::getArtifact)
                .filter(artifact -> artifact != null)
                .toList();
    }

    public synchronized boolean equipArtifacts(List<String> artifactIds) {
        ensureCultivationDefaults();
        List<String> validArtifactIds = new ArrayList<>();
        if (artifactIds != null) {
            NovelReaderSettings settings = NovelReaderSettings.getInstance();
            for (String artifactId : artifactIds) {
                if (artifactId != null
                        && ARTIFACT_BY_ID.containsKey(artifactId)
                        && settings.isArtifactUnlocked(artifactId)
                        && !validArtifactIds.contains(artifactId)
                        && validArtifactIds.size() < MAX_EQUIPPED_ARTIFACT_COUNT) {
                    validArtifactIds.add(artifactId);
                }
            }
        }
        NovelReaderSettings.getInstance().setEquippedArtifactIds(validArtifactIds);
        lastMessage = FishToucherBundle.message("cultivation.status.artifactsEquipped", validArtifactIds.size(), MAX_EQUIPPED_ARTIFACT_COUNT);
        fireChange();
        return true;
    }

    public List<SectCatalog.SectDefinition> getSectDefinitions() {
        return SectCatalog.sects();
    }

    public List<SectCatalog.SectTaskDefinition> getSectTaskDefinitions() {
        return SectCatalog.tasks();
    }

    public List<SectCatalog.SectInheritanceDefinition> getSectInheritanceDefinitions() {
        return SectCatalog.inheritances();
    }

    public List<SectCatalog.SectTrialDefinition> getSectTrialDefinitions() {
        return SectCatalog.trials();
    }

    public List<SectCatalog.SectEventDefinition> getSectEventDefinitions() {
        return SectCatalog.events();
    }

    public synchronized List<SectCatalog.SectSecretRealmDefinition> getCurrentSectSecretRealmDefinitions() {
        SectCatalog.SectDefinition sect = getCurrentSect();
        if (sect == null) {
            return Collections.emptyList();
        }
        List<SectCatalog.SectSecretRealmDefinition> result = new ArrayList<>();
        for (SectCatalog.SectSecretRealmDefinition secretRealm : SectCatalog.secretRealms()) {
            if (sect.id().equals(secretRealm.sectId())) {
                result.add(secretRealm);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized SectCatalog.SectSecretRealmDefinition getActiveSectSecretRealm() {
        purgeInvalidSectSecretRealm(NovelReaderSettings.getInstance());
        return SectCatalog.secretRealm(NovelReaderSettings.getInstance().getActiveSectSecretRealmId());
    }

    public synchronized boolean hasActiveSectSecretRealm() {
        return getActiveSectSecretRealm() != null;
    }

    public synchronized boolean canStartSectSecretRealm(String secretRealmId) {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        SectCatalog.SectSecretRealmDefinition secretRealm = SectCatalog.secretRealm(secretRealmId);
        return secretRealm != null
                && secretRealm.sectId().equals(settings.getCultivationSectId())
                && settings.getCurrentSectRankIndex() >= secretRealm.minRankIndex()
                && settings.getSectSecretRealmCooldownUntilMillis() <= System.currentTimeMillis()
                && !hasActiveSectSecretRealm()
                && !hasActiveSectTask()
                && !hasActiveTravel()
                && !hasActiveBattle();
    }

    public synchronized boolean startSectSecretRealm(String secretRealmId) {
        settleProgress(false);
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        SectCatalog.SectSecretRealmDefinition secretRealm = SectCatalog.secretRealm(secretRealmId);
        if (secretRealm == null || !secretRealm.sectId().equals(settings.getCultivationSectId())) {
            lastMessage = FishToucherBundle.message("cultivation.sect.secretRealmUnknown");
            fireChange();
            return false;
        }
        if (settings.getCurrentSectRankIndex() < secretRealm.minRankIndex()) {
            lastMessage = FishToucherBundle.message("cultivation.sect.secretRealmLocked", SectCatalog.rank(secretRealm.minRankIndex()).name());
            fireChange();
            return false;
        }
        if (settings.getSectSecretRealmCooldownUntilMillis() > System.currentTimeMillis()) {
            lastMessage = FishToucherBundle.message("cultivation.sect.secretRealmCooldown", getSectSecretRealmCooldownText());
            fireChange();
            return false;
        }
        if (hasActiveSectTask() || hasActiveTravel() || hasActiveBattle() || hasActiveSectSecretRealm()) {
            lastMessage = FishToucherBundle.message("cultivation.sect.secretRealmActivityBusy");
            fireChange();
            return false;
        }
        settings.clearSectSecretRealmProgress();
        settings.setActiveSectSecretRealmId(secretRealm.id());
        settings.setSectSecretRealmNodeIndex(0);
        settings.setSectSecretRealmStartedMillis(System.currentTimeMillis());
        lastMessage = FishToucherBundle.message("cultivation.sect.secretRealmStarted", secretRealm.name());
        fireChange();
        return true;
    }

    public synchronized SectCatalog.SectSecretRealmNodeDefinition getSectSecretRealmCurrentNode() {
        SectCatalog.SectSecretRealmDefinition secretRealm = getActiveSectSecretRealm();
        if (secretRealm == null || secretRealm.nodes().isEmpty()) {
            return null;
        }
        int nodeIndex = NovelReaderSettings.getInstance().getSectSecretRealmNodeIndex();
        if (nodeIndex < 0 || nodeIndex >= secretRealm.nodes().size()) {
            return null;
        }
        return secretRealm.nodes().get(nodeIndex);
    }

    public synchronized boolean resolveSectSecretRealmNode(String optionId) {
        settleProgress(false);
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        SectCatalog.SectSecretRealmDefinition secretRealm = getActiveSectSecretRealm();
        SectCatalog.SectSecretRealmNodeDefinition node = getSectSecretRealmCurrentNode();
        if (secretRealm == null || node == null) {
            lastMessage = FishToucherBundle.message("cultivation.sect.secretRealmNone");
            fireChange();
            return false;
        }

        String resultText = switch (node.type()) {
            case ENTRY -> {
                advanceSectSecretRealmNode(settings, node);
                yield FishToucherBundle.message("cultivation.sect.secretRealmEntered", node.title());
            }
            case BATTLE, BOSS -> resolveSectSecretRealmBattle(settings, secretRealm, node);
            case CHOICE -> resolveSectSecretRealmChoice(settings, node, optionId);
            case CHEST -> resolveSectSecretRealmChest(settings, node);
        };
        lastMessage = resultText;
        fireChange();
        return true;
    }

    public synchronized String getSectSecretRealmStatusText() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        SectCatalog.SectDefinition sect = getCurrentSect();
        if (sect == null) {
            return "";
        }
        List<SectCatalog.SectSecretRealmDefinition> secretRealms = getCurrentSectSecretRealmDefinitions();
        if (secretRealms.isEmpty()) {
            return "";
        }
        SectCatalog.SectSecretRealmDefinition secretRealm = getActiveSectSecretRealm();
        if (secretRealm == null) {
            SectCatalog.SectSecretRealmDefinition first = secretRealms.get(0);
            if (settings.getCurrentSectRankIndex() < first.minRankIndex()) {
                return FishToucherBundle.message("cultivation.sect.secretRealmLocked", SectCatalog.rank(first.minRankIndex()).name());
            }
            long cooldownUntilMillis = settings.getSectSecretRealmCooldownUntilMillis();
            if (cooldownUntilMillis > System.currentTimeMillis()) {
                return FishToucherBundle.message("cultivation.sect.secretRealmCooldown", getSectSecretRealmCooldownText());
            }
            return FishToucherBundle.message("cultivation.sect.secretRealmReady");
        }
        SectCatalog.SectSecretRealmNodeDefinition node = getSectSecretRealmCurrentNode();
        if (node == null) {
            return FishToucherBundle.message("cultivation.sect.secretRealmNone");
        }
        return FishToucherBundle.message(
                "cultivation.sect.secretRealmProgress",
                secretRealm.name(),
                settings.getSectSecretRealmNodeIndex() + 1,
                secretRealm.nodes().size(),
                node.title()
        );
    }

    public synchronized String getSectSecretRealmCooldownText() {
        long remainingMillis = NovelReaderSettings.getInstance().getSectSecretRealmCooldownUntilMillis() - System.currentTimeMillis();
        return remainingMillis <= 0L
                ? FishToucherBundle.message("cultivation.sect.secretRealmReady")
                : formatRemainingDuration(remainingMillis);
    }

    public synchronized boolean isSectUnlocked() {
        return SectRules.isSectUnlocked(NovelReaderSettings.getInstance().getCultivationRealmIndex());
    }

    public synchronized SectCatalog.SectDefinition getCurrentSect() {
        return SectCatalog.sect(NovelReaderSettings.getInstance().getCultivationSectId());
    }

    public synchronized String getCurrentSectTitle() {
        SectCatalog.SectDefinition sect = getCurrentSect();
        if (sect == null) {
            return FishToucherBundle.message("cultivation.sect.none");
        }
        return sect.name() + " · " + SectRules.currentRank(NovelReaderSettings.getInstance()).name();
    }

    public synchronized String getSectProgressText() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        SectCatalog.SectDefinition sect = getCurrentSect();
        if (sect == null) {
            return isSectUnlocked()
                    ? FishToucherBundle.message("cultivation.sect.chooseHint")
                    : FishToucherBundle.message("cultivation.sect.locked", getRealmName(SectCatalog.UNLOCK_REALM_INDEX));
        }
        SectCatalog.SectRankDefinition nextRank = SectRules.nextRank(settings);
        String next = nextRank == null
                ? FishToucherBundle.message("cultivation.sect.rankMax")
                : FishToucherBundle.message("cultivation.sect.nextRank", nextRank.name(), nextRank.prestigeRequired(), getRealmName(nextRank.realmRequired()));
        return FishToucherBundle.message(
                "cultivation.sect.progress",
                sect.bonusText(),
                settings.getCurrentSectPrestige(),
                settings.getCurrentSectContribution(),
                next
        );
    }

    public synchronized List<SectEventInstance> getPendingSectEvents() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        purgeInvalidSectEvents(settings);
        String currentSectId = settings.getCultivationSectId();
        if (currentSectId.isEmpty()) {
            return Collections.emptyList();
        }
        List<SectEventInstance> result = new ArrayList<>();
        for (NovelReaderSettings.SectPendingEventState eventState : settings.getPendingSectEvents()) {
            if (!currentSectId.equals(eventState.sectId)) {
                continue;
            }
            SectCatalog.SectEventDefinition event = SectCatalog.event(eventState.eventId);
            if (event != null && SectCatalog.sect(eventState.sectId) != null) {
                result.add(new SectEventInstance(eventState.instanceId, eventState.sectId, eventState.createdMillis, event));
            }
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized SectEventInstance getCurrentSectEvent() {
        List<SectEventInstance> events = getPendingSectEvents();
        return events.isEmpty() ? null : events.get(0);
    }

    public synchronized boolean canResolveSectEvent(String instanceId, String optionId) {
        SectEventInstance event = findCurrentSectEvent(instanceId);
        if (event == null || findEventOption(event.event, optionId) == null) {
            return false;
        }
        return !("junior_help".equals(event.event.id()) && "give_qi_pill".equals(optionId)
                && NovelReaderSettings.getInstance().getPillCount(QI_PILL_ID) <= 0);
    }

    public synchronized boolean resolveSectEvent(String instanceId, String optionId) {
        settleProgress(false);
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        SectEventInstance event = findCurrentSectEvent(instanceId);
        if (event == null) {
            lastMessage = FishToucherBundle.message("cultivation.sect.eventUnknown");
            fireChange();
            return false;
        }
        SectCatalog.SectEventOptionDefinition option = findEventOption(event.event, optionId);
        if (option == null) {
            lastMessage = FishToucherBundle.message("cultivation.sect.eventOptionUnknown");
            fireChange();
            return false;
        }
        if (!canResolveSectEvent(instanceId, optionId)) {
            lastMessage = FishToucherBundle.message("cultivation.sect.eventOptionBlocked");
            fireChange();
            return false;
        }
        String rewardText = applySectEventReward(settings, event.event.id(), optionId);
        settings.removePendingSectEvent(instanceId);
        lastMessage = rewardText.isEmpty()
                ? FishToucherBundle.message("cultivation.sect.eventResolvedNone", event.event.title(), option.label())
                : FishToucherBundle.message("cultivation.sect.eventResolved", event.event.title(), option.label(), rewardText);
        fireChange();
        return true;
    }

    public synchronized boolean joinSect(String sectId) {
        settleProgress(false);
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        SectCatalog.SectDefinition sect = SectCatalog.sect(sectId);
        if (!isSectUnlocked()) {
            lastMessage = FishToucherBundle.message("cultivation.sect.locked", getRealmName(SectCatalog.UNLOCK_REALM_INDEX));
            fireChange();
            return false;
        }
        if (sect == null) {
            lastMessage = FishToucherBundle.message("cultivation.sect.unknown");
            fireChange();
            return false;
        }
        settings.clearSectTask();
        settings.clearSectSecretRealmProgress();
        settings.setCultivationSectId(sect.id());
        settings.setCurrentSectRankIndex(settings.getSectRankIndex(sect.id()));
        lastMessage = FishToucherBundle.message("cultivation.sect.joined", sect.name());
        fireChange();
        return true;
    }

    public synchronized boolean leaveSect() {
        settleProgress(false);
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        SectCatalog.SectDefinition sect = getCurrentSect();
        if (sect == null) {
            lastMessage = FishToucherBundle.message("cultivation.sect.none");
            fireChange();
            return false;
        }
        settings.clearSectTask();
        settings.clearSectSecretRealmProgress();
        settings.clearCurrentSectContribution();
        settings.setCultivationSectId("");
        lastMessage = FishToucherBundle.message("cultivation.sect.left", sect.name());
        fireChange();
        return true;
    }

    public synchronized boolean canPromoteSectRank() {
        return SectRules.canPromote(NovelReaderSettings.getInstance());
    }

    public synchronized boolean promoteSectRank() {
        settleProgress(false);
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        if (!canPromoteSectRank()) {
            lastMessage = FishToucherBundle.message("cultivation.sect.promoteBlocked");
            fireChange();
            return false;
        }
        SectCatalog.SectRankDefinition nextRank = SectRules.nextRank(settings);
        settings.setCurrentSectRankIndex(nextRank.rankIndex());
        lastMessage = FishToucherBundle.message("cultivation.sect.promoted", nextRank.name());
        fireChange();
        return true;
    }

    public synchronized boolean startSectTask(String taskId) {
        settleProgress(false);
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        SectCatalog.SectTaskDefinition task = SectCatalog.task(taskId);
        if (getCurrentSect() == null) {
            lastMessage = FishToucherBundle.message("cultivation.sect.needJoin");
            fireChange();
            return false;
        }
        if (task == null) {
            lastMessage = FishToucherBundle.message("cultivation.sect.taskUnknown");
            fireChange();
            return false;
        }
        if (hasActiveSectTask()) {
            lastMessage = FishToucherBundle.message("cultivation.sect.taskBusy");
            fireChange();
            return false;
        }
        if (hasActiveSectSecretRealm()) {
            lastMessage = FishToucherBundle.message("cultivation.sect.secretRealmBusy");
            fireChange();
            return false;
        }
        if (!SectRules.isTaskUnlocked(settings, task)) {
            lastMessage = FishToucherBundle.message("cultivation.sect.taskLocked");
            fireChange();
            return false;
        }
        long now = System.currentTimeMillis();
        settings.setActiveSectTaskId(task.id());
        settings.setSectTaskStartMillis(now);
        settings.setSectTaskEndMillis(0L);
        settings.setActiveSectTaskElapsedMillis(0L);
        lastMessage = FishToucherBundle.message("cultivation.sect.taskStarted", task.name(), getSectTaskDurationMinutes(task));
        fireChange();
        return true;
    }

    public synchronized boolean hasActiveSectTask() {
        return SectCatalog.task(NovelReaderSettings.getInstance().getActiveSectTaskId()) != null;
    }

    public synchronized SectCatalog.SectTaskDefinition getActiveSectTask() {
        return SectCatalog.task(NovelReaderSettings.getInstance().getActiveSectTaskId());
    }

    public synchronized boolean isSectTaskReady() {
        SectCatalog.SectTaskDefinition task = getActiveSectTask();
        return task != null && NovelReaderSettings.getInstance().getActiveSectTaskElapsedMillis() >= getSectTaskDurationMillis(task);
    }

    public synchronized int getSectTaskProgressPercent() {
        SectCatalog.SectTaskDefinition task = getActiveSectTask();
        if (task == null) {
            return 0;
        }
        long total = Math.max(1L, getSectTaskDurationMillis(task));
        long done = Math.max(0L, NovelReaderSettings.getInstance().getActiveSectTaskElapsedMillis());
        return (int) Math.min(100L, done * 100L / total);
    }

    public synchronized String getSectTaskRemainingText() {
        SectCatalog.SectTaskDefinition task = getActiveSectTask();
        if (task == null) {
            return FishToucherBundle.message("cultivation.sect.taskNone");
        }
        if (isSectTaskReady()) {
            return FishToucherBundle.message("cultivation.sect.taskClaimReady");
        }
        long remaining = Math.max(0L, getSectTaskDurationMillis(task) - NovelReaderSettings.getInstance().getActiveSectTaskElapsedMillis());
        return formatRemainingDuration(remaining);
    }

    public synchronized String getSectTaskDescription(SectCatalog.SectTaskDefinition task) {
        if (task == null) {
            return "";
        }
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        long durationMinutes = getSectTaskDurationMinutes(task, settings);
        int reductionPercent = getSectTaskDurationReductionPercent(settings);
        if (reductionPercent > 0) {
            return FishToucherBundle.message(
                    "cultivation.sect.taskDescReduced",
                    durationMinutes,
                    reductionPercent,
                    task.contributionReward(),
                    task.prestigeReward(),
                    task.extraText(),
                    SectCatalog.rank(task.minRankIndex()).name()
            );
        }
        return FishToucherBundle.message(
                "cultivation.sect.taskDesc",
                durationMinutes,
                task.contributionReward(),
                task.prestigeReward(),
                task.extraText(),
                SectCatalog.rank(task.minRankIndex()).name()
        );
    }

    public synchronized boolean claimSectTask() {
        settleProgress(false);
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        SectCatalog.SectTaskDefinition task = getActiveSectTask();
        if (task == null) {
            lastMessage = FishToucherBundle.message("cultivation.sect.taskNone");
            fireChange();
            return false;
        }
        if (!isSectTaskReady()) {
            lastMessage = FishToucherBundle.message("cultivation.sect.taskNotReady", getSectTaskRemainingText());
            fireChange();
            return false;
        }
        long qiGain = addCultivationQi(settings, applyQiBonus(task.qiReward()));
        long stoneGain = applyStoneBonus(task.contributionReward() * 4L);
        settings.addCurrentSectContribution(task.contributionReward());
        settings.addCurrentSectPrestige(task.prestigeReward());
        settings.setCultivationSpiritStones(settings.getCultivationSpiritStones() + stoneGain);
        if (SectRules.currentSectBonus(settings, SectCatalog.BonusType.PILL) > 0 && task.durationMinutes() >= 60) {
            settings.addPill(QI_PILL_ID, 1);
        }
        settings.clearSectTask();
        boolean eventGenerated = maybeGenerateSectEvent(task.id());
        lastMessage = FishToucherBundle.message("cultivation.sect.taskClaimed", task.contributionReward(), task.prestigeReward(), qiGain, stoneGain);
        if (eventGenerated) {
            lastMessage = lastMessage + " " + FishToucherBundle.message("cultivation.sect.eventGenerated");
        }
        fireChange();
        return true;
    }

    public synchronized boolean maybeGenerateSectEvent(String taskId) {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        purgeInvalidSectEvents(settings);
        String currentSectId = settings.getCultivationSectId();
        SectCatalog.SectTaskDefinition task = SectCatalog.task(taskId);
        if (currentSectId.isEmpty() || SectCatalog.sect(currentSectId) == null || task == null) {
            return false;
        }
        if (settings.getPendingSectEvents().size() >= MAX_PENDING_SECT_EVENTS) {
            return false;
        }
        int chance = "explore_secret".equals(task.id()) ? 45 : 25;
        if (nextSectEventInt(100) >= chance) {
            return false;
        }
        List<SectCatalog.SectEventDefinition> events = SectCatalog.events();
        if (events.isEmpty()) {
            return false;
        }
        SectCatalog.SectEventDefinition event = events.get(nextSectEventInt(events.size()));
        return settings.addPendingSectEvent(
                UUID.randomUUID().toString(),
                event.id(),
                currentSectId,
                System.currentTimeMillis()
        );
    }

    public synchronized boolean canPurchaseSectInheritance(SectCatalog.SectInheritanceDefinition inheritance) {
        return SectRules.canPurchaseInheritance(NovelReaderSettings.getInstance(), inheritance);
    }

    public synchronized boolean purchaseSectInheritance(String inheritanceId) {
        settleProgress(false);
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        SectCatalog.SectInheritanceDefinition inheritance = SectCatalog.inheritance(inheritanceId);
        if (inheritance == null) {
            lastMessage = FishToucherBundle.message("cultivation.sect.inheritanceUnknown");
            fireChange();
            return false;
        }
        if (!SectRules.isInheritanceUnlocked(settings, inheritance)) {
            lastMessage = FishToucherBundle.message("cultivation.sect.inheritanceLocked");
            fireChange();
            return false;
        }
        if (settings.isSectInheritanceLearned(inheritance.id())) {
            lastMessage = FishToucherBundle.message("cultivation.sect.inheritanceLearned", inheritance.name());
            fireChange();
            return false;
        }
        if (!settings.spendCurrentSectContribution(inheritance.contributionCost())) {
            lastMessage = FishToucherBundle.message("cultivation.sect.contributionNotEnough", inheritance.contributionCost());
            fireChange();
            return false;
        }
        grantSectInheritance(settings, inheritance);
        settings.markSectInheritanceLearned(inheritance.id());
        lastMessage = FishToucherBundle.message("cultivation.sect.inheritancePurchased", inheritance.name());
        fireChange();
        return true;
    }

    public synchronized boolean startSectTrial(String trialId) {
        settleProgress(false);
        if (!running) {
            start();
        }
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        SectCatalog.SectTrialDefinition trial = SectCatalog.trial(trialId);
        if (trial == null) {
            lastMessage = FishToucherBundle.message("cultivation.sect.trialUnknown");
            fireChange();
            return false;
        }
        if (!SectRules.isTrialUnlocked(settings, trial)) {
            lastMessage = FishToucherBundle.message("cultivation.sect.trialLocked");
            fireChange();
            return false;
        }
        if (settings.isSectTrialDefeated(trial.id())) {
            lastMessage = FishToucherBundle.message("cultivation.sect.trialDefeated");
            fireChange();
            return false;
        }
        if (hasActiveBattle()) {
            lastMessage = FishToucherBundle.message("cultivation.status.challengeBusy");
            fireChange();
            return false;
        }
        if (hasActiveTravel()) {
            lastMessage = FishToucherBundle.message("cultivation.status.challengeBlockedByTravel");
            fireChange();
            return false;
        }
        if (hasActiveSectSecretRealm()) {
            lastMessage = FishToucherBundle.message("cultivation.sect.secretRealmBusy");
            fireChange();
            return false;
        }
        CultivatorDefinition enemy = new CultivatorDefinition(
                "sect_trial_" + trial.id(),
                trial.enemyName(),
                trial.maxHealth(),
                trial.attack(),
                trial.defense(),
                trial.mana(),
                trial.stoneReward(),
                "",
                "",
                ""
        );
        battleState = new BattleState(enemy, calculateCombatStats(), getEquippedSpellDefinitions(), trial.id());
        addBattleLog(battleState, FishToucherBundle.message("cultivation.battle.log.started", enemy.name()));
        lastMessage = FishToucherBundle.message("cultivation.sect.trialStarted", enemy.name());
        if (battleTask != null) {
            battleTask.cancel(false);
        }
        if (scheduler != null) {
            battleTask = scheduler.scheduleAtFixedRate(() -> {
                try {
                    advanceBattle();
                } catch (Exception e) {
                    LOG.warn("battle: failed to advance sect trial: " + e.getMessage());
                }
            }, BATTLE_TICK_SECONDS, BATTLE_TICK_SECONDS, TimeUnit.SECONDS);
        }
        fireChange();
        return true;
    }

    public List<CultivatorDefinition> getCultivatorDefinitions() {
        return CULTIVATORS;
    }

    public CultivatorDefinition getCultivator(String id) {
        return CULTIVATOR_BY_ID.get(id);
    }

    public synchronized boolean isCultivatorDefeated(CultivatorDefinition cultivator) {
        return cultivator != null && NovelReaderSettings.getInstance().isCultivatorDefeated(cultivator.id());
    }

    public synchronized boolean isCultivatorUnlocked(CultivatorDefinition cultivator) {
        if (cultivator == null) {
            return false;
        }
        int index = CULTIVATORS.indexOf(cultivator);
        if (index <= 0) {
            return true;
        }
        return NovelReaderSettings.getInstance().isCultivatorDefeated(CULTIVATORS.get(index - 1).id());
    }

    public synchronized String getCultivatorStatusText(CultivatorDefinition cultivator) {
        if (isCultivatorDefeated(cultivator)) {
            return FishToucherBundle.message("cultivation.status.challengeDefeated");
        }
        if (!isCultivatorUnlocked(cultivator)) {
            return FishToucherBundle.message("cultivation.status.challengeLocked");
        }
        return FishToucherBundle.message("cultivation.status.challengeAvailable");
    }

    public synchronized String getCultivatorRewardText(CultivatorDefinition cultivator) {
        if (cultivator == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        parts.add(FishToucherBundle.message("cultivation.reward.stones", applyStoneBonus(cultivator.stoneReward())));
        if (!cultivator.spellRewardId().isEmpty()) {
            SpellDefinition spell = getSpell(cultivator.spellRewardId());
            if (spell != null) {
                parts.add(FishToucherBundle.message("cultivation.reward.spell", spell.name()));
            }
        }
        if (!cultivator.techniqueRewardId().isEmpty()) {
            TechniqueDefinition technique = getTechnique(cultivator.techniqueRewardId());
            if (technique != null) {
                parts.add(FishToucherBundle.message("cultivation.reward.technique", technique.name()));
            }
        }
        if (!cultivator.artifactRewardId().isEmpty()) {
            ArtifactDefinition artifact = getArtifact(cultivator.artifactRewardId());
            if (artifact != null) {
                parts.add(FishToucherBundle.message("cultivation.reward.artifact", artifact.name()));
            }
        }
        return String.join(", ", parts);
    }

    public synchronized boolean startChallenge(String cultivatorId) {
        settleProgress(false);
        if (!running) {
            start();
        }
        CultivatorDefinition cultivator = getCultivator(cultivatorId);
        if (cultivator == null) {
            lastMessage = FishToucherBundle.message("cultivation.status.challengeUnknown");
            fireChange();
            return false;
        }
        if (hasActiveBattle()) {
            lastMessage = FishToucherBundle.message("cultivation.status.challengeBusy");
            fireChange();
            return false;
        }
        if (hasActiveTravel()) {
            lastMessage = FishToucherBundle.message("cultivation.status.challengeBlockedByTravel");
            fireChange();
            return false;
        }
        if (hasActiveSectSecretRealm()) {
            lastMessage = FishToucherBundle.message("cultivation.sect.secretRealmBusy");
            fireChange();
            return false;
        }
        if (isCultivatorDefeated(cultivator)) {
            lastMessage = FishToucherBundle.message("cultivation.status.challengeAlreadyDefeated", cultivator.name());
            fireChange();
            return false;
        }
        if (!isCultivatorUnlocked(cultivator)) {
            lastMessage = FishToucherBundle.message("cultivation.status.challengeLocked");
            fireChange();
            return false;
        }

        battleState = new BattleState(cultivator, calculateCombatStats(), getEquippedSpellDefinitions());
        addBattleLog(battleState, FishToucherBundle.message("cultivation.battle.log.started", cultivator.name()));
        lastMessage = FishToucherBundle.message("cultivation.status.challengeStarted", cultivator.name());
        if (battleTask != null) {
            battleTask.cancel(false);
        }
        if (scheduler != null) {
            battleTask = scheduler.scheduleAtFixedRate(() -> {
                try {
                    advanceBattle();
                } catch (Exception e) {
                    LOG.warn("battle: failed to advance challenge: " + e.getMessage());
                }
            }, BATTLE_TICK_SECONDS, BATTLE_TICK_SECONDS, TimeUnit.SECONDS);
        }
        fireChange();
        return true;
    }

    public synchronized boolean endChallenge() {
        if (!hasActiveBattle()) {
            lastMessage = FishToucherBundle.message("cultivation.status.challengeNone");
            fireChange();
            return false;
        }
        settleProgress(false);
        battleState.finished = true;
        battleState.victory = false;
        addBattleLog(battleState, FishToucherBundle.message("cultivation.battle.log.forfeit"));
        cancelBattleTask();
        lastMessage = FishToucherBundle.message("cultivation.status.challengeForfeited");
        fireChange();
        return true;
    }

    public synchronized boolean hasActiveBattle() {
        return battleState != null && !battleState.finished;
    }

    public synchronized boolean isActivityBusy() {
        return hasActiveTravel() || hasActiveBattle() || hasActiveSectSecretRealm();
    }

    public synchronized boolean isSeclusionPaused() {
        return isSeclusionPaused(NovelReaderSettings.getInstance());
    }

    public synchronized BattleSnapshot getBattleSnapshot() {
        if (battleState == null) {
            return null;
        }
        return new BattleSnapshot(
                battleState.cultivator,
                battleState.playerStats,
                battleState.playerHealth,
                battleState.playerMana,
                battleState.enemyHealth,
                battleState.cultivator.maxHealth(),
                battleState.finished,
                battleState.victory,
                battleState.statusText(),
                List.copyOf(battleState.logs)
        );
    }

    public synchronized String getRealmName() {
        return getRealmName(NovelReaderSettings.getInstance().getCultivationRealmIndex());
    }

    public String getRealmName(int realmIndex) {
        return FishToucherBundle.message(
                "cultivation.realm." + CultivationRules.clampRealm(realmIndex)
        );
    }

    public synchronized long getCurrentQi() {
        return clampCultivationQi(NovelReaderSettings.getInstance());
    }

    public synchronized long getRequiredQi() {
        return getRequiredQi(NovelReaderSettings.getInstance().getCultivationRealmIndex());
    }

    public long getRequiredQi(int realmIndex) {
        return CultivationRules.requiredQi(realmIndex);
    }

    public synchronized long getSpiritStones() {
        return NovelReaderSettings.getInstance().getCultivationSpiritStones();
    }

    public synchronized int getProgressPercent() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        int realmIndex = settings.getCultivationRealmIndex();
        long currentQi = clampCultivationQi(settings);
        if (isMaxRealm(realmIndex)) {
            return 100;
        }
        long requiredQi = getRequiredQi(realmIndex);
        if (requiredQi <= 0L) {
            return 0;
        }
        return (int) Math.min(100L, currentQi * 100L / requiredQi);
    }

    public synchronized boolean canBreakthrough() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        int realmIndex = settings.getCultivationRealmIndex();
        return !isMaxRealm(realmIndex) && clampCultivationQi(settings) >= getRequiredQi(realmIndex);
    }

    public synchronized boolean canMeditate() {
        return canMeditate(System.currentTimeMillis());
    }

    public synchronized String getMeditationRemainingText() {
        return getMeditationRemainingText(System.currentTimeMillis());
    }

    public synchronized int getRebirthCount() {
        return NovelReaderSettings.getInstance().getCultivationRebirthCount();
    }

    public synchronized String getRebirthStatusText() {
        return FishToucherBundle.message("cultivation.status.rebirthCount", getRebirthCount());
    }

    public synchronized String getRebirthTrainingStatusText() {
        int rebirthCount = getRebirthCount();
        return FishToucherBundle.message(
                "cultivation.status.rebirthTraining",
                rebirthCount,
                getRebirthBattleMultiplierText(rebirthCount, REBIRTH_ATTACK_BONUS_PERCENT),
                getRebirthBattleMultiplierText(rebirthCount, REBIRTH_DEFENSE_BONUS_PERCENT),
                getRebirthBattleMultiplierText(rebirthCount, REBIRTH_MANA_BONUS_PERCENT)
        );
    }

    public synchronized String getRebirthEffectText() {
        return FishToucherBundle.message("cultivation.effect.rebirthQi", getRebirthMultiplierText());
    }

    public synchronized String getRebirthBreakthroughEffectText() {
        return FishToucherBundle.message("cultivation.effect.rebirthBreakthrough", getRebirthBreakthroughMultiplierText());
    }

    public synchronized int getBreakthroughChance() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        return getBreakthroughChance(settings.getCultivationRealmIndex(), settings.getCultivationBreakthroughFailures());
    }

    public synchronized String getLastMessage() {
        return lastMessage != null ? lastMessage : FishToucherBundle.message("cultivation.status.idle");
    }

    public synchronized String getStatusLine() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        int realmIndex = settings.getCultivationRealmIndex();
        long currentQi = clampCultivationQi(settings);
        String baseStatus;
        if (isMaxRealm(realmIndex)) {
            baseStatus = FishToucherBundle.message(
                    "cultivation.status.format",
                    getRealmName(realmIndex),
                    currentQi,
                    FishToucherBundle.message("cultivation.status.max"),
                    100,
                    settings.getCultivationSpiritStones()
            );
        } else {
            baseStatus = FishToucherBundle.message(
                    "cultivation.status.format",
                    getRealmName(realmIndex),
                    currentQi,
                    getRequiredQi(realmIndex),
                    getProgressPercent(),
                    settings.getCultivationSpiritStones()
            );
        }
        List<String> notices = new ArrayList<>();
        notices.add(FishToucherBundle.message(getSeclusionStatusKey(settings)));
        if (isTravelReady()) {
            notices.add(FishToucherBundle.message("cultivation.status.travelClaimReady"));
        }
        if (hasClaimableAbodeReward()) {
            notices.add(FishToucherBundle.message("cultivation.status.abodeClaimReady"));
        }
        if (hasActiveBattle()) {
            notices.add(FishToucherBundle.message("cultivation.status.battleRunning"));
        }
        if (isSectTaskReady()) {
            notices.add(FishToucherBundle.message("cultivation.sect.taskClaimReady"));
        } else if (hasActiveSectTask()) {
            notices.add(FishToucherBundle.message("cultivation.sect.taskStatus", getSectTaskRemainingText()));
        }
        if (hasActiveSectSecretRealm()) {
            notices.add(getSectSecretRealmStatusText());
        }
        if (settings.getCultivationRebirthCount() > 0) {
            notices.add(getRebirthStatusText());
        }
        return notices.isEmpty() ? baseStatus : baseStatus + " | " + String.join(" | ", notices);
    }

    public synchronized String getRateText() {
        int realmIndex = NovelReaderSettings.getInstance().getCultivationRealmIndex();
        return FishToucherBundle.message(
                "cultivation.status.rate",
                getPassiveQiPerMinute(realmIndex)
        );
    }

    public synchronized String getSeclusionRateText() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        int realmIndex = settings.getCultivationRealmIndex();
        return FishToucherBundle.message(
                "cultivation.status.seclusionRate",
                getSeclusionQiPerMinute(realmIndex),
                FishToucherBundle.message(getSeclusionStatusKey(settings))
        );
    }

    public synchronized String getChanceText() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        int realmIndex = settings.getCultivationRealmIndex();
        if (isMaxRealm(realmIndex)) {
            return FishToucherBundle.message("cultivation.status.maxRealm");
        }
        return FishToucherBundle.message(
                "cultivation.status.chance",
                getBreakthroughChance(),
                settings.getCultivationBreakthroughFailures()
        );
    }

    public synchronized String getActiveEffectsText() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        List<String> effects = new ArrayList<>();
        if (settings.isBreakthroughPillActive()) {
            effects.add(FishToucherBundle.message("cultivation.effect.breakthroughPill"));
        }
        if (settings.isMeridianPillActive()) {
            effects.add(FishToucherBundle.message("cultivation.effect.meridianPill"));
        }
        if (settings.getCultivationRebirthCount() > 0) {
            effects.add(getRebirthEffectText());
            effects.add(getRebirthBreakthroughEffectText());
        }
        return effects.isEmpty()
                ? FishToucherBundle.message("cultivation.effect.none")
                : String.join(", ", effects);
    }

    public synchronized TechniqueDefinition getEquippedTechnique() {
        return getTechnique(NovelReaderSettings.getInstance().getEquippedTechniqueId());
    }

    public List<TechniqueDefinition> getTechniqueDefinitions() {
        return TECHNIQUES;
    }

    public synchronized List<TechniqueDefinition> getRetainableTechniqueDefinitions() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        return TECHNIQUES.stream()
                .filter(technique -> settings.isTechniqueUnlocked(technique.id()))
                .toList();
    }

    public TechniqueDefinition getTechnique(String id) {
        TechniqueDefinition technique = TECHNIQUE_BY_ID.get(id);
        return technique != null ? technique : TECHNIQUE_BY_ID.get(BASIC_TECHNIQUE_ID);
    }

    public List<PillDefinition> getPillDefinitions() {
        return PILLS;
    }

    public PillDefinition getPill(String id) {
        return PILL_BY_ID.get(id);
    }

    public List<TravelLocationDefinition> getTravelLocationDefinitions() {
        return TRAVEL_LOCATIONS;
    }

    public TravelLocationDefinition getTravelLocation(String id) {
        return TRAVEL_BY_ID.get(id);
    }

    public synchronized TravelLocationDefinition getActiveTravelLocation() {
        String id = NovelReaderSettings.getInstance().getActiveTravelLocationId();
        return id.isEmpty() ? null : getTravelLocation(id);
    }

    public synchronized boolean hasActiveTravel() {
        return getActiveTravelLocation() != null;
    }

    private boolean hasActiveTravel(NovelReaderSettings settings) {
        return getTravelLocation(settings.getActiveTravelLocationId()) != null;
    }

    public synchronized boolean isTravelReady() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        TravelLocationDefinition location = getActiveTravelLocation();
        return location != null && settings.getActiveTravelElapsedMillis() >= getTravelDurationMillis(location);
    }

    public synchronized long getTravelRemainingMillis() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        if (!hasActiveTravel()) {
            return 0L;
        }
        TravelLocationDefinition location = getActiveTravelLocation();
        if (location == null) {
            return 0L;
        }
        return getTravelRemainingMillis(settings, location);
    }

    public synchronized String getTravelRemainingText() {
        if (!hasActiveTravel()) {
            return FishToucherBundle.message("cultivation.travel.none");
        }
        if (isTravelReady()) {
            return FishToucherBundle.message("cultivation.status.travelClaimReady");
        }
        return formatDuration(getTravelRemainingMillis());
    }

    public synchronized int getTravelProgressPercent() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        if (!hasActiveTravel()) {
            return 0;
        }
        TravelLocationDefinition location = getActiveTravelLocation();
        if (location == null) {
            return 0;
        }
        long total = Math.max(1L, getTravelDurationMillis(location));
        long done = Math.max(0L, settings.getActiveTravelElapsedMillis());
        return (int) Math.min(100L, done * 100L / total);
    }

    public synchronized String getTravelDurationText(TravelLocationDefinition location) {
        long durationMinutes = getTravelDurationMinutes(location);
        int reductionPercent = getTravelDurationReductionPercent();
        if (reductionPercent > 0) {
            return FishToucherBundle.message("cultivation.travel.durationReduced", durationMinutes, reductionPercent);
        }
        return FishToucherBundle.message("cultivation.travel.duration", durationMinutes);
    }

    public synchronized long getTravelDurationMinutes(TravelLocationDefinition location) {
        return getTravelDurationMinutes(location, NovelReaderSettings.getInstance().getCultivationRealmIndex());
    }

    public synchronized int getTravelDurationReductionPercent() {
        return getTravelDurationReductionPercent(NovelReaderSettings.getInstance().getCultivationRealmIndex());
    }

    public synchronized boolean isTravelUnlocked(TravelLocationDefinition location) {
        return NovelReaderSettings.getInstance().getCultivationRealmIndex() >= location.minRealmIndex();
    }

    public List<AbodeFacilityDefinition> getAbodeFacilityDefinitions() {
        return ABODE_FACILITIES;
    }

    public AbodeFacilityDefinition getAbodeFacility(String id) {
        return ABODE_BY_ID.get(id);
    }

    public synchronized boolean isAbodeUnlocked() {
        return NovelReaderSettings.getInstance().getCultivationRealmIndex() >= ABODE_UNLOCK_REALM_INDEX;
    }

    public synchronized String getAbodeLockedText() {
        return FishToucherBundle.message("cultivation.abode.locked", getRealmName(ABODE_UNLOCK_REALM_INDEX));
    }

    public synchronized int getAbodeFacilityLevel(String facilityId) {
        int level = NovelReaderSettings.getInstance().getAbodeFacilityLevel(facilityId);
        return Math.max(0, Math.min(MAX_ABODE_LEVEL, level));
    }

    public synchronized String getAbodeFacilityLevelText(String facilityId) {
        return FishToucherBundle.message("cultivation.abode.level", getAbodeFacilityLevel(facilityId), MAX_ABODE_LEVEL);
    }

    public synchronized long getAbodeUpgradeCost(String facilityId) {
        AbodeFacilityDefinition facility = getAbodeFacility(facilityId);
        if (facility == null) {
            return 0L;
        }
        int currentLevel = getAbodeFacilityLevel(facilityId);
        if (currentLevel >= MAX_ABODE_LEVEL) {
            return 0L;
        }
        if (SPIRIT_GATHERING_ARRAY_ID.equals(facilityId)) {
            return SPIRIT_GATHERING_UPGRADE_COST[currentLevel];
        }
        long targetLevel = currentLevel + 1L;
        return facility.baseCost() * targetLevel * targetLevel;
    }

    public synchronized String getAbodeUpgradeCostText(String facilityId) {
        if (getAbodeFacilityLevel(facilityId) >= MAX_ABODE_LEVEL) {
            return FishToucherBundle.message("cultivation.abode.costMax");
        }
        return FishToucherBundle.message("cultivation.abode.cost", getAbodeUpgradeCost(facilityId));
    }

    public synchronized boolean canUpgradeAbodeFacility(String facilityId) {
        if (!isAbodeUnlocked()) {
            return false;
        }
        AbodeFacilityDefinition facility = getAbodeFacility(facilityId);
        if (facility == null || getAbodeFacilityLevel(facilityId) >= MAX_ABODE_LEVEL) {
            return false;
        }
        return NovelReaderSettings.getInstance().getCultivationSpiritStones() >= getAbodeUpgradeCost(facilityId);
    }

    public synchronized boolean upgradeAbodeFacility(String facilityId) {
        settleProgress(false);
        ensureCultivationDefaults();
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        if (!isAbodeUnlocked()) {
            lastMessage = getAbodeLockedText();
            fireChange();
            return false;
        }
        AbodeFacilityDefinition facility = getAbodeFacility(facilityId);
        if (facility == null) {
            lastMessage = FishToucherBundle.message("cultivation.status.facilityUnknown");
            fireChange();
            return false;
        }

        int currentLevel = getAbodeFacilityLevel(facilityId);
        if (currentLevel >= MAX_ABODE_LEVEL) {
            lastMessage = FishToucherBundle.message("cultivation.status.facilityMaxLevel");
            fireChange();
            return false;
        }

        long cost = getAbodeUpgradeCost(facilityId);
        if (settings.getCultivationSpiritStones() < cost) {
            lastMessage = FishToucherBundle.message("cultivation.status.insufficientStones", cost);
            fireChange();
            return false;
        }

        int preservedAlchemyPillCount = 0;
        if (ALCHEMY_ROOM_ID.equals(facilityId) && currentLevel > 0) {
            preservedAlchemyPillCount = preservePendingAlchemyPillsBeforeUpgrade(settings, currentLevel);
        } else if (isProductionAbodeFacility(facilityId) && currentLevel > 0) {
            claimAbodeFacilityInternal(facilityId, false);
        }
        settings.setCultivationSpiritStones(settings.getCultivationSpiritStones() - cost);
        int nextLevel = currentLevel + 1;
        settings.setAbodeFacilityLevel(facilityId, nextLevel);
        if (isProductionAbodeFacility(facilityId) && (!ALCHEMY_ROOM_ID.equals(facilityId) || currentLevel <= 0)) {
            settings.setAbodeLastClaimMillis(facilityId, System.currentTimeMillis());
        }
        lastMessage = preservedAlchemyPillCount > 0
                ? FishToucherBundle.message("cultivation.status.facilityUpgradedWithPendingAlchemy", facility.name(), nextLevel, preservedAlchemyPillCount)
                : FishToucherBundle.message("cultivation.status.facilityUpgraded", facility.name(), nextLevel);
        fireChange();
        return true;
    }

    public synchronized String getAbodeFacilityEffectText(String facilityId) {
        int level = getAbodeFacilityLevel(facilityId);
        return switch (facilityId) {
            case SPIRIT_GATHERING_ARRAY_ID -> FishToucherBundle.message("cultivation.abode.effect.spiritGathering", getSpiritGatheringBonusPercent(level));
            case SPIRIT_VEIN_ID -> FishToucherBundle.message("cultivation.abode.effect.spiritVein", applyStoneBonus(10L * level));
            case ALCHEMY_ROOM_ID -> FishToucherBundle.message("cultivation.abode.effect.alchemyRoom", getAlchemyRarePillChance(level));
            case INSIGHT_ROOM_ID -> FishToucherBundle.message(
                    "cultivation.abode.effect.insightRoom",
                    getInsightRoomBreakthroughBonusPercent(level)
            );
            default -> "";
        };
    }

    public synchronized String getAbodeClaimableText(String facilityId) {
        return switch (facilityId) {
            case SPIRIT_VEIN_ID -> {
                long stones = getClaimableSpiritVeinStones();
                yield stones > 0L
                        ? FishToucherBundle.message("cultivation.abode.claimStones", stones)
                        : FishToucherBundle.message("cultivation.abode.claimNone");
            }
            case ALCHEMY_ROOM_ID -> {
                long pillCount = getClaimableAlchemyPillCount();
                yield pillCount > 0L
                        ? FishToucherBundle.message("cultivation.abode.claimPills", pillCount)
                        : FishToucherBundle.message("cultivation.abode.claimNone");
            }
            default -> FishToucherBundle.message("cultivation.abode.claimNone");
        };
    }

    public synchronized boolean canClaimAbodeFacility(String facilityId) {
        if (!isAbodeUnlocked()) {
            return false;
        }
        return switch (facilityId) {
            case SPIRIT_VEIN_ID -> getClaimableSpiritVeinStones() > 0L;
            case ALCHEMY_ROOM_ID -> getClaimableAlchemyPillCount() > 0L;
            default -> false;
        };
    }

    public boolean isAbodeProductionFacility(String facilityId) {
        return isProductionAbodeFacility(facilityId);
    }

    public synchronized boolean hasClaimableAbodeReward() {
        if (!isAbodeUnlocked()) {
            return false;
        }
        return getClaimableSpiritVeinStones() > 0L || getClaimableAlchemyPillCount() > 0L;
    }

    public synchronized AbodeReward claimAbodeFacility(String facilityId) {
        settleProgress(false);
        if (!isAbodeUnlocked()) {
            lastMessage = getAbodeLockedText();
            fireChange();
            return AbodeReward.empty();
        }
        AbodeReward reward = claimAbodeFacilityInternal(facilityId, true);
        fireChange();
        return reward;
    }

    private void advanceBattle() {
        BattleState state;
        synchronized (this) {
            state = battleState;
            if (state == null || state.finished) {
                cancelBattleTask();
                return;
            }

            state.elapsedSeconds++;
            state.spellCooldowns.replaceAll((id, cooldown) -> Math.max(0, cooldown - 1));
            state.playerAttackCooldown = Math.max(0, state.playerAttackCooldown - 1);
            state.enemyAttackCooldown = Math.max(0, state.enemyAttackCooldown - 1);
            recoverBattleResources(state);

            castReadySpell(state);
            if (state.enemyHealth <= 0L) {
                finishBattle(state, true);
                fireChange();
                return;
            }

            if (state.playerAttackCooldown <= 0) {
                long damage = calculateBattleDamage(state.playerStats.attack(), state.cultivator.defense(), 65, 25);
                state.enemyHealth = Math.max(0L, state.enemyHealth - damage);
                state.playerAttackCooldown = 2;
                addBattleLog(state, FishToucherBundle.message("cultivation.battle.log.playerAttack", damage));
            }
            if (state.enemyHealth <= 0L) {
                finishBattle(state, true);
                fireChange();
                return;
            }

            if (state.enemyAttackCooldown <= 0) {
                if (state.skipEnemyAttacks > 0) {
                    state.skipEnemyAttacks--;
                    addBattleLog(state, FishToucherBundle.message("cultivation.battle.log.enemySkipped"));
                } else {
                    long damage = calculateBattleDamage(state.cultivator.attack(), state.playerStats.defense(), 65, 25);
                    if (state.shieldHits > 0) {
                        damage = Math.max(1L, damage * (100L - 45L) / 100L);
                        state.shieldHits--;
                        addBattleLog(state, FishToucherBundle.message("cultivation.battle.log.shieldReduced", damage));
                    } else {
                        addBattleLog(state, FishToucherBundle.message("cultivation.battle.log.enemyAttack", damage));
                    }
                    state.playerHealth = Math.max(0L, state.playerHealth - damage);
                }
                state.enemyAttackCooldown = 2;
            }
            if (state.playerHealth <= 0L) {
                finishBattle(state, false);
            }
            fireChange();
        }
    }

    private void castReadySpell(BattleState state) {
        for (SpellDefinition spell : state.spells) {
            if (state.spellCooldowns.getOrDefault(spell.id(), 0) > 0 || state.playerMana < spell.manaCost()) {
                continue;
            }
            state.playerMana -= spell.manaCost();
            state.spellCooldowns.put(spell.id(), spell.cooldownSeconds());
            switch (spell.type()) {
                case DAMAGE -> {
                    long damage = calculateBattleDamage(state.playerStats.attack(), state.cultivator.defense(), spell.powerPercent(), 15);
                    state.enemyHealth = Math.max(0L, state.enemyHealth - damage);
                    addBattleLog(state, FishToucherBundle.message("cultivation.battle.log.spellDamage", spell.name(), damage));
                }
                case FROST -> {
                    long damage = calculateBattleDamage(state.playerStats.attack(), state.cultivator.defense(), spell.powerPercent(), 15);
                    state.enemyHealth = Math.max(0L, state.enemyHealth - damage);
                    state.skipEnemyAttacks++;
                    addBattleLog(state, FishToucherBundle.message("cultivation.battle.log.spellFrost", spell.name(), damage));
                }
                case HEAL -> {
                    long heal = Math.max(1L, state.playerStats.health() * spell.powerPercent() / 100L);
                    long actualHeal = Math.min(heal, state.playerStats.health() - state.playerHealth);
                    state.playerHealth += actualHeal;
                    addBattleLog(state, FishToucherBundle.message("cultivation.battle.log.spellHeal", spell.name(), actualHeal));
                }
                case SHIELD -> {
                    state.shieldHits += 2;
                    addBattleLog(state, FishToucherBundle.message("cultivation.battle.log.spellShield", spell.name()));
                }
            }
            return;
        }
    }

    private void recoverBattleResources(BattleState state) {
        if (state.playerHealth > 0L && state.playerHealth < state.playerStats.health()) {
            long healthRecovery = calculateBattleHealthRecovery(state.playerStats);
            state.playerHealth = Math.min(state.playerStats.health(), state.playerHealth + healthRecovery);
        }
        if (state.playerMana < state.playerStats.mana()) {
            long manaRecovery = calculateBattleManaRecovery(state.playerStats);
            state.playerMana = Math.min(state.playerStats.mana(), state.playerMana + manaRecovery);
        }
    }

    private long calculateBattleHealthRecovery(CombatStats stats) {
        return Math.max(1L, stats.health() / BATTLE_HEALTH_RECOVERY_DIVISOR);
    }

    private long calculateBattleManaRecovery(CombatStats stats) {
        return Math.max(1L, stats.mana() / BATTLE_MANA_RECOVERY_DIVISOR);
    }

    private void finishBattle(BattleState state, boolean victory) {
        settleProgress(false);
        state.finished = true;
        state.victory = victory;
        cancelBattleTask();
        if (victory) {
            ChallengeReward reward = state.sectTrialId.isEmpty()
                    ? grantChallengeReward(state.cultivator)
                    : grantSectTrialReward(state.sectTrialId, state.cultivator);
            addBattleLog(state, FishToucherBundle.message("cultivation.battle.log.victory", reward.summary()));
            lastMessage = FishToucherBundle.message("cultivation.status.challengeVictory", state.cultivator.name(), reward.summary());
        } else {
            addBattleLog(state, FishToucherBundle.message("cultivation.battle.log.defeat"));
            lastMessage = FishToucherBundle.message("cultivation.status.challengeDefeat", state.cultivator.name());
        }
    }

    private ChallengeReward grantChallengeReward(CultivatorDefinition cultivator) {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        long stones = applyStoneBonus(cultivator.stoneReward());
        List<String> rewardParts = new ArrayList<>();
        settings.markCultivatorDefeated(cultivator.id());
        rewardParts.add(FishToucherBundle.message("cultivation.reward.stones", stones));

        if (!cultivator.spellRewardId().isEmpty()) {
            SpellDefinition spell = getSpell(cultivator.spellRewardId());
            if (spell != null) {
                if (settings.unlockSpell(spell.id())) {
                    rewardParts.add(FishToucherBundle.message("cultivation.reward.spell", spell.name()));
                } else {
                    long compensation = applyStoneBonus(260L + getCultivatorOrder(cultivator) * 90L);
                    stones += compensation;
                    rewardParts.add(FishToucherBundle.message("cultivation.reward.duplicateSpell", spell.name(), compensation));
                }
            }
        }
        if (!cultivator.techniqueRewardId().isEmpty()) {
            TechniqueDefinition technique = getTechnique(cultivator.techniqueRewardId());
            if (technique != null) {
                if (settings.unlockTechnique(technique.id())) {
                    rewardParts.add(FishToucherBundle.message("cultivation.reward.technique", technique.name()));
                } else {
                    long compensation = applyStoneBonus(320L + getCultivatorOrder(cultivator) * 110L);
                    stones += compensation;
                    rewardParts.add(FishToucherBundle.message("cultivation.reward.duplicateTechniqueWithStones", technique.name(), compensation));
                }
            }
        }
        if (!cultivator.artifactRewardId().isEmpty()) {
            ArtifactDefinition artifact = getArtifact(cultivator.artifactRewardId());
            if (artifact != null) {
                if (settings.unlockArtifact(artifact.id())) {
                    rewardParts.add(FishToucherBundle.message("cultivation.reward.artifact", artifact.name()));
                } else {
                    long compensation = applyStoneBonus(420L + getCultivatorOrder(cultivator) * 130L);
                    stones += compensation;
                    rewardParts.add(FishToucherBundle.message("cultivation.reward.duplicateArtifact", artifact.name(), compensation));
                }
            }
        }
        settings.setCultivationSpiritStones(settings.getCultivationSpiritStones() + stones);
        return new ChallengeReward(stones, rewardParts);
    }

    private ChallengeReward grantSectTrialReward(String trialId, CultivatorDefinition cultivator) {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        SectCatalog.SectTrialDefinition trial = SectCatalog.trial(trialId);
        long stones = applyStoneBonus(cultivator.stoneReward());
        long contribution = trial == null ? 0L : 120L * trial.floor();
        long prestige = trial == null ? 0L : 40L * trial.floor();
        List<String> rewardParts = new ArrayList<>();
        settings.markSectTrialDefeated(trialId);
        settings.setCultivationSpiritStones(settings.getCultivationSpiritStones() + stones);
        settings.addCurrentSectContribution(contribution);
        settings.addCurrentSectPrestige(prestige);
        if (trial != null && trial.floor() >= 5) {
            settings.markSectGraduated(trial.sectId());
        }
        rewardParts.add(FishToucherBundle.message("cultivation.reward.stones", stones));
        rewardParts.add(FishToucherBundle.message("cultivation.sect.rewardContribution", contribution));
        rewardParts.add(FishToucherBundle.message("cultivation.sect.rewardPrestige", prestige));
        return new ChallengeReward(stones, rewardParts);
    }

    private int getCultivatorOrder(CultivatorDefinition cultivator) {
        int index = CULTIVATORS.indexOf(cultivator);
        return index < 0 ? 1 : index + 1;
    }

    private long calculateBattleDamage(long attack, long defense, int attackPercent, int defensePercent) {
        long rawDamage = attack * attackPercent / 100L - defense * defensePercent / 100L;
        return Math.max(1L, rawDamage);
    }

    private void addBattleLog(BattleState state, String message) {
        if (state == null || message == null || message.isEmpty()) {
            return;
        }
        String line = FishToucherBundle.message("cultivation.battle.log.line", state.elapsedSeconds, message);
        state.logs.add(line);
        while (state.logs.size() > BATTLE_LOG_LIMIT) {
            state.logs.remove(0);
        }
    }

    private void cancelBattleTask() {
        if (battleTask != null) {
            battleTask.cancel(false);
            battleTask = null;
        }
    }

    private CombatStats calculateCombatStats() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        int realmIndex = settings.getCultivationRealmIndex();
        TechniqueDefinition technique = getEquippedTechnique();
        int rebirthCount = settings.getCultivationRebirthCount();
        long attack = 120L + Math.max(0, realmIndex) * 80L;
        long defense = 95L + Math.max(0, realmIndex) * 65L;
        long mana = 180L + Math.max(0, realmIndex) * 90L;
        attack = applyPercent(attack, technique.attackBonusPercent()
                + getArtifactAttackBonusPercent()
                + SectRules.currentSectBonus(settings, SectCatalog.BonusType.ATTACK));
        defense = applyPercent(defense, technique.defenseBonusPercent()
                + getArtifactDefenseBonusPercent()
                + SectRules.currentSectBonus(settings, SectCatalog.BonusType.DEFENSE));
        mana = applyPercent(mana, technique.manaBonusPercent() + getArtifactManaBonusPercent());
        attack = applyPercent(attack, rebirthCount * REBIRTH_ATTACK_BONUS_PERCENT);
        defense = applyPercent(defense, rebirthCount * REBIRTH_DEFENSE_BONUS_PERCENT);
        mana = applyPercent(mana, rebirthCount * REBIRTH_MANA_BONUS_PERCENT);
        long health = defense * 12L + mana * 3L;
        return new CombatStats(attack, defense, mana, health);
    }

    private void ensureCultivationDefaults() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        if (!settings.isTechniqueUnlocked(BASIC_TECHNIQUE_ID)) {
            settings.unlockTechnique(BASIC_TECHNIQUE_ID);
        }
        if (!settings.isTechniqueUnlocked(settings.getEquippedTechniqueId())) {
            settings.setEquippedTechniqueId(BASIC_TECHNIQUE_ID);
        }
        long now = System.currentTimeMillis();
        for (AbodeFacilityDefinition facility : ABODE_FACILITIES) {
            if (!isAbodeUnlocked() && isProductionAbodeFacility(facility.id())) {
                settings.setAbodeLastClaimMillis(facility.id(), 0L);
            }
            if (isAbodeUnlocked() && isProductionAbodeFacility(facility.id()) && settings.getAbodeLastClaimMillis(facility.id()) <= 0L) {
                settings.setAbodeLastClaimMillis(facility.id(), now);
            }
            int level = getAbodeFacilityLevel(facility.id());
            if (level != settings.getAbodeFacilityLevel(facility.id())) {
                settings.setAbodeFacilityLevel(facility.id(), level);
            }
        }
        clampCultivationQi(settings);
        purgeInvalidSectSecretRealm(settings);
    }

    private boolean canMeditate(long now) {
        return getMeditationRemainingMillis(now) <= 0L;
    }

    private long getMeditationRemainingMillis(long now) {
        long lastMeditationMillis = NovelReaderSettings.getInstance().getCultivationLastMeditationMillis();
        if (lastMeditationMillis <= 0L) {
            return 0L;
        }
        return Math.max(0L, lastMeditationMillis + MEDITATION_COOLDOWN_MILLIS - now);
    }

    private String getMeditationRemainingText(long now) {
        long remainingMillis = getMeditationRemainingMillis(now);
        return remainingMillis <= 0L
                ? FishToucherBundle.message("cultivation.status.meditationReady")
                : formatRemainingDuration(remainingMillis);
    }

    private boolean advanceActiveTravel(NovelReaderSettings settings, long creditedMillis) {
        if (creditedMillis <= 0L) {
            return false;
        }
        TravelLocationDefinition location = getTravelLocation(settings.getActiveTravelLocationId());
        if (location == null) {
            return false;
        }
        long durationMillis = getTravelDurationMillis(location);
        long currentElapsedMillis = Math.min(settings.getActiveTravelElapsedMillis(), durationMillis);
        long nextElapsedMillis = Math.min(durationMillis, currentElapsedMillis + creditedMillis);
        if (nextElapsedMillis == currentElapsedMillis) {
            return false;
        }
        settings.setActiveTravelElapsedMillis(nextElapsedMillis);
        return true;
    }

    private boolean advanceActiveSectTask(NovelReaderSettings settings, long creditedMillis) {
        if (creditedMillis <= 0L) {
            return false;
        }
        SectCatalog.SectTaskDefinition task = SectCatalog.task(settings.getActiveSectTaskId());
        if (task == null) {
            return false;
        }
        long durationMillis = getSectTaskDurationMillis(task);
        long currentElapsedMillis = Math.min(settings.getActiveSectTaskElapsedMillis(), durationMillis);
        long nextElapsedMillis = Math.min(durationMillis, currentElapsedMillis + creditedMillis);
        if (nextElapsedMillis == currentElapsedMillis) {
            return false;
        }
        settings.setActiveSectTaskElapsedMillis(nextElapsedMillis);
        return true;
    }

    private long getSectTaskDurationMillis(SectCatalog.SectTaskDefinition task) {
        return TimeUnit.MINUTES.toMillis(getSectTaskDurationMinutes(task));
    }

    synchronized long getSectTaskDurationMinutes(SectCatalog.SectTaskDefinition task) {
        return getSectTaskDurationMinutes(task, NovelReaderSettings.getInstance());
    }

    synchronized long getSectTaskDurationMinutes(SectCatalog.SectTaskDefinition task, NovelReaderSettings settings) {
        if (task == null || settings == null) {
            return 0L;
        }
        int reductionPercent = getSectTaskDurationReductionPercent(settings);
        long reducedMinutes = Math.round(task.durationMinutes() * (100.0 - reductionPercent) / 100.0);
        return Math.max(1L, reducedMinutes);
    }

    synchronized int getSectTaskDurationReductionPercent() {
        return getSectTaskDurationReductionPercent(NovelReaderSettings.getInstance());
    }

    synchronized int getSectTaskDurationReductionPercent(NovelReaderSettings settings) {
        if (settings == null) {
            return 0;
        }
        int reductionPercent = getTravelDurationReductionPercent(settings.getCultivationRealmIndex())
                + SectRules.currentSectBonus(settings, SectCatalog.BonusType.TRAVEL_DURATION);
        return Math.min(MAX_TRAVEL_DURATION_REDUCTION_PERCENT, reductionPercent);
    }

    private void grantSectInheritance(NovelReaderSettings settings, SectCatalog.SectInheritanceDefinition inheritance) {
        switch (inheritance.type()) {
            case TECHNIQUE -> settings.unlockTechnique(inheritance.rewardId());
            case SPELL -> settings.unlockSpell(inheritance.rewardId());
            case ARTIFACT -> settings.unlockArtifact(inheritance.rewardId());
            case PILL -> settings.addPill(inheritance.rewardId(), 2);
        }
    }

    private SectEventInstance findCurrentSectEvent(String instanceId) {
        if (instanceId == null || instanceId.isEmpty()) {
            return null;
        }
        for (SectEventInstance event : getPendingSectEvents()) {
            if (instanceId.equals(event.instanceId())) {
                return event;
            }
        }
        return null;
    }

    private void purgeInvalidSectEvents(NovelReaderSettings settings) {
        for (NovelReaderSettings.SectPendingEventState eventState : settings.getPendingSectEvents()) {
            if (SectCatalog.event(eventState.eventId) == null || SectCatalog.sect(eventState.sectId) == null) {
                settings.removePendingSectEvent(eventState.instanceId);
            }
        }
    }

    private void purgeInvalidSectSecretRealm(NovelReaderSettings settings) {
        SectCatalog.SectSecretRealmDefinition secretRealm = SectCatalog.secretRealm(settings.getActiveSectSecretRealmId());
        if (secretRealm == null || !secretRealm.sectId().equals(settings.getCultivationSectId())) {
            settings.clearSectSecretRealmProgress();
            return;
        }
        if (settings.getSectSecretRealmNodeIndex() >= secretRealm.nodes().size()) {
            settings.clearSectSecretRealmProgress();
        }
    }

    private SectCatalog.SectEventOptionDefinition findEventOption(SectCatalog.SectEventDefinition event, String optionId) {
        if (event == null || optionId == null || optionId.isEmpty()) {
            return null;
        }
        for (SectCatalog.SectEventOptionDefinition option : event.options()) {
            if (optionId.equals(option.id())) {
                return option;
            }
        }
        return null;
    }

    private String resolveSectSecretRealmBattle(NovelReaderSettings settings,
                                                SectCatalog.SectSecretRealmDefinition secretRealm,
                                                SectCatalog.SectSecretRealmNodeDefinition node) {
        int chance = calculateSectSecretRealmWinChance(node);
        boolean victory = nextSectSecretRealmInt(100) < chance;
        if (!victory) {
            finishSectSecretRealm(settings);
            return FishToucherBundle.message("cultivation.sect.secretRealmBattleFailed", node.title(), chance);
        }
        advanceSectSecretRealmNode(settings, node);
        if (node.type() == SectCatalog.SecretRealmNodeType.BOSS) {
            String reward = grantSectSecretRealmFinalReward(settings, secretRealm);
            finishSectSecretRealm(settings);
            return FishToucherBundle.message("cultivation.sect.secretRealmCompleted", node.title(), chance, reward);
        }
        return FishToucherBundle.message("cultivation.sect.secretRealmBattleWon", node.title(), chance);
    }

    private String resolveSectSecretRealmChoice(NovelReaderSettings settings,
                                                SectCatalog.SectSecretRealmNodeDefinition node,
                                                String optionId) {
        SectCatalog.SectSecretRealmOptionDefinition option = findSectSecretRealmOption(node, optionId);
        if (option == null) {
            return FishToucherBundle.message("cultivation.sect.secretRealmOptionUnknown");
        }
        String reward = "";
        if ("study".equals(option.id())) {
            long qiGain = addCultivationQi(settings, applyQiBonus(1_800L + settings.getCultivationRealmIndex() * 850L));
            reward = qiGain > 0L ? FishToucherBundle.message("cultivation.reward.qi", qiGain) : "";
        }
        advanceSectSecretRealmNode(settings, node);
        return reward.isEmpty()
                ? FishToucherBundle.message("cultivation.sect.secretRealmChoiceResolved", node.title(), option.label())
                : FishToucherBundle.message("cultivation.sect.secretRealmChoiceReward", node.title(), option.label(), reward);
    }

    private String resolveSectSecretRealmChest(NovelReaderSettings settings,
                                               SectCatalog.SectSecretRealmNodeDefinition node) {
        int realmIndex = settings.getCultivationRealmIndex();
        int rankIndex = settings.getCurrentSectRankIndex();
        long qiGain = addCultivationQi(settings, applyQiBonus(2_400L + realmIndex * 1_100L));
        long stones = applyStoneBonus(1_200L + rankIndex * 650L);
        long contribution = 160L + rankIndex * 55L;
        long prestige = 55L + rankIndex * 18L;
        settings.setCultivationSpiritStones(settings.getCultivationSpiritStones() + stones);
        settings.addCurrentSectContribution(contribution);
        settings.addCurrentSectPrestige(prestige);
        advanceSectSecretRealmNode(settings, node);
        return FishToucherBundle.message("cultivation.sect.secretRealmChestOpened", node.title(), qiGain, stones, contribution, prestige);
    }

    private String grantSectSecretRealmFinalReward(NovelReaderSettings settings,
                                                   SectCatalog.SectSecretRealmDefinition secretRealm) {
        int realmIndex = settings.getCultivationRealmIndex();
        int rankIndex = settings.getCurrentSectRankIndex();
        long qiGain = addCultivationQi(settings, applyQiBonus(5_000L + realmIndex * 2_400L));
        long stones = applyStoneBonus(2_800L + rankIndex * 1_100L);
        long contribution = 420L + rankIndex * 130L;
        long prestige = 150L + rankIndex * 45L;
        List<String> parts = new ArrayList<>();
        settings.setCultivationSpiritStones(settings.getCultivationSpiritStones() + stones);
        settings.addCurrentSectContribution(contribution);
        settings.addCurrentSectPrestige(prestige);
        parts.add(FishToucherBundle.message("cultivation.reward.qi", qiGain));
        parts.add(FishToucherBundle.message("cultivation.reward.stones", stones));
        parts.add(FishToucherBundle.message("cultivation.sect.rewardContribution", contribution));
        parts.add(FishToucherBundle.message("cultivation.sect.rewardPrestige", prestige));
        if (nextSectSecretRealmInt(100) < 35) {
            settings.addPill(nextSectSecretRealmInt(100) < 55 ? SPIRIT_PILL_ID : MERIDIAN_PILL_ID, 1);
            parts.add(FishToucherBundle.message("cultivation.sect.secretRealmRewardPill"));
        }
        if (nextSectSecretRealmInt(100) < 18) {
            parts.add(grantSectSecretRealmRareReward(settings, secretRealm));
        }
        return String.join(", ", parts);
    }

    private String grantSectSecretRealmRareReward(NovelReaderSettings settings,
                                                  SectCatalog.SectSecretRealmDefinition secretRealm) {
        String spellId = switch (secretRealm.sectId()) {
            case "qingyun_sword" -> FIRE_SWORD_SPELL_ID;
            case "danxia_valley" -> GREENWOOD_HEAL_SPELL_ID;
            case "xuanwu_gate" -> GOLDEN_LIGHT_SPELL_ID;
            case "tianji_pavilion" -> FROST_BIND_SPELL_ID;
            default -> PALM_THUNDER_SPELL_ID;
        };
        SpellDefinition spell = getSpell(spellId);
        if (spell != null && settings.unlockSpell(spell.id())) {
            return FishToucherBundle.message("cultivation.reward.spell", spell.name());
        }
        String artifactId = switch (secretRealm.sectId()) {
            case "qingyun_sword" -> GREEN_SWORD_ARTIFACT_ID;
            case "danxia_valley" -> TAIXU_CAULDRON_ARTIFACT_ID;
            case "xuanwu_gate" -> TURTLE_SHIELD_ARTIFACT_ID;
            case "tianji_pavilion" -> WIND_THUNDER_BOOTS_ARTIFACT_ID;
            default -> SPIRIT_JADE_ARTIFACT_ID;
        };
        ArtifactDefinition artifact = getArtifact(artifactId);
        if (artifact != null && settings.unlockArtifact(artifact.id())) {
            return FishToucherBundle.message("cultivation.reward.artifact", artifact.name());
        }
        long compensation = applyStoneBonus(900L + settings.getCurrentSectRankIndex() * 280L);
        settings.setCultivationSpiritStones(settings.getCultivationSpiritStones() + compensation);
        return FishToucherBundle.message("cultivation.reward.stones", compensation);
    }

    private void advanceSectSecretRealmNode(NovelReaderSettings settings,
                                            SectCatalog.SectSecretRealmNodeDefinition node) {
        settings.markSectSecretRealmNodeResolved(node.id());
        settings.setSectSecretRealmNodeIndex(settings.getSectSecretRealmNodeIndex() + 1);
    }

    private void finishSectSecretRealm(NovelReaderSettings settings) {
        settings.clearSectSecretRealmProgress();
        settings.setSectSecretRealmCooldownUntilMillis(System.currentTimeMillis() + SECT_SECRET_REALM_COOLDOWN_MILLIS);
    }

    private SectCatalog.SectSecretRealmOptionDefinition findSectSecretRealmOption(
            SectCatalog.SectSecretRealmNodeDefinition node,
            String optionId
    ) {
        if (node == null || optionId == null || optionId.isEmpty()) {
            return null;
        }
        for (SectCatalog.SectSecretRealmOptionDefinition option : node.options()) {
            if (optionId.equals(option.id())) {
                return option;
            }
        }
        return null;
    }

    private int calculateSectSecretRealmWinChance(SectCatalog.SectSecretRealmNodeDefinition node) {
        CombatStats stats = calculateCombatStats();
        long playerPower = calculateSecretRealmPower(stats.attack(), stats.defense(), stats.mana(), stats.health());
        long enemyHealth = node.maxHealth() > 0L ? node.maxHealth() : node.defense() * 12L + node.mana() * 3L;
        long enemyPower = calculateSecretRealmPower(node.attack(), node.defense(), node.mana(), enemyHealth);
        if (enemyPower <= 0L) {
            return 90;
        }
        int chance = (int) Math.round(playerPower * 100.0 / (playerPower + enemyPower));
        return Math.max(15, Math.min(90, chance));
    }

    private long calculateSecretRealmPower(long attack, long defense, long mana, long health) {
        return Math.max(1L, attack * 3L + defense * 2L + mana * 2L + health / 8L);
    }

    private String applySectEventReward(NovelReaderSettings settings, String eventId, String optionId) {
        int realmIndex = settings.getCultivationRealmIndex();
        return switch (eventId + ":" + optionId) {
            case "sparring:accept" -> resolveSparringReward(settings, realmIndex);
            case "elder_lecture:listen" -> {
                long qiGain = addCultivationQi(settings, applyQiBonus(900L + realmIndex * 350L));
                yield qiGain > 0L ? "修为 +" + qiGain : "";
            }
            case "back_mountain:inspect" -> resolveBackMountainReward(settings, realmIndex);
            case "junior_help:give_qi_pill" -> {
                if (!settings.consumePill(QI_PILL_ID)) {
                    yield "";
                }
                settings.addCurrentSectPrestige(60L);
                settings.addCurrentSectContribution(80L);
                yield "消耗聚气丹 x1，威望 +60，贡献 +80";
            }
            case "inheritance_fragment:study" -> {
                long qiGain = addCultivationQi(settings, applyQiBonus(1_200L + realmIndex * 400L));
                yield qiGain > 0L ? "修为 +" + qiGain : "";
            }
            case "inheritance_fragment:submit" -> {
                settings.addCurrentSectContribution(120L);
                yield "贡献 +120";
            }
            default -> "";
        };
    }

    private String resolveSparringReward(NovelReaderSettings settings, int realmIndex) {
        CombatStats stats = calculateCombatStats();
        long combatScore = stats.attack() + stats.defense() + stats.mana() / 2L + stats.health() / 20L;
        long targetScore = 480L + Math.max(0, realmIndex) * 180L;
        boolean victory = combatScore >= targetScore || nextSectEventInt(100) < 35;
        if (victory) {
            long stones = applyStoneBonus(180L + realmIndex * 80L);
            settings.addCurrentSectPrestige(45L);
            settings.setCultivationSpiritStones(settings.getCultivationSpiritStones() + stones);
            return "切磋胜利，威望 +45，灵石 +" + stones;
        }
        settings.addCurrentSectPrestige(10L);
        return "切磋落败，威望 +10";
    }

    private String resolveBackMountainReward(NovelReaderSettings settings, int realmIndex) {
        return switch (nextSectEventInt(3)) {
            case 0 -> {
                long stones = applyStoneBonus(240L + realmIndex * 120L);
                settings.setCultivationSpiritStones(settings.getCultivationSpiritStones() + stones);
                yield "灵石 +" + stones;
            }
            case 1 -> {
                settings.addPill(QI_PILL_ID, 1);
                yield "聚气丹 +1";
            }
            default -> {
                long qiGain = addCultivationQi(settings, applyQiBonus(700L + realmIndex * 300L));
                yield qiGain > 0L ? "修为 +" + qiGain : "";
            }
        };
    }

    private int nextSectEventInt(int bound) {
        Random random = sectEventRandom;
        return random != null ? random.nextInt(bound) : ThreadLocalRandom.current().nextInt(bound);
    }

    private int nextSectSecretRealmInt(int bound) {
        Random random = sectSecretRealmRandom;
        return random != null ? random.nextInt(bound) : ThreadLocalRandom.current().nextInt(bound);
    }

    private long getTravelDurationMillis(TravelLocationDefinition location) {
        return TimeUnit.MINUTES.toMillis(getTravelDurationMinutes(location));
    }

    private long getTravelDurationMinutes(TravelLocationDefinition location, int realmIndex) {
        if (location == null) {
            return 0L;
        }
        int reductionPercent = getTravelDurationReductionPercent(realmIndex)
                + SectRules.currentSectBonus(NovelReaderSettings.getInstance(), SectCatalog.BonusType.TRAVEL_DURATION);
        reductionPercent = Math.min(MAX_TRAVEL_DURATION_REDUCTION_PERCENT, reductionPercent);
        long reducedMinutes = Math.round(location.durationMinutes() * (100.0 - reductionPercent) / 100.0);
        return Math.max(1L, reducedMinutes);
    }

    private int getTravelDurationReductionPercent(int realmIndex) {
        int highestRealmIndex = Math.max(1, getRealmCount() - 1);
        int clampedRealmIndex = Math.max(0, Math.min(highestRealmIndex, realmIndex));
        return Math.min(
                MAX_TRAVEL_DURATION_REDUCTION_PERCENT,
                (int) Math.round(clampedRealmIndex * MAX_TRAVEL_DURATION_REDUCTION_PERCENT / (double) highestRealmIndex)
        );
    }

    private long getTravelRemainingMillis(NovelReaderSettings settings, TravelLocationDefinition location) {
        if (location == null) {
            return 0L;
        }
        return Math.max(0L, getTravelDurationMillis(location) - settings.getActiveTravelElapsedMillis());
    }

    private boolean isSeclusionPaused(NovelReaderSettings settings) {
        return hasActiveTravel(settings) || hasActiveBattle();
    }

    private String getSeclusionStatusKey(NovelReaderSettings settings) {
        if (hasActiveBattle()) {
            return "cultivation.status.seclusionPausedByChallenge";
        }
        if (hasActiveTravel(settings)) {
            return "cultivation.status.seclusionPaused";
        }
        return "cultivation.status.seclusionActive";
    }

    private boolean isMaxRealm(int realmIndex) {
        return CultivationRules.isMaxRealm(realmIndex);
    }

    private int getRealmCount() {
        return CultivationRules.realmCount();
    }

    private long getCultivationQiLimit(int realmIndex) {
        return isMaxRealm(realmIndex) ? 0L : getRequiredQi(realmIndex);
    }

    private long clampCultivationQi(NovelReaderSettings settings) {
        int realmIndex = settings.getCultivationRealmIndex();
        long currentQi = settings.getCultivationQi();
        long limit = getCultivationQiLimit(realmIndex);
        long clampedQi = limit <= 0L ? 0L : Math.min(currentQi, limit);
        if (currentQi != clampedQi) {
            settings.setCultivationQi(clampedQi);
        }
        return clampedQi;
    }

    private long getAvailableCultivationQiGain(NovelReaderSettings settings, long gain) {
        if (gain <= 0L) {
            clampCultivationQi(settings);
            return 0L;
        }
        int realmIndex = settings.getCultivationRealmIndex();
        long limit = getCultivationQiLimit(realmIndex);
        if (limit <= 0L) {
            clampCultivationQi(settings);
            return 0L;
        }
        long currentQi = clampCultivationQi(settings);
        return Math.min(gain, Math.max(0L, limit - currentQi));
    }

    private long addCultivationQi(NovelReaderSettings settings, long gain) {
        long actualGain = getAvailableCultivationQiGain(settings, gain);
        if (actualGain > 0L) {
            settings.setCultivationQi(settings.getCultivationQi() + actualGain);
        }
        return actualGain;
    }

    private int getBreakthroughChance(int realmIndex, int failures) {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        int baseChance = CultivationRules.baseBreakthroughChance(realmIndex);
        int techniqueBonus = getEquippedTechnique().breakthroughBonus();
        int pillBonus = settings.isBreakthroughPillActive() ? 18 : 0;
        int abodeBonus = isAbodeUnlocked() ? getInsightRoomBreakthroughBonusPercent(getAbodeFacilityLevel(INSIGHT_ROOM_ID)) : 0;
        int additiveChance = baseChance + failures * 16 + techniqueBonus + pillBonus + abodeBonus;
        return CultivationRules.finalBreakthroughChance(
                additiveChance,
                settings.getCultivationRebirthCount(),
                REBIRTH_BREAKTHROUGH_BONUS_PERCENT
        );
    }

    private int getInsightRoomBreakthroughBonusPercent(int level) {
        return Math.max(0, level) * INSIGHT_ROOM_BREAKTHROUGH_BONUS_PER_LEVEL;
    }

    private long getPassiveQiPerMinute(int realmIndex) {
        long base = 4L + Math.max(0, realmIndex) * 2L;
        return applyQiBonus(base);
    }

    private long getSeclusionQiPerMinute(int realmIndex) {
        long base = 24L + Math.max(0, realmIndex) * 10L;
        return applySeclusionQiBonus(base);
    }

    private long getManualQiGain(int realmIndex) {
        long base = 55L + Math.max(0, realmIndex) * 18L;
        return applyQiBonus(base);
    }

    private long applyQiBonus(long value) {
        int bonusPercent = getEquippedTechnique().qiBonusPercent()
                + getArtifactQiBonusPercent()
                + SectRules.currentSectBonus(NovelReaderSettings.getInstance(), SectCatalog.BonusType.QI);
        return applyRebirthQiBonus(applyPercent(value, bonusPercent));
    }

    private long applySeclusionQiBonus(long value) {
        int bonusPercent = getEquippedTechnique().qiBonusPercent()
                + getArtifactQiBonusPercent()
                + (isAbodeUnlocked() ? getSpiritGatheringBonusPercent(getAbodeFacilityLevel(SPIRIT_GATHERING_ARRAY_ID)) : 0)
                + SectRules.currentSectBonus(NovelReaderSettings.getInstance(), SectCatalog.BonusType.QI);
        return applyRebirthQiBonus(applyPercent(value, bonusPercent));
    }

    private int getArtifactAttackBonusPercent() {
        return getEquippedArtifacts().stream().mapToInt(ArtifactDefinition::attackBonusPercent).sum();
    }

    private int getArtifactDefenseBonusPercent() {
        return getEquippedArtifacts().stream().mapToInt(ArtifactDefinition::defenseBonusPercent).sum();
    }

    private int getArtifactManaBonusPercent() {
        return getEquippedArtifacts().stream().mapToInt(ArtifactDefinition::manaBonusPercent).sum();
    }

    private int getArtifactQiBonusPercent() {
        return getEquippedArtifacts().stream().mapToInt(ArtifactDefinition::qiBonusPercent).sum();
    }

    private List<ArtifactDefinition> getEquippedArtifacts() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        return settings.getEquippedArtifactIds().stream()
                .map(this::getArtifact)
                .filter(artifact -> artifact != null)
                .toList();
    }

    private int getSpiritGatheringBonusPercent(int level) {
        int clampedLevel = Math.max(0, Math.min(MAX_ABODE_LEVEL, level));
        return SPIRIT_GATHERING_BONUS_PERCENT[clampedLevel];
    }

    private long applyStoneBonus(long value) {
        return applyPercent(value, getEquippedTechnique().stoneBonusPercent());
    }

    private long applyRebirthQiBonus(long value) {
        return applyPercent(value, NovelReaderSettings.getInstance().getCultivationRebirthCount() * REBIRTH_QI_BONUS_PERCENT);
    }

    private String getRebirthMultiplierText() {
        double multiplier = 1.0 + NovelReaderSettings.getInstance().getCultivationRebirthCount() * REBIRTH_QI_BONUS_PERCENT / 100.0;
        return String.format(Locale.ROOT, "%.2f", multiplier);
    }

    private String getRebirthBreakthroughMultiplierText() {
        double multiplier = 1.0 + NovelReaderSettings.getInstance().getCultivationRebirthCount() * REBIRTH_BREAKTHROUGH_BONUS_PERCENT / 100.0;
        return String.format(Locale.ROOT, "%.2f", multiplier);
    }

    private String getRebirthBattleMultiplierText(int rebirthCount, int bonusPercent) {
        double multiplier = 1.0 + rebirthCount * bonusPercent / 100.0;
        return String.format(Locale.ROOT, "%.2f", multiplier);
    }

    private long applyPercent(long value, int percent) {
        if (value <= 0L) {
            return 0L;
        }
        return Math.max(1L, value * (100L + percent) / 100L);
    }

    private boolean isProductionAbodeFacility(String facilityId) {
        return SPIRIT_VEIN_ID.equals(facilityId) || ALCHEMY_ROOM_ID.equals(facilityId);
    }

    private long getClaimableSpiritVeinStones() {
        if (!isAbodeUnlocked()) {
            return 0L;
        }
        int level = getAbodeFacilityLevel(SPIRIT_VEIN_ID);
        if (level <= 0) {
            return 0L;
        }
        long periods = getClaimableAbodePeriods(SPIRIT_VEIN_ID, SPIRIT_VEIN_INTERVAL_MILLIS);
        return applyStoneBonus(periods * 10L * level);
    }

    private long getClaimableAlchemyPillCount() {
        if (!isAbodeUnlocked()) {
            return 0L;
        }
        int level = getAbodeFacilityLevel(ALCHEMY_ROOM_ID);
        long pendingCount = NovelReaderSettings.getInstance().getPendingAlchemyPillCount();
        if (level <= 0) {
            return pendingCount;
        }
        return pendingCount + getClaimableAbodePeriods(ALCHEMY_ROOM_ID, ALCHEMY_ROOM_INTERVAL_MILLIS);
    }

    private int preservePendingAlchemyPillsBeforeUpgrade(NovelReaderSettings settings, int oldLevel) {
        if (!isAbodeUnlocked() || oldLevel <= 0) {
            return 0;
        }
        long periods = getClaimableAbodePeriods(ALCHEMY_ROOM_ID, ALCHEMY_ROOM_INTERVAL_MILLIS);
        if (periods <= 0L) {
            return 0;
        }
        Map<String, Integer> pills = rollAlchemyPills(oldLevel, periods);
        settings.addPendingAlchemyPills(pills);
        advanceAbodeClaimTime(settings, ALCHEMY_ROOM_ID, ALCHEMY_ROOM_INTERVAL_MILLIS, periods);
        int total = 0;
        for (Integer count : pills.values()) {
            if (count != null && count > 0) {
                total += count;
            }
        }
        return total;
    }

    private long getClaimableAbodePeriods(String facilityId, long intervalMillis) {
        if (!isAbodeUnlocked()) {
            return 0L;
        }
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        long now = System.currentTimeMillis();
        long lastClaimMillis = settings.getAbodeLastClaimMillis(facilityId);
        if (lastClaimMillis <= 0L || lastClaimMillis > now) {
            settings.setAbodeLastClaimMillis(facilityId, now);
            return 0L;
        }
        long creditedMillis = Math.min(now - lastClaimMillis, OFFLINE_CAP_MILLIS);
        return creditedMillis / intervalMillis;
    }

    private AbodeReward claimAbodeFacilityInternal(String facilityId, boolean updateMessage) {
        ensureCultivationDefaults();
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        AbodeFacilityDefinition facility = getAbodeFacility(facilityId);
        if (facility == null) {
            if (updateMessage) {
                lastMessage = FishToucherBundle.message("cultivation.status.facilityUnknown");
            }
            return AbodeReward.empty();
        }

        AbodeReward reward = switch (facilityId) {
            case SPIRIT_VEIN_ID -> claimSpiritVein(settings);
            case ALCHEMY_ROOM_ID -> claimAlchemyRoom(settings);
            default -> AbodeReward.empty();
        };
        if (updateMessage) {
            lastMessage = reward.isEmpty()
                    ? FishToucherBundle.message("cultivation.status.nothingToClaim")
                    : FishToucherBundle.message("cultivation.status.claimedAbode", reward.summary());
        }
        return reward;
    }

    private AbodeReward claimSpiritVein(NovelReaderSettings settings) {
        long periods = getClaimableAbodePeriods(SPIRIT_VEIN_ID, SPIRIT_VEIN_INTERVAL_MILLIS);
        if (periods <= 0L) {
            return AbodeReward.empty();
        }
        long stones = applyStoneBonus(periods * 10L * getAbodeFacilityLevel(SPIRIT_VEIN_ID));
        settings.setCultivationSpiritStones(settings.getCultivationSpiritStones() + stones);
        advanceAbodeClaimTime(settings, SPIRIT_VEIN_ID, SPIRIT_VEIN_INTERVAL_MILLIS, periods);
        return new AbodeReward(stones, Collections.emptyMap());
    }

    private AbodeReward claimAlchemyRoom(NovelReaderSettings settings) {
        long periods = getClaimableAbodePeriods(ALCHEMY_ROOM_ID, ALCHEMY_ROOM_INTERVAL_MILLIS);
        Map<String, Integer> pills = new LinkedHashMap<>(settings.getPendingAlchemyPills());
        if (periods <= 0L && pills.isEmpty()) {
            return AbodeReward.empty();
        }
        int level = getAbodeFacilityLevel(ALCHEMY_ROOM_ID);
        if (periods > 0L && level > 0) {
            Map<String, Integer> currentPills = rollAlchemyPills(level, periods);
            for (Map.Entry<String, Integer> entry : currentPills.entrySet()) {
                pills.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
            advanceAbodeClaimTime(settings, ALCHEMY_ROOM_ID, ALCHEMY_ROOM_INTERVAL_MILLIS, periods);
        }
        for (Map.Entry<String, Integer> entry : pills.entrySet()) {
            if (PILL_BY_ID.containsKey(entry.getKey()) && entry.getValue() != null && entry.getValue() > 0) {
                settings.addPill(entry.getKey(), entry.getValue());
            }
        }
        settings.clearPendingAlchemyPills();
        return new AbodeReward(0L, pills);
    }

    private Map<String, Integer> rollAlchemyPills(int level, long periods) {
        Map<String, Integer> pills = new LinkedHashMap<>();
        for (long i = 0; i < periods; i++) {
            String pillId = chooseAlchemyPill(level);
            pills.merge(pillId, 1, Integer::sum);
        }
        return pills;
    }

    private void advanceAbodeClaimTime(NovelReaderSettings settings, String facilityId, long intervalMillis, long periods) {
        long now = System.currentTimeMillis();
        long lastClaimMillis = settings.getAbodeLastClaimMillis(facilityId);
        if (lastClaimMillis <= 0L || lastClaimMillis > now || now - lastClaimMillis > OFFLINE_CAP_MILLIS) {
            settings.setAbodeLastClaimMillis(facilityId, now);
            return;
        }
        settings.setAbodeLastClaimMillis(facilityId, lastClaimMillis + periods * intervalMillis);
    }

    private String chooseAlchemyPill(int level) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int rareChance = getAlchemyRarePillChance(level);
        if (random.nextInt(100) < rareChance) {
            return random.nextBoolean() ? BREAKTHROUGH_PILL_ID : MERIDIAN_PILL_ID;
        }
        return random.nextBoolean() ? QI_PILL_ID : SPIRIT_PILL_ID;
    }

    private int getAlchemyRarePillChance(int level) {
        if (level <= 0) {
            return 0;
        }
        return Math.min(35, 6 + level * 5);
    }

    private String choosePillForLocation(String locationId) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return switch (locationId) {
            case "abandoned_alchemy_room" -> switch (random.nextInt(4)) {
                case 0 -> QI_PILL_ID;
                case 1 -> BREAKTHROUGH_PILL_ID;
                case 2 -> MERIDIAN_PILL_ID;
                default -> SPIRIT_PILL_ID;
            };
            case "spirit_mine" -> random.nextInt(100) < 70 ? SPIRIT_PILL_ID : QI_PILL_ID;
            case "cloud_dream_secret" -> PILLS.get(random.nextInt(PILLS.size())).id();
            default -> random.nextInt(100) < 75 ? QI_PILL_ID : SPIRIT_PILL_ID;
        };
    }

    private String chooseTechniqueForTravel() {
        List<TechniqueDefinition> candidates = TECHNIQUES.stream()
                .filter(technique -> EVERGREEN_TECHNIQUE_ID.equals(technique.id())
                        || STONE_GATHERING_TECHNIQUE_ID.equals(technique.id())
                        || MYSTIC_ORTHODOX_TECHNIQUE_ID.equals(technique.id()))
                .toList();
        if (candidates.isEmpty()) {
            return "";
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size())).id();
    }

    private int getTravelSpellChance(String locationId) {
        return switch (locationId) {
            case "abandoned_alchemy_room" -> 10;
            case "spirit_mine" -> 14;
            case "cloud_dream_secret" -> 20;
            default -> 6;
        };
    }

    private String chooseSpellForTravel() {
        if (SPELLS.isEmpty()) {
            return "";
        }
        return SPELLS.get(ThreadLocalRandom.current().nextInt(SPELLS.size())).id();
    }

    private String formatDuration(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long hours = minutes / 60L;
        long remainingMinutes = minutes % 60L;
        if (hours > 0L) {
            return hours + "h " + remainingMinutes + "m";
        }
        return Math.max(1L, remainingMinutes) + "m";
    }

    private String formatRemainingDuration(long millis) {
        long minutes = Math.max(1L, TimeUnit.MILLISECONDS.toMinutes(millis + TimeUnit.MINUTES.toMillis(1) - 1L));
        long hours = minutes / 60L;
        long remainingMinutes = minutes % 60L;
        if (hours > 0L) {
            return hours + "h " + remainingMinutes + "m";
        }
        return minutes + "m";
    }

    private static Map<String, TechniqueDefinition> indexTechniques() {
        Map<String, TechniqueDefinition> result = new LinkedHashMap<>();
        for (TechniqueDefinition technique : TECHNIQUES) {
            result.put(technique.id(), technique);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, PillDefinition> indexPills() {
        Map<String, PillDefinition> result = new LinkedHashMap<>();
        for (PillDefinition pill : PILLS) {
            result.put(pill.id(), pill);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, SpellDefinition> indexSpells() {
        Map<String, SpellDefinition> result = new LinkedHashMap<>();
        for (SpellDefinition spell : SPELLS) {
            result.put(spell.id(), spell);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, ArtifactDefinition> indexArtifacts() {
        Map<String, ArtifactDefinition> result = new LinkedHashMap<>();
        for (ArtifactDefinition artifact : ARTIFACTS) {
            result.put(artifact.id(), artifact);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, TravelLocationDefinition> indexTravelLocations() {
        Map<String, TravelLocationDefinition> result = new LinkedHashMap<>();
        for (TravelLocationDefinition location : TRAVEL_LOCATIONS) {
            result.put(location.id(), location);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, CultivatorDefinition> indexCultivators() {
        Map<String, CultivatorDefinition> result = new LinkedHashMap<>();
        for (CultivatorDefinition cultivator : CULTIVATORS) {
            result.put(cultivator.id(), cultivator);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, AbodeFacilityDefinition> indexAbodeFacilities() {
        Map<String, AbodeFacilityDefinition> result = new LinkedHashMap<>();
        for (AbodeFacilityDefinition facility : ABODE_FACILITIES) {
            result.put(facility.id(), facility);
        }
        return Collections.unmodifiableMap(result);
    }

    public enum BattleSpellType {
        DAMAGE,
        FROST,
        HEAL,
        SHIELD
    }

    public record TechniqueDefinition(String id, String nameKey, String descriptionKey,
                                      int qiBonusPercent, int stoneBonusPercent, int breakthroughBonus,
                                      int attackBonusPercent, int defenseBonusPercent, int manaBonusPercent) {
        public String name() {
            return FishToucherBundle.message(nameKey);
        }

        public String description() {
            return FishToucherBundle.message(descriptionKey);
        }
    }

    public record SpellDefinition(String id, String nameKey, String descriptionKey,
                                  int manaCost, int cooldownSeconds,
                                  BattleSpellType type, int powerPercent) {
        public String name() {
            return FishToucherBundle.message(nameKey);
        }

        public String description() {
            return FishToucherBundle.message(descriptionKey);
        }
    }

    public record ArtifactDefinition(String id, String nameKey, String descriptionKey,
                                     int attackBonusPercent, int defenseBonusPercent,
                                     int manaBonusPercent, int qiBonusPercent) {
        public String name() {
            return FishToucherBundle.message(nameKey);
        }

        public String description() {
            return FishToucherBundle.message(descriptionKey);
        }
    }

    public record PillDefinition(String id, String nameKey, String descriptionKey) {
        public String name() {
            return FishToucherBundle.message(nameKey);
        }

        public String description() {
            return FishToucherBundle.message(descriptionKey);
        }
    }

    public record TravelLocationDefinition(String id, String nameKey, String descriptionKey,
                                           int durationMinutes, int minRealmIndex,
                                           long baseQiReward, long baseStoneReward,
                                           int pillChance, int techniqueChance) {
        public String name() {
            return FishToucherBundle.message(nameKey);
        }

        public String description() {
            return FishToucherBundle.message(descriptionKey);
        }
    }

    public record CultivatorDefinition(String id, String nameKey,
                                       long maxHealth, long attack, long defense, long mana,
                                       long stoneReward, String spellRewardId,
                                       String techniqueRewardId, String artifactRewardId) {
        public String name() {
            return nameKey != null && nameKey.startsWith("cultivation.")
                    ? FishToucherBundle.message(nameKey)
                    : nameKey;
        }
    }

    public record CombatStats(long attack, long defense, long mana, long health) {}

    public record SectEventInstance(String instanceId, String sectId, long createdMillis,
                                    SectCatalog.SectEventDefinition event) {}

    public record BattleSnapshot(CultivatorDefinition cultivator, CombatStats playerStats,
                                 long playerHealth, long playerMana,
                                 long enemyHealth, long enemyMaxHealth,
                                 boolean finished, boolean victory,
                                 String statusText, List<String> logs) {}

    public record AbodeFacilityDefinition(String id, String nameKey, String descriptionKey, long baseCost) {
        public String name() {
            return FishToucherBundle.message(nameKey);
        }

        public String description() {
            return FishToucherBundle.message(descriptionKey);
        }
    }

    public record AbodeReward(long stones, Map<String, Integer> pills) {
        public static AbodeReward empty() {
            return new AbodeReward(0L, Collections.emptyMap());
        }

        public boolean isEmpty() {
            return stones <= 0L && (pills == null || pills.isEmpty());
        }

        public String summary() {
            List<String> parts = new ArrayList<>();
            if (stones > 0L) {
                parts.add(FishToucherBundle.message("cultivation.reward.stones", stones));
            }
            if (pills != null) {
                for (Map.Entry<String, Integer> entry : pills.entrySet()) {
                    PillDefinition pill = PILL_BY_ID.get(entry.getKey());
                    if (pill != null && entry.getValue() != null && entry.getValue() > 0) {
                        parts.add(FishToucherBundle.message("cultivation.reward.pill", pill.name(), entry.getValue()));
                    }
                }
            }
            return parts.isEmpty() ? FishToucherBundle.message("cultivation.abode.claimNone") : String.join(", ", parts);
        }
    }

    @Override
    public void dispose() {
        stop();
        listeners.clear();
    }

    public record TravelReward(long qi, long stones, String pillId, int pillCount,
                               String techniqueId, boolean duplicateTechnique,
                               String spellId, boolean duplicateSpell) {
        public static TravelReward empty() {
            return new TravelReward(0L, 0L, "", 0, "", false, "", false);
        }

        public String summary() {
            List<String> parts = new ArrayList<>();
            parts.add(FishToucherBundle.message("cultivation.reward.qi", qi));
            parts.add(FishToucherBundle.message("cultivation.reward.stones", stones));
            if (pillId != null && !pillId.isEmpty() && pillCount > 0) {
                PillDefinition pill = PILL_BY_ID.get(pillId);
                if (pill != null) {
                    parts.add(FishToucherBundle.message("cultivation.reward.pill", pill.name(), pillCount));
                }
            }
            if (techniqueId != null && !techniqueId.isEmpty()) {
                TechniqueDefinition technique = TECHNIQUE_BY_ID.get(techniqueId);
                if (technique != null) {
                    parts.add(duplicateTechnique
                            ? FishToucherBundle.message("cultivation.reward.duplicateTechnique", technique.name())
                            : FishToucherBundle.message("cultivation.reward.technique", technique.name()));
                }
            }
            if (spellId != null && !spellId.isEmpty()) {
                SpellDefinition spell = SPELL_BY_ID.get(spellId);
                if (spell != null) {
                    parts.add(duplicateSpell
                            ? FishToucherBundle.message("cultivation.reward.duplicateSpellTravel", spell.name())
                            : FishToucherBundle.message("cultivation.reward.spell", spell.name()));
                }
            }
            return FishToucherBundle.message("cultivation.status.travelClaimed", String.join(", ", parts));
        }
    }

    private record ChallengeReward(long stones, List<String> parts) {
        public String summary() {
            return String.join(", ", parts);
        }
    }

    private static class BattleState {
        private final CultivatorDefinition cultivator;
        private final CombatStats playerStats;
        private final List<SpellDefinition> spells;
        private final String sectTrialId;
        private final Map<String, Integer> spellCooldowns = new LinkedHashMap<>();
        private final List<String> logs = new ArrayList<>();
        private long playerHealth;
        private long playerMana;
        private long enemyHealth;
        private int elapsedSeconds;
        private int playerAttackCooldown;
        private int enemyAttackCooldown;
        private int skipEnemyAttacks;
        private int shieldHits;
        private boolean finished;
        private boolean victory;

        private BattleState(CultivatorDefinition cultivator, CombatStats playerStats, List<SpellDefinition> spells) {
            this(cultivator, playerStats, spells, "");
        }

        private BattleState(CultivatorDefinition cultivator, CombatStats playerStats, List<SpellDefinition> spells, String sectTrialId) {
            this.cultivator = cultivator;
            this.playerStats = playerStats;
            this.spells = List.copyOf(spells);
            this.sectTrialId = sectTrialId != null ? sectTrialId : "";
            this.playerHealth = playerStats.health();
            this.playerMana = playerStats.mana();
            this.enemyHealth = cultivator.maxHealth();
        }

        private String statusText() {
            if (finished) {
                return FishToucherBundle.message(victory
                        ? "cultivation.status.battleVictory"
                        : "cultivation.status.battleDefeat");
            }
            return FishToucherBundle.message("cultivation.status.battleRunning");
        }
    }
}
