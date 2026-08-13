package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;

import javax.swing.*;
import java.awt.*;

import static com.fish.toucher.ui.IdleCultivationUiSupport.*;

final class IdleCultivationSectTab {

    private final JComponent component;
    private final JComboBox<SectOption> sectComboBox = new JComboBox<>();
    private final JComboBox<TaskOption> taskComboBox = new JComboBox<>();
    private final JComboBox<InheritanceOption> inheritanceComboBox = new JComboBox<>();
    private final JComboBox<TrialOption> trialComboBox = new JComboBox<>();
    private final JTextArea statusText = createSectionTextArea("");
    private final JTextArea sectDescText = createHintTextArea();
    private final JTextArea taskDescText = createHintTextArea();
    private final JTextArea inheritanceDescText = createHintTextArea();
    private final JTextArea trialDescText = createHintTextArea();
    private final JProgressBar taskProgressBar = new JProgressBar(0, 100);
    private final JButton joinButton = new JButton(FishToucherBundle.message("cultivation.sect.button.join"));
    private final JButton leaveButton = new JButton(FishToucherBundle.message("cultivation.sect.button.leave"));
    private final JButton promoteButton = new JButton(FishToucherBundle.message("cultivation.sect.button.promote"));
    private final JButton startTaskButton = new JButton(FishToucherBundle.message("cultivation.sect.button.startTask"));
    private final JButton claimTaskButton = new JButton(FishToucherBundle.message("cultivation.sect.button.claimTask"));
    private final JButton purchaseButton = new JButton(FishToucherBundle.message("cultivation.sect.button.purchase"));
    private final JButton startTrialButton = new JButton(FishToucherBundle.message("cultivation.sect.button.startTrial"));
    private boolean refreshing;

    IdleCultivationSectTab() {
        taskProgressBar.setStringPainted(true);
        component = createContent();
    }

    JComponent getComponent() {
        return component;
    }

    void setRefreshing(boolean refreshing) {
        this.refreshing = refreshing;
    }

    void reloadSectState(IdleCultivationManager manager) {
        refreshing = true;
        try {
            reloadSects(manager);
            reloadTasks(manager);
            reloadInheritances(manager);
            reloadTrials(manager);
            updateButtons(manager);
            setWrappingText(statusText, manager.getCurrentSectTitle() + "\n" + manager.getSectProgressText());
            setProgressTextIfChanged(taskProgressBar, manager.getSectTaskProgressPercent(), manager.getSectTaskRemainingText());
        } finally {
            refreshing = false;
        }
        updateDescriptions();
    }

    private JComponent createContent() {
        JPanel contentPanel = createFormPanel();
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        row = addFullWidthRow(contentPanel, gbc, row, createSectionLabel(FishToucherBundle.message("cultivation.sect.title")));
        row = addFullWidthRow(contentPanel, gbc, row, statusText);
        addLabelRow(contentPanel, gbc, row++, FishToucherBundle.message("cultivation.sect.label.choose"), sectComboBox);
        row = addFullWidthRow(contentPanel, gbc, row, sectDescText);

        JPanel sectActions = createActionPanel();
        joinButton.addActionListener(e -> {
            SectOption option = (SectOption) sectComboBox.getSelectedItem();
            if (option != null) IdleCultivationManager.getInstance().joinSect(option.sect.id());
        });
        leaveButton.addActionListener(e -> IdleCultivationManager.getInstance().leaveSect());
        promoteButton.addActionListener(e -> IdleCultivationManager.getInstance().promoteSectRank());
        sectActions.add(joinButton);
        sectActions.add(leaveButton);
        sectActions.add(promoteButton);
        row = addActionRow(contentPanel, gbc, row, sectActions);

        row = addSeparatorRow(contentPanel, gbc, row);
        row = addFullWidthRow(contentPanel, gbc, row, createSectionLabel(FishToucherBundle.message("cultivation.sect.section.task")));
        addLabelRow(contentPanel, gbc, row++, FishToucherBundle.message("cultivation.sect.label.task"), taskComboBox);
        row = addFullWidthRow(contentPanel, gbc, row, taskDescText);
        row = addFullWidthRow(contentPanel, gbc, row, taskProgressBar);
        JPanel taskActions = createActionPanel();
        startTaskButton.addActionListener(e -> {
            TaskOption option = (TaskOption) taskComboBox.getSelectedItem();
            if (option != null) IdleCultivationManager.getInstance().startSectTask(option.task.id());
        });
        claimTaskButton.addActionListener(e -> IdleCultivationManager.getInstance().claimSectTask());
        taskActions.add(startTaskButton);
        taskActions.add(claimTaskButton);
        row = addActionRow(contentPanel, gbc, row, taskActions);

        row = addSeparatorRow(contentPanel, gbc, row);
        row = addFullWidthRow(contentPanel, gbc, row, createSectionLabel(FishToucherBundle.message("cultivation.sect.section.inheritance")));
        addLabelRow(contentPanel, gbc, row++, FishToucherBundle.message("cultivation.sect.label.inheritance"), inheritanceComboBox);
        row = addFullWidthRow(contentPanel, gbc, row, inheritanceDescText);
        JPanel inheritanceActions = createActionPanel();
        purchaseButton.addActionListener(e -> {
            InheritanceOption option = (InheritanceOption) inheritanceComboBox.getSelectedItem();
            if (option != null) IdleCultivationManager.getInstance().purchaseSectInheritance(option.inheritance.id());
        });
        inheritanceActions.add(purchaseButton);
        row = addActionRow(contentPanel, gbc, row, inheritanceActions);

        row = addSeparatorRow(contentPanel, gbc, row);
        row = addFullWidthRow(contentPanel, gbc, row, createSectionLabel(FishToucherBundle.message("cultivation.sect.section.trial")));
        addLabelRow(contentPanel, gbc, row++, FishToucherBundle.message("cultivation.sect.label.trial"), trialComboBox);
        row = addFullWidthRow(contentPanel, gbc, row, trialDescText);
        JPanel trialActions = createActionPanel();
        startTrialButton.addActionListener(e -> {
            TrialOption option = (TrialOption) trialComboBox.getSelectedItem();
            if (option != null) IdleCultivationManager.getInstance().startSectTrial(option.trial.id());
        });
        trialActions.add(startTrialButton);
        row = addActionRow(contentPanel, gbc, row, trialActions);

        addBottomGlue(contentPanel, gbc, row);
        sectComboBox.addActionListener(e -> updateDescriptions());
        taskComboBox.addActionListener(e -> updateDescriptions());
        inheritanceComboBox.addActionListener(e -> updateDescriptions());
        trialComboBox.addActionListener(e -> updateDescriptions());
        return createScrollableTab(contentPanel);
    }

    private void reloadSects(IdleCultivationManager manager) {
        String currentSectId = NovelReaderSettings.getInstance().getCultivationSectId();
        sectComboBox.removeAllItems();
        for (SectCatalog.SectDefinition sect : manager.getSectDefinitions()) {
            SectOption option = new SectOption(sect);
            sectComboBox.addItem(option);
            if (sect.id().equals(currentSectId)) {
                sectComboBox.setSelectedItem(option);
            }
        }
    }

    private void reloadTasks(IdleCultivationManager manager) {
        Object selected = taskComboBox.getSelectedItem();
        String selectedId = selected instanceof TaskOption option ? option.task.id() : "";
        taskComboBox.removeAllItems();
        for (SectCatalog.SectTaskDefinition task : manager.getSectTaskDefinitions()) {
            TaskOption option = new TaskOption(task);
            taskComboBox.addItem(option);
            if (task.id().equals(selectedId)) {
                taskComboBox.setSelectedItem(option);
            }
        }
    }

    private void reloadInheritances(IdleCultivationManager manager) {
        String currentSectName = manager.getCurrentSect() == null ? "" : manager.getCurrentSect().name();
        inheritanceComboBox.removeAllItems();
        for (SectCatalog.SectInheritanceDefinition inheritance : manager.getSectInheritanceDefinitions()) {
            if (currentSectName.isEmpty() || currentSectName.equals(inheritance.sectName())) {
                inheritanceComboBox.addItem(new InheritanceOption(inheritance));
            }
        }
    }

    private void reloadTrials(IdleCultivationManager manager) {
        String currentSectId = NovelReaderSettings.getInstance().getCultivationSectId();
        trialComboBox.removeAllItems();
        for (SectCatalog.SectTrialDefinition trial : manager.getSectTrialDefinitions()) {
            if (currentSectId.isEmpty() || currentSectId.equals(trial.sectId())) {
                trialComboBox.addItem(new TrialOption(trial));
            }
        }
    }

    private void updateButtons(IdleCultivationManager manager) {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        boolean joined = manager.getCurrentSect() != null;
        joinButton.setEnabled(manager.isSectUnlocked());
        leaveButton.setEnabled(joined);
        promoteButton.setEnabled(manager.canPromoteSectRank());
        startTaskButton.setEnabled(joined && !manager.hasActiveSectTask());
        claimTaskButton.setEnabled(manager.isSectTaskReady());
        InheritanceOption inheritance = (InheritanceOption) inheritanceComboBox.getSelectedItem();
        purchaseButton.setEnabled(inheritance != null && manager.canPurchaseSectInheritance(inheritance.inheritance));
        TrialOption trial = (TrialOption) trialComboBox.getSelectedItem();
        startTrialButton.setEnabled(trial != null
                && SectRules.isTrialUnlocked(settings, trial.trial)
                && !settings.isSectTrialDefeated(trial.trial.id())
                && !manager.hasActiveBattle()
                && !manager.hasActiveTravel());
    }

    private void updateDescriptions() {
        if (refreshing) return;
        SectOption sect = (SectOption) sectComboBox.getSelectedItem();
        setWrappingText(sectDescText, sect == null ? "" : sect.sect.style() + "；" + sect.sect.bonusText());
        TaskOption task = (TaskOption) taskComboBox.getSelectedItem();
        setWrappingText(taskDescText, task == null ? "" : IdleCultivationManager.getInstance().getSectTaskDescription(task.task));
        InheritanceOption inheritance = (InheritanceOption) inheritanceComboBox.getSelectedItem();
        setWrappingText(inheritanceDescText, inheritance == null ? "" : formatInheritance(inheritance.inheritance));
        TrialOption trial = (TrialOption) trialComboBox.getSelectedItem();
        setWrappingText(trialDescText, trial == null ? "" : formatTrial(trial.trial));
    }

    private String formatInheritance(SectCatalog.SectInheritanceDefinition inheritance) {
        String learned = NovelReaderSettings.getInstance().isSectInheritanceLearned(inheritance.id())
                ? "；已学习"
                : "";
        return inheritance.description() + "；成本 " + inheritance.contributionCost() + " 贡献；需要 "
                + SectCatalog.rank(inheritance.minRankIndex()).name() + learned;
    }

    private String formatTrial(SectCatalog.SectTrialDefinition trial) {
        String defeated = NovelReaderSettings.getInstance().isSectTrialDefeated(trial.id()) ? "；已通关" : "";
        return "第 " + trial.floor() + " 层：" + trial.enemyName()
                + "；生命 " + trial.maxHealth()
                + "，攻击 " + trial.attack()
                + "，防御 " + trial.defense()
                + defeated;
    }

    private record SectOption(SectCatalog.SectDefinition sect) {
        @Override
        public String toString() {
            return sect.name();
        }
    }

    private record TaskOption(SectCatalog.SectTaskDefinition task) {
        @Override
        public String toString() {
            return task.name();
        }
    }

    private record InheritanceOption(SectCatalog.SectInheritanceDefinition inheritance) {
        @Override
        public String toString() {
            return inheritance.name();
        }
    }

    private record TrialOption(SectCatalog.SectTrialDefinition trial) {
        @Override
        public String toString() {
            return "第 " + trial.floor() + " 层 · " + trial.enemyName();
        }
    }
}
