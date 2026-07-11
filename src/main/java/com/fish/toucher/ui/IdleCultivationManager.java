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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntUnaryOperator;
import java.util.function.LongSupplier;

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

    private static final List<TechniqueDefinition> TECHNIQUES = CultivationCatalog.TECHNIQUES;
    private static final Map<String, TechniqueDefinition> TECHNIQUE_BY_ID = CultivationCatalog.TECHNIQUE_BY_ID;

    private static final List<PillDefinition> PILLS = CultivationCatalog.PILLS;
    private static final Map<String, PillDefinition> PILL_BY_ID = CultivationCatalog.PILL_BY_ID;

    private static final List<SpellDefinition> SPELLS = CultivationCatalog.SPELLS;
    private static final Map<String, SpellDefinition> SPELL_BY_ID = CultivationCatalog.SPELL_BY_ID;

    private static final List<ArtifactDefinition> ARTIFACTS = CultivationCatalog.ARTIFACTS;
    private static final Map<String, ArtifactDefinition> ARTIFACT_BY_ID = CultivationCatalog.ARTIFACT_BY_ID;

    private static final List<TravelLocationDefinition> TRAVEL_LOCATIONS = CultivationCatalog.TRAVEL_LOCATIONS;
    private static final Map<String, TravelLocationDefinition> TRAVEL_BY_ID = CultivationCatalog.TRAVEL_BY_ID;

    private static final List<CultivatorDefinition> CULTIVATORS = CultivationCatalog.CULTIVATORS;
    private static final Map<String, CultivatorDefinition> CULTIVATOR_BY_ID = CultivationCatalog.CULTIVATOR_BY_ID;

    private static final List<AbodeFacilityDefinition> ABODE_FACILITIES = CultivationCatalog.ABODE_FACILITIES;
    private static final Map<String, AbodeFacilityDefinition> ABODE_BY_ID = CultivationCatalog.ABODE_BY_ID;

    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean notificationPending = new AtomicBoolean();
    private final LongSupplier currentTimeMillis;
    private final IntUnaryOperator randomInt;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> tickTask;
    private ScheduledFuture<?> battleTask;
    private boolean running;
    private String lastMessage;
    private BattleState battleState;

    public static IdleCultivationManager getInstance() {
        return ApplicationManager.getApplication().getService(IdleCultivationManager.class);
    }

    public IdleCultivationManager() {
        this(System::currentTimeMillis, bound -> ThreadLocalRandom.current().nextInt(bound));
    }

    IdleCultivationManager(LongSupplier currentTimeMillis, IntUnaryOperator randomInt) {
        this.currentTimeMillis = currentTimeMillis;
        this.randomInt = randomInt;
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
                try {
                    listener.run();
                } catch (RuntimeException exception) {
                    LOG.warn("fireChange: cultivation listener failed", exception);
                }
            }
        });
    }

    private long now() {
        return Math.max(0L, currentTimeMillis.getAsLong());
    }

    private int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        return Math.floorMod(randomInt.applyAsInt(bound), bound);
    }

    public synchronized void start() {
        NovelReaderSettings.getInstance().registerCultivationTransactionMonitor(this);
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
        long now = now();
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
        boolean success = nextInt(100) < successChance;
        settings.setBreakthroughPillActive(false);
        settings.setMeridianPillActive(false);

        if (success) {
            int nextRealm = realmIndex + 1;
            settings.setCultivationRealmIndex(nextRealm);
            settings.setCultivationQi(0L);
            settings.setCultivationBreakthroughFailures(0);
            lastMessage = FishToucherBundle.message("cultivation.status.breakthroughSuccess", getRealmName(nextRealm));
        } else {
            settings.setCultivationBreakthroughFailures(settings.getCultivationBreakthroughFailures() + 1);
            settings.setCultivationQi(CultivationProgressEngine.retainedQiAfterFailedBreakthrough(
                    requiredQi,
                    usedMeridianPill
            ));
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
        long now = now();
        long lastUpdate = settings.getCultivationLastUpdateMillis();
        if (lastUpdate <= 0L || lastUpdate > now) {
            settings.setCultivationLastUpdateMillis(now);
            return;
        }

        long elapsedMillis = now - lastUpdate;
        if (elapsedMillis < 1_000L) {
            return;
        }

        int realmIndex = settings.getCultivationRealmIndex();
        boolean offlineCatchUp = showOfflineMessage || elapsedMillis > SECLUSION_ONLINE_WINDOW_MILLIS;
        CultivationProgressEngine.ProgressGain progress = CultivationProgressEngine.settle(
                elapsedMillis,
                OFFLINE_CAP_MILLIS,
                settings.getCultivationQiRemainderSeconds(),
                getPassiveQiPerMinute(realmIndex),
                getSeclusionQiPerMinute(realmIndex),
                !offlineCatchUp && !isSeclusionPaused(settings)
        );
        long creditedMillis = progress.creditedMillis();
        long qiGain = progress.qiGain();
        settings.setCultivationQiRemainderSeconds(progress.remainderUnits());
        settings.setCultivationSpiritStoneRemainderSeconds(0L);
        boolean travelProgressed = advanceActiveTravel(settings, creditedMillis);

        long actualQiGain = 0L;
        if (qiGain > 0L) {
            actualQiGain = addCultivationQi(settings, qiGain);
        }
        settings.setCultivationLastUpdateMillis(now);

        if (actualQiGain > 0L || travelProgressed) {
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
        if (!isTravelUnlocked(location)) {
            lastMessage = FishToucherBundle.message("cultivation.status.travelLocked", getRealmName(location.minRealmIndex()));
            fireChange();
            return false;
        }

        long now = now();
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
        long qiGain = applyQiBonus(Math.round(location.baseQiReward() * (1.0 + realmIndex * 0.04)));
        long stoneGain = applyStoneBonus(Math.round(location.baseStoneReward() * (1.0 + realmIndex * 0.08)));
        String pillId = "";
        int pillCount = 0;
        if (nextInt(100) < location.pillChance()) {
            pillId = choosePillForLocation(location.id());
            pillCount = nextInt(100) < 18 ? 2 : 1;
            settings.addPill(pillId, pillCount);
        }

        String techniqueId = "";
        boolean duplicateTechnique = false;
        if (nextInt(100) < location.techniqueChance()) {
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
        if (nextInt(100) < getTravelSpellChance(location.id())) {
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

    public synchronized boolean rebirth(String retainedTechniqueId) {
        settleProgress(false);
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        if (!canRebirth()) {
            lastMessage = FishToucherBundle.message("cultivation.status.rebirthUnavailable");
            fireChange();
            return false;
        }

        TechniqueDefinition retainedTechnique = TECHNIQUE_BY_ID.get(retainedTechniqueId);
        if (retainedTechnique == null || !settings.isTechniqueUnlocked(retainedTechnique.id())) {
            lastMessage = FishToucherBundle.message("cultivation.status.rebirthTechniqueInvalid");
            fireChange();
            return false;
        }

        int nextRebirthCount = settings.getCultivationRebirthCount() + 1;
        long now = now();
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
        settings.clearPillInventory();
        settings.clearAbodeState();
        settings.resetUnlockedTechniquesForRebirth(retainedTechnique.id());
        ensureCultivationDefaults();

        lastMessage = FishToucherBundle.message(
                "cultivation.status.rebirthSuccess",
                nextRebirthCount,
                retainedTechnique.name()
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
        return hasActiveTravel() || hasActiveBattle();
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
        return canMeditate(now());
    }

    public synchronized String getMeditationRemainingText() {
        return getMeditationRemainingText(now());
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

        if (isProductionAbodeFacility(facilityId) && currentLevel > 0) {
            claimAbodeFacilityInternal(facilityId, false);
        }
        settings.setCultivationSpiritStones(settings.getCultivationSpiritStones() - cost);
        int nextLevel = currentLevel + 1;
        settings.setAbodeFacilityLevel(facilityId, nextLevel);
        if (isProductionAbodeFacility(facilityId)) {
            settings.setAbodeLastClaimMillis(facilityId, now());
        }
        lastMessage = FishToucherBundle.message("cultivation.status.facilityUpgraded", facility.name(), nextLevel);
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
        return getClaimableSpiritVeinStones() > 0L || getClaimableAlchemyPillCount() > 0L;
    }

    public synchronized AbodeReward claimAbodeFacility(String facilityId) {
        settleProgress(false);
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
        return CultivationBattleEngine.recovery(stats.health(), BATTLE_HEALTH_RECOVERY_DIVISOR);
    }

    private long calculateBattleManaRecovery(CombatStats stats) {
        return CultivationBattleEngine.recovery(stats.mana(), BATTLE_MANA_RECOVERY_DIVISOR);
    }

    private void finishBattle(BattleState state, boolean victory) {
        settleProgress(false);
        state.finished = true;
        state.victory = victory;
        cancelBattleTask();
        if (victory) {
            ChallengeReward reward = grantChallengeReward(state.cultivator);
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

    private int getCultivatorOrder(CultivatorDefinition cultivator) {
        int index = CULTIVATORS.indexOf(cultivator);
        return index < 0 ? 1 : index + 1;
    }

    private long calculateBattleDamage(long attack, long defense, int attackPercent, int defensePercent) {
        return CultivationBattleEngine.damage(attack, defense, attackPercent, defensePercent);
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
        attack = applyPercent(attack, technique.attackBonusPercent() + getArtifactAttackBonusPercent());
        defense = applyPercent(defense, technique.defenseBonusPercent() + getArtifactDefenseBonusPercent());
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
        long now = now();
        for (AbodeFacilityDefinition facility : ABODE_FACILITIES) {
            if (settings.getAbodeLastClaimMillis(facility.id()) <= 0L) {
                settings.setAbodeLastClaimMillis(facility.id(), now);
            }
            int level = getAbodeFacilityLevel(facility.id());
            if (level != settings.getAbodeFacilityLevel(facility.id())) {
                settings.setAbodeFacilityLevel(facility.id(), level);
            }
        }
        clampCultivationQi(settings);
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

    private long getTravelDurationMillis(TravelLocationDefinition location) {
        return TimeUnit.MINUTES.toMillis(getTravelDurationMinutes(location));
    }

    private long getTravelDurationMinutes(TravelLocationDefinition location, int realmIndex) {
        if (location == null) {
            return 0L;
        }
        int reductionPercent = getTravelDurationReductionPercent(realmIndex);
        return CultivationProgressEngine.travelDurationMinutes(location.durationMinutes(), reductionPercent);
    }

    private int getTravelDurationReductionPercent(int realmIndex) {
        return CultivationProgressEngine.travelReductionPercent(
                realmIndex,
                getRealmCount(),
                MAX_TRAVEL_DURATION_REDUCTION_PERCENT
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
        int abodeBonus = getInsightRoomBreakthroughBonusPercent(getAbodeFacilityLevel(INSIGHT_ROOM_ID));
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
        int bonusPercent = getEquippedTechnique().qiBonusPercent() + getArtifactQiBonusPercent();
        return applyRebirthQiBonus(applyPercent(value, bonusPercent));
    }

    private long applySeclusionQiBonus(long value) {
        int bonusPercent = getEquippedTechnique().qiBonusPercent()
                + getArtifactQiBonusPercent()
                + getSpiritGatheringBonusPercent(getAbodeFacilityLevel(SPIRIT_GATHERING_ARRAY_ID));
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
        int level = getAbodeFacilityLevel(SPIRIT_VEIN_ID);
        if (level <= 0) {
            return 0L;
        }
        long periods = getClaimableAbodePeriods(SPIRIT_VEIN_ID, SPIRIT_VEIN_INTERVAL_MILLIS);
        return applyStoneBonus(periods * 10L * level);
    }

    private long getClaimableAlchemyPillCount() {
        int level = getAbodeFacilityLevel(ALCHEMY_ROOM_ID);
        if (level <= 0) {
            return 0L;
        }
        return getClaimableAbodePeriods(ALCHEMY_ROOM_ID, ALCHEMY_ROOM_INTERVAL_MILLIS);
    }

    private long getClaimableAbodePeriods(String facilityId, long intervalMillis) {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        long now = now();
        long lastClaimMillis = settings.getAbodeLastClaimMillis(facilityId);
        if (lastClaimMillis <= 0L || lastClaimMillis > now) {
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
        if (periods <= 0L) {
            return AbodeReward.empty();
        }
        int level = getAbodeFacilityLevel(ALCHEMY_ROOM_ID);
        Map<String, Integer> pills = new LinkedHashMap<>();
        for (long i = 0; i < periods; i++) {
            String pillId = chooseAlchemyPill(level);
            pills.merge(pillId, 1, Integer::sum);
            settings.addPill(pillId, 1);
        }
        advanceAbodeClaimTime(settings, ALCHEMY_ROOM_ID, ALCHEMY_ROOM_INTERVAL_MILLIS, periods);
        return new AbodeReward(0L, pills);
    }

    private void advanceAbodeClaimTime(NovelReaderSettings settings, String facilityId, long intervalMillis, long periods) {
        long now = now();
        long lastClaimMillis = settings.getAbodeLastClaimMillis(facilityId);
        if (lastClaimMillis <= 0L || lastClaimMillis > now || now - lastClaimMillis > OFFLINE_CAP_MILLIS) {
            settings.setAbodeLastClaimMillis(facilityId, now);
            return;
        }
        settings.setAbodeLastClaimMillis(facilityId, lastClaimMillis + periods * intervalMillis);
    }

    private String chooseAlchemyPill(int level) {
        int rareChance = getAlchemyRarePillChance(level);
        if (nextInt(100) < rareChance) {
            return nextInt(2) == 0 ? BREAKTHROUGH_PILL_ID : MERIDIAN_PILL_ID;
        }
        return nextInt(2) == 0 ? QI_PILL_ID : SPIRIT_PILL_ID;
    }

    private int getAlchemyRarePillChance(int level) {
        if (level <= 0) {
            return 0;
        }
        return Math.min(35, 6 + level * 5);
    }

    private String choosePillForLocation(String locationId) {
        return switch (locationId) {
            case "abandoned_alchemy_room" -> switch (nextInt(4)) {
                case 0 -> QI_PILL_ID;
                case 1 -> BREAKTHROUGH_PILL_ID;
                case 2 -> MERIDIAN_PILL_ID;
                default -> SPIRIT_PILL_ID;
            };
            case "spirit_mine" -> nextInt(100) < 70 ? SPIRIT_PILL_ID : QI_PILL_ID;
            case "cloud_dream_secret" -> PILLS.get(nextInt(PILLS.size())).id();
            default -> nextInt(100) < 75 ? QI_PILL_ID : SPIRIT_PILL_ID;
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
        return candidates.get(nextInt(candidates.size())).id();
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
        return SPELLS.get(nextInt(SPELLS.size())).id();
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
            return FishToucherBundle.message(nameKey);
        }
    }

    public record CombatStats(long attack, long defense, long mana, long health) {}

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
            this.cultivator = cultivator;
            this.playerStats = playerStats;
            this.spells = List.copyOf(spells);
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
