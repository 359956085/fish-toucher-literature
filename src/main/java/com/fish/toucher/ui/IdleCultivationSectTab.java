package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static com.fish.toucher.ui.IdleCultivationUiSupport.*;

final class IdleCultivationSectTab {

    private static final Logger LOG = Logger.getInstance(IdleCultivationSectTab.class);
    private static final boolean DEBUG_LAYOUT = true;
    private static final String LAYOUT_LOG_PREFIX = "[CultivationLayout] ";
    private static final String CARD_NORMAL = "normal";
    private static final String CARD_ASCENDED = "ascended";
    private final JComponent component;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final JPanel contentPanel = createFormPanel();
    private final JPanel unlockedPanel = createNestedFormPanel();
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
    private final JTextArea trialResultText = createHintTextArea();
    private final List<JLabel> ownSectHeaderLabels = new ArrayList<>();
    private final JPanel ownSectHeaderPanel = createOwnSectHeaderPanel();
    private final JPanel eventActions = createActionPanel();
    private final JPanel secretRealmActions = createActionPanel();
    private final JProgressBar taskProgressBar = createReadableProgressBar();
    private final JButton joinButton = new JButton(FishToucherBundle.message("cultivation.sect.button.join"));
    private final JButton leaveButton = new JButton(FishToucherBundle.message("cultivation.sect.button.leave"));
    private final JButton promoteButton = new JButton(FishToucherBundle.message("cultivation.sect.button.promote"));
    private final JButton startTaskButton = new JButton(FishToucherBundle.message("cultivation.sect.button.startTask"));
    private final JButton claimTaskButton = new JButton(FishToucherBundle.message("cultivation.sect.button.claimTask"));
    private final JButton startSecretRealmButton = new JButton(FishToucherBundle.message("cultivation.sect.button.startSecretRealm"));
    private final JButton purchaseButton = new JButton(FishToucherBundle.message("cultivation.sect.button.purchase"));
    private final JButton startTrialButton = new JButton(FishToucherBundle.message("cultivation.sect.button.startTrial"));
    private final Map<String, OwnSectBuildingComponents> ownSectBuildingComponents = new LinkedHashMap<>();
    private final Map<String, OwnSectDiscipleRowComponents> ownSectDiscipleRows = new LinkedHashMap<>();
    private final Map<String, String> ownSectAssignableDiscipleSignatures = new LinkedHashMap<>();
    private String ownSectLayoutSignature = "";
    private String ownSectDiscipleSignature;
    private boolean ascendedVisible;
    private String activeCard = CARD_NORMAL;
    private JButton ownSectPromoteButton;
    private JButton ownSectRecruitButton;
    private JPanel ownSectRecruitmentActionsPanel;
    private JTextArea ownSectRecruitmentStatusText;
    private JPanel ownSectCandidateControlsPanel;
    private JComboBox<DiscipleOption> ownSectCandidateComboBox;
    private JButton ownSectCompleteRecruitmentButton;
    private JPanel ownSectDiscipleListPanel;
    private boolean refreshing;

    IdleCultivationSectTab() {
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
        boolean ascendedState = false;
        try {
            logRefreshPath("reloadSectState.enter ascended=" + manager.isAscended()
                    + " activeCard=" + activeCard
                    + " thread=" + currentThreadName());
            if (manager.isAscended()) {
                ascendedState = true;
                boolean firstAscendedRefresh = !ascendedVisible;
                logRefreshPath("reloadSectState.ascended firstAscendedRefresh=" + firstAscendedRefresh
                        + " activeCardBefore=" + activeCard
                        + " thread=" + currentThreadName());
                ascendedVisible = true;
                showSectCard(CARD_ASCENDED);
                ownSectPanel.setVisible(true);
                reloadOwnSectState(manager, firstAscendedRefresh);
                debugSectLayout("reloadSectState.ascended");
            } else {
                logRefreshPath("reloadSectState.normal activeCardBefore=" + activeCard
                        + " thread=" + currentThreadName());
                ascendedVisible = false;
                showSectCard(CARD_NORMAL);
                statusText.setVisible(true);
                ownSectLayoutSignature = "";
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
            }
        } finally {
            refreshing = false;
        }
        if (!ascendedState) {
            updateDescriptions();
        }
        logRefreshPath("reloadSectState.exit ascendedState=" + ascendedState
                + " activeCard=" + activeCard
                + " thread=" + currentThreadName());
    }

    private JComponent createContent() {
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
        unlockedRow = addFullWidthRow(unlockedPanel, unlockedGbc, unlockedRow, trialResultText);
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
        cardPanel.add(createScrollableTab(contentPanel), CARD_NORMAL);
        cardPanel.add(createScrollableTab(ownSectPanel), CARD_ASCENDED);
        showSectCard(CARD_NORMAL);
        return cardPanel;
    }

    private void reloadOwnSectState(IdleCultivationManager manager, boolean forceScrollTop) {
        String layoutSignature = createOwnSectLayoutSignature(manager);
        boolean rebuild = !layoutSignature.equals(ownSectLayoutSignature);
        logRefreshPath("reloadOwnSectState.enter oldSignature=" + ownSectLayoutSignature
                + " newSignature=" + layoutSignature
                + " rebuild=" + rebuild
                + " forceScrollTop=" + forceScrollTop
                + " thread=" + currentThreadName());
        if (rebuild) {
            rebuildOwnSectState(manager, layoutSignature);
            updateAscendedStatusText(manager);
            scrollContentToTop();
            debugSectLayout("reloadOwnSectState.rebuild");
            return;
        }
        logRefreshPath("reloadOwnSectState.dynamic forceScrollTop=" + forceScrollTop
                + " thread=" + currentThreadName());
        preserveOuterScrollPositions(component, () -> {
            updateOwnSectDynamicState(manager);
            updateAscendedStatusText(manager);
        });
        debugSectLayout("reloadOwnSectState.dynamic");
        if (forceScrollTop) {
            scrollContentToTop();
        }
    }

    private void rebuildOwnSectState(IdleCultivationManager manager, String layoutSignature) {
        logRefreshPath("rebuildOwnSectState.enter signature=" + layoutSignature
                + " beforeRemoveCount=" + ownSectPanel.getComponentCount()
                + " thread=" + currentThreadName());
        ownSectPanel.removeAll();
        logRefreshPath("rebuildOwnSectState.afterRemove count=" + ownSectPanel.getComponentCount());
        ownSectBuildingComponents.clear();
        ownSectDiscipleRows.clear();
        ownSectAssignableDiscipleSignatures.clear();
        ownSectPromoteButton = null;
        ownSectRecruitButton = null;
        ownSectRecruitmentActionsPanel = null;
        ownSectRecruitmentStatusText = null;
        ownSectCandidateControlsPanel = null;
        ownSectCandidateComboBox = null;
        ownSectCompleteRecruitmentButton = null;
        ownSectDiscipleListPanel = null;
        ownSectDiscipleSignature = null;
        ownSectLayoutSignature = layoutSignature;
        GridBagConstraints gbc = createConstraints();
        int row = 0;
        row = addFullWidthRow(ownSectPanel, gbc, row, createSectionLabel(FishToucherBundle.message("cultivation.sect.title")));
        row = addFullWidthRow(ownSectPanel, gbc, row, ownSectHeaderPanel);
        row = addSeparatorRow(ownSectPanel, gbc, row);
        if (manager.canCreateOwnSect()) {
            row = addFullWidthRow(ownSectPanel, gbc, row, createGuideTextArea(FishToucherBundle.message("cultivation.ownSect.createHint")));
            JButton createButton = new JButton(FishToucherBundle.message("cultivation.ownSect.button.create"));
            createButton.addActionListener(e -> createOwnSect());
            JPanel actions = createRightActionPanel();
            actions.add(createButton);
            row = addActionRow(ownSectPanel, gbc, row, actions);
            addBottomGlue(ownSectPanel, gbc, row);
            logRefreshPath("rebuildOwnSectState.create rows=" + row
                    + " ownSectPanelCount=" + ownSectPanel.getComponentCount());
            refreshOwnSectPanel("rebuildOwnSectState.create");
            debugSectLayout("rebuildOwnSectState.create");
            return;
        }

        ownSectPromoteButton = new JButton(FishToucherBundle.message("cultivation.ownSect.button.promote"));
        setButtonEnabledWithReason(
                ownSectPromoteButton,
                manager.canPromoteOwnSect(),
                null,
                FishToucherBundle.message("cultivation.ownSect.promoteUnavailable")
        );
        ownSectPromoteButton.addActionListener(e -> IdleCultivationManager.getInstance().promoteOwnSect());
        JPanel promoteActions = createActionPanel();
        promoteActions.add(ownSectPromoteButton);
        row = addActionRow(ownSectPanel, gbc, row, promoteActions);

        row = addSeparatorRow(ownSectPanel, gbc, row);
        row = addFullWidthRow(ownSectPanel, gbc, row, createSectionLabel(FishToucherBundle.message("cultivation.ownSect.section.buildings")));
        for (AscendedSectCatalog.BuildingDefinition building : manager.getOwnSectBuildingDefinitions()) {
            JTextArea titleText = createSectionTextArea(building.name() + "  " + manager.getOwnSectBuildingLevelText(building.id()));
            row = addFullWidthRow(ownSectPanel, gbc, row, titleText);
            row = addFullWidthRow(ownSectPanel, gbc, row, createHintTextArea(building.description()));
            JTextArea effectText = createHintTextArea(manager.getOwnSectBuildingEffectText(building.id()));
            JTextArea costText = createHintTextArea(manager.getOwnSectBuildingCostText(building.id()));
            JTextArea assignedText = createHintTextArea(manager.getOwnSectBuildingAssignedText(building.id()));
            addLabelRow(ownSectPanel, gbc, row++, FishToucherBundle.message("cultivation.label.effect"), effectText);
            addLabelRow(ownSectPanel, gbc, row++, FishToucherBundle.message("cultivation.label.upgradeCost"), costText);
            addLabelRow(ownSectPanel, gbc, row++, FishToucherBundle.message("cultivation.ownSect.label.assigned"), assignedText);
            JPanel actions = createActionPanel();
            JButton upgradeButton = new JButton(FishToucherBundle.message("cultivation.button.upgradeFacility"));
            setButtonEnabledWithReason(
                    upgradeButton,
                    manager.canUpgradeOwnSectBuilding(building.id()),
                    null,
                    FishToucherBundle.message("cultivation.ownSect.upgradeUnavailable")
            );
            upgradeButton.addActionListener(e -> IdleCultivationManager.getInstance().upgradeOwnSectBuilding(building.id()));
            actions.add(upgradeButton);
            JButton claimButton = null;
            if (AscendedSectCatalog.ALCHEMY_HALL_ID.equals(building.id())) {
                claimButton = new JButton(FishToucherBundle.message("cultivation.button.claimAbode"));
                setButtonEnabledWithReason(
                        claimButton,
                        manager.canClaimOwnSectAlchemy(),
                        null,
                        FishToucherBundle.message("cultivation.status.nothingToClaim")
                );
                claimButton.addActionListener(e -> IdleCultivationManager.getInstance().claimOwnSectAlchemy());
                actions.add(claimButton);
            }
            JComboBox<DiscipleOption> discipleComboBox = new JComboBox<>();
            JButton assignButton = new JButton(FishToucherBundle.message("cultivation.ownSect.button.assign"));
            refreshOwnSectAssignableDiscipleCombo(discipleComboBox, assignButton, building);
            assignButton.addActionListener(e -> {
                DiscipleOption option = (DiscipleOption) discipleComboBox.getSelectedItem();
                if (option != null && option.disciple != null) {
                    IdleCultivationManager.getInstance().assignOwnSectDisciple(option.disciple.id, building.id());
                }
            });
            actions.add(discipleComboBox);
            actions.add(assignButton);
            row = addActionRow(ownSectPanel, gbc, row, actions);
            row = addSeparatorRow(ownSectPanel, gbc, row);
            ownSectBuildingComponents.put(
                    building.id(),
                    new OwnSectBuildingComponents(titleText, effectText, costText, assignedText, upgradeButton, claimButton, discipleComboBox, assignButton)
            );
        }

        row = addFullWidthRow(ownSectPanel, gbc, row, createSectionLabel(FishToucherBundle.message("cultivation.ownSect.section.stewardHall")));
        JComboBox<SpecialtyOption> specialtyComboBox = new JComboBox<>();
        for (AscendedSectCatalog.Specialty specialty : manager.getOwnSectSpecialties()) {
            specialtyComboBox.addItem(new SpecialtyOption(specialty));
        }
        addLabelRow(ownSectPanel, gbc, row++, FishToucherBundle.message("cultivation.ownSect.label.specialty"), specialtyComboBox);
        ownSectRecruitButton = new JButton(FishToucherBundle.message("cultivation.ownSect.button.recruit"));
        ownSectRecruitButton.addActionListener(e -> {
            SpecialtyOption option = (SpecialtyOption) specialtyComboBox.getSelectedItem();
            if (option != null) IdleCultivationManager.getInstance().startOwnSectRecruitment(option.specialty);
        });
        ownSectRecruitmentStatusText = createHintTextArea(FishToucherBundle.message("cultivation.ownSect.noRecruitmentCandidates"));
        row = addFullWidthRow(ownSectPanel, gbc, row, ownSectRecruitmentStatusText);
        ownSectCandidateControlsPanel = createOwnSectSubPanel();
        ownSectCandidateComboBox = new JComboBox<>();
        ownSectCandidateComboBox.addActionListener(e -> updateOwnSectCompleteRecruitmentButton());
        GridBagConstraints candidateGbc = createConstraints();
        int candidateRow = 0;
        addLabelRow(ownSectCandidateControlsPanel, candidateGbc, candidateRow++, FishToucherBundle.message("cultivation.ownSect.label.candidate"), ownSectCandidateComboBox);
        ownSectCompleteRecruitmentButton = new JButton(FishToucherBundle.message("cultivation.ownSect.button.completeRecruitment"));
        ownSectCompleteRecruitmentButton.addActionListener(e -> {
            DiscipleOption option = (DiscipleOption) ownSectCandidateComboBox.getSelectedItem();
            if (option != null && option.disciple != null) {
                IdleCultivationManager.getInstance().recruitOwnSectDisciple(option.disciple.id);
            }
        });
        row = addFullWidthRow(ownSectPanel, gbc, row, ownSectCandidateControlsPanel);
        ownSectRecruitmentActionsPanel = createActionPanel();
        ownSectRecruitmentActionsPanel.add(ownSectRecruitButton);
        ownSectRecruitmentActionsPanel.add(ownSectCompleteRecruitmentButton);
        row = addActionRow(ownSectPanel, gbc, row, ownSectRecruitmentActionsPanel);

        ownSectDiscipleListPanel = createOwnSectSubPanel();
        row = addFullWidthRow(ownSectPanel, gbc, row, ownSectDiscipleListPanel);
        updateOwnSectDynamicState(manager);
        addBottomGlue(ownSectPanel, gbc, row);
        logRefreshPath("rebuildOwnSectState.created rows=" + row
                + " ownSectPanelCount=" + ownSectPanel.getComponentCount()
                + " buildings=" + manager.getOwnSectBuildingDefinitions().size()
                + " disciples=" + NovelReaderSettings.getInstance().getOwnSectDisciples().size());
        refreshOwnSectPanel("rebuildOwnSectState.created");
        debugSectLayout("rebuildOwnSectState.created");
    }

    private void updateOwnSectDynamicState(IdleCultivationManager manager) {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        logRefreshPath("updateOwnSectDynamicState.enter canCreate=" + manager.canCreateOwnSect()
                + " recruitButtonNull=" + (ownSectRecruitButton == null)
                + " buildings=" + manager.getOwnSectBuildingDefinitions().size()
                + " disciples=" + settings.getOwnSectDisciples().size()
                + " candidates=" + settings.getOwnSectRecruitmentCandidates().size()
                + " thread=" + currentThreadName());
        if (manager.canCreateOwnSect() || ownSectRecruitButton == null) {
            logRefreshPath("updateOwnSectDynamicState.skip canCreate=" + manager.canCreateOwnSect()
                    + " recruitButtonNull=" + (ownSectRecruitButton == null));
            return;
        }
        setButtonEnabledWithReason(
                ownSectPromoteButton,
                manager.canPromoteOwnSect(),
                null,
                FishToucherBundle.message("cultivation.ownSect.promoteUnavailable")
        );
        setButtonEnabledWithReason(
                ownSectRecruitButton,
                manager.canStartOwnSectRecruitment(),
                null,
                getOwnSectRecruitmentStartBlockReason(manager, settings)
        );
        setWrappingText(ownSectRecruitmentStatusText, manager.getOwnSectRecruitmentStatusText());
        for (AscendedSectCatalog.BuildingDefinition building : manager.getOwnSectBuildingDefinitions()) {
            OwnSectBuildingComponents components = ownSectBuildingComponents.get(building.id());
            if (components == null) {
                continue;
            }
            setWrappingText(components.titleText, building.name() + "  " + manager.getOwnSectBuildingLevelText(building.id()));
            setWrappingText(components.effectText, manager.getOwnSectBuildingEffectText(building.id()));
            setWrappingText(components.costText, manager.getOwnSectBuildingCostText(building.id()));
            setWrappingText(components.assignedText, manager.getOwnSectBuildingAssignedText(building.id()));
            setButtonEnabledWithReason(
                    components.upgradeButton,
                    manager.canUpgradeOwnSectBuilding(building.id()),
                    null,
                    FishToucherBundle.message("cultivation.ownSect.upgradeUnavailable")
            );
            if (components.claimButton != null) {
                setButtonEnabledWithReason(
                        components.claimButton,
                        manager.canClaimOwnSectAlchemy(),
                        null,
                        FishToucherBundle.message("cultivation.status.nothingToClaim")
                );
            }
            refreshOwnSectAssignableDiscipleCombo(components.discipleComboBox, components.assignButton, building);
        }
        updateOwnSectCandidateList(settings);
        updateOwnSectDiscipleList(settings);
        debugSectLayout("updateOwnSectDynamicState");
    }

    private void updateAscendedStatusText(IdleCultivationManager manager) {
        String overviewText = manager.getOwnSectOverviewText();
        String bonusText = manager.getOwnSectBonusText();
        updateOwnSectHeaderLabels(overviewText, bonusText);
        ownSectHeaderPanel.revalidate();
        ownSectHeaderPanel.repaint();
        logRefreshPath("ownSectHeaderLayout.labelLines headerLabelCount=" + ownSectHeaderLabels.size()
                + " "
                + describeComponent("ownSectHeaderPanel", ownSectHeaderPanel)
                + " " + describeComponent("firstHeaderLabel", ownSectHeaderLabels.isEmpty() ? null : ownSectHeaderLabels.get(0)));
    }

    private void updateOwnSectCandidateList(NovelReaderSettings settings) {
        if (ownSectRecruitmentStatusText == null || ownSectCandidateControlsPanel == null || ownSectCandidateComboBox == null) {
            return;
        }
        IdleCultivationManager manager = IdleCultivationManager.getInstance();
        String selectedId = selectedDiscipleId(ownSectCandidateComboBox);
        ownSectCandidateComboBox.removeAllItems();
        DiscipleOption selectedOption = null;
        java.util.List<NovelReaderSettings.OwnSectDiscipleState> candidates = settings.getOwnSectRecruitmentCandidates();
        for (NovelReaderSettings.OwnSectDiscipleState candidate : candidates) {
            DiscipleOption option = new DiscipleOption(candidate);
            ownSectCandidateComboBox.addItem(option);
            if (candidate.id.equals(selectedId)) {
                selectedOption = option;
            }
        }
        if (selectedOption != null) {
            ownSectCandidateComboBox.setSelectedItem(selectedOption);
        }
        boolean hasCandidates = !candidates.isEmpty();
        ownSectRecruitmentStatusText.setVisible(true);
        ownSectCandidateControlsPanel.setVisible(hasCandidates);
        updateOwnSectCompleteRecruitmentButton();
        logRefreshPath("updateOwnSectCandidateList candidates=" + candidates.size()
                + " hasCandidates=" + hasCandidates
                + " selectedId=" + selectedDiscipleId(ownSectCandidateComboBox));
        refreshOwnSectSubPanel("updateOwnSectCandidateList", ownSectCandidateControlsPanel);
    }

    private void updateOwnSectCompleteRecruitmentButton() {
        if (ownSectCompleteRecruitmentButton == null || ownSectCandidateComboBox == null) {
            return;
        }
        IdleCultivationManager manager = IdleCultivationManager.getInstance();
        String candidateId = selectedDiscipleId(ownSectCandidateComboBox);
        setButtonEnabledWithReason(
                ownSectCompleteRecruitmentButton,
                manager.canCompleteOwnSectRecruitment(candidateId),
                null,
                getOwnSectCompleteRecruitmentBlockReason(manager, candidateId)
        );
    }

    private void updateOwnSectDiscipleList(NovelReaderSettings settings) {
        if (ownSectDiscipleListPanel == null) {
            return;
        }
        String signature = createDiscipleSignature(settings);
        boolean missingDiscipleRows = hasMissingDiscipleRows(settings.getOwnSectDisciples());
        if (!signature.equals(ownSectDiscipleSignature) || missingDiscipleRows) {
            ownSectDiscipleSignature = signature;
            rebuildOwnSectDiscipleList(settings);
            return;
        }
        logRefreshPath("updateOwnSectDiscipleList.dynamic disciples=" + settings.getOwnSectDisciples().size());
        for (NovelReaderSettings.OwnSectDiscipleState disciple : settings.getOwnSectDisciples()) {
            OwnSectDiscipleRowComponents components = ownSectDiscipleRows.get(disciple.id);
            if (components == null) {
                continue;
            }
            setWrappingText(components.text, formatOwnSectDisciple(disciple));
            setButtonEnabledWithReason(
                    components.unassignButton,
                    !disciple.assignedBuildingId.isEmpty(),
                    null,
                    FishToucherBundle.message("cultivation.ownSect.unassigned")
            );
        }
    }

    private void rebuildOwnSectDiscipleList(NovelReaderSettings settings) {
        ownSectDiscipleRows.clear();
        ownSectDiscipleListPanel.removeAll();
        logRefreshPath("rebuildOwnSectDiscipleList.afterRemove disciples=" + settings.getOwnSectDisciples().size()
                + " panelCount=" + ownSectDiscipleListPanel.getComponentCount());
        GridBagConstraints gbc = createConstraints();
        int row = 0;
        if (settings.getOwnSectDisciples().isEmpty()) {
            addFullWidthRow(ownSectDiscipleListPanel, gbc, row, createHintTextArea(FishToucherBundle.message("cultivation.ownSect.noDisciple")));
            logRefreshPath("rebuildOwnSectDiscipleList.empty rows=" + row
                    + " panelCount=" + ownSectDiscipleListPanel.getComponentCount());
            refreshOwnSectSubPanel("rebuildOwnSectDiscipleList.empty", ownSectDiscipleListPanel);
            return;
        }
        for (NovelReaderSettings.OwnSectDiscipleState disciple : settings.getOwnSectDisciples()) {
            JPanel discipleActions = createRightActionPanel();
            JButton unassignButton = new JButton(FishToucherBundle.message("cultivation.ownSect.button.unassign"));
            setButtonEnabledWithReason(
                    unassignButton,
                    !disciple.assignedBuildingId.isEmpty(),
                    null,
                    FishToucherBundle.message("cultivation.ownSect.unassigned")
            );
            unassignButton.addActionListener(e -> IdleCultivationManager.getInstance().unassignOwnSectDisciple(disciple.id));
            JButton dismissButton = new JButton(FishToucherBundle.message("cultivation.ownSect.button.dismiss"));
            dismissButton.addActionListener(e -> {
                if (confirmSectAction(FishToucherBundle.message("cultivation.ownSect.confirm.dismiss", disciple.name))) {
                    IdleCultivationManager.getInstance().dismissOwnSectDisciple(disciple.id);
                }
            });
            discipleActions.add(unassignButton);
            discipleActions.add(dismissButton);
            JTextArea discipleText = createHintTextArea(formatOwnSectDisciple(disciple));
            row = addFullWidthRow(ownSectDiscipleListPanel, gbc, row, createInlineActionPanel(discipleText, discipleActions));
            ownSectDiscipleRows.put(disciple.id, new OwnSectDiscipleRowComponents(discipleText, unassignButton));
        }
        logRefreshPath("rebuildOwnSectDiscipleList.created rows=" + row
                + " panelCount=" + ownSectDiscipleListPanel.getComponentCount());
        refreshOwnSectSubPanel("rebuildOwnSectDiscipleList.created", ownSectDiscipleListPanel);
    }

    private void refreshOwnSectAssignableDiscipleCombo(JComboBox<DiscipleOption> comboBox,
                                                       JButton assignButton,
                                                       AscendedSectCatalog.BuildingDefinition building) {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        String selectedId = selectedDiscipleId(comboBox);
        java.util.List<DiscipleOption> options = new ArrayList<>();
        StringJoiner signatureJoiner = new StringJoiner("|");
        for (NovelReaderSettings.OwnSectDiscipleState disciple : settings.getOwnSectDisciples()) {
            if (AscendedSectRules.canAssignOrReplaceDisciple(settings, disciple, building)) {
                DiscipleOption option = new DiscipleOption(disciple);
                options.add(option);
                signatureJoiner.add(nullToEmpty(disciple.id)
                        + "," + nullToEmpty(disciple.name)
                        + "," + nullToEmpty(disciple.specialty)
                        + "," + disciple.aptitude
                        + "," + nullToEmpty(disciple.assignedBuildingId));
            }
        }
        boolean hasAssignableDisciple = !options.isEmpty();
        String signature = hasAssignableDisciple ? signatureJoiner.toString() : "empty";
        String oldSignature = ownSectAssignableDiscipleSignatures.get(building.id());
        if (!signature.equals(oldSignature)) {
            ownSectAssignableDiscipleSignatures.put(building.id(), signature);
            comboBox.removeAllItems();
            DiscipleOption selectedOption = null;
            if (!hasAssignableDisciple) {
                comboBox.addItem(DiscipleOption.empty());
            } else {
                for (DiscipleOption option : options) {
                    comboBox.addItem(option);
                    if (option.disciple != null && option.disciple.id.equals(selectedId)) {
                        selectedOption = option;
                    }
                }
                if (selectedOption != null) {
                    comboBox.setSelectedItem(selectedOption);
                }
            }
            logRefreshPath("refreshOwnSectAssignableDiscipleCombo.rebuild building=" + building.id()
                    + " itemCount=" + comboBox.getItemCount()
                    + " selectedId=" + selectedDiscipleId(comboBox));
        } else {
            logRefreshPath("refreshOwnSectAssignableDiscipleCombo.skip building=" + building.id()
                    + " itemCount=" + comboBox.getItemCount()
                    + " selectedId=" + selectedDiscipleId(comboBox));
        }
        comboBox.setEnabled(hasAssignableDisciple);
        setButtonEnabledWithReason(
                assignButton,
                hasAssignableDisciple,
                null,
                getOwnSectAssignBlockReason(settings, building)
        );
    }

    private String getOwnSectAssignBlockReason(NovelReaderSettings settings,
                                               AscendedSectCatalog.BuildingDefinition building) {
        int buildingLevel = settings.getOwnSectBuildingLevel(building.id());
        int slotCount = AscendedSectRules.buildingSlotCount(buildingLevel);
        if (slotCount <= 0) {
            return FishToucherBundle.message("cultivation.ownSect.assignNoSlot");
        }
        return FishToucherBundle.message("cultivation.ownSect.noAssignableDisciple");
    }

    private String selectedDiscipleId(JComboBox<DiscipleOption> comboBox) {
        DiscipleOption option = (DiscipleOption) comboBox.getSelectedItem();
        return option == null || option.disciple == null ? "" : option.disciple.id;
    }

    private String getOwnSectRecruitmentStartBlockReason(IdleCultivationManager manager, NovelReaderSettings settings) {
        if (settings.getOwnSectDisciples().size() >= AscendedSectRules.discipleLimit(settings)) {
            return FishToucherBundle.message("cultivation.ownSect.discipleFull");
        }
        if (manager.hasActiveOwnSectRecruitment()) {
            return FishToucherBundle.message("cultivation.ownSect.recruitmentBusy");
        }
        if (!settings.getOwnSectRecruitmentCandidates().isEmpty()) {
            return FishToucherBundle.message("cultivation.ownSect.recruitmentPending");
        }
        return FishToucherBundle.message("cultivation.ownSect.createUnavailable");
    }

    private String getOwnSectCompleteRecruitmentBlockReason(IdleCultivationManager manager, String candidateId) {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        if (settings.getOwnSectDisciples().size() >= AscendedSectRules.discipleLimit(settings)) {
            return FishToucherBundle.message("cultivation.ownSect.discipleFull");
        }
        if (manager.hasActiveOwnSectRecruitment()) {
            return FishToucherBundle.message("cultivation.ownSect.recruitmentBusy");
        }
        if (!manager.isOwnSectRecruitmentReady()) {
            return FishToucherBundle.message("cultivation.ownSect.recruitmentPending");
        }
        return candidateId == null || candidateId.isEmpty()
                ? FishToucherBundle.message("cultivation.ownSect.discipleInvalid")
                : FishToucherBundle.message("cultivation.ownSect.discipleInvalid");
    }

    private JPanel createOwnSectHeaderPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createConstraints();
        for (int row = 0; row < 10; row++) {
            JLabel label = createOwnSectHeaderLabel(row < 5);
            ownSectHeaderLabels.add(label);
            addFullWidthRow(panel, gbc, row, label);
        }
        allowHorizontalShrink(panel);
        logRefreshPath("ownSectHeaderLayout.labelLines count=" + panel.getComponentCount());
        return panel;
    }

    private JLabel createOwnSectHeaderLabel(boolean overviewStyle) {
        JLabel label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setForeground(UIManager.getColor("Label.foreground"));
        if (overviewStyle) {
            label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        }
        label.setMinimumSize(new Dimension(0, label.getFontMetrics(label.getFont()).getHeight()));
        return label;
    }

    private void updateOwnSectHeaderLabels(String overviewText, String bonusText) {
        List<String> lines = new ArrayList<>();
        addNonBlankLines(lines, overviewText);
        addNonBlankLines(lines, bonusText);
        for (int i = 0; i < ownSectHeaderLabels.size(); i++) {
            JLabel label = ownSectHeaderLabels.get(i);
            String text = i < lines.size() ? lines.get(i) : "";
            if (!text.equals(label.getText())) {
                label.setText(text);
            }
            label.setMinimumSize(new Dimension(0, label.getPreferredSize().height));
            boolean visible = !text.isEmpty();
            if (label.isVisible() != visible) {
                label.setVisible(visible);
            }
        }
    }

    private void addNonBlankLines(List<String> lines, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String line : text.split("\\R")) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty() && lines.size() < ownSectHeaderLabels.size()) {
                lines.add(trimmedLine);
            }
        }
    }

    private JPanel createOwnSectSubPanel() {
        // 最小宽度保持可读（>0），最小高度跟随内容：
        // 容器比首选尺寸窄时 GridBagLayout 走 MINSIZE 分支、按最小尺寸布局，
        // 高度为 0 的行会被整体 setBounds(0,0,0,0) 卸载（弟子列表曾因此整块消失）。
        // 最小高度=首选高度后，窄宽度下两个分支渲染一致，弟子行始终可见。
        return new JPanel(new GridBagLayout()) {
            @Override
            public Dimension getMinimumSize() {
                return new Dimension(CULTIVATION_MIN_WIDTH, getPreferredSize().height);
            }
        };
    }

    private JPanel createNestedFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            public Dimension getMinimumSize() {
                return new Dimension(CULTIVATION_MIN_WIDTH, getPreferredSize().height);
            }
        };
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return panel;
    }

    private String createOwnSectLayoutSignature(IdleCultivationManager manager) {
        if (manager.canCreateOwnSect()) {
            return "create";
        }
        StringJoiner joiner = new StringJoiner("|", "created:", "");
        for (AscendedSectCatalog.BuildingDefinition building : manager.getOwnSectBuildingDefinitions()) {
            joiner.add(building.id());
        }
        return joiner.toString();
    }

    private String createDiscipleSignature(NovelReaderSettings settings) {
        StringJoiner joiner = new StringJoiner("|");
        for (NovelReaderSettings.OwnSectDiscipleState disciple : settings.getOwnSectDisciples()) {
            joiner.add(nullToEmpty(disciple.id)
                    + "," + nullToEmpty(disciple.name)
                    + "," + nullToEmpty(disciple.specialty)
                    + "," + disciple.aptitude);
        }
        return joiner.toString();
    }

    private boolean hasMissingDiscipleRows(List<NovelReaderSettings.OwnSectDiscipleState> disciples) {
        if (disciples.isEmpty()) {
            return false;
        }
        if (ownSectDiscipleListPanel.getComponentCount() == 0) {
            return true;
        }
        if (ownSectDiscipleRows.size() != disciples.size()) {
            return true;
        }
        for (NovelReaderSettings.OwnSectDiscipleState disciple : disciples) {
            if (!ownSectDiscipleRows.containsKey(disciple.id)) {
                return true;
            }
        }
        return false;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
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
        refreshOwnSectPanel("unknown");
    }

    private void refreshOwnSectPanel(String stage) {
        logRefreshPath("refreshOwnSectPanel stage=" + stage
                + " " + describeComponent("ownSectPanel", ownSectPanel));
        ownSectPanel.revalidate();
        ownSectPanel.repaint();
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    private void refreshOwnSectSubPanel(JPanel panel) {
        refreshOwnSectSubPanel("unknown", panel);
    }

    private void refreshOwnSectSubPanel(String stage, JPanel panel) {
        logRefreshPath("refreshOwnSectSubPanel stage=" + stage
                + " " + describeComponent(ownSectSubPanelName(panel), panel));
        panel.revalidate();
        panel.repaint();
        // 子区块从空列表变成候选/弟子列表时，需要通知父容器重新计算高度，否则滚动页会停留在旧布局。
        ownSectPanel.revalidate();
        ownSectPanel.repaint();
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    private void scrollContentToTop() {
        logRefreshPath("scrollContentToTop.schedule activeCard=" + activeCard
                + " thread=" + currentThreadName());
        SwingUtilities.invokeLater(() -> {
            Container parent = activeContentPanel().getParent();
            while (parent != null) {
                if (parent instanceof JViewport viewport) {
                    Point before = viewport.getViewPosition();
                    viewport.setViewPosition(new Point(0, 0));
                    logRefreshPath("scrollContentToTop.run before=" + formatPoint(before)
                            + " after=" + formatPoint(viewport.getViewPosition())
                            + " activeCard=" + activeCard
                            + " thread=" + currentThreadName());
                    debugSectLayout("scrollContentToTop");
                    return;
                }
                parent = parent.getParent();
            }
        });
    }

    private void showSectCard(String card) {
        String oldCard = activeCard;
        logRefreshPath("showSectCard " + oldCard + " -> " + card
                + (card.equals(oldCard) ? " skip=true" : " skip=false")
                + " thread=" + currentThreadName());
        activeCard = card;
        cardLayout.show(cardPanel, card);
    }

    private JPanel activeContentPanel() {
        return CARD_ASCENDED.equals(activeCard) ? ownSectPanel : contentPanel;
    }

    private void debugSectLayout(String stage) {
        if (!DEBUG_LAYOUT) {
            return;
        }
        SwingUtilities.invokeLater(() -> debugSectLayoutNow(stage));
    }

    private void debugSectLayoutNow(String stage) {
        if (!DEBUG_LAYOUT) {
            return;
        }
        logLayout(stage);
        logLayout("activeCard=" + activeCard);
        debugComponent("cardPanel", cardPanel);
        debugComponent("normalContentPanel", contentPanel);
        debugComponent("activeContentPanel", activeContentPanel());
        debugComponent("statusText", statusText);
        debugComponent("unlockedPanel", unlockedPanel);
        debugComponent("ownSectPanel", ownSectPanel);
        debugComponent("ownSectHeaderPanel", ownSectHeaderPanel);
        debugComponent("firstHeaderLabel", ownSectHeaderLabels.isEmpty() ? null : ownSectHeaderLabels.get(0));
        debugComponent("ownSectRecruitmentActionsPanel", ownSectRecruitmentActionsPanel);
        debugComponent("ownSectCandidateControlsPanel", ownSectCandidateControlsPanel);
        debugComponent("ownSectDiscipleListPanel", ownSectDiscipleListPanel);
        debugViewport(stage);
    }

    private void debugComponent(String name, Component component) {
        if (component == null) {
            logLayout(name + " null");
            return;
        }
        Dimension size = component.getSize();
        Dimension preferredSize = component.getPreferredSize();
        Dimension minimumSize = component.getMinimumSize();
        Rectangle bounds = component.getBounds();
        int componentCount = component instanceof Container container ? container.getComponentCount() : -1;
        String parentName = component.getParent() == null ? "null" : component.getParent().getClass().getSimpleName();
        logLayout(name
                + " visible=" + component.isVisible()
                + " bounds=" + formatRectangle(bounds)
                + " size=" + formatDimension(size)
                + " preferredSize=" + formatDimension(preferredSize)
                + " minimumSize=" + formatDimension(minimumSize)
                + " componentCount=" + componentCount
                + " parent=" + parentName);
    }

    private void debugViewport(String stage) {
        JViewport viewport = findViewport();
        if (viewport == null) {
            logLayout(stage + " viewport=null");
            return;
        }
        Component view = viewport.getView();
        logLayout(stage
                + " viewport extent=" + formatDimension(viewport.getExtentSize())
                + " viewSize=" + (view == null ? "null" : formatDimension(view.getSize()))
                + " viewPreferredSize=" + (view == null ? "null" : formatDimension(view.getPreferredSize()))
                + " viewPosition=" + formatPoint(viewport.getViewPosition()));
    }

    private JViewport findViewport() {
        Container parent = activeContentPanel().getParent();
        while (parent != null) {
            if (parent instanceof JViewport viewport) {
                return viewport;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private void logRefreshPath(String message) {
        if (!DEBUG_LAYOUT) {
            return;
        }
        logLayout("[path] " + message);
    }

    private String describeComponent(String name, Component component) {
        if (component == null) {
            return name + "=null";
        }
        int componentCount = component instanceof Container container ? container.getComponentCount() : -1;
        return name
                + " visible=" + component.isVisible()
                + " size=" + formatDimension(component.getSize())
                + " preferredSize=" + formatDimension(component.getPreferredSize())
                + " minimumSize=" + formatDimension(component.getMinimumSize())
                + " componentCount=" + componentCount;
    }

    private String ownSectSubPanelName(JPanel panel) {
        if (panel == ownSectCandidateControlsPanel) {
            return "ownSectCandidateControlsPanel";
        }
        if (panel == ownSectDiscipleListPanel) {
            return "ownSectDiscipleListPanel";
        }
        if (panel == ownSectHeaderPanel) {
            return "ownSectHeaderPanel";
        }
        if (panel == ownSectPanel) {
            return "ownSectPanel";
        }
        return panel == null ? "nullPanel" : panel.getClass().getSimpleName();
    }

    private String currentThreadName() {
        return Thread.currentThread().getName();
    }

    private void logLayout(String message) {
        String text = LAYOUT_LOG_PREFIX + message;
        LOG.info(text);
        System.out.println(text);
    }

    private String formatDimension(Dimension dimension) {
        return dimension == null ? "null" : dimension.width + "x" + dimension.height;
    }

    private String formatRectangle(Rectangle rectangle) {
        return rectangle == null
                ? "null"
                : rectangle.x + "," + rectangle.y + "," + rectangle.width + "x" + rectangle.height;
    }

    private String formatPoint(Point point) {
        return point == null ? "null" : point.x + "," + point.y;
    }

    private static String formatOwnSectDisciple(NovelReaderSettings.OwnSectDiscipleState disciple) {
        String assigned = formatAssignedBuilding(disciple.assignedBuildingId);
        return FishToucherBundle.message("cultivation.ownSect.discipleText", disciple.name, formatSpecialty(disciple.specialty), disciple.aptitude, assigned);
    }

    private static String formatAssignedBuilding(String buildingId) {
        if (buildingId == null || buildingId.isEmpty()) {
            return FishToucherBundle.message("cultivation.ownSect.unassigned");
        }
        AscendedSectCatalog.BuildingDefinition building = AscendedSectCatalog.building(buildingId);
        return building == null ? FishToucherBundle.message("cultivation.ownSect.unassigned") : building.name();
    }

    private static String formatSpecialty(String specialty) {
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
        setButtonEnabledWithReason(
                joinButton,
                manager.isSectUnlocked() && !joined,
                null,
                getJoinBlockReason(manager, joined)
        );
        setButtonEnabledWithReason(
                leaveButton,
                joined,
                null,
                FishToucherBundle.message("cultivation.sect.needJoin")
        );
        setButtonEnabledWithReason(
                promoteButton,
                manager.canPromoteSectRank(),
                null,
                joined ? FishToucherBundle.message("cultivation.sect.promoteBlocked") : FishToucherBundle.message("cultivation.sect.needJoin")
        );
        TaskOption task = (TaskOption) taskComboBox.getSelectedItem();
        String taskBlockReason = getTaskBlockReason(manager, settings, joined, task);
        setButtonEnabledWithReason(startTaskButton, taskBlockReason.isEmpty(), null, taskBlockReason);
        boolean taskReady = manager.isSectTaskReady();
        setButtonEnabledWithReason(
                claimTaskButton,
                taskReady,
                null,
                manager.getActiveSectTask() == null
                        ? FishToucherBundle.message("cultivation.sect.taskNone")
                        : FishToucherBundle.message("cultivation.sect.taskNotReady", manager.getSectTaskRemainingText())
        );
        SecretRealmOption secretRealm = (SecretRealmOption) secretRealmComboBox.getSelectedItem();
        String secretRealmBlockReason = getSecretRealmBlockReason(manager, settings, joined, secretRealm);
        setButtonEnabledWithReason(startSecretRealmButton, secretRealmBlockReason.isEmpty(), null, secretRealmBlockReason);
        InheritanceOption inheritance = (InheritanceOption) inheritanceComboBox.getSelectedItem();
        String inheritanceBlockReason = getInheritanceBlockReason(manager, settings, joined, inheritance);
        setButtonEnabledWithReason(purchaseButton, inheritanceBlockReason.isEmpty(), null, inheritanceBlockReason);
        TrialOption trial = (TrialOption) trialComboBox.getSelectedItem();
        String trialBlockReason = getTrialBlockReason(manager, settings, joined, trial);
        setButtonEnabledWithReason(startTrialButton, trialBlockReason.isEmpty(), null, trialBlockReason);
    }

    private String getJoinBlockReason(IdleCultivationManager manager, boolean joined) {
        if (!manager.isSectUnlocked()) {
            return FishToucherBundle.message("cultivation.sect.locked", manager.getRealmName(SectCatalog.UNLOCK_REALM_INDEX));
        }
        return joined
                ? FishToucherBundle.message("cultivation.sect.alreadyJoined")
                : "";
    }

    private String getTaskBlockReason(IdleCultivationManager manager,
                                      NovelReaderSettings settings,
                                      boolean joined,
                                      TaskOption task) {
        if (!joined) {
            return FishToucherBundle.message("cultivation.sect.needJoin");
        }
        if (task == null) {
            return FishToucherBundle.message("cultivation.sect.taskUnknown");
        }
        if (manager.hasActiveSectTask()) {
            return FishToucherBundle.message("cultivation.sect.taskBusy");
        }
        if (manager.hasActiveSectSecretRealm()) {
            return FishToucherBundle.message("cultivation.sect.secretRealmBusy");
        }
        if (!SectRules.isTaskUnlocked(settings, task.task)) {
            return FishToucherBundle.message("cultivation.sect.taskLocked");
        }
        return "";
    }

    private String getSecretRealmBlockReason(IdleCultivationManager manager,
                                             NovelReaderSettings settings,
                                             boolean joined,
                                             SecretRealmOption secretRealm) {
        if (!joined) {
            return FishToucherBundle.message("cultivation.sect.needJoin");
        }
        if (secretRealm == null) {
            return FishToucherBundle.message("cultivation.sect.secretRealmUnknown");
        }
        if (settings.getCurrentSectRankIndex() < secretRealm.secretRealm.minRankIndex()) {
            return FishToucherBundle.message("cultivation.sect.secretRealmLocked", SectCatalog.rank(secretRealm.secretRealm.minRankIndex()).name());
        }
        if (manager.hasActiveSectSecretRealm()) {
            return FishToucherBundle.message("cultivation.sect.secretRealmBusy");
        }
        if (settings.getSectSecretRealmCooldownUntilMillis() > System.currentTimeMillis()) {
            return FishToucherBundle.message("cultivation.sect.secretRealmCooldown", manager.getSectSecretRealmCooldownText());
        }
        if (!manager.canStartSectSecretRealm(secretRealm.secretRealm.id())) {
            return FishToucherBundle.message("cultivation.sect.secretRealmActivityBusy");
        }
        return "";
    }

    private String getInheritanceBlockReason(IdleCultivationManager manager,
                                             NovelReaderSettings settings,
                                             boolean joined,
                                             InheritanceOption inheritance) {
        if (!joined) {
            return FishToucherBundle.message("cultivation.sect.needJoin");
        }
        if (inheritance == null) {
            return FishToucherBundle.message("cultivation.sect.inheritanceUnknown");
        }
        if (!SectRules.isInheritanceUnlocked(settings, inheritance.inheritance)) {
            return FishToucherBundle.message("cultivation.sect.inheritanceLocked");
        }
        if (settings.isSectInheritanceLearned(inheritance.inheritance.id())) {
            return FishToucherBundle.message("cultivation.sect.inheritanceLearned", inheritance.inheritance.name());
        }
        if (settings.getCurrentSectContribution() < inheritance.inheritance.contributionCost()) {
            return FishToucherBundle.message("cultivation.sect.contributionNotEnough", inheritance.inheritance.contributionCost());
        }
        if (!manager.canPurchaseSectInheritance(inheritance.inheritance)) {
            return FishToucherBundle.message("cultivation.sect.inheritanceLocked");
        }
        return "";
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
            boolean enabled = manager.canResolveSectEvent(event.instanceId(), option.id());
            setButtonEnabledWithReason(
                    button,
                    enabled,
                    option.description(),
                    FishToucherBundle.message("cultivation.sect.eventOptionBlocked")
            );
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
        updateTrialResultText(trial);
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

    private void updateTrialResultText(TrialOption trial) {
        if (trial == null || !NovelReaderSettings.getInstance().isSectTrialDefeated(trial.trial.id())) {
            setWrappingText(trialResultText, "");
            trialResultText.setVisible(false);
            return;
        }
        SectCatalog.SectTrialDefinition definition = trial.trial;
        long contribution = 120L * definition.floor();
        long prestige = 40L * definition.floor();
        setWrappingText(trialResultText, FishToucherBundle.message(
                "cultivation.sect.trialResult",
                definition.stoneReward(),
                contribution,
                prestige
        ));
        trialResultText.setVisible(true);
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

    private record OwnSectBuildingComponents(JTextArea titleText,
                                             JTextArea effectText,
                                             JTextArea costText,
                                             JTextArea assignedText,
                                             JButton upgradeButton,
                                             JButton claimButton,
                                             JComboBox<DiscipleOption> discipleComboBox,
                                             JButton assignButton) {
    }

    private record OwnSectDiscipleRowComponents(JTextArea text, JButton unassignButton) {
    }

    private record DiscipleOption(NovelReaderSettings.OwnSectDiscipleState disciple) {
        static DiscipleOption empty() {
            return new DiscipleOption(null);
        }

        @Override
        public String toString() {
            return disciple == null
                    ? FishToucherBundle.message("cultivation.ownSect.noAssignableDisciple")
                    : formatOwnSectDisciple(disciple);
        }
    }
}
