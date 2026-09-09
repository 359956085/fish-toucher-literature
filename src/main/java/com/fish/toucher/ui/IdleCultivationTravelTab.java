package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;

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
        java.util.List<TravelOption> options = new java.util.ArrayList<>();
        for (var location : manager.getTravelLocationDefinitions()) {
            options.add(new TravelOption(location, manager.isTravelUnlocked(location)));
        }
        StableComboOptions.update(travelComboBox, options, option -> option.location.id(),
                NovelReaderSettings.getInstance().getSelectedTravelLocationId());
    }

    void updateActiveTravel(IdleCultivationManager manager) {
        IdleCultivationManager.TravelLocationDefinition active = manager.getActiveTravelLocation();
        if (active == null) {
            setWrappingText(activeTravelLabel, FishToucherBundle.message("cultivation.travel.none"));
            setProgressTextIfChanged(travelProgressBar, 0, FishToucherBundle.message("cultivation.travel.none"));
            TravelOption option = (TravelOption) travelComboBox.getSelectedItem();
            updateStartTravelButton(manager, option);
            setButtonEnabledWithReason(
                    claimTravelButton,
                    false,
                    null,
                    FishToucherBundle.message("cultivation.status.travelNone")
            );
            return;
        }
        setWrappingText(activeTravelLabel, active.name() + " | " + manager.getTravelRemainingText());
        int percent = manager.getTravelProgressPercent();
        setProgressTextIfChanged(travelProgressBar, percent, manager.isTravelReady() ? FishToucherBundle.message("cultivation.status.travelClaimReady") : percent + "%");
        setButtonEnabledWithReason(
                startTravelButton,
                false,
                null,
                FishToucherBundle.message("cultivation.status.travelBusy")
        );
        boolean travelReady = manager.isTravelReady();
        setButtonEnabledWithReason(
                claimTravelButton,
                travelReady,
                null,
                FishToucherBundle.message("cultivation.status.travelNotReady", manager.getTravelRemainingText())
        );
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

        travelComboBox.addActionListener(e -> {
            if (!refreshing) {
                TravelOption option = (TravelOption) travelComboBox.getSelectedItem();
                if (option != null) {
                    NovelReaderSettings.getInstance().setSelectedTravelLocationId(option.location.id());
                }
            }
            updateTravelDescription();
        });
        addBottomGlue(panel, gbc, row);
        return createScrollableTab(panel);
    }

    private void updateTravelDescription() {
        if (refreshing) return;
        TravelOption option = (TravelOption) travelComboBox.getSelectedItem();
        if (option == null) return;
        IdleCultivationManager manager = IdleCultivationManager.getInstance();
        String blockReason = getTravelBlockReason(manager, option);
        String suffix = option.unlocked ? manager.getTravelDurationText(option.location) : blockReason;
        setWrappingText(travelDescriptionLabel, option.location.description() + "  " + suffix);
        updateStartTravelButton(manager, option);
    }

    private void updateStartTravelButton(IdleCultivationManager manager, TravelOption option) {
        String blockReason = getTravelBlockReason(manager, option);
        setButtonEnabledWithReason(startTravelButton, blockReason.isEmpty(), null, blockReason);
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
        if (manager.hasActiveSectSecretRealm()) {
            return FishToucherBundle.message("cultivation.sect.secretRealmBusy");
        }
        return "";
    }

    record TravelOption(IdleCultivationManager.TravelLocationDefinition location, boolean unlocked) {
        @Override
        public String toString() {
            String name = location.name();
            return unlocked ? name : name + " (" + FishToucherBundle.message("cultivation.status.locked") + ")";
        }
    }
}
