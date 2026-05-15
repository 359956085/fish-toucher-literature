package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;
import com.intellij.openapi.application.ApplicationManager;
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

public class IdleCultivationManager {

    public static final String BASIC_TECHNIQUE_ID = "basic_breathing";
    private static final String QI_PILL_ID = "qi_pill";
    private static final String SPIRIT_PILL_ID = "spirit_pill";
    private static final String BREAKTHROUGH_PILL_ID = "breakthrough_pill";
    private static final String MERIDIAN_PILL_ID = "meridian_pill";
    private static final String SPIRIT_GATHERING_ARRAY_ID = "spirit_gathering_array";
    private static final String SPIRIT_VEIN_ID = "spirit_vein";
    private static final String ALCHEMY_ROOM_ID = "alchemy_room";
    private static final String INSIGHT_ROOM_ID = "insight_room";

    private static final Logger LOG = Logger.getInstance(IdleCultivationManager.class);
    private static final IdleCultivationManager INSTANCE = new IdleCultivationManager();

    private static final int OFFLINE_CAP_HOURS = 8;
    public static final int MAX_ABODE_LEVEL = 5;
    private static final long OFFLINE_CAP_MILLIS = TimeUnit.HOURS.toMillis(OFFLINE_CAP_HOURS);
    private static final long TICK_SECONDS = 10;
    private static final long MEDITATION_COOLDOWN_MILLIS = TimeUnit.MINUTES.toMillis(10);
    private static final int REBIRTH_QI_BONUS_PERCENT = 25;
    private static final int REBIRTH_BREAKTHROUGH_BONUS_PERCENT = 15;
    private static final long SPIRIT_VEIN_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(1);
    private static final long ALCHEMY_ROOM_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(3);
    private static final long[] REQUIRED_QI = {
            20_000L, 45_000L, 90_000L, 165_000L, 285_000L, 455_000L, 710_000L, 1_050_000L
    };

    private static final List<TechniqueDefinition> TECHNIQUES = List.of(
            new TechniqueDefinition(BASIC_TECHNIQUE_ID, "cultivation.technique.basic.name", "cultivation.technique.basic.desc", 0, 0, 0),
            new TechniqueDefinition("evergreen_method", "cultivation.technique.evergreen.name", "cultivation.technique.evergreen.desc", 18, 0, 0),
            new TechniqueDefinition("stone_gathering", "cultivation.technique.stone.name", "cultivation.technique.stone.desc", 0, 25, 0),
            new TechniqueDefinition("mystic_orthodox", "cultivation.technique.mystic.name", "cultivation.technique.mystic.desc", 12, 12, 6)
    );
    private static final Map<String, TechniqueDefinition> TECHNIQUE_BY_ID = indexTechniques();

    private static final List<PillDefinition> PILLS = List.of(
            new PillDefinition(QI_PILL_ID, "cultivation.pill.qi.name", "cultivation.pill.qi.desc"),
            new PillDefinition(SPIRIT_PILL_ID, "cultivation.pill.spirit.name", "cultivation.pill.spirit.desc"),
            new PillDefinition(BREAKTHROUGH_PILL_ID, "cultivation.pill.breakthrough.name", "cultivation.pill.breakthrough.desc"),
            new PillDefinition(MERIDIAN_PILL_ID, "cultivation.pill.meridian.name", "cultivation.pill.meridian.desc")
    );
    private static final Map<String, PillDefinition> PILL_BY_ID = indexPills();

    private static final List<TravelLocationDefinition> TRAVEL_LOCATIONS = List.of(
            new TravelLocationDefinition("bamboo_forest", "cultivation.travel.bamboo.name", "cultivation.travel.bamboo.desc", 60, 0, 900, 25, 45, 4),
            new TravelLocationDefinition("abandoned_alchemy_room", "cultivation.travel.alchemy.name", "cultivation.travel.alchemy.desc", 120, 0, 2_400, 60, 70, 6),
            new TravelLocationDefinition("spirit_mine", "cultivation.travel.mine.name", "cultivation.travel.mine.desc", 240, 1, 7_000, 260, 40, 8),
            new TravelLocationDefinition("cloud_dream_secret", "cultivation.travel.secret.name", "cultivation.travel.secret.desc", 480, 2, 18_000, 600, 65, 15)
    );
    private static final Map<String, TravelLocationDefinition> TRAVEL_BY_ID = indexTravelLocations();

    private static final List<AbodeFacilityDefinition> ABODE_FACILITIES = List.of(
            new AbodeFacilityDefinition(SPIRIT_GATHERING_ARRAY_ID, "cultivation.abode.spiritGathering.name", "cultivation.abode.spiritGathering.desc", 180),
            new AbodeFacilityDefinition(SPIRIT_VEIN_ID, "cultivation.abode.spiritVein.name", "cultivation.abode.spiritVein.desc", 220),
            new AbodeFacilityDefinition(ALCHEMY_ROOM_ID, "cultivation.abode.alchemyRoom.name", "cultivation.abode.alchemyRoom.desc", 260),
            new AbodeFacilityDefinition(INSIGHT_ROOM_ID, "cultivation.abode.insightRoom.name", "cultivation.abode.insightRoom.desc", 300)
    );
    private static final Map<String, AbodeFacilityDefinition> ABODE_BY_ID = indexAbodeFacilities();

    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> tickTask;
    private boolean running;
    private String lastMessage = FishToucherBundle.message("cultivation.status.idle");

    public static IdleCultivationManager getInstance() {
        return INSTANCE;
    }

    private IdleCultivationManager() {}

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void fireChange() {
        for (Runnable listener : listeners) {
            ApplicationManager.getApplication().invokeLater(listener);
        }
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
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        tickTask = null;
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
        settings.setCultivationQi(settings.getCultivationQi() + qiGain);
        settings.setCultivationLastMeditationMillis(now);
        lastMessage = FishToucherBundle.message("cultivation.status.meditate", qiGain);
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
        long elapsedSeconds = creditedMillis / 1_000L;
        int realmIndex = settings.getCultivationRealmIndex();
        long qiUnits = settings.getCultivationQiRemainderSeconds() + elapsedSeconds * getQiPerMinute(realmIndex);
        long qiGain = qiUnits / 60L;
        settings.setCultivationQiRemainderSeconds(qiUnits % 60L);
        settings.setCultivationSpiritStoneRemainderSeconds(0L);
        boolean travelProgressed = advanceActiveTravel(settings, creditedMillis);

        if (qiGain > 0L) {
            settings.setCultivationQi(settings.getCultivationQi() + qiGain);
        }
        settings.setCultivationLastUpdateMillis(now);

        if (qiGain > 0L || travelProgressed) {
            if (showOfflineMessage && elapsedMillis > TICK_SECONDS * 1_000L) {
                lastMessage = FishToucherBundle.message(
                        "cultivation.status.offline",
                        qiGain,
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
                if (!settings.consumePill(pillId)) return false;
                long gain = applyQiBonus(Math.max(1_200L, getRequiredQi(realmIndex) / 25L));
                settings.setCultivationQi(settings.getCultivationQi() + gain);
                lastMessage = FishToucherBundle.message("cultivation.status.usedQiPill", gain);
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

        long now = System.currentTimeMillis();
        settings.setActiveTravelLocationId(location.id());
        settings.setTravelStartMillis(now);
        settings.setTravelEndMillis(0L);
        settings.setActiveTravelElapsedMillis(0L);
        lastMessage = FishToucherBundle.message("cultivation.status.travelStarted", location.name(), location.durationMinutes());
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
        long qiGain = applyQiBonus(Math.round(location.baseQiReward() * (1.0 + realmIndex * 0.18)));
        long stoneGain = applyStoneBonus(Math.round(location.baseStoneReward() * (1.0 + realmIndex * 0.22)));
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

        settings.setCultivationQi(settings.getCultivationQi() + qiGain);
        settings.setCultivationSpiritStones(settings.getCultivationSpiritStones() + stoneGain);
        settings.clearTravel();
        TravelReward reward = new TravelReward(qiGain, stoneGain, pillId, pillCount, techniqueId, duplicateTechnique);
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

    public synchronized String getRealmName() {
        return getRealmName(NovelReaderSettings.getInstance().getCultivationRealmIndex());
    }

    public String getRealmName(int realmIndex) {
        int clampedIndex = Math.max(0, Math.min(getRealmCount() - 1, realmIndex));
        return FishToucherBundle.message("cultivation.realm." + clampedIndex);
    }

    public synchronized long getCurrentQi() {
        return NovelReaderSettings.getInstance().getCultivationQi();
    }

    public synchronized long getRequiredQi() {
        return getRequiredQi(NovelReaderSettings.getInstance().getCultivationRealmIndex());
    }

    public long getRequiredQi(int realmIndex) {
        if (isMaxRealm(realmIndex)) {
            return 0L;
        }
        return REQUIRED_QI[Math.max(0, Math.min(REQUIRED_QI.length - 1, realmIndex))];
    }

    public synchronized long getSpiritStones() {
        return NovelReaderSettings.getInstance().getCultivationSpiritStones();
    }

    public synchronized int getProgressPercent() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        int realmIndex = settings.getCultivationRealmIndex();
        if (isMaxRealm(realmIndex)) {
            return 100;
        }
        long requiredQi = getRequiredQi(realmIndex);
        if (requiredQi <= 0L) {
            return 0;
        }
        return (int) Math.min(100L, settings.getCultivationQi() * 100L / requiredQi);
    }

    public synchronized boolean canBreakthrough() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        int realmIndex = settings.getCultivationRealmIndex();
        return !isMaxRealm(realmIndex) && settings.getCultivationQi() >= getRequiredQi(realmIndex);
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
        return lastMessage;
    }

    public synchronized String getStatusLine() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        int realmIndex = settings.getCultivationRealmIndex();
        String baseStatus;
        if (isMaxRealm(realmIndex)) {
            baseStatus = FishToucherBundle.message(
                    "cultivation.status.format",
                    getRealmName(realmIndex),
                    settings.getCultivationQi(),
                    FishToucherBundle.message("cultivation.status.max"),
                    100,
                    settings.getCultivationSpiritStones()
            );
        } else {
            baseStatus = FishToucherBundle.message(
                    "cultivation.status.format",
                    getRealmName(realmIndex),
                    settings.getCultivationQi(),
                    getRequiredQi(realmIndex),
                    getProgressPercent(),
                    settings.getCultivationSpiritStones()
            );
        }
        List<String> notices = new ArrayList<>();
        if (isTravelReady()) {
            notices.add(FishToucherBundle.message("cultivation.status.travelClaimReady"));
        }
        if (hasClaimableAbodeReward()) {
            notices.add(FishToucherBundle.message("cultivation.status.abodeClaimReady"));
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
                getQiPerMinute(realmIndex)
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
        return Math.max(0L, getTravelDurationMillis(location) - settings.getActiveTravelElapsedMillis());
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
            settings.setAbodeLastClaimMillis(facilityId, System.currentTimeMillis());
        }
        lastMessage = FishToucherBundle.message("cultivation.status.facilityUpgraded", facility.name(), nextLevel);
        fireChange();
        return true;
    }

    public synchronized String getAbodeFacilityEffectText(String facilityId) {
        int level = getAbodeFacilityLevel(facilityId);
        return switch (facilityId) {
            case SPIRIT_GATHERING_ARRAY_ID -> FishToucherBundle.message("cultivation.abode.effect.spiritGathering", level * 4);
            case SPIRIT_VEIN_ID -> FishToucherBundle.message("cultivation.abode.effect.spiritVein", applyStoneBonus(10L * level));
            case ALCHEMY_ROOM_ID -> FishToucherBundle.message("cultivation.abode.effect.alchemyRoom", getAlchemyRarePillChance(level));
            case INSIGHT_ROOM_ID -> FishToucherBundle.message("cultivation.abode.effect.insightRoom", level * 3);
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
            if (settings.getAbodeLastClaimMillis(facility.id()) <= 0L) {
                settings.setAbodeLastClaimMillis(facility.id(), now);
            }
            int level = getAbodeFacilityLevel(facility.id());
            if (level != settings.getAbodeFacilityLevel(facility.id())) {
                settings.setAbodeFacilityLevel(facility.id(), level);
            }
        }
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
        return TimeUnit.MINUTES.toMillis(location.durationMinutes());
    }

    private boolean isMaxRealm(int realmIndex) {
        return realmIndex >= getRealmCount() - 1;
    }

    private int getRealmCount() {
        return REQUIRED_QI.length + 1;
    }

    private int getBreakthroughChance(int realmIndex, int failures) {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        int baseChance = Math.max(42, 72 - realmIndex * 4);
        int techniqueBonus = getEquippedTechnique().breakthroughBonus();
        int pillBonus = settings.isBreakthroughPillActive() ? 18 : 0;
        int abodeBonus = getAbodeFacilityLevel(INSIGHT_ROOM_ID) * 3;
        int additiveChance = baseChance + failures * 16 + techniqueBonus + pillBonus + abodeBonus;
        long rebirthChance = additiveChance * (100L + settings.getCultivationRebirthCount() * REBIRTH_BREAKTHROUGH_BONUS_PERCENT) / 100L;
        return (int) Math.min(96L, rebirthChance);
    }

    private long getQiPerMinute(int realmIndex) {
        long base = 8L + Math.max(0, realmIndex) * 4L;
        return applyQiBonus(base);
    }

    private long getManualQiGain(int realmIndex) {
        long base = 55L + Math.max(0, realmIndex) * 18L;
        return applyQiBonus(base);
    }

    private long applyQiBonus(long value) {
        int bonusPercent = getEquippedTechnique().qiBonusPercent() + getAbodeFacilityLevel(SPIRIT_GATHERING_ARRAY_ID) * 4;
        return applyRebirthQiBonus(applyPercent(value, bonusPercent));
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
        long now = System.currentTimeMillis();
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
                .filter(technique -> !BASIC_TECHNIQUE_ID.equals(technique.id()))
                .toList();
        if (candidates.isEmpty()) {
            return "";
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size())).id();
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

    private static Map<String, TravelLocationDefinition> indexTravelLocations() {
        Map<String, TravelLocationDefinition> result = new LinkedHashMap<>();
        for (TravelLocationDefinition location : TRAVEL_LOCATIONS) {
            result.put(location.id(), location);
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

    public record TechniqueDefinition(String id, String nameKey, String descriptionKey,
                                      int qiBonusPercent, int stoneBonusPercent, int breakthroughBonus) {
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

    public record TravelReward(long qi, long stones, String pillId, int pillCount,
                               String techniqueId, boolean duplicateTechnique) {
        public static TravelReward empty() {
            return new TravelReward(0L, 0L, "", 0, "", false);
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
            return FishToucherBundle.message("cultivation.status.travelClaimed", String.join(", ", parts));
        }
    }
}
