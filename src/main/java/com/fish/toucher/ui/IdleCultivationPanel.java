package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.fish.toucher.ui.IdleCultivationUiSupport.*;

public class IdleCultivationPanel extends JPanel implements Disposable {

    private static final Logger LOG = Logger.getInstance(IdleCultivationPanel.class);
    private static final int CHALLENGE_LOG_HEIGHT = 150;

    private final JLabel realmValue;
    private final JLabel techniqueValue;
    private final JLabel qiValue;
    private final JLabel attackValue;
    private final JLabel defenseValue;
    private final JLabel healthValue;
    private final JLabel manaValue;
    private final JLabel stonesValue;
    private final JLabel rateValue;
    private final JLabel seclusionRateValue;
    private final JLabel chanceValue;
    private final JTextArea effectsValue;
    private final JLabel rebirthLabel;
    private final JTextArea rebirthValue;
    private final JTextArea messageLabel;
    private final JProgressBar progressBar;
    private final JButton meditateButton;
    private final JButton koiBlessingButton;
    private final JButton breakthroughButton;
    private final JButton rebirthButton;

    private final JComboBox<TechniqueOption> techniqueComboBox;
    private final JButton equipTechniqueButton;
    private final JTextArea techniqueDescriptionLabel;
    private final JComboBox<PillOption> pillComboBox;
    private final JButton usePillButton;
    private final JTextArea pillDescriptionLabel;
    private final List<JComboBox<SpellOption>> spellSlotComboBoxes;
    private final List<JTextArea> spellDescriptionLabels;
    private final JButton saveSpellSetupButton;
    private final List<JComboBox<ArtifactOption>> artifactSlotComboBoxes;
    private final JButton saveArtifactSetupButton;
    private final JTextArea artifactDescriptionLabel;

    private final JComboBox<TravelOption> travelComboBox;
    private final JTextArea travelDescriptionLabel;
    private final JTextArea activeTravelLabel;
    private final JProgressBar travelProgressBar;
    private final JButton startTravelButton;
    private final JButton claimTravelButton;

    private final JLabel abodeStonesValue;
    private final JPanel abodeFacilitiesPanel;

    private final JComboBox<CultivatorOption> cultivatorComboBox;
    private final JTextArea cultivatorDescriptionLabel;
    private final JTextArea battleStatsLabel;
    private final JLabel battleStatusLabel;
    private final JProgressBar playerHealthBar;
    private final JProgressBar enemyHealthBar;
    private final JProgressBar battleManaBar;
    private final JTextArea battleLogArea;
    private final JButton startChallengeButton;
    private final JButton endChallengeButton;

    private final Runnable changeListener;
    private boolean refreshing;
    private boolean adjustingArtifactSelection;

    public IdleCultivationPanel() {
        LOG.info("IdleCultivationPanel: initializing");
        setLayout(new BorderLayout());

        add(createTopBar(), BorderLayout.NORTH);

        realmValue = new JLabel();
        techniqueValue = new JLabel();
        qiValue = new JLabel();
        attackValue = new JLabel();
        defenseValue = new JLabel();
        healthValue = new JLabel();
        manaValue = new JLabel();
        stonesValue = new JLabel();
        rateValue = new JLabel();
        seclusionRateValue = new JLabel();
        chanceValue = new JLabel();
        effectsValue = createHintTextArea();
        rebirthLabel = new JLabel(FishToucherBundle.message("cultivation.label.rebirth"));
        rebirthLabel.setForeground(JBColor.GRAY);
        allowHorizontalShrink(rebirthLabel);
        rebirthValue = createHintTextArea();
        messageLabel = createHintTextArea();
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        meditateButton = new JButton(FishToucherBundle.message("cultivation.button.meditate"));
        koiBlessingButton = new JButton(FishToucherBundle.message("cultivation.button.koiBlessing"));
        breakthroughButton = new JButton(FishToucherBundle.message("cultivation.button.breakthrough"));
        rebirthButton = new JButton(FishToucherBundle.message("cultivation.button.rebirth"));

        techniqueComboBox = new JComboBox<>();
        equipTechniqueButton = new JButton(FishToucherBundle.message("cultivation.button.equipTechnique"));
        techniqueDescriptionLabel = createHintTextArea();
        pillComboBox = new JComboBox<>();
        usePillButton = new JButton(FishToucherBundle.message("cultivation.button.usePill"));
        pillDescriptionLabel = createHintTextArea();
        spellSlotComboBoxes = new ArrayList<>();
        spellDescriptionLabels = new ArrayList<>();
        saveSpellSetupButton = new JButton(FishToucherBundle.message("cultivation.button.saveSpellSetup"));
        artifactSlotComboBoxes = new ArrayList<>();
        saveArtifactSetupButton = new JButton(FishToucherBundle.message("cultivation.button.saveArtifactSetup"));
        artifactDescriptionLabel = createHintTextArea();

        travelComboBox = new JComboBox<>();
        travelDescriptionLabel = createHintTextArea();
        activeTravelLabel = createHintTextArea();
        travelProgressBar = new JProgressBar(0, 100);
        travelProgressBar.setStringPainted(true);
        startTravelButton = new JButton(FishToucherBundle.message("cultivation.button.startTravel"));
        claimTravelButton = new JButton(FishToucherBundle.message("cultivation.button.claimTravel"));
        abodeStonesValue = new JLabel();
        abodeFacilitiesPanel = new JPanel(new GridBagLayout());
        cultivatorComboBox = new JComboBox<>();
        cultivatorDescriptionLabel = createChallengeInfoTextArea(true, 4);
        battleStatsLabel = createChallengeInfoTextArea(false, 2);
        battleStatusLabel = new JLabel();
        battleStatusLabel.setForeground(JBColor.GRAY);
        playerHealthBar = new JProgressBar(0, 100);
        playerHealthBar.setStringPainted(true);
        enemyHealthBar = new JProgressBar(0, 100);
        enemyHealthBar.setStringPainted(true);
        battleManaBar = new JProgressBar(0, 100);
        battleManaBar.setStringPainted(true);
        battleLogArea = createGuideTextArea("");
        battleLogArea.setRows(8);
        startChallengeButton = new JButton(FishToucherBundle.message("cultivation.button.startChallenge"));
        endChallengeButton = new JButton(FishToucherBundle.message("cultivation.button.endChallenge"));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.addTab(FishToucherBundle.message("cultivation.tab.training"), createTrainingTab());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.bag"), createBagTab());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.travel"), createTravelTab());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.abode"), createAbodeTab());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.challenge"), createChallengeTab());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.guide"), createGuideTab());
        add(tabs, BorderLayout.CENTER);

        changeListener = this::refreshContent;
        IdleCultivationManager manager = IdleCultivationManager.getInstance();
        manager.addChangeListener(changeListener);
        manager.start();
        refreshContent();
    }

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()));
        JComboBox<String> modeCombo = PluginModeSelector.createCombo(NovelReaderSettings.MODE_CULTIVATION);
        modeCombo.addActionListener(e -> {
            String selectedMode = PluginModeSelector.getSelectedMode(modeCombo);
            if (!NovelReaderSettings.MODE_CULTIVATION.equals(selectedMode)) {
                NovelReaderToolWindowFactory.switchMode(selectedMode);
            }
        });
        topBar.add(modeCombo);
        JComboBox<String> languageCombo = LanguageSelector.createCombo(NovelReaderSettings.getInstance().getUiLanguage());
        languageCombo.addActionListener(e -> {
            String selectedLanguage = LanguageSelector.getSelectedLanguage(languageCombo);
            if (!selectedLanguage.equals(NovelReaderSettings.getInstance().getUiLanguage())) {
                NovelReaderToolWindowFactory.switchLanguage(selectedLanguage);
            }
        });
        topBar.add(languageCombo);
        return topBar;
    }

    private JPanel createTrainingTab() {
        JPanel panel = createFormPanel();
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.realm"), realmValue);
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.technique"), techniqueValue);
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.cultivation"), qiValue);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weightx = 1.0;
        panel.add(progressBar, gbc);
        row++;

        gbc.gridwidth = 1; gbc.weightx = 0;
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.attack"), attackValue);
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.defense"), defenseValue);
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.health"), healthValue);
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.mana"), manaValue);
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.spiritStones"), stonesValue);
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.rate"), rateValue);
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.seclusionRate"), seclusionRateValue);
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.chance"), chanceValue);
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.effects"), effectsValue);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(rebirthLabel, gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1.0;
        panel.add(rebirthValue, gbc);
        gbc.weightx = 0;
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        panel.add(messageLabel, gbc);
        row++;

        JPanel actions = createActionPanel();
        meditateButton.setToolTipText(FishToucherBundle.message("cultivation.tooltip.meditate"));
        meditateButton.setFocusable(false);
        meditateButton.addActionListener(e -> IdleCultivationManager.getInstance().meditateOnce());
        actions.add(meditateButton);

        koiBlessingButton.setToolTipText(FishToucherBundle.message("cultivation.tooltip.koiBlessing"));
        koiBlessingButton.setFocusable(false);
        koiBlessingButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                IdleCultivationManager.getInstance().receiveKoiBlessing();
            }
        });
        actions.add(koiBlessingButton);

        breakthroughButton.setToolTipText(FishToucherBundle.message("cultivation.tooltip.breakthrough"));
        breakthroughButton.setFocusable(false);
        breakthroughButton.addActionListener(e -> IdleCultivationManager.getInstance().tryBreakthrough());
        actions.add(breakthroughButton);

        rebirthButton.setFocusable(false);
        rebirthButton.addActionListener(e -> showRebirthDialog());
        actions.add(rebirthButton);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        panel.add(actions, gbc);
        addBottomGlue(panel, gbc, row + 1);
        return createScrollableTab(panel);
    }

    private JPanel createBagTab() {
        JPanel panel = createFormPanel();
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        JLabel techniqueTitle = createSectionLabel(FishToucherBundle.message("cultivation.section.techniques"));
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(techniqueTitle, gbc);

        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.technique"), techniqueComboBox);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(techniqueDescriptionLabel, gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        equipTechniqueButton.setFocusable(false);
        equipTechniqueButton.addActionListener(e -> {
            TechniqueOption option = (TechniqueOption) techniqueComboBox.getSelectedItem();
            if (option != null) {
                IdleCultivationManager.getInstance().equipTechnique(option.technique.id());
            }
        });
        panel.add(equipTechniqueButton, gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);

        JLabel pillTitle = createSectionLabel(FishToucherBundle.message("cultivation.section.pills"));
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(pillTitle, gbc);

        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.pill"), pillComboBox);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(pillDescriptionLabel, gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        usePillButton.setFocusable(false);
        usePillButton.addActionListener(e -> {
            PillOption option = (PillOption) pillComboBox.getSelectedItem();
            if (option != null) {
                IdleCultivationManager.getInstance().usePill(option.pill.id());
            }
        });
        panel.add(usePillButton, gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);

        JLabel spellTitle = createSectionLabel(FishToucherBundle.message("cultivation.section.spells"));
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(spellTitle, gbc);

        for (int i = 0; i < 3; i++) {
            JComboBox<SpellOption> spellComboBox = new JComboBox<>();
            spellComboBox.addActionListener(e -> updateSpellDescription());
            spellSlotComboBoxes.add(spellComboBox);
            addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.spellSlot", i + 1), spellComboBox);

            JTextArea spellDescription = createGuideTextArea("");
            spellDescriptionLabels.add(spellDescription);
            gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
            panel.add(spellDescription, gbc);
        }

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        saveSpellSetupButton.setFocusable(false);
        saveSpellSetupButton.addActionListener(e -> saveSpellSetup());
        panel.add(saveSpellSetupButton, gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);

        JLabel artifactTitle = createSectionLabel(FishToucherBundle.message("cultivation.section.artifacts"));
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(artifactTitle, gbc);

        for (int i = 0; i < 2; i++) {
            JComboBox<ArtifactOption> artifactComboBox = new JComboBox<>();
            artifactComboBox.addActionListener(e -> updateArtifactDescription());
            artifactSlotComboBoxes.add(artifactComboBox);
            addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.artifactSlot", i + 1), artifactComboBox);
        }
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(artifactDescriptionLabel, gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        saveArtifactSetupButton.setFocusable(false);
        saveArtifactSetupButton.addActionListener(e -> saveArtifactSetup());
        panel.add(saveArtifactSetupButton, gbc);

        techniqueComboBox.addActionListener(e -> updateTechniqueDescription());
        pillComboBox.addActionListener(e -> updatePillDescription());
        addBottomGlue(panel, gbc, row);
        return createScrollableTab(panel);
    }

    private JPanel createTravelTab() {
        JPanel panel = createFormPanel();
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.location"), travelComboBox);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(travelDescriptionLabel, gbc);

        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.activeTravel"), activeTravelLabel);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0;
        panel.add(travelProgressBar, gbc);
        gbc.weightx = 0;

        JPanel actions = createActionPanel();
        startTravelButton.setFocusable(false);
        startTravelButton.addActionListener(e -> {
            TravelOption option = (TravelOption) travelComboBox.getSelectedItem();
            if (option != null) {
                IdleCultivationManager.getInstance().startTravel(option.location.id());
            }
        });
        actions.add(startTravelButton);

        claimTravelButton.setFocusable(false);
        claimTravelButton.addActionListener(e -> IdleCultivationManager.getInstance().claimTravelReward());
        actions.add(claimTravelButton);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        panel.add(actions, gbc);

        travelComboBox.addActionListener(e -> updateTravelDescription());
        addBottomGlue(panel, gbc, row + 1);
        return createScrollableTab(panel);
    }

    private JPanel createAbodeTab() {
        JPanel contentPanel = createFormPanel();
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        addLabelRow(contentPanel, gbc, row++, FishToucherBundle.message("cultivation.label.spiritStones"), abodeStonesValue);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0;
        contentPanel.add(new JSeparator(), gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0;
        contentPanel.add(abodeFacilitiesPanel, gbc);

        addBottomGlue(contentPanel, gbc, row);
        return createScrollableTab(contentPanel);
    }

    private JPanel createChallengeTab() {
        JPanel contentPanel = createFormPanel();
        allowHorizontalShrink(battleStatsLabel);
        allowHorizontalShrink(cultivatorComboBox);
        allowHorizontalShrink(cultivatorDescriptionLabel);
        allowHorizontalShrink(battleStatusLabel);
        allowHorizontalShrink(playerHealthBar);
        allowHorizontalShrink(enemyHealthBar);
        allowHorizontalShrink(battleManaBar);
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        JLabel battleStatsTitle = createSectionLabel(FishToucherBundle.message("cultivation.label.combatStats"));
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0;
        contentPanel.add(battleStatsTitle, gbc);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        contentPanel.add(battleStatsLabel, gbc);

        addLabelRow(contentPanel, gbc, row++, FishToucherBundle.message("cultivation.label.cultivator"), cultivatorComboBox);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        contentPanel.add(cultivatorDescriptionLabel, gbc);

        addLabelRow(contentPanel, gbc, row++, FishToucherBundle.message("cultivation.label.battleStatus"), battleStatusLabel);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0;
        contentPanel.add(playerHealthBar, gbc);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        contentPanel.add(enemyHealthBar, gbc);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        contentPanel.add(battleManaBar, gbc);
        gbc.weightx = 0;

        JPanel actions = createActionPanel();
        startChallengeButton.setFocusable(false);
        startChallengeButton.addActionListener(e -> {
            CultivatorOption option = (CultivatorOption) cultivatorComboBox.getSelectedItem();
            if (option != null) {
                IdleCultivationManager.getInstance().startChallenge(option.cultivator.id());
            }
        });
        actions.add(startChallengeButton);

        endChallengeButton.setFocusable(false);
        endChallengeButton.addActionListener(e -> IdleCultivationManager.getInstance().endChallenge());
        actions.add(endChallengeButton);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        contentPanel.add(actions, gbc);

        JLabel logTitle = createSectionLabel(FishToucherBundle.message("cultivation.section.battleLog"));
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        contentPanel.add(logTitle, gbc);

        JScrollPane logScrollPane = new JScrollPane(battleLogArea);
        logScrollPane.setBorder(BorderFactory.createLineBorder(JBColor.border()));
        Dimension challengeLogSize = new Dimension(CULTIVATION_MIN_WIDTH, CHALLENGE_LOG_HEIGHT);
        logScrollPane.setMinimumSize(new Dimension(0, CHALLENGE_LOG_HEIGHT));
        logScrollPane.setPreferredSize(challengeLogSize);
        logScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        contentPanel.add(logScrollPane, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;

        addBottomGlue(contentPanel, gbc, row);
        cultivatorComboBox.addActionListener(e -> updateCultivatorDescription());
        return createScrollableTab(contentPanel);
    }

    private JPanel createGuideTab() {
        JPanel contentPanel = createFormPanel();
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        row = addGuideSection(contentPanel, gbc, row, "loop");
        row = addGuideSection(contentPanel, gbc, row, "gains");
        row = addGuideSection(contentPanel, gbc, row, "realms");
        row = addRealmDescriptionRows(contentPanel, gbc, row);
        row = addGuideSection(contentPanel, gbc, row, "travel");
        row = addGuideSection(contentPanel, gbc, row, "abode");
        row = addGuideSection(contentPanel, gbc, row, "bag");
        row = addGuideSection(contentPanel, gbc, row, "breakthrough");

        addBottomGlue(contentPanel, gbc, row);
        return createScrollableTab(contentPanel);
    }

    private void refreshContent() {
        preserveOuterScrollPositions(this, this::refreshContentWithoutScrollJump);
    }

    private void refreshContentWithoutScrollJump() {
        refreshing = true;
        try {
            IdleCultivationManager manager = IdleCultivationManager.getInstance();
            NovelReaderSettings settings = NovelReaderSettings.getInstance();
            long currentQi = manager.getCurrentQi();
            long requiredQi = manager.getRequiredQi();
            int percent = manager.getProgressPercent();

            realmValue.setText(manager.getRealmName());
            techniqueValue.setText(manager.getEquippedTechnique().name());
            qiValue.setText(requiredQi > 0L ? currentQi + " / " + requiredQi : String.valueOf(currentQi));
            IdleCultivationManager.CombatStats combatStats = manager.getCombatStats();
            IdleCultivationManager.BattleSnapshot battleSnapshot = manager.getBattleSnapshot();
            boolean activeBattle = battleSnapshot != null && !battleSnapshot.finished();
            long currentHealth = activeBattle ? battleSnapshot.playerHealth() : combatStats.health();
            long currentMana = activeBattle ? battleSnapshot.playerMana() : combatStats.mana();
            attackValue.setText(String.valueOf(combatStats.attack()));
            defenseValue.setText(String.valueOf(combatStats.defense()));
            healthValue.setText(FishToucherBundle.message(
                    "cultivation.status.resourceWithRecovery",
                    currentHealth,
                    activeBattle ? battleSnapshot.playerStats().health() : combatStats.health(),
                    manager.getHealthRecoveryPerSecond()
            ));
            manaValue.setText(FishToucherBundle.message(
                    "cultivation.status.resourceWithRecovery",
                    currentMana,
                    activeBattle ? battleSnapshot.playerStats().mana() : combatStats.mana(),
                    manager.getManaRecoveryPerSecond()
            ));
            stonesValue.setText(String.valueOf(manager.getSpiritStones()));
            rateValue.setText(manager.getRateText());
            seclusionRateValue.setText(manager.getSeclusionRateText());
            chanceValue.setText(manager.getChanceText());
            setWrappingText(effectsValue, manager.getActiveEffectsText());
            setWrappingText(messageLabel, manager.getLastMessage());

            progressBar.setValue(percent);
            progressBar.setString(requiredQi > 0L ? percent + "%" : FishToucherBundle.message("cultivation.status.max"));
            updateMeditationButton(manager);
            updateRebirthControls(manager);
            breakthroughButton.setEnabled(manager.canBreakthrough());

            reloadTechniqueOptions(manager, settings);
            reloadPillOptions(manager, settings);
            reloadSpellOptions(manager, settings);
            reloadArtifactOptions(manager, settings);
            reloadTravelOptions(manager);
            updateActiveTravel(manager);
            reloadAbodeFacilities(manager);
            reloadCultivatorOptions(manager);
            updateBattleState(manager);
        } finally {
            refreshing = false;
        }
        updateTechniqueDescription();
        updatePillDescription();
        updateSpellDescription();
        updateArtifactDescription();
        updateTravelDescription();
        updateCultivatorDescription();
    }

    private void updateMeditationButton(IdleCultivationManager manager) {
        boolean canMeditate = manager.canMeditate();
        String baseText = FishToucherBundle.message("cultivation.button.meditate");
        meditateButton.setEnabled(canMeditate);
        meditateButton.setText(canMeditate ? baseText : baseText + " (" + manager.getMeditationRemainingText() + ")");
        meditateButton.setToolTipText(canMeditate
                ? FishToucherBundle.message("cultivation.tooltip.meditate")
                : FishToucherBundle.message("cultivation.tooltip.meditateCooldown", manager.getMeditationRemainingText()));
    }

    private void updateRebirthControls(IdleCultivationManager manager) {
        boolean hasRebirth = manager.getRebirthCount() > 0;
        rebirthLabel.setVisible(hasRebirth);
        rebirthValue.setVisible(hasRebirth);
        if (hasRebirth) {
            setWrappingText(rebirthValue, manager.getRebirthTrainingStatusText());
        }

        boolean canRebirth = manager.canRebirth();
        rebirthButton.setVisible(canRebirth);
        rebirthButton.setEnabled(canRebirth);
    }

    private void showRebirthDialog() {
        IdleCultivationManager manager = IdleCultivationManager.getInstance();
        if (!manager.canRebirth()) {
            return;
        }

        List<IdleCultivationManager.TechniqueDefinition> techniques = manager.getRetainableTechniqueDefinitions();
        if (techniques.isEmpty()) {
            return;
        }

        RebirthTechniqueOption[] options = techniques.stream()
                .map(RebirthTechniqueOption::new)
                .toArray(RebirthTechniqueOption[]::new);
        JComboBox<RebirthTechniqueOption> comboBox = new JComboBox<>(options);
        String equippedTechniqueId = NovelReaderSettings.getInstance().getEquippedTechniqueId();
        for (RebirthTechniqueOption option : options) {
            if (option.technique.id().equals(equippedTechniqueId)) {
                comboBox.setSelectedItem(option);
                break;
            }
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                comboBox,
                FishToucherBundle.message("cultivation.rebirth.dialog.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        RebirthTechniqueOption selected = (RebirthTechniqueOption) comboBox.getSelectedItem();
        if (selected != null) {
            manager.rebirth(selected.technique.id());
        }
    }

    private void reloadTechniqueOptions(IdleCultivationManager manager, NovelReaderSettings settings) {
        String selectedId = settings.getEquippedTechniqueId();
        techniqueComboBox.removeAllItems();
        for (IdleCultivationManager.TechniqueDefinition technique : manager.getTechniqueDefinitions()) {
            boolean unlocked = settings.isTechniqueUnlocked(technique.id());
            TechniqueOption option = new TechniqueOption(technique, unlocked);
            techniqueComboBox.addItem(option);
            if (technique.id().equals(selectedId)) {
                techniqueComboBox.setSelectedItem(option);
            }
        }
    }

    private void reloadPillOptions(IdleCultivationManager manager, NovelReaderSettings settings) {
        String selectedId = null;
        PillOption selected = (PillOption) pillComboBox.getSelectedItem();
        if (selected != null) {
            selectedId = selected.pill.id();
        }
        pillComboBox.removeAllItems();
        for (IdleCultivationManager.PillDefinition pill : manager.getPillDefinitions()) {
            PillOption option = new PillOption(pill, settings.getPillCount(pill.id()));
            pillComboBox.addItem(option);
            if (pill.id().equals(selectedId)) {
                pillComboBox.setSelectedItem(option);
            }
        }
    }

    private void reloadSpellOptions(IdleCultivationManager manager, NovelReaderSettings settings) {
        List<String> equippedSpellIds = settings.getEquippedSpellIds();
        for (int i = 0; i < spellSlotComboBoxes.size(); i++) {
            JComboBox<SpellOption> comboBox = spellSlotComboBoxes.get(i);
            String selectedId = i < equippedSpellIds.size() ? equippedSpellIds.get(i) : "";
            comboBox.removeAllItems();
            comboBox.addItem(SpellOption.none());
            for (IdleCultivationManager.SpellDefinition spell : manager.getSpellDefinitions()) {
                boolean unlocked = settings.isSpellUnlocked(spell.id());
                SpellOption option = new SpellOption(spell, false, unlocked);
                comboBox.addItem(option);
                if (spell.id().equals(selectedId)) {
                    comboBox.setSelectedItem(option);
                }
            }
        }
    }

    private void reloadArtifactOptions(IdleCultivationManager manager, NovelReaderSettings settings) {
        List<String> equippedArtifactIds = settings.getEquippedArtifactIds();
        for (int i = 0; i < artifactSlotComboBoxes.size(); i++) {
            JComboBox<ArtifactOption> comboBox = artifactSlotComboBoxes.get(i);
            String selectedId = i < equippedArtifactIds.size() ? equippedArtifactIds.get(i) : "";
            comboBox.removeAllItems();
            comboBox.addItem(ArtifactOption.none());
            for (IdleCultivationManager.ArtifactDefinition artifact : manager.getArtifactDefinitions()) {
                boolean unlocked = settings.isArtifactUnlocked(artifact.id());
                boolean equipped = equippedArtifactIds.contains(artifact.id());
                ArtifactOption option = new ArtifactOption(artifact, false, unlocked, equipped);
                comboBox.addItem(option);
                if (artifact.id().equals(selectedId)) {
                    comboBox.setSelectedItem(option);
                }
            }
        }
    }

    private void saveSpellSetup() {
        List<String> spellIds = new ArrayList<>();
        for (JComboBox<SpellOption> comboBox : spellSlotComboBoxes) {
            SpellOption option = (SpellOption) comboBox.getSelectedItem();
            if (option != null && !option.empty && option.unlocked && option.spell != null) {
                spellIds.add(option.spell.id());
            }
        }
        IdleCultivationManager.getInstance().equipSpells(spellIds);
    }

    private void saveArtifactSetup() {
        List<String> artifactIds = new ArrayList<>();
        Set<String> selectedArtifactIds = new HashSet<>();
        for (JComboBox<ArtifactOption> comboBox : artifactSlotComboBoxes) {
            ArtifactOption option = (ArtifactOption) comboBox.getSelectedItem();
            if (option != null
                    && !option.empty
                    && option.unlocked
                    && option.artifact != null
                    && selectedArtifactIds.add(option.artifact.id())) {
                artifactIds.add(option.artifact.id());
            }
        }
        IdleCultivationManager.getInstance().equipArtifacts(artifactIds);
    }

    private void reloadTravelOptions(IdleCultivationManager manager) {
        String selectedId = null;
        TravelOption selected = (TravelOption) travelComboBox.getSelectedItem();
        if (selected != null) {
            selectedId = selected.location.id();
        }
        travelComboBox.removeAllItems();
        List<IdleCultivationManager.TravelLocationDefinition> locations = manager.getTravelLocationDefinitions();
        for (IdleCultivationManager.TravelLocationDefinition location : locations) {
            TravelOption option = new TravelOption(location, manager.isTravelUnlocked(location));
            travelComboBox.addItem(option);
            if (location.id().equals(selectedId)) {
                travelComboBox.setSelectedItem(option);
            }
        }
    }

    private void reloadAbodeFacilities(IdleCultivationManager manager) {
        abodeStonesValue.setText(String.valueOf(manager.getSpiritStones()));
        abodeFacilitiesPanel.removeAll();
        GridBagConstraints gbc = createConstraints();
        int row = 0;
        for (IdleCultivationManager.AbodeFacilityDefinition facility : manager.getAbodeFacilityDefinitions()) {
            JTextArea title = createSectionTextArea(facility.name() + "  " + manager.getAbodeFacilityLevelText(facility.id()));
            gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0;
            abodeFacilitiesPanel.add(title, gbc);

            JTextArea description = createHintTextArea(facility.description());
            gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
            abodeFacilitiesPanel.add(description, gbc);

            addLabelRow(abodeFacilitiesPanel, gbc, row++, FishToucherBundle.message("cultivation.label.effect"), createHintTextArea(manager.getAbodeFacilityEffectText(facility.id())));
            addLabelRow(abodeFacilitiesPanel, gbc, row++, FishToucherBundle.message("cultivation.label.upgradeCost"), createHintTextArea(manager.getAbodeUpgradeCostText(facility.id())));

            boolean productionFacility = manager.isAbodeProductionFacility(facility.id());
            if (productionFacility) {
                addLabelRow(abodeFacilitiesPanel, gbc, row++, FishToucherBundle.message("cultivation.label.claimable"), createHintTextArea(manager.getAbodeClaimableText(facility.id())));
            }

            JPanel actions = createActionPanel();
            JButton upgradeButton = new JButton(FishToucherBundle.message("cultivation.button.upgradeFacility"));
            upgradeButton.setFocusable(false);
            upgradeButton.setEnabled(manager.canUpgradeAbodeFacility(facility.id()));
            upgradeButton.addActionListener(e -> IdleCultivationManager.getInstance().upgradeAbodeFacility(facility.id()));
            actions.add(upgradeButton);

            if (productionFacility) {
                JButton claimButton = new JButton(FishToucherBundle.message("cultivation.button.claimAbode"));
                claimButton.setFocusable(false);
                claimButton.setEnabled(manager.canClaimAbodeFacility(facility.id()));
                claimButton.addActionListener(e -> IdleCultivationManager.getInstance().claimAbodeFacility(facility.id()));
                actions.add(claimButton);
            }

            gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
            abodeFacilitiesPanel.add(actions, gbc);

            gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0;
            abodeFacilitiesPanel.add(new JSeparator(), gbc);
        }
        abodeFacilitiesPanel.revalidate();
        abodeFacilitiesPanel.repaint();
        Container parent = abodeFacilitiesPanel.getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    private void updateTechniqueDescription() {
        TechniqueOption option = (TechniqueOption) techniqueComboBox.getSelectedItem();
        if (option == null) return;
        IdleCultivationManager.TechniqueDefinition technique = option.technique;
        setWrappingText(techniqueDescriptionLabel, technique.description());
        equipTechniqueButton.setEnabled(option.unlocked && !technique.id().equals(NovelReaderSettings.getInstance().getEquippedTechniqueId()));
    }

    private void updatePillDescription() {
        PillOption option = (PillOption) pillComboBox.getSelectedItem();
        if (option == null) return;
        setWrappingText(pillDescriptionLabel, option.pill.description());
        usePillButton.setEnabled(option.count > 0);
    }

    private void updateSpellDescription() {
        if (refreshing) return;
        for (int i = 0; i < spellSlotComboBoxes.size(); i++) {
            JComboBox<SpellOption> comboBox = spellSlotComboBoxes.get(i);
            JTextArea descriptionLabel = spellDescriptionLabels.get(i);
            SpellOption option = (SpellOption) comboBox.getSelectedItem();
            if (option != null && !option.empty && option.spell != null) {
                setWrappingText(descriptionLabel, option.spell.name() + " - " + option.spell.description());
            } else {
                setWrappingText(descriptionLabel, "");
            }
        }
    }

    private void updateArtifactDescription() {
        if (refreshing) return;
        normalizeArtifactSelection();
        List<String> names = new ArrayList<>();
        for (JComboBox<ArtifactOption> comboBox : artifactSlotComboBoxes) {
            ArtifactOption option = (ArtifactOption) comboBox.getSelectedItem();
            if (option != null && !option.empty && option.artifact != null) {
                names.add(option.artifact.name() + " - " + option.artifact.description());
            }
        }
        setWrappingText(
                artifactDescriptionLabel,
                names.isEmpty()
                        ? FishToucherBundle.message("cultivation.status.noArtifactEquipped")
                        : String.join("  |  ", names)
        );
    }

    private void normalizeArtifactSelection() {
        if (adjustingArtifactSelection) {
            return;
        }
        adjustingArtifactSelection = true;
        try {
            Set<String> selectedArtifactIds = new HashSet<>();
            for (JComboBox<ArtifactOption> comboBox : artifactSlotComboBoxes) {
                ArtifactOption option = (ArtifactOption) comboBox.getSelectedItem();
                if (option == null || option.empty || option.artifact == null) {
                    continue;
                }
                if (!selectedArtifactIds.add(option.artifact.id())) {
                    comboBox.setSelectedIndex(0);
                }
            }
        } finally {
            adjustingArtifactSelection = false;
        }
    }

    private void updateTravelDescription() {
        if (refreshing) return;
        TravelOption option = (TravelOption) travelComboBox.getSelectedItem();
        if (option == null) return;
        IdleCultivationManager manager = IdleCultivationManager.getInstance();
        String suffix = option.unlocked
                ? manager.getTravelDurationText(option.location)
                : FishToucherBundle.message("cultivation.travel.locked", manager.getRealmName(option.location.minRealmIndex()));
        String blockReason = getTravelBlockReason(manager, option);
        setWrappingText(travelDescriptionLabel, option.location.description() + "  " + suffix + appendBlockReason(blockReason));
        updateStartTravelButton(manager, option);
    }

    private void updateActiveTravel(IdleCultivationManager manager) {
        IdleCultivationManager.TravelLocationDefinition active = manager.getActiveTravelLocation();
        if (active == null) {
            setWrappingText(activeTravelLabel, FishToucherBundle.message("cultivation.travel.none"));
            travelProgressBar.setValue(0);
            travelProgressBar.setString(FishToucherBundle.message("cultivation.travel.none"));
            TravelOption option = (TravelOption) travelComboBox.getSelectedItem();
            updateStartTravelButton(manager, option);
            claimTravelButton.setEnabled(false);
            return;
        }
        setWrappingText(activeTravelLabel, active.name() + " | " + manager.getTravelRemainingText());
        int percent = manager.getTravelProgressPercent();
        travelProgressBar.setValue(percent);
        travelProgressBar.setString(manager.isTravelReady() ? FishToucherBundle.message("cultivation.status.travelClaimReady") : percent + "%");
        startTravelButton.setEnabled(false);
        claimTravelButton.setEnabled(manager.isTravelReady());
    }

    private void reloadCultivatorOptions(IdleCultivationManager manager) {
        String selectedId = null;
        CultivatorOption selected = (CultivatorOption) cultivatorComboBox.getSelectedItem();
        if (selected != null) {
            selectedId = selected.cultivator.id();
        }
        cultivatorComboBox.removeAllItems();
        for (IdleCultivationManager.CultivatorDefinition cultivator : manager.getCultivatorDefinitions()) {
            CultivatorOption option = new CultivatorOption(
                    cultivator,
                    manager.isCultivatorUnlocked(cultivator),
                    manager.isCultivatorDefeated(cultivator)
            );
            cultivatorComboBox.addItem(option);
            if (cultivator.id().equals(selectedId)) {
                cultivatorComboBox.setSelectedItem(option);
            }
        }
    }

    private void updateCultivatorDescription() {
        if (refreshing) return;
        CultivatorOption option = (CultivatorOption) cultivatorComboBox.getSelectedItem();
        if (option == null) return;
        IdleCultivationManager manager = IdleCultivationManager.getInstance();
        IdleCultivationManager.CultivatorDefinition cultivator = option.cultivator;
        String stats = FishToucherBundle.message(
                "cultivation.status.cultivatorStats",
                cultivator.maxHealth(),
                cultivator.attack(),
                cultivator.defense(),
                cultivator.mana()
        );
        String reward = FishToucherBundle.message("cultivation.status.challengeReward", manager.getCultivatorRewardText(cultivator));
        String blockReason = getChallengeBlockReason(manager, option);
        setWrappingText(cultivatorDescriptionLabel, manager.getCultivatorStatusText(cultivator) + "  " + stats + "  " + reward + appendBlockReason(blockReason));
        updateStartChallengeButton(manager);
    }

    private void updateBattleState(IdleCultivationManager manager) {
        IdleCultivationManager.CombatStats stats = manager.getCombatStats();
        setWrappingText(battleStatsLabel, FishToucherBundle.message("cultivation.status.combatStats", stats.attack(), stats.defense(), stats.mana(), stats.health()));

        IdleCultivationManager.BattleSnapshot snapshot = manager.getBattleSnapshot();
        if (snapshot == null) {
            battleStatusLabel.setText(FishToucherBundle.message("cultivation.status.challengeNone"));
            setProgressText(playerHealthBar, 100, FishToucherBundle.message("cultivation.battle.playerHp", stats.health(), stats.health()));
            setProgressText(enemyHealthBar, 0, FishToucherBundle.message("cultivation.battle.enemyHp", 0, 0));
            setProgressText(battleManaBar, 100, FishToucherBundle.message("cultivation.battle.mana", stats.mana(), stats.mana()));
            setWrappingText(battleLogArea, "");
            updateStartChallengeButton(manager);
            endChallengeButton.setEnabled(false);
            return;
        }

        battleStatusLabel.setText(snapshot.statusText());
        int playerHpPercent = percent(snapshot.playerHealth(), snapshot.playerStats().health());
        int enemyHpPercent = percent(snapshot.enemyHealth(), snapshot.enemyMaxHealth());
        int manaPercent = percent(snapshot.playerMana(), snapshot.playerStats().mana());
        setProgressText(playerHealthBar, playerHpPercent, FishToucherBundle.message("cultivation.battle.playerHp", snapshot.playerHealth(), snapshot.playerStats().health()));
        setProgressText(enemyHealthBar, enemyHpPercent, FishToucherBundle.message("cultivation.battle.enemyHp", snapshot.enemyHealth(), snapshot.enemyMaxHealth()));
        setProgressText(battleManaBar, manaPercent, FishToucherBundle.message("cultivation.battle.mana", snapshot.playerMana(), snapshot.playerStats().mana()));
        setWrappingText(battleLogArea, String.join("\n", snapshot.logs()));
        battleLogArea.setCaretPosition(battleLogArea.getDocument().getLength());
        updateStartChallengeButton(manager);
        endChallengeButton.setEnabled(manager.hasActiveBattle());
    }

    private void updateStartChallengeButton(IdleCultivationManager manager) {
        CultivatorOption option = (CultivatorOption) cultivatorComboBox.getSelectedItem();
        String blockReason = getChallengeBlockReason(manager, option);
        startChallengeButton.setEnabled(blockReason.isEmpty());
        startChallengeButton.setToolTipText(blockReason.isEmpty() ? null : blockReason);
    }

    private void updateStartTravelButton(IdleCultivationManager manager, TravelOption option) {
        String blockReason = getTravelBlockReason(manager, option);
        startTravelButton.setEnabled(blockReason.isEmpty());
        startTravelButton.setToolTipText(blockReason.isEmpty() ? null : blockReason);
    }

    private String getTravelBlockReason(IdleCultivationManager manager, TravelOption option) {
        if (option == null) {
            return FishToucherBundle.message("cultivation.status.travelUnknown");
        }
        if (!option.unlocked) {
            return FishToucherBundle.message("cultivation.travel.locked", manager.getRealmName(option.location.minRealmIndex()));
        }
        if (manager.hasActiveBattle()) {
            return FishToucherBundle.message("cultivation.status.travelBlockedByChallenge");
        }
        if (manager.hasActiveTravel()) {
            return FishToucherBundle.message("cultivation.status.travelBusy");
        }
        return "";
    }

    private String getChallengeBlockReason(IdleCultivationManager manager, CultivatorOption option) {
        if (option == null) {
            return FishToucherBundle.message("cultivation.status.challengeUnknown");
        }
        if (!option.unlocked) {
            return FishToucherBundle.message("cultivation.status.challengeLocked");
        }
        if (option.defeated) {
            return FishToucherBundle.message("cultivation.status.challengeAlreadyDefeated", option.cultivator.name());
        }
        if (manager.hasActiveBattle()) {
            return FishToucherBundle.message("cultivation.status.challengeBusy");
        }
        if (manager.hasActiveTravel()) {
            return FishToucherBundle.message("cultivation.status.challengeBlockedByTravel");
        }
        return "";
    }

    private String appendBlockReason(String blockReason) {
        return blockReason.isEmpty() ? "" : "  " + blockReason;
    }

    private int percent(long current, long total) {
        if (total <= 0L) {
            return 0;
        }
        return (int) Math.max(0L, Math.min(100L, current * 100L / total));
    }

    private void setProgressText(JProgressBar progressBar, int value, String text) {
        progressBar.setValue(value);
        progressBar.setString(text);
    }

    @Override
    public void dispose() {
        IdleCultivationManager.getInstance().removeChangeListener(changeListener);
    }

    private record TechniqueOption(IdleCultivationManager.TechniqueDefinition technique, boolean unlocked) {
        @Override
        public String toString() {
            String name = technique.name();
            return unlocked ? name : name + " (" + FishToucherBundle.message("cultivation.status.locked") + ")";
        }
    }

    private record PillOption(IdleCultivationManager.PillDefinition pill, int count) {
        @Override
        public String toString() {
            return pill.name() + " x" + count;
        }
    }

    private record SpellOption(IdleCultivationManager.SpellDefinition spell, boolean empty, boolean unlocked) {
        private static SpellOption none() {
            return new SpellOption(null, true, true);
        }

        @Override
        public String toString() {
            if (empty || spell == null) {
                return FishToucherBundle.message("cultivation.spell.none");
            }
            return unlocked
                    ? spell.name()
                    : spell.name() + " (" + FishToucherBundle.message("cultivation.status.locked") + ")";
        }
    }

    private record ArtifactOption(IdleCultivationManager.ArtifactDefinition artifact,
                                  boolean empty,
                                  boolean unlocked,
                                  boolean equipped) {
        private static ArtifactOption none() {
            return new ArtifactOption(null, true, true, false);
        }

        @Override
        public String toString() {
            if (empty || artifact == null) {
                return FishToucherBundle.message("cultivation.artifact.none");
            }
            if (!unlocked) {
                return artifact.name() + " (" + FishToucherBundle.message("cultivation.status.locked") + ")";
            }
            return equipped
                    ? artifact.name() + " (" + FishToucherBundle.message("cultivation.status.equipped") + ")"
                    : artifact.name();
        }
    }

    private record TravelOption(IdleCultivationManager.TravelLocationDefinition location, boolean unlocked) {
        @Override
        public String toString() {
            String name = location.name();
            return unlocked ? name : name + " (" + FishToucherBundle.message("cultivation.status.locked") + ")";
        }
    }

    private record CultivatorOption(IdleCultivationManager.CultivatorDefinition cultivator,
                                    boolean unlocked,
                                    boolean defeated) {
        @Override
        public String toString() {
            String name = cultivator.name();
            if (defeated) {
                return name + " (" + FishToucherBundle.message("cultivation.status.challengeDefeated") + ")";
            }
            return unlocked ? name : name + " (" + FishToucherBundle.message("cultivation.status.locked") + ")";
        }
    }

    private record RebirthTechniqueOption(IdleCultivationManager.TechniqueDefinition technique) {
        @Override
        public String toString() {
            return technique.name();
        }
    }

}
