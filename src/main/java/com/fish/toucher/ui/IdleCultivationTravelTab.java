package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static com.fish.toucher.ui.IdleCultivationUiSupport.*;

final class IdleCultivationTravelTab {

    private final JComponent component;
    private final JComboBox<TravelOption> travelComboBox;
    private final JTextArea travelDescriptionLabel;
    private final JTextArea activeTravelLabel;
    private final JProgressBar travelProgressBar;
    private final JButton startTravelButton;
    private final JButton claimTravelButton;
    private boolean refreshing;

    IdleCultivationTravelTab() {
        travelComboBox = new JComboBox<>();
        travelDescriptionLabel = createHintTextArea();
        activeTravelLabel = createHintTextArea();
        travelProgressBar = new JProgressBar(0, 100);
        travelProgressBar.setStringPainted(true);
        startTravelButton = new JButton(FishToucherBundle.message("cultivation.button.startTravel"));
        claimTravelButton = new JButton(FishToucherBundle.message("cultivation.button.claimTravel"));
        component = createContent();
    }

    JComponent getComponent() {
        return component;
    }

    void setRefreshing(boolean refreshing) {
        this.refreshing = refreshing;
    }

    void reloadTravelOptions(IdleCultivationManager manager) {
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

    void updateActiveTravel(IdleCultivationManager manager) {
        IdleCultivationManager.TravelLocationDefinition active = manager.getActiveTravelLocation();
        if (active == null) {
            setWrappingText(activeTravelLabel, FishToucherBundle.message("cultivation.travel.none"));
            setProgressTextIfChanged(travelProgressBar, 0, FishToucherBundle.message("cultivation.travel.none"));
            TravelOption option = (TravelOption) travelComboBox.getSelectedItem();
            updateStartTravelButton(manager, option);
            claimTravelButton.setEnabled(false);
            return;
        }
        setWrappingText(activeTravelLabel, active.name() + " | " + manager.getTravelRemainingText());
        int percent = manager.getTravelProgressPercent();
        setProgressTextIfChanged(travelProgressBar, percent, manager.isTravelReady() ? FishToucherBundle.message("cultivation.status.travelClaimReady") : percent + "%");
        startTravelButton.setEnabled(false);
        claimTravelButton.setEnabled(manager.isTravelReady());
    }

    void updateSelectionDescriptions() {
        updateTravelDescription();
    }

    private JComponent createContent() {
        JPanel panel = createFormPanel();
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.location"), travelComboBox);
        row = addFullWidthRow(panel, gbc, row, travelDescriptionLabel);

        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.activeTravel"), activeTravelLabel);

        row = addFullWidthRow(panel, gbc, row, travelProgressBar);

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

        row = addActionRow(panel, gbc, row, actions);

        travelComboBox.addActionListener(e -> updateTravelDescription());
        addBottomGlue(panel, gbc, row);
        return createScrollableTab(panel);
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

    private String appendBlockReason(String blockReason) {
        return blockReason.isEmpty() ? "" : "  " + blockReason;
    }

    record TravelOption(IdleCultivationManager.TravelLocationDefinition location, boolean unlocked) {
        @Override
        public String toString() {
            String name = location.name();
            return unlocked ? name : name + " (" + FishToucherBundle.message("cultivation.status.locked") + ")";
        }
    }
}
