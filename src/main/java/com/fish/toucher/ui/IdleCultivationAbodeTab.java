package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;

import javax.swing.*;
import java.awt.*;

import static com.fish.toucher.ui.IdleCultivationUiSupport.*;

final class IdleCultivationAbodeTab {

    private final JComponent component;
    private final JPanel contentPanel;
    private final JLabel stonesValue;

    IdleCultivationAbodeTab() {
        contentPanel = createFormPanel();
        stonesValue = new JLabel();
        component = createScrollableTab(contentPanel);
    }

    JComponent getComponent() {
        return component;
    }

    void reloadAbodeFacilities(IdleCultivationManager manager) {
        setLabelTextIfChanged(stonesValue, String.valueOf(manager.getSpiritStones()));
        contentPanel.removeAll();
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        addLabelRow(contentPanel, gbc, row++, FishToucherBundle.message("cultivation.label.spiritStones"), stonesValue);
        row = addSeparatorRow(contentPanel, gbc, row);
        if (!manager.isAbodeUnlocked()) {
            row = addFullWidthRow(contentPanel, gbc, row, createSectionTextArea(manager.getAbodeLockedText()));
            addBottomGlue(contentPanel, gbc, row);
            refreshContent();
            return;
        }

        for (IdleCultivationManager.AbodeFacilityDefinition facility : manager.getAbodeFacilityDefinitions()) {
            JTextArea title = createSectionTextArea(facility.name() + "  " + manager.getAbodeFacilityLevelText(facility.id()));
            row = addFullWidthRow(contentPanel, gbc, row, title);

            JTextArea description = createHintTextArea(facility.description());
            row = addFullWidthRow(contentPanel, gbc, row, description);

            addLabelRow(contentPanel, gbc, row++, FishToucherBundle.message("cultivation.label.effect"), createHintTextArea(manager.getAbodeFacilityEffectText(facility.id())));
            addLabelRow(contentPanel, gbc, row++, FishToucherBundle.message("cultivation.label.upgradeCost"), createHintTextArea(manager.getAbodeUpgradeCostText(facility.id())));

            boolean productionFacility = manager.isAbodeProductionFacility(facility.id());
            if (productionFacility) {
                addLabelRow(contentPanel, gbc, row++, FishToucherBundle.message("cultivation.label.claimable"), createHintTextArea(manager.getAbodeClaimableText(facility.id())));
            }

            JPanel actions = createActionPanel();
            JButton upgradeButton = new JButton(FishToucherBundle.message("cultivation.button.upgradeFacility"));
            upgradeButton.setFocusable(false);
            boolean canUpgrade = manager.canUpgradeAbodeFacility(facility.id());
            setButtonEnabledWithReason(
                    upgradeButton,
                    canUpgrade,
                    null,
                    getAbodeUpgradeBlockReason(manager, facility.id())
            );
            upgradeButton.addActionListener(e -> IdleCultivationManager.getInstance().upgradeAbodeFacility(facility.id()));
            actions.add(upgradeButton);

            if (productionFacility) {
                JButton claimButton = new JButton(FishToucherBundle.message("cultivation.button.claimAbode"));
                claimButton.setFocusable(false);
                boolean canClaim = manager.canClaimAbodeFacility(facility.id());
                setButtonEnabledWithReason(
                        claimButton,
                        canClaim,
                        null,
                        getAbodeClaimBlockReason(manager)
                );
                claimButton.addActionListener(e -> IdleCultivationManager.getInstance().claimAbodeFacility(facility.id()));
                actions.add(claimButton);
            }

            row = addActionRow(contentPanel, gbc, row, actions);

            row = addSeparatorRow(contentPanel, gbc, row);
        }
        addBottomGlue(contentPanel, gbc, row);
        refreshContent();
    }

    private void refreshContent() {
        contentPanel.revalidate();
        contentPanel.repaint();
        Container parent = contentPanel.getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    private String getAbodeUpgradeBlockReason(IdleCultivationManager manager, String facilityId) {
        if (!manager.isAbodeUnlocked()) {
            return manager.getAbodeLockedText();
        }
        if (FishToucherBundle.message("cultivation.abode.costMax").equals(manager.getAbodeUpgradeCostText(facilityId))) {
            return FishToucherBundle.message("cultivation.status.facilityMaxLevel");
        }
        return FishToucherBundle.message("cultivation.status.insufficientStones", manager.getAbodeUpgradeCost(facilityId));
    }

    private String getAbodeClaimBlockReason(IdleCultivationManager manager) {
        return manager.isAbodeUnlocked()
                ? FishToucherBundle.message("cultivation.status.nothingToClaim")
                : manager.getAbodeLockedText();
    }
}
