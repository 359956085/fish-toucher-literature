package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.fish.toucher.ui.IdleCultivationUiSupport.*;

final class IdleCultivationTrainingTab {

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
    private final JButton ascendButton;

    IdleCultivationTrainingTab(Component dialogParent) {
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
        ascendButton = new JButton(FishToucherBundle.message("cultivation.ascension.button"));
        component = createContent();
    }

    JComponent getComponent() {
        return component;
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
        updateAscensionControls(manager);
        boolean canBreakthrough = manager.canBreakthrough();
        setButtonEnabledWithReason(
                breakthroughButton,
                canBreakthrough,
                FishToucherBundle.message("cultivation.tooltip.breakthrough"),
                manager.getRequiredQi() <= 0L
                        ? FishToucherBundle.message("cultivation.status.maxRealm")
                        : FishToucherBundle.message("cultivation.status.needMore", manager.getRequiredQi() - manager.getCurrentQi())
        );
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
        rebirthButton.addActionListener(e -> performRebirth());
        actions.add(rebirthButton);

        ascendButton.setFocusable(false);
        ascendButton.setToolTipText(FishToucherBundle.message("cultivation.ascension.tooltip"));
        ascendButton.addActionListener(e -> performAscension());
        actions.add(ascendButton);

        row = addActionRow(panel, gbc, row, actions);
        addBottomGlue(panel, gbc, row);
        return createScrollableTab(panel);
    }

    private void updateMeditationButton(IdleCultivationManager manager) {
        boolean canMeditate = manager.canMeditate();
        String baseText = FishToucherBundle.message("cultivation.button.meditate");
        setButtonEnabledWithReason(
                meditateButton,
                canMeditate,
                FishToucherBundle.message("cultivation.tooltip.meditate"),
                FishToucherBundle.message("cultivation.tooltip.meditateCooldown", manager.getMeditationRemainingText())
        );
        setButtonTextIfChanged(meditateButton, canMeditate ? baseText : baseText + " (" + manager.getMeditationRemainingText() + ")");
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

    private void updateAscensionControls(IdleCultivationManager manager) {
        boolean showAscend = manager.canShowAscensionButton();
        boolean canAscend = manager.canAscend();
        ascendButton.setVisible(showAscend);
        setButtonEnabledWithReason(
                ascendButton,
                canAscend,
                FishToucherBundle.message("cultivation.ascension.tooltip"),
                FishToucherBundle.message("cultivation.ascension.unavailable", IdleCultivationManager.ASCENSION_REQUIRED_REBIRTH_COUNT)
        );
    }

    private void performRebirth() {
        IdleCultivationManager manager = IdleCultivationManager.getInstance();
        if (!manager.canRebirth()) {
            return;
        }
        manager.rebirth("");
    }

    private void performAscension() {
        IdleCultivationManager manager = IdleCultivationManager.getInstance();
        if (!manager.canAscend()) {
            return;
        }
        int result = JOptionPane.showConfirmDialog(
                component,
                FishToucherBundle.message("cultivation.ascension.confirm"),
                FishToucherBundle.message("cultivation.ascension.confirmTitle"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (result == JOptionPane.OK_OPTION) {
            manager.ascend();
        }
    }
}
