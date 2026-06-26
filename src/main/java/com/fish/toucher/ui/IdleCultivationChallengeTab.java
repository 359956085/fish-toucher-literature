package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;

import static com.fish.toucher.ui.IdleCultivationUiSupport.*;

final class IdleCultivationChallengeTab {

    private static final int CHALLENGE_LOG_HEIGHT = 150;

    private final JComponent component;
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
    private boolean refreshing;

    IdleCultivationChallengeTab() {
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
        component = createContent();
    }

    JComponent getComponent() {
        return component;
    }

    void setRefreshing(boolean refreshing) {
        this.refreshing = refreshing;
    }

    void reloadCultivatorOptions(IdleCultivationManager manager) {
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

    void updateBattleState(IdleCultivationManager manager) {
        IdleCultivationManager.CombatStats stats = manager.getCombatStats();
        setWrappingText(battleStatsLabel, FishToucherBundle.message("cultivation.status.combatStats", stats.attack(), stats.defense(), stats.mana(), stats.health()));

        IdleCultivationManager.BattleSnapshot snapshot = manager.getBattleSnapshot();
        if (snapshot == null) {
            setLabelTextIfChanged(battleStatusLabel, FishToucherBundle.message("cultivation.status.challengeNone"));
            setProgressTextIfChanged(playerHealthBar, 100, FishToucherBundle.message("cultivation.battle.playerHp", stats.health(), stats.health()));
            setProgressTextIfChanged(enemyHealthBar, 0, FishToucherBundle.message("cultivation.battle.enemyHp", 0, 0));
            setProgressTextIfChanged(battleManaBar, 100, FishToucherBundle.message("cultivation.battle.mana", stats.mana(), stats.mana()));
            setWrappingText(battleLogArea, "");
            updateStartChallengeButton(manager);
            endChallengeButton.setEnabled(false);
            return;
        }

        setLabelTextIfChanged(battleStatusLabel, snapshot.statusText());
        int playerHpPercent = percent(snapshot.playerHealth(), snapshot.playerStats().health());
        int enemyHpPercent = percent(snapshot.enemyHealth(), snapshot.enemyMaxHealth());
        int manaPercent = percent(snapshot.playerMana(), snapshot.playerStats().mana());
        setProgressTextIfChanged(playerHealthBar, playerHpPercent, FishToucherBundle.message("cultivation.battle.playerHp", snapshot.playerHealth(), snapshot.playerStats().health()));
        setProgressTextIfChanged(enemyHealthBar, enemyHpPercent, FishToucherBundle.message("cultivation.battle.enemyHp", snapshot.enemyHealth(), snapshot.enemyMaxHealth()));
        setProgressTextIfChanged(battleManaBar, manaPercent, FishToucherBundle.message("cultivation.battle.mana", snapshot.playerMana(), snapshot.playerStats().mana()));
        setWrappingText(battleLogArea, String.join("\n", snapshot.logs()));
        battleLogArea.setCaretPosition(battleLogArea.getDocument().getLength());
        updateStartChallengeButton(manager);
        endChallengeButton.setEnabled(manager.hasActiveBattle());
    }

    void updateSelectionDescriptions() {
        updateCultivatorDescription();
    }

    private JComponent createContent() {
        JPanel contentPanel = createFormPanel();
        allowHorizontalShrink(cultivatorComboBox);
        allowHorizontalShrink(playerHealthBar);
        allowHorizontalShrink(enemyHealthBar);
        allowHorizontalShrink(battleManaBar);
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        JLabel battleStatsTitle = createSectionLabel(FishToucherBundle.message("cultivation.label.combatStats"));
        row = addFullWidthRow(contentPanel, gbc, row, battleStatsTitle);
        row = addFullWidthRow(contentPanel, gbc, row, battleStatsLabel);

        addLabelRow(contentPanel, gbc, row++, FishToucherBundle.message("cultivation.label.cultivator"), cultivatorComboBox);

        row = addFullWidthRow(contentPanel, gbc, row, cultivatorDescriptionLabel);

        addLabelRow(contentPanel, gbc, row++, FishToucherBundle.message("cultivation.label.battleStatus"), battleStatusLabel);

        row = addFullWidthRow(contentPanel, gbc, row, playerHealthBar);
        row = addFullWidthRow(contentPanel, gbc, row, enemyHealthBar);
        row = addFullWidthRow(contentPanel, gbc, row, battleManaBar);
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

        row = addActionRow(contentPanel, gbc, row, actions);

        JLabel logTitle = createSectionLabel(FishToucherBundle.message("cultivation.section.battleLog"));
        row = addFullWidthRow(contentPanel, gbc, row, logTitle);

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

    private void updateStartChallengeButton(IdleCultivationManager manager) {
        CultivatorOption option = (CultivatorOption) cultivatorComboBox.getSelectedItem();
        String blockReason = getChallengeBlockReason(manager, option);
        startChallengeButton.setEnabled(blockReason.isEmpty());
        startChallengeButton.setToolTipText(blockReason.isEmpty() ? null : blockReason);
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

    record CultivatorOption(IdleCultivationManager.CultivatorDefinition cultivator,
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
}
