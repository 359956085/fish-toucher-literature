package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;

import javax.swing.*;
import java.awt.*;

import static com.fish.toucher.ui.IdleCultivationUiSupport.*;

final class IdleCultivationSectTab {

    private final JComponent component;
    private final JPanel unlockedPanel = createFormPanel();
    private final JComboBox<SectOption> sectComboBox = new JComboBox<>();
    private final JComboBox<TaskOption> taskComboBox = new JComboBox<>();
    private final JComboBox<InheritanceOption> inheritanceComboBox = new JComboBox<>();
    private final JComboBox<TrialOption> trialComboBox = new JComboBox<>();
    private final JTextArea statusText = createSectionTextArea("");
    private final JTextArea sectDescText = createHintTextArea();
    private final JTextArea taskDescText = createHintTextArea();
    private final JTextArea eventDescText = createHintTextArea();
    private final JTextArea inheritanceDescText = createHintTextArea();
    private final JTextArea trialDescText = createHintTextArea();
    private final JPanel eventActions = createActionPanel();
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
            unlockedPanel.setVisible(manager.isSectUnlocked());
            reloadSects(manager);
            reloadTasks(manager);
            reloadInheritances(manager);
            reloadTrials(manager);
            updateButtons(manager);
            updateEventState(manager);
            setWrappingText(statusText, manager.isSectUnlocked()
                    ? manager.getCurrentSectTitle() + "\n" + manager.getSectProgressText()
                    : FishToucherBundle.message("cultivation.sect.locked", manager.getRealmName(SectCatalog.UNLOCK_REALM_INDEX)));
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
        row = addFullWidthRow(contentPanel, gbc, row, unlockedPanel);

        GridBagConstraints unlockedGbc = createConstraints();
        int unlockedRow = 0;
        addLabelRow(unlockedPanel, unlockedGbc, unlockedRow++, FishToucherBundle.message("cultivation.sect.label.choose"), sectComboBox);
        unlockedRow = addFullWidthRow(unlockedPanel, unlockedGbc, unlockedRow, sectDescText);

        JPanel sectActions = createActionPanel();
        joinButton.addActionListener(e -> {
            SectOption option = (SectOption) sectComboBox.getSelectedItem();
            if (option != null && confirmSectAction(FishToucherBundle.message("cultivation.sect.confirm.join", option.sect.name()))) {
                IdleCultivationManager.getInstance().joinSect(option.sect.id());
            }
        });
        leaveButton.addActionListener(e -> {
            if (confirmSectAction(FishToucherBundle.message("cultivation.sect.confirm.leave"))) {
                IdleCultivationManager.getInstance().leaveSect();
            }
        });
        promoteButton.addActionListener(e -> IdleCultivationManager.getInstance().promoteSectRank());
        sectActions.add(joinButton);
        sectActions.add(leaveButton);
        sectActions.add(promoteButton);
        unlockedRow = addActionRow(unlockedPanel, unlockedGbc, unlockedRow, sectActions);

        unlockedRow = addSeparatorRow(unlockedPanel, unlockedGbc, unlockedRow);
        unlockedRow = addFullWidthRow(unlockedPanel, unlockedGbc, unlockedRow, createSectionLabel(FishToucherBundle.message("cultivation.sect.section.task")));
        addLabelRow(unlockedPanel, unlockedGbc, unlockedRow++, FishToucherBundle.message("cultivation.sect.label.task"), taskComboBox);
        unlockedRow = addFullWidthRow(unlockedPanel, unlockedGbc, unlockedRow, taskDescText);
        unlockedRow = addFullWidthRow(unlockedPanel, unlockedGbc, unlockedRow, taskProgressBar);
        JPanel taskActions = createActionPanel();
        startTaskButton.addActionListener(e -> {
            TaskOption option = (TaskOption) taskComboBox.getSelectedItem();
            if (option != null) IdleCultivationManager.getInstance().startSectTask(option.task.id());
        });
        claimTaskButton.addActionListener(e -> IdleCultivationManager.getInstance().claimSectTask());
        taskActions.add(startTaskButton);
        taskActions.add(claimTaskButton);
        unlockedRow = addActionRow(unlockedPanel, unlockedGbc, unlockedRow, taskActions);

        unlockedRow = addSeparatorRow(unlockedPanel, unlockedGbc, unlockedRow);
        unlockedRow = addFullWidthRow(unlockedPanel, unlockedGbc, unlockedRow, createSectionLabel(FishToucherBundle.message("cultivation.sect.section.event")));
        unlockedRow = addFullWidthRow(unlockedPanel, unlockedGbc, unlockedRow, eventDescText);
        unlockedRow = addActionRow(unlockedPanel, unlockedGbc, unlockedRow, eventActions);

        unlockedRow = addSeparatorRow(unlockedPanel, unlockedGbc, unlockedRow);
        unlockedRow = addFullWidthRow(unlockedPanel, unlockedGbc, unlockedRow, createSectionLabel(FishToucherBundle.message("cultivation.sect.section.inheritance")));
        addLabelRow(unlockedPanel, unlockedGbc, unlockedRow++, FishToucherBundle.message("cultivation.sect.label.inheritance"), inheritanceComboBox);
        unlockedRow = addFullWidthRow(unlockedPanel, unlockedGbc, unlockedRow, inheritanceDescText);
        JPanel inheritanceActions = createActionPanel();
        purchaseButton.addActionListener(e -> {
            InheritanceOption option = (InheritanceOption) inheritanceComboBox.getSelectedItem();
            if (option != null) IdleCultivationManager.getInstance().purchaseSectInheritance(option.inheritance.id());
        });
        inheritanceActions.add(purchaseButton);
        unlockedRow = addActionRow(unlockedPanel, unlockedGbc, unlockedRow, inheritanceActions);

        unlockedRow = addSeparatorRow(unlockedPanel, unlockedGbc, unlockedRow);
        unlockedRow = addFullWidthRow(unlockedPanel, unlockedGbc, unlockedRow, createSectionLabel(FishToucherBundle.message("cultivation.sect.section.trial")));
        addLabelRow(unlockedPanel, unlockedGbc, unlockedRow++, FishToucherBundle.message("cultivation.sect.label.trial"), trialComboBox);
        unlockedRow = addFullWidthRow(unlockedPanel, unlockedGbc, unlockedRow, trialDescText);
        JPanel trialActions = createActionPanel();
        startTrialButton.addActionListener(e -> {
            TrialOption option = (TrialOption) trialComboBox.getSelectedItem();
            if (option != null) IdleCultivationManager.getInstance().startSectTrial(option.trial.id());
        });
        trialActions.add(startTrialButton);
        addActionRow(unlockedPanel, unlockedGbc, unlockedRow, trialActions);

        addBottomGlue(contentPanel, gbc, row);
        sectComboBox.addActionListener(e -> onSectSelectionChanged());
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
        inheritanceComboBox.removeAllItems();
        SectCatalog.SectDefinition displaySect = getDisplaySect(manager);
        if (displaySect == null) {
            return;
        }
        for (SectCatalog.SectInheritanceDefinition inheritance : manager.getSectInheritanceDefinitions()) {
            if (displaySect.name().equals(inheritance.sectName())) {
                inheritanceComboBox.addItem(new InheritanceOption(inheritance));
            }
        }
    }

    private void reloadTrials(IdleCultivationManager manager) {
        SectCatalog.SectDefinition displaySect = getDisplaySect(manager);
        trialComboBox.removeAllItems();
        if (displaySect == null) {
            return;
        }
        for (SectCatalog.SectTrialDefinition trial : manager.getSectTrialDefinitions()) {
            if (displaySect.id().equals(trial.sectId())) {
                trialComboBox.addItem(new TrialOption(trial));
            }
        }
    }

    private SectCatalog.SectDefinition getDisplaySect(IdleCultivationManager manager) {
        SectCatalog.SectDefinition currentSect = manager.getCurrentSect();
        if (currentSect != null) {
            return currentSect;
        }
        if (!manager.isSectUnlocked()) {
            return null;
        }
        SectOption selected = (SectOption) sectComboBox.getSelectedItem();
        return selected != null ? selected.sect : null;
    }

    private void onSectSelectionChanged() {
        if (refreshing) return;
        IdleCultivationManager manager = IdleCultivationManager.getInstance();
        reloadInheritances(manager);
        reloadTrials(manager);
        updateButtons(manager);
        updateDescriptions();
    }

    private void updateButtons(IdleCultivationManager manager) {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        boolean joined = manager.getCurrentSect() != null;
        joinButton.setEnabled(manager.isSectUnlocked() && !joined);
        leaveButton.setEnabled(joined);
        promoteButton.setEnabled(manager.canPromoteSectRank());
        startTaskButton.setEnabled(joined && !manager.hasActiveSectTask());
        claimTaskButton.setEnabled(manager.isSectTaskReady());
        InheritanceOption inheritance = (InheritanceOption) inheritanceComboBox.getSelectedItem();
        purchaseButton.setEnabled(joined && inheritance != null && manager.canPurchaseSectInheritance(inheritance.inheritance));
        TrialOption trial = (TrialOption) trialComboBox.getSelectedItem();
        startTrialButton.setEnabled(joined
                && trial != null
                && SectRules.isTrialUnlocked(settings, trial.trial)
                && !settings.isSectTrialDefeated(trial.trial.id())
                && !manager.hasActiveBattle()
                && !manager.hasActiveTravel());
    }

    private boolean confirmSectAction(String message) {
        return JOptionPane.showConfirmDialog(
                component,
                message,
                FishToucherBundle.message("cultivation.sect.confirm.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        ) == JOptionPane.OK_OPTION;
    }

    private void updateEventState(IdleCultivationManager manager) {
        eventActions.removeAll();
        if (manager.getCurrentSect() == null) {
            setWrappingText(eventDescText, "");
            eventActions.revalidate();
            eventActions.repaint();
            return;
        }
        IdleCultivationManager.SectEventInstance event = manager.getCurrentSectEvent();
        if (event == null) {
            setWrappingText(eventDescText, FishToucherBundle.message("cultivation.sect.eventNone"));
            eventActions.revalidate();
            eventActions.repaint();
            return;
        }
        int pendingCount = manager.getPendingSectEvents().size();
        setWrappingText(eventDescText, FishToucherBundle.message(
                "cultivation.sect.eventDesc",
                event.event().title(),
                event.event().description(),
                pendingCount
        ));
        for (SectCatalog.SectEventOptionDefinition option : event.event().options()) {
            JButton button = new JButton(option.label());
            button.setToolTipText(option.description());
            button.setEnabled(manager.canResolveSectEvent(event.instanceId(), option.id()));
            button.addActionListener(e -> IdleCultivationManager.getInstance().resolveSectEvent(event.instanceId(), option.id()));
            eventActions.add(button);
        }
        eventActions.revalidate();
        eventActions.repaint();
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
