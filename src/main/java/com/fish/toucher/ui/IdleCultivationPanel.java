package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class IdleCultivationPanel extends JPanel implements Disposable {

    private static final Logger LOG = Logger.getInstance(IdleCultivationPanel.class);

    private final JLabel realmValue;
    private final JLabel techniqueValue;
    private final JLabel qiValue;
    private final JLabel stonesValue;
    private final JLabel rateValue;
    private final JLabel chanceValue;
    private final JLabel effectsValue;
    private final JLabel rebirthLabel;
    private final JLabel rebirthValue;
    private final JLabel messageLabel;
    private final JProgressBar progressBar;
    private final JButton meditateButton;
    private final JButton breakthroughButton;
    private final JButton rebirthButton;

    private final JComboBox<TechniqueOption> techniqueComboBox;
    private final JButton equipTechniqueButton;
    private final JLabel techniqueDescriptionLabel;
    private final JComboBox<PillOption> pillComboBox;
    private final JButton usePillButton;
    private final JLabel pillDescriptionLabel;

    private final JComboBox<TravelOption> travelComboBox;
    private final JLabel travelDescriptionLabel;
    private final JLabel activeTravelLabel;
    private final JProgressBar travelProgressBar;
    private final JButton startTravelButton;
    private final JButton claimTravelButton;

    private final JLabel abodeStonesValue;
    private final JPanel abodeFacilitiesPanel;

    private final Runnable changeListener;
    private boolean refreshing;

    public IdleCultivationPanel() {
        LOG.info("IdleCultivationPanel: initializing");
        setLayout(new BorderLayout());

        add(createTopBar(), BorderLayout.NORTH);

        realmValue = new JLabel();
        techniqueValue = new JLabel();
        qiValue = new JLabel();
        stonesValue = new JLabel();
        rateValue = new JLabel();
        chanceValue = new JLabel();
        effectsValue = new JLabel();
        rebirthLabel = new JLabel(FishToucherBundle.message("cultivation.label.rebirth"));
        rebirthLabel.setForeground(JBColor.GRAY);
        rebirthValue = new JLabel();
        messageLabel = new JLabel();
        messageLabel.setForeground(JBColor.GRAY);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        meditateButton = new JButton(FishToucherBundle.message("cultivation.button.meditate"));
        breakthroughButton = new JButton(FishToucherBundle.message("cultivation.button.breakthrough"));
        rebirthButton = new JButton(FishToucherBundle.message("cultivation.button.rebirth"));

        techniqueComboBox = new JComboBox<>();
        equipTechniqueButton = new JButton(FishToucherBundle.message("cultivation.button.equipTechnique"));
        techniqueDescriptionLabel = createHintLabel();
        pillComboBox = new JComboBox<>();
        usePillButton = new JButton(FishToucherBundle.message("cultivation.button.usePill"));
        pillDescriptionLabel = createHintLabel();

        travelComboBox = new JComboBox<>();
        travelDescriptionLabel = createHintLabel();
        activeTravelLabel = new JLabel();
        travelProgressBar = new JProgressBar(0, 100);
        travelProgressBar.setStringPainted(true);
        startTravelButton = new JButton(FishToucherBundle.message("cultivation.button.startTravel"));
        claimTravelButton = new JButton(FishToucherBundle.message("cultivation.button.claimTravel"));
        abodeStonesValue = new JLabel();
        abodeFacilitiesPanel = new JPanel(new GridBagLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(FishToucherBundle.message("cultivation.tab.training"), createTrainingTab());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.bag"), createBagTab());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.travel"), createTravelTab());
        tabs.addTab(FishToucherBundle.message("cultivation.tab.abode"), createAbodeTab());
        add(tabs, BorderLayout.CENTER);

        changeListener = this::refreshContent;
        IdleCultivationManager manager = IdleCultivationManager.getInstance();
        manager.addChangeListener(changeListener);
        manager.start();
        refreshContent();
    }

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()));
        JComboBox<String> modeCombo = PluginModeSelector.createCombo(NovelReaderSettings.MODE_CULTIVATION);
        modeCombo.addActionListener(e -> {
            String selectedMode = PluginModeSelector.getSelectedMode(modeCombo);
            if (!NovelReaderSettings.MODE_CULTIVATION.equals(selectedMode)) {
                NovelReaderToolWindowFactory.switchMode(selectedMode);
            }
        });
        topBar.add(modeCombo);
        JComboBox<String> languageCombo = LanguageSelector.createCombo(NovelReaderSettings.getInstance().getUiLanguage());
        languageCombo.addActionListener(e -> {
            String selectedLanguage = LanguageSelector.getSelectedLanguage(languageCombo);
            if (!selectedLanguage.equals(NovelReaderSettings.getInstance().getUiLanguage())) {
                NovelReaderToolWindowFactory.switchLanguage(selectedLanguage);
            }
        });
        topBar.add(languageCombo);
        return topBar;
    }

    private JPanel createTrainingTab() {
        JPanel panel = createFormPanel();
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.realm"), realmValue);
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.technique"), techniqueValue);
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.cultivation"), qiValue);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weightx = 1.0;
        panel.add(progressBar, gbc);
        row++;

        gbc.gridwidth = 1; gbc.weightx = 0;
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.spiritStones"), stonesValue);
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.rate"), rateValue);
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.chance"), chanceValue);
        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.effects"), effectsValue);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(rebirthLabel, gbc);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1.0;
        panel.add(rebirthValue, gbc);
        gbc.weightx = 0;
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        panel.add(messageLabel, gbc);
        row++;

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        meditateButton.setToolTipText(FishToucherBundle.message("cultivation.tooltip.meditate"));
        meditateButton.setFocusable(false);
        meditateButton.addActionListener(e -> IdleCultivationManager.getInstance().meditateOnce());
        actions.add(meditateButton);

        breakthroughButton.setToolTipText(FishToucherBundle.message("cultivation.tooltip.breakthrough"));
        breakthroughButton.setFocusable(false);
        breakthroughButton.addActionListener(e -> IdleCultivationManager.getInstance().tryBreakthrough());
        actions.add(breakthroughButton);

        rebirthButton.setFocusable(false);
        rebirthButton.addActionListener(e -> showRebirthDialog());
        actions.add(rebirthButton);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        panel.add(actions, gbc);
        addBottomGlue(panel, gbc, row + 1);
        return panel;
    }

    private JPanel createBagTab() {
        JPanel panel = createFormPanel();
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        JLabel techniqueTitle = createSectionLabel(FishToucherBundle.message("cultivation.section.techniques"));
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(techniqueTitle, gbc);

        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.technique"), techniqueComboBox);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(techniqueDescriptionLabel, gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        equipTechniqueButton.setFocusable(false);
        equipTechniqueButton.addActionListener(e -> {
            TechniqueOption option = (TechniqueOption) techniqueComboBox.getSelectedItem();
            if (option != null) {
                IdleCultivationManager.getInstance().equipTechnique(option.technique.id());
            }
        });
        panel.add(equipTechniqueButton, gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(new JSeparator(), gbc);

        JLabel pillTitle = createSectionLabel(FishToucherBundle.message("cultivation.section.pills"));
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(pillTitle, gbc);

        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.pill"), pillComboBox);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(pillDescriptionLabel, gbc);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        usePillButton.setFocusable(false);
        usePillButton.addActionListener(e -> {
            PillOption option = (PillOption) pillComboBox.getSelectedItem();
            if (option != null) {
                IdleCultivationManager.getInstance().usePill(option.pill.id());
            }
        });
        panel.add(usePillButton, gbc);

        techniqueComboBox.addActionListener(e -> updateTechniqueDescription());
        pillComboBox.addActionListener(e -> updatePillDescription());
        addBottomGlue(panel, gbc, row + 1);
        return panel;
    }

    private JPanel createTravelTab() {
        JPanel panel = createFormPanel();
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.location"), travelComboBox);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(travelDescriptionLabel, gbc);

        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.activeTravel"), activeTravelLabel);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0;
        panel.add(travelProgressBar, gbc);
        gbc.weightx = 0;

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
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

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        panel.add(actions, gbc);

        travelComboBox.addActionListener(e -> updateTravelDescription());
        addBottomGlue(panel, gbc, row + 1);
        return panel;
    }

    private JPanel createAbodeTab() {
        JPanel contentPanel = createFormPanel();
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        addLabelRow(contentPanel, gbc, row++, FishToucherBundle.message("cultivation.label.spiritStones"), abodeStonesValue);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0;
        contentPanel.add(new JSeparator(), gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0;
        contentPanel.add(abodeFacilitiesPanel, gbc);

        addBottomGlue(contentPanel, gbc, row);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        return panel;
    }

    private GridBagConstraints createConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 4, 5, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    private JLabel createHintLabel() {
        JLabel label = new JLabel();
        label.setForeground(JBColor.GRAY);
        return label;
    }

    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        return label;
    }

    private void addLabelRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent value) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0;
        JLabel labelComponent = new JLabel(label);
        labelComponent.setForeground(JBColor.GRAY);
        panel.add(labelComponent, gbc);

        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1.0;
        panel.add(value, gbc);
        gbc.weightx = 0;
    }

    private void addBottomGlue(JPanel panel, GridBagConstraints gbc, int row) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);
        gbc.weighty = 0;
    }

    private void refreshContent() {
        refreshing = true;
        try {
            IdleCultivationManager manager = IdleCultivationManager.getInstance();
            NovelReaderSettings settings = NovelReaderSettings.getInstance();
            long currentQi = manager.getCurrentQi();
            long requiredQi = manager.getRequiredQi();
            int percent = manager.getProgressPercent();

            realmValue.setText(manager.getRealmName());
            techniqueValue.setText(manager.getEquippedTechnique().name());
            qiValue.setText(requiredQi > 0L ? currentQi + " / " + requiredQi : String.valueOf(currentQi));
            stonesValue.setText(String.valueOf(manager.getSpiritStones()));
            rateValue.setText(manager.getRateText());
            chanceValue.setText(manager.getChanceText());
            effectsValue.setText(manager.getActiveEffectsText());
            messageLabel.setText(manager.getLastMessage());

            progressBar.setValue(percent);
            progressBar.setString(requiredQi > 0L ? percent + "%" : FishToucherBundle.message("cultivation.status.max"));
            updateMeditationButton(manager);
            updateRebirthControls(manager);
            breakthroughButton.setEnabled(manager.canBreakthrough());

            reloadTechniqueOptions(manager, settings);
            reloadPillOptions(manager, settings);
            reloadTravelOptions(manager);
            updateActiveTravel(manager);
            reloadAbodeFacilities(manager);
        } finally {
            refreshing = false;
        }
        updateTechniqueDescription();
        updatePillDescription();
        updateTravelDescription();
    }

    private void updateMeditationButton(IdleCultivationManager manager) {
        boolean canMeditate = manager.canMeditate();
        String baseText = FishToucherBundle.message("cultivation.button.meditate");
        meditateButton.setEnabled(canMeditate);
        meditateButton.setText(canMeditate ? baseText : baseText + " (" + manager.getMeditationRemainingText() + ")");
        meditateButton.setToolTipText(canMeditate
                ? FishToucherBundle.message("cultivation.tooltip.meditate")
                : FishToucherBundle.message("cultivation.tooltip.meditateCooldown", manager.getMeditationRemainingText()));
    }

    private void updateRebirthControls(IdleCultivationManager manager) {
        boolean hasRebirth = manager.getRebirthCount() > 0;
        rebirthLabel.setVisible(hasRebirth);
        rebirthValue.setVisible(hasRebirth);
        if (hasRebirth) {
            rebirthValue.setText(String.valueOf(manager.getRebirthCount()));
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
                this,
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

    private void reloadTechniqueOptions(IdleCultivationManager manager, NovelReaderSettings settings) {
        String selectedId = settings.getEquippedTechniqueId();
        techniqueComboBox.removeAllItems();
        for (IdleCultivationManager.TechniqueDefinition technique : manager.getTechniqueDefinitions()) {
            boolean unlocked = settings.isTechniqueUnlocked(technique.id());
            TechniqueOption option = new TechniqueOption(technique, unlocked);
            techniqueComboBox.addItem(option);
            if (technique.id().equals(selectedId)) {
                techniqueComboBox.setSelectedItem(option);
            }
        }
    }

    private void reloadPillOptions(IdleCultivationManager manager, NovelReaderSettings settings) {
        String selectedId = null;
        PillOption selected = (PillOption) pillComboBox.getSelectedItem();
        if (selected != null) {
            selectedId = selected.pill.id();
        }
        pillComboBox.removeAllItems();
        for (IdleCultivationManager.PillDefinition pill : manager.getPillDefinitions()) {
            PillOption option = new PillOption(pill, settings.getPillCount(pill.id()));
            pillComboBox.addItem(option);
            if (pill.id().equals(selectedId)) {
                pillComboBox.setSelectedItem(option);
            }
        }
    }

    private void reloadTravelOptions(IdleCultivationManager manager) {
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

    private void reloadAbodeFacilities(IdleCultivationManager manager) {
        abodeStonesValue.setText(String.valueOf(manager.getSpiritStones()));
        abodeFacilitiesPanel.removeAll();
        GridBagConstraints gbc = createConstraints();
        int row = 0;
        for (IdleCultivationManager.AbodeFacilityDefinition facility : manager.getAbodeFacilityDefinitions()) {
            JLabel title = createSectionLabel(facility.name() + "  " + manager.getAbodeFacilityLevelText(facility.id()));
            gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0;
            abodeFacilitiesPanel.add(title, gbc);

            JLabel description = createHintLabel();
            description.setText(facility.description());
            gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
            abodeFacilitiesPanel.add(description, gbc);

            addLabelRow(abodeFacilitiesPanel, gbc, row++, FishToucherBundle.message("cultivation.label.effect"), new JLabel(manager.getAbodeFacilityEffectText(facility.id())));
            addLabelRow(abodeFacilitiesPanel, gbc, row++, FishToucherBundle.message("cultivation.label.upgradeCost"), new JLabel(manager.getAbodeUpgradeCostText(facility.id())));

            boolean productionFacility = manager.isAbodeProductionFacility(facility.id());
            if (productionFacility) {
                addLabelRow(abodeFacilitiesPanel, gbc, row++, FishToucherBundle.message("cultivation.label.claimable"), new JLabel(manager.getAbodeClaimableText(facility.id())));
            }

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            JButton upgradeButton = new JButton(FishToucherBundle.message("cultivation.button.upgradeFacility"));
            upgradeButton.setFocusable(false);
            upgradeButton.setEnabled(manager.canUpgradeAbodeFacility(facility.id()));
            upgradeButton.addActionListener(e -> IdleCultivationManager.getInstance().upgradeAbodeFacility(facility.id()));
            actions.add(upgradeButton);

            if (productionFacility) {
                JButton claimButton = new JButton(FishToucherBundle.message("cultivation.button.claimAbode"));
                claimButton.setFocusable(false);
                claimButton.setEnabled(manager.canClaimAbodeFacility(facility.id()));
                claimButton.addActionListener(e -> IdleCultivationManager.getInstance().claimAbodeFacility(facility.id()));
                actions.add(claimButton);
            }

            gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
            abodeFacilitiesPanel.add(actions, gbc);

            gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0;
            abodeFacilitiesPanel.add(new JSeparator(), gbc);
        }
        abodeFacilitiesPanel.revalidate();
        abodeFacilitiesPanel.repaint();
    }

    private void updateTechniqueDescription() {
        TechniqueOption option = (TechniqueOption) techniqueComboBox.getSelectedItem();
        if (option == null) return;
        IdleCultivationManager.TechniqueDefinition technique = option.technique;
        techniqueDescriptionLabel.setText(technique.description());
        equipTechniqueButton.setEnabled(option.unlocked && !technique.id().equals(NovelReaderSettings.getInstance().getEquippedTechniqueId()));
    }

    private void updatePillDescription() {
        PillOption option = (PillOption) pillComboBox.getSelectedItem();
        if (option == null) return;
        pillDescriptionLabel.setText(option.pill.description());
        usePillButton.setEnabled(option.count > 0);
    }

    private void updateTravelDescription() {
        if (refreshing) return;
        TravelOption option = (TravelOption) travelComboBox.getSelectedItem();
        if (option == null) return;
        IdleCultivationManager manager = IdleCultivationManager.getInstance();
        String suffix = option.unlocked
                ? FishToucherBundle.message("cultivation.travel.duration", option.location.durationMinutes())
                : FishToucherBundle.message("cultivation.travel.locked", manager.getRealmName(option.location.minRealmIndex()));
        travelDescriptionLabel.setText(option.location.description() + "  " + suffix);
        startTravelButton.setEnabled(option.unlocked && !manager.hasActiveTravel());
    }

    private void updateActiveTravel(IdleCultivationManager manager) {
        IdleCultivationManager.TravelLocationDefinition active = manager.getActiveTravelLocation();
        if (active == null) {
            activeTravelLabel.setText(FishToucherBundle.message("cultivation.travel.none"));
            travelProgressBar.setValue(0);
            travelProgressBar.setString(FishToucherBundle.message("cultivation.travel.none"));
            startTravelButton.setEnabled(true);
            claimTravelButton.setEnabled(false);
            return;
        }
        activeTravelLabel.setText(active.name() + " | " + manager.getTravelRemainingText());
        int percent = manager.getTravelProgressPercent();
        travelProgressBar.setValue(percent);
        travelProgressBar.setString(manager.isTravelReady() ? FishToucherBundle.message("cultivation.status.travelClaimReady") : percent + "%");
        startTravelButton.setEnabled(false);
        claimTravelButton.setEnabled(manager.isTravelReady());
    }

    @Override
    public void dispose() {
        IdleCultivationManager.getInstance().removeChangeListener(changeListener);
    }

    private record TechniqueOption(IdleCultivationManager.TechniqueDefinition technique, boolean unlocked) {
        @Override
        public String toString() {
            String name = technique.name();
            return unlocked ? name : name + " (" + FishToucherBundle.message("cultivation.status.locked") + ")";
        }
    }

    private record PillOption(IdleCultivationManager.PillDefinition pill, int count) {
        @Override
        public String toString() {
            return pill.name() + " x" + count;
        }
    }

    private record TravelOption(IdleCultivationManager.TravelLocationDefinition location, boolean unlocked) {
        @Override
        public String toString() {
            String name = location.name();
            return unlocked ? name : name + " (" + FishToucherBundle.message("cultivation.status.locked") + ")";
        }
    }

    private record RebirthTechniqueOption(IdleCultivationManager.TechniqueDefinition technique) {
        @Override
        public String toString() {
            return technique.name();
        }
    }
}
