package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;

import javax.swing.*;
import java.awt.*;

import static com.fish.toucher.ui.IdleCultivationUiSupport.*;

final class IdleCultivationSectTab {

    private final JComponent component;
    private final JPanel contentPanel = createFormPanel();
    private final JPanel unlockedPanel = createFormPanel();
    private final JPanel ownSectPanel = createFormPanel();
    private final JComboBox<SectOption> sectComboBox = new JComboBox<>();
    private final JComboBox<TaskOption> taskComboBox = new JComboBox<>();
    private final JComboBox<SecretRealmOption> secretRealmComboBox = new JComboBox<>();
    private final JComboBox<InheritanceOption> inheritanceComboBox = new JComboBox<>();
    private final JComboBox<TrialOption> trialComboBox = new JComboBox<>();
    private final JTextArea statusText = createSectionTextArea("");
    private final JTextArea sectDescText = createHintTextArea();
    private final JTextArea taskDescText = createHintTextArea();
    private final JTextArea taskStatusText = createHintTextArea();
    private final JTextArea eventDescText = createHintTextArea();
    private final JTextArea secretRealmDescText = createHintTextArea();
    private final JTextArea inheritanceDescText = createHintTextArea();
    private final JTextArea trialDescText = createHintTextArea();
    private final JPanel eventActions = createActionPanel();
    private final JPanel secretRealmActions = createActionPanel();
    private final JProgressBar taskProgressBar = new JProgressBar(0, 100);
    private final JButton joinButton = new JButton(FishToucherBundle.message("cultivation.sect.button.join"));
    private final JButton leaveButton = new JButton(FishToucherBundle.message("cultivation.sect.button.leave"));
    private final JButton promoteButton = new JButton(FishToucherBundle.message("cultivation.sect.button.promote"));
    private final JButton startTaskButton = new JButton(FishToucherBundle.message("cultivation.sect.button.startTask"));
    private final JButton claimTaskButton = new JButton(FishToucherBundle.message("cultivation.sect.button.claimTask"));
    private final JButton startSecretRealmButton = new JButton(FishToucherBundle.message("cultivation.sect.button.startSecretRealm"));
    private final JButton purchaseButton = new JButton(FishToucherBundle.message("cultivation.sect.button.purchase"));
    private final JButton startTrialButton = new JButton(FishToucherBundle.message("cultivation.sect.button.startTrial"));
    private boolean refreshing;

    IdleCultivationSectTab() {
        taskProgressBar.setStringPainted(true);
        taskProgressBar.setForeground(UIManager.getColor("Label.foreground"));
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
            if (manager.isAscended()) {
                unlockedPanel.setVisible(false);
                ownSectPanel.setVisible(true);
                reloadOwnSectState(manager);
                setWrappingText(statusText, manager.getOwnSectOverviewText());
                return;
            }
            ownSectPanel.setVisible(false);
            unlockedPanel.setVisible(manager.isSectUnlocked());
            reloadSects(manager);
            reloadTasks(manager);
            reloadSecretRealms(manager);
            reloadInheritances(manager);
            reloadTrials(manager);
            updateButtons(manager);
            updateEventState(manager);
            updateSecretRealmState(manager);
            setWrappingText(statusText, manager.isSectUnlocked()
                    ? manager.getCurrentSectTitle() + "\n" + manager.getSectProgressText()
                    : FishToucherBundle.message("cultivation.sect.locked", manager.getRealmName(SectCatalog.UNLOCK_REALM_INDEX)));
            setProgressTextIfChanged(taskProgressBar, manager.getSectTaskProgressPercent(), manager.getSectTaskRemainingText());
            setWrappingText(taskStatusText, getTaskStatusText(manager));
        } finally {
            refreshing = false;
        }
        updateDescriptions();
    }

    private JComponent createContent() {
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        row = addFullWidthRow(contentPanel, gbc, row, createSectionLabel(FishToucherBundle.message("cultivation.sect.title")));
        row = addFullWidthRow(contentPanel, gbc, row, statusText);
        row = addFullWidthRow(contentPanel, gbc, row, unlockedPanel);
        row = addFullWidthRow(contentPanel, gbc, row, ownSectPanel);

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
        unlockedRow = addFullWidthRow(unlockedPanel, unlockedGbc, unlockedRow, taskStatusText);
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
        unlockedRow = addActionRow(unlockedPanel, unlockedGbc, unlockedRow, trialActions);

        unlockedRow = addSeparatorRow(unlockedPanel, unlockedGbc, unlockedRow);
        unlockedRow = addFullWidthRow(unlockedPanel, unlockedGbc, unlockedRow, createSectionLabel(FishToucherBundle.message("cultivation.sect.section.secretRealm")));
        addLabelRow(unlockedPanel, unlockedGbc, unlockedRow++, FishToucherBundle.message("cultivation.sect.label.secretRealm"), secretRealmComboBox);
        unlockedRow = addFullWidthRow(unlockedPanel, unlockedGbc, unlockedRow, secretRealmDescText);
        startSecretRealmButton.addActionListener(e -> {
            SecretRealmOption option = (SecretRealmOption) secretRealmComboBox.getSelectedItem();
            if (option != null) IdleCultivationManager.getInstance().startSectSecretRealm(option.secretRealm.id());
        });
        secretRealmActions.add(startSecretRealmButton);
        addActionRow(unlockedPanel, unlockedGbc, unlockedRow, secretRealmActions);

        addBottomGlue(contentPanel, gbc, row);
        sectComboBox.addActionListener(e -> onSectSelectionChanged());
        taskComboBox.addActionListener(e -> {
            if (!refreshing) {
                TaskOption option = (TaskOption) taskComboBox.getSelectedItem();
                if (option != null) {
                    NovelReaderSettings.getInstance().setSelectedSectTaskId(option.task.id());
                }
            }
            updateButtons(IdleCultivationManager.getInstance());
            updateDescriptions();
        });
        secretRealmComboBox.addActionListener(e -> {
            if (!refreshing) {
                SecretRealmOption option = (SecretRealmOption) secretRealmComboBox.getSelectedItem();
                if (option != null) {
                    NovelReaderSettings.getInstance().setSelectedSectSecretRealmId(option.secretRealm.id());
                }
            }
            updateButtons(IdleCultivationManager.getInstance());
            updateDescriptions();
        });
        inheritanceComboBox.addActionListener(e -> {
            if (!refreshing) {
                InheritanceOption option = (InheritanceOption) inheritanceComboBox.getSelectedItem();
                if (option != null) {
                    NovelReaderSettings.getInstance().setSelectedSectInheritanceId(option.inheritance.id());
                }
            }
            updateButtons(IdleCultivationManager.getInstance());
            updateDescriptions();
        });
        trialComboBox.addActionListener(e -> {
            if (!refreshing) {
                TrialOption option = (TrialOption) trialComboBox.getSelectedItem();
                if (option != null) {
                    NovelReaderSettings.getInstance().setSelectedSectTrialId(option.trial.id());
                }
            }
            updateButtons(IdleCultivationManager.getInstance());
            updateDescriptions();
        });
        return createScrollableTab(contentPanel);
    }

    private void reloadOwnSectState(IdleCultivationManager manager) {
        ownSectPanel.removeAll();
        GridBagConstraints gbc = createConstraints();
        int row = 0;
        if (manager.canCreateOwnSect()) {
            row = addFullWidthRow(ownSectPanel, gbc, row, createGuideTextArea(FishToucherBundle.message("cultivation.ownSect.createHint")));
            JButton createButton = new JButton(FishToucherBundle.message("cultivation.ownSect.button.create"));
            createButton.addActionListener(e -> createOwnSect());
            JPanel actions = createActionPanel();
            actions.add(createButton);
            row = addActionRow(ownSectPanel, gbc, row, actions);
            addBottomGlue(ownSectPanel, gbc, row);
            refreshOwnSectPanel();
            return;
        }

        row = addFullWidthRow(ownSectPanel, gbc, row, createSectionLabel(FishToucherBundle.message("cultivation.ownSect.section.overview")));
        row = addFullWidthRow(ownSectPanel, gbc, row, createHintTextArea(manager.getOwnSectBonusText()));
        JButton promoteButton = new JButton(FishToucherBundle.message("cultivation.ownSect.button.promote"));
        promoteButton.setEnabled(manager.canPromoteOwnSect());
        promoteButton.addActionListener(e -> IdleCultivationManager.getInstance().promoteOwnSect());
        JPanel promoteActions = createActionPanel();
        promoteActions.add(promoteButton);
        row = addActionRow(ownSectPanel, gbc, row, promoteActions);

        row = addSeparatorRow(ownSectPanel, gbc, row);
        row = addFullWidthRow(ownSectPanel, gbc, row, createSectionLabel(FishToucherBundle.message("cultivation.ownSect.section.recruit")));
        JComboBox<SpecialtyOption> specialtyComboBox = new JComboBox<>();
        for (AscendedSectCatalog.Specialty specialty : manager.getOwnSectSpecialties()) {
            specialtyComboBox.addItem(new SpecialtyOption(specialty));
        }
        addLabelRow(ownSectPanel, gbc, row++, FishToucherBundle.message("cultivation.ownSect.label.specialty"), specialtyComboBox);
        JButton recruitButton = new JButton(FishToucherBundle.message("cultivation.ownSect.button.recruit"));
        recruitButton.addActionListener(e -> {
            SpecialtyOption option = (SpecialtyOption) specialtyComboBox.getSelectedItem();
            if (option != null) IdleCultivationManager.getInstance().startOwnSectRecruitment(option.specialty);
        });
        JPanel recruitActions = createActionPanel();
        recruitActions.add(recruitButton);
        row = addActionRow(ownSectPanel, gbc, row, recruitActions);
        for (NovelReaderSettings.OwnSectDiscipleState candidate : NovelReaderSettings.getInstance().getOwnSectRecruitmentCandidates()) {
            JPanel candidateActions = createActionPanel();
            candidateActions.add(createHintTextArea(formatOwnSectDisciple(candidate)));
            JButton chooseButton = new JButton(FishToucherBundle.message("cultivation.ownSect.button.chooseDisciple"));
            chooseButton.addActionListener(e -> IdleCultivationManager.getInstance().recruitOwnSectDisciple(candidate.id));
            candidateActions.add(chooseButton);
            row = addActionRow(ownSectPanel, gbc, row, candidateActions);
        }

        row = addSeparatorRow(ownSectPanel, gbc, row);
        row = addFullWidthRow(ownSectPanel, gbc, row, createSectionLabel(FishToucherBundle.message("cultivation.ownSect.section.buildings")));
        for (AscendedSectCatalog.BuildingDefinition building : manager.getOwnSectBuildingDefinitions()) {
            row = addFullWidthRow(ownSectPanel, gbc, row, createSectionTextArea(building.name() + "  " + manager.getOwnSectBuildingLevelText(building.id())));
            row = addFullWidthRow(ownSectPanel, gbc, row, createHintTextArea(building.description()));
            addLabelRow(ownSectPanel, gbc, row++, FishToucherBundle.message("cultivation.label.effect"), createHintTextArea(manager.getOwnSectBuildingEffectText(building.id())));
            addLabelRow(ownSectPanel, gbc, row++, FishToucherBundle.message("cultivation.label.upgradeCost"), createHintTextArea(manager.getOwnSectBuildingCostText(building.id())));
            addLabelRow(ownSectPanel, gbc, row++, FishToucherBundle.message("cultivation.ownSect.label.assigned"), createHintTextArea(manager.getOwnSectBuildingAssignedText(building.id())));
            JPanel actions = createActionPanel();
            JButton upgradeButton = new JButton(FishToucherBundle.message("cultivation.button.upgradeFacility"));
            upgradeButton.setEnabled(manager.canUpgradeOwnSectBuilding(building.id()));
            upgradeButton.addActionListener(e -> IdleCultivationManager.getInstance().upgradeOwnSectBuilding(building.id()));
            actions.add(upgradeButton);
            if (AscendedSectCatalog.ALCHEMY_HALL_ID.equals(building.id())) {
                JButton claimButton = new JButton(FishToucherBundle.message("cultivation.button.claimAbode"));
                claimButton.setEnabled(manager.canClaimOwnSectAlchemy());
                claimButton.addActionListener(e -> IdleCultivationManager.getInstance().claimOwnSectAlchemy());
                actions.add(claimButton);
            }
            JComboBox<DiscipleOption> discipleComboBox = new JComboBox<>();
            for (NovelReaderSettings.OwnSectDiscipleState disciple : NovelReaderSettings.getInstance().getOwnSectDisciples()) {
                if (building.specialty().name().equals(disciple.specialty) && disciple.assignedBuildingId.isEmpty()) {
                    discipleComboBox.addItem(new DiscipleOption(disciple));
                }
            }
            JButton assignButton = new JButton(FishToucherBundle.message("cultivation.ownSect.button.assign"));
            assignButton.setEnabled(discipleComboBox.getItemCount() > 0);
            assignButton.addActionListener(e -> {
                DiscipleOption option = (DiscipleOption) discipleComboBox.getSelectedItem();
                if (option != null) IdleCultivationManager.getInstance().assignOwnSectDisciple(option.disciple.id, building.id());
            });
            actions.add(discipleComboBox);
            actions.add(assignButton);
            row = addActionRow(ownSectPanel, gbc, row, actions);
            row = addSeparatorRow(ownSectPanel, gbc, row);
        }

        row = addFullWidthRow(ownSectPanel, gbc, row, createSectionLabel(FishToucherBundle.message("cultivation.ownSect.section.disciples")));
        for (NovelReaderSettings.OwnSectDiscipleState disciple : NovelReaderSettings.getInstance().getOwnSectDisciples()) {
            JPanel discipleActions = createActionPanel();
            discipleActions.add(createHintTextArea(formatOwnSectDisciple(disciple)));
            JButton unassignButton = new JButton(FishToucherBundle.message("cultivation.ownSect.button.unassign"));
            unassignButton.setEnabled(!disciple.assignedBuildingId.isEmpty());
            unassignButton.addActionListener(e -> IdleCultivationManager.getInstance().unassignOwnSectDisciple(disciple.id));
            JButton dismissButton = new JButton(FishToucherBundle.message("cultivation.ownSect.button.dismiss"));
            dismissButton.addActionListener(e -> IdleCultivationManager.getInstance().dismissOwnSectDisciple(disciple.id));
            discipleActions.add(unassignButton);
            discipleActions.add(dismissButton);
            row = addActionRow(ownSectPanel, gbc, row, discipleActions);
        }
        addBottomGlue(ownSectPanel, gbc, row);
        refreshOwnSectPanel();
    }

    private void createOwnSect() {
        String name = JOptionPane.showInputDialog(
                component,
                FishToucherBundle.message("cultivation.ownSect.createPrompt"),
                FishToucherBundle.message("cultivation.ownSect.createTitle"),
                JOptionPane.QUESTION_MESSAGE
        );
        if (name != null) {
            IdleCultivationManager.getInstance().createOwnSect(name);
            reloadSectState(IdleCultivationManager.getInstance());
        }
    }

    private void refreshOwnSectPanel() {
        ownSectPanel.revalidate();
        ownSectPanel.repaint();
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private String formatOwnSectDisciple(NovelReaderSettings.OwnSectDiscipleState disciple) {
        String assigned = disciple.assignedBuildingId == null || disciple.assignedBuildingId.isEmpty()
                ? FishToucherBundle.message("cultivation.ownSect.unassigned")
                : disciple.assignedBuildingId;
        return FishToucherBundle.message("cultivation.ownSect.discipleText", disciple.name, formatSpecialty(disciple.specialty), disciple.aptitude, assigned);
    }

    private String formatSpecialty(String specialty) {
        try {
            return AscendedSectCatalog.Specialty.valueOf(specialty).label();
        } catch (RuntimeException ignored) {
            return specialty == null ? "" : specialty;
        }
    }

    private void reloadSects(IdleCultivationManager manager) {
        String currentSectId = NovelReaderSettings.getInstance().getCultivationSectId();
        String selectedId = currentSectId.isEmpty()
                ? NovelReaderSettings.getInstance().getSelectedSectPreviewId()
                : currentSectId;
        sectComboBox.removeAllItems();
        for (SectCatalog.SectDefinition sect : manager.getSectDefinitions()) {
            SectOption option = new SectOption(sect);
            sectComboBox.addItem(option);
            if (sect.id().equals(selectedId)) {
                sectComboBox.setSelectedItem(option);
            }
        }
    }

    private void reloadTasks(IdleCultivationManager manager) {
        SectCatalog.SectTaskDefinition activeTask = manager.getActiveSectTask();
        String selectedId = activeTask != null
                ? activeTask.id()
                : NovelReaderSettings.getInstance().getSelectedSectTaskId();
        taskComboBox.removeAllItems();
        for (SectCatalog.SectTaskDefinition task : manager.getSectTaskDefinitions()) {
            TaskOption option = new TaskOption(task);
            taskComboBox.addItem(option);
            if (task.id().equals(selectedId)) {
                taskComboBox.setSelectedItem(option);
            }
        }
    }

    private void reloadSecretRealms(IdleCultivationManager manager) {
        String selectedId = NovelReaderSettings.getInstance().getSelectedSectSecretRealmId();
        secretRealmComboBox.removeAllItems();
        for (SectCatalog.SectSecretRealmDefinition secretRealm : manager.getCurrentSectSecretRealmDefinitions()) {
            SecretRealmOption option = new SecretRealmOption(secretRealm);
            secretRealmComboBox.addItem(option);
            if (secretRealm.id().equals(selectedId)) {
                secretRealmComboBox.setSelectedItem(option);
            }
        }
    }

    private void reloadInheritances(IdleCultivationManager manager) {
        String selectedId = NovelReaderSettings.getInstance().getSelectedSectInheritanceId();
        inheritanceComboBox.removeAllItems();
        SectCatalog.SectDefinition displaySect = getDisplaySect(manager);
        if (displaySect == null) {
            return;
        }
        for (SectCatalog.SectInheritanceDefinition inheritance : manager.getSectInheritanceDefinitions()) {
            if (displaySect.name().equals(inheritance.sectName())) {
                InheritanceOption option = new InheritanceOption(inheritance);
                inheritanceComboBox.addItem(option);
                if (inheritance.id().equals(selectedId)) {
                    inheritanceComboBox.setSelectedItem(option);
                }
            }
        }
    }

    private void reloadTrials(IdleCultivationManager manager) {
        SectCatalog.SectDefinition displaySect = getDisplaySect(manager);
        String selectedId = NovelReaderSettings.getInstance().getSelectedSectTrialId();
        trialComboBox.removeAllItems();
        if (displaySect == null) {
            return;
        }
        for (SectCatalog.SectTrialDefinition trial : manager.getSectTrialDefinitions()) {
            if (displaySect.id().equals(trial.sectId())) {
                TrialOption option = new TrialOption(trial);
                trialComboBox.addItem(option);
                if (trial.id().equals(selectedId)) {
                    trialComboBox.setSelectedItem(option);
                }
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
        SectOption selected = (SectOption) sectComboBox.getSelectedItem();
        if (manager.getCurrentSect() == null && selected != null) {
            NovelReaderSettings.getInstance().setSelectedSectPreviewId(selected.sect.id());
        }
        reloadInheritances(manager);
        reloadTrials(manager);
        reloadSecretRealms(manager);
        updateButtons(manager);
        updateSecretRealmState(manager);
        updateDescriptions();
    }

    private void updateButtons(IdleCultivationManager manager) {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        boolean joined = manager.getCurrentSect() != null;
        joinButton.setEnabled(manager.isSectUnlocked() && !joined);
        leaveButton.setEnabled(joined);
        promoteButton.setEnabled(manager.canPromoteSectRank());
        TaskOption task = (TaskOption) taskComboBox.getSelectedItem();
        startTaskButton.setEnabled(joined
                && task != null
                && SectRules.isTaskUnlocked(settings, task.task)
                && !manager.hasActiveSectTask()
                && !manager.hasActiveSectSecretRealm());
        claimTaskButton.setEnabled(manager.isSectTaskReady());
        SecretRealmOption secretRealm = (SecretRealmOption) secretRealmComboBox.getSelectedItem();
        startSecretRealmButton.setEnabled(joined
                && secretRealm != null
                && manager.canStartSectSecretRealm(secretRealm.secretRealm.id()));
        InheritanceOption inheritance = (InheritanceOption) inheritanceComboBox.getSelectedItem();
        purchaseButton.setEnabled(joined && inheritance != null && manager.canPurchaseSectInheritance(inheritance.inheritance));
        TrialOption trial = (TrialOption) trialComboBox.getSelectedItem();
        String trialBlockReason = getTrialBlockReason(manager, settings, joined, trial);
        startTrialButton.setEnabled(trialBlockReason.isEmpty());
        startTrialButton.setToolTipText(trialBlockReason.isEmpty() ? null : trialBlockReason);
    }

    private String getTrialBlockReason(IdleCultivationManager manager,
                                       NovelReaderSettings settings,
                                       boolean joined,
                                       TrialOption trial) {
        if (!joined) {
            return FishToucherBundle.message("cultivation.sect.needJoin");
        }
        if (trial == null) {
            return FishToucherBundle.message("cultivation.sect.trialUnknown");
        }
        if (!SectRules.isTrialUnlocked(settings, trial.trial)) {
            return FishToucherBundle.message("cultivation.sect.trialLocked");
        }
        if (settings.isSectTrialDefeated(trial.trial.id())) {
            return FishToucherBundle.message("cultivation.sect.trialDefeatedHint");
        }
        if (manager.hasActiveBattle()) {
            return FishToucherBundle.message("cultivation.status.challengeBusy");
        }
        if (manager.hasActiveTravel()) {
            return FishToucherBundle.message("cultivation.status.challengeBlockedByTravel");
        }
        if (manager.hasActiveSectSecretRealm()) {
            return FishToucherBundle.message("cultivation.sect.secretRealmBusy");
        }
        return "";
    }

    private String getTaskStatusText(IdleCultivationManager manager) {
        SectCatalog.SectTaskDefinition activeTask = manager.getActiveSectTask();
        String remainingText = manager.getSectTaskRemainingText();
        return activeTask == null
                ? remainingText
                : FishToucherBundle.message("cultivation.sect.taskActiveStatus", activeTask.name(), remainingText);
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

    private void updateSecretRealmState(IdleCultivationManager manager) {
        secretRealmActions.removeAll();
        SecretRealmOption selected = (SecretRealmOption) secretRealmComboBox.getSelectedItem();
        SectCatalog.SectSecretRealmNodeDefinition node = manager.getSectSecretRealmCurrentNode();
        setWrappingText(secretRealmDescText, formatSecretRealm(manager, selected, node));
        if (node == null) {
            secretRealmActions.add(startSecretRealmButton);
        } else if (node.type() == SectCatalog.SecretRealmNodeType.CHOICE) {
            for (SectCatalog.SectSecretRealmOptionDefinition option : node.options()) {
                JButton button = new JButton(option.label());
                button.setToolTipText(option.description());
                button.addActionListener(e -> IdleCultivationManager.getInstance().resolveSectSecretRealmNode(option.id()));
                secretRealmActions.add(button);
            }
        } else {
            JButton button = new JButton(FishToucherBundle.message("cultivation.sect.button.resolveSecretRealmNode"));
            button.addActionListener(e -> IdleCultivationManager.getInstance().resolveSectSecretRealmNode(""));
            secretRealmActions.add(button);
        }
        secretRealmActions.revalidate();
        secretRealmActions.repaint();
    }

    private void updateDescriptions() {
        if (refreshing) return;
        SectOption sect = (SectOption) sectComboBox.getSelectedItem();
        setWrappingText(sectDescText, sect == null ? "" : sect.sect.style() + "；" + sect.sect.bonusText());
        TaskOption task = (TaskOption) taskComboBox.getSelectedItem();
        setWrappingText(taskDescText, task == null ? "" : IdleCultivationManager.getInstance().getSectTaskDescription(task.task));
        SecretRealmOption secretRealm = (SecretRealmOption) secretRealmComboBox.getSelectedItem();
        setWrappingText(secretRealmDescText, formatSecretRealm(IdleCultivationManager.getInstance(), secretRealm, IdleCultivationManager.getInstance().getSectSecretRealmCurrentNode()));
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

    private String formatSecretRealm(IdleCultivationManager manager, SecretRealmOption option,
                                     SectCatalog.SectSecretRealmNodeDefinition node) {
        if (manager.getCurrentSect() == null) {
            return "";
        }
        String status = manager.getSectSecretRealmStatusText();
        if (option == null) {
            return status;
        }
        if (node == null) {
            return option.secretRealm.description() + "；" + status;
        }
        String chance = node.type() == SectCatalog.SecretRealmNodeType.BATTLE || node.type() == SectCatalog.SecretRealmNodeType.BOSS
                ? "；" + FishToucherBundle.message("cultivation.sect.secretRealmBuildRequired")
                : "";
        return status + "\n" + node.description() + chance;
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

    private record SecretRealmOption(SectCatalog.SectSecretRealmDefinition secretRealm) {
        @Override
        public String toString() {
            return secretRealm.name();
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

    private record SpecialtyOption(AscendedSectCatalog.Specialty specialty) {
        @Override
        public String toString() {
            return specialty.label();
        }
    }

    private record DiscipleOption(NovelReaderSettings.OwnSectDiscipleState disciple) {
        @Override
        public String toString() {
            return disciple.name + " · " + disciple.aptitude;
        }
    }
}
