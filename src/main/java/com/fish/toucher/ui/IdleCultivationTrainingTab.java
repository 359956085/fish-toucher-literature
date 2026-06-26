package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import static com.fish.toucher.ui.IdleCultivationUiSupport.*;

final class IdleCultivationTrainingTab {

    private final Component dialogParent;
    private final JComponent component;
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

    IdleCultivationTrainingTab(Component dialogParent) {
        this.dialogParent = dialogParent;
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
        rebirthValue = createHintTextArea();
        messageLabel = createHintTextArea();
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        meditateButton = new JButton(FishToucherBundle.message("cultivation.button.meditate"));
        koiBlessingButton = new JButton(FishToucherBundle.message("cultivation.button.koiBlessing"));
        breakthroughButton = new JButton(FishToucherBundle.message("cultivation.button.breakthrough"));
        rebirthButton = new JButton(FishToucherBundle.message("cultivation.button.rebirth"));
        component = createContent();
    }

    JComponent getComponent() {
        return component;
    }

    void setRefreshing(boolean refreshing) {
    }

    void updateTrainingState(IdleCultivationManager manager) {
        long currentQi = manager.getCurrentQi();
        long requiredQi = manager.getRequiredQi();
        int percent = manager.getProgressPercent();

        setLabelTextIfChanged(realmValue, manager.getRealmName());
        setLabelTextIfChanged(techniqueValue, manager.getEquippedTechnique().name());
        setLabelTextIfChanged(qiValue, requiredQi > 0L ? currentQi + " / " + requiredQi : String.valueOf(currentQi));
        IdleCultivationManager.CombatStats combatStats = manager.getCombatStats();
        IdleCultivationManager.BattleSnapshot battleSnapshot = manager.getBattleSnapshot();
        boolean activeBattle = battleSnapshot != null && !battleSnapshot.finished();
        long currentHealth = activeBattle ? battleSnapshot.playerHealth() : combatStats.health();
        long currentMana = activeBattle ? battleSnapshot.playerMana() : combatStats.mana();
        setLabelTextIfChanged(attackValue, String.valueOf(combatStats.attack()));
        setLabelTextIfChanged(defenseValue, String.valueOf(combatStats.defense()));
        setLabelTextIfChanged(healthValue, FishToucherBundle.message(
                "cultivation.status.resourceWithRecovery",
                currentHealth,
                activeBattle ? battleSnapshot.playerStats().health() : combatStats.health(),
                manager.getHealthRecoveryPerSecond()
        ));
        setLabelTextIfChanged(manaValue, FishToucherBundle.message(
                "cultivation.status.resourceWithRecovery",
                currentMana,
                activeBattle ? battleSnapshot.playerStats().mana() : combatStats.mana(),
                manager.getManaRecoveryPerSecond()
        ));
        setLabelTextIfChanged(stonesValue, String.valueOf(manager.getSpiritStones()));
        setLabelTextIfChanged(rateValue, manager.getRateText());
        setLabelTextIfChanged(seclusionRateValue, manager.getSeclusionRateText());
        setLabelTextIfChanged(chanceValue, manager.getChanceText());
        setWrappingText(effectsValue, manager.getActiveEffectsText());
        setWrappingText(messageLabel, manager.getLastMessage());

        setProgressTextIfChanged(progressBar, percent, requiredQi > 0L ? percent + "%" : FishToucherBundle.message("cultivation.status.max"));
        updateMeditationButton(manager);
        updateRebirthControls(manager);
        breakthroughButton.setEnabled(manager.canBreakthrough());
    }

    private JComponent createContent() {
        JPanel panel = createFormPanel();
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.realm"), realmValue);
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.technique"), techniqueValue);
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.cultivation"), qiValue);

        row = addFullWidthRow(panel, gbc, row, progressBar);

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

        row = addFullWidthRow(panel, gbc, row, messageLabel);

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

        row = addActionRow(panel, gbc, row, actions);
        addBottomGlue(panel, gbc, row);
        return createScrollableTab(panel);
    }

    private void updateMeditationButton(IdleCultivationManager manager) {
        boolean canMeditate = manager.canMeditate();
        String baseText = FishToucherBundle.message("cultivation.button.meditate");
        meditateButton.setEnabled(canMeditate);
        setButtonTextIfChanged(meditateButton, canMeditate ? baseText : baseText + " (" + manager.getMeditationRemainingText() + ")");
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
                dialogParent,
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

    record RebirthTechniqueOption(IdleCultivationManager.TechniqueDefinition technique) {
        @Override
        public String toString() {
            return technique.name();
        }
    }
}
