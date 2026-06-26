package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.fish.toucher.ui.IdleCultivationUiSupport.*;

final class IdleCultivationBagTab {

    private final JComponent component;
    private final JComboBox<TechniqueOption> techniqueComboBox;
    private final JButton equipTechniqueButton;
    private final JTextArea techniqueDescriptionLabel;
    private final JComboBox<PillOption> pillComboBox;
    private final JButton usePillButton;
    private final JTextArea pillDescriptionLabel;
    private final List<JComboBox<SpellOption>> spellSlotComboBoxes;
    private final List<JTextArea> spellDescriptionLabels;
    private final JButton saveSpellSetupButton;
    private final List<JComboBox<ArtifactOption>> artifactSlotComboBoxes;
    private final JButton saveArtifactSetupButton;
    private final JTextArea artifactDescriptionLabel;
    private boolean refreshing;
    private boolean adjustingArtifactSelection;

    IdleCultivationBagTab() {
        techniqueComboBox = new JComboBox<>();
        equipTechniqueButton = new JButton(FishToucherBundle.message("cultivation.button.equipTechnique"));
        techniqueDescriptionLabel = createHintTextArea();
        pillComboBox = new JComboBox<>();
        usePillButton = new JButton(FishToucherBundle.message("cultivation.button.usePill"));
        pillDescriptionLabel = createHintTextArea();
        spellSlotComboBoxes = new ArrayList<>();
        spellDescriptionLabels = new ArrayList<>();
        saveSpellSetupButton = new JButton(FishToucherBundle.message("cultivation.button.saveSpellSetup"));
        artifactSlotComboBoxes = new ArrayList<>();
        saveArtifactSetupButton = new JButton(FishToucherBundle.message("cultivation.button.saveArtifactSetup"));
        artifactDescriptionLabel = createHintTextArea();
        component = createContent();
    }

    JComponent getComponent() {
        return component;
    }

    void setRefreshing(boolean refreshing) {
        this.refreshing = refreshing;
    }

    void reloadBagOptions(IdleCultivationManager manager, NovelReaderSettings settings) {
        reloadTechniqueOptions(manager, settings);
        reloadPillOptions(manager, settings);
        reloadSpellOptions(manager, settings);
        reloadArtifactOptions(manager, settings);
    }

    void updateSelectionDescriptions() {
        updateTechniqueDescription();
        updatePillDescription();
        updateSpellDescription();
        updateArtifactDescription();
    }

    private JComponent createContent() {
        JPanel panel = createFormPanel();
        GridBagConstraints gbc = createConstraints();
        int row = 0;

        JLabel techniqueTitle = createSectionLabel(FishToucherBundle.message("cultivation.section.techniques"));
        row = addFullWidthRow(panel, gbc, row, techniqueTitle);

        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.technique"), techniqueComboBox);
        row = addFullWidthRow(panel, gbc, row, techniqueDescriptionLabel);

        equipTechniqueButton.setFocusable(false);
        equipTechniqueButton.addActionListener(e -> {
            TechniqueOption option = (TechniqueOption) techniqueComboBox.getSelectedItem();
            if (option != null) {
                IdleCultivationManager.getInstance().equipTechnique(option.technique.id());
            }
        });
        row = addActionRow(panel, gbc, row, equipTechniqueButton);

        row = addSeparatorRow(panel, gbc, row);

        JLabel pillTitle = createSectionLabel(FishToucherBundle.message("cultivation.section.pills"));
        row = addFullWidthRow(panel, gbc, row, pillTitle);

        addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.pill"), pillComboBox);
        row = addFullWidthRow(panel, gbc, row, pillDescriptionLabel);

        usePillButton.setFocusable(false);
        usePillButton.addActionListener(e -> {
            PillOption option = (PillOption) pillComboBox.getSelectedItem();
            if (option != null) {
                IdleCultivationManager.getInstance().usePill(option.pill.id());
            }
        });
        row = addActionRow(panel, gbc, row, usePillButton);

        row = addSeparatorRow(panel, gbc, row);

        JLabel spellTitle = createSectionLabel(FishToucherBundle.message("cultivation.section.spells"));
        row = addFullWidthRow(panel, gbc, row, spellTitle);

        for (int i = 0; i < 3; i++) {
            JComboBox<SpellOption> spellComboBox = new JComboBox<>();
            spellComboBox.addActionListener(e -> updateSpellDescription());
            spellSlotComboBoxes.add(spellComboBox);
            addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.spellSlot", i + 1), spellComboBox);

            JTextArea spellDescription = createGuideTextArea("");
            spellDescriptionLabels.add(spellDescription);
            row = addFullWidthRow(panel, gbc, row, spellDescription);
        }

        saveSpellSetupButton.setFocusable(false);
        saveSpellSetupButton.addActionListener(e -> saveSpellSetup());
        row = addActionRow(panel, gbc, row, saveSpellSetupButton);

        row = addSeparatorRow(panel, gbc, row);

        JLabel artifactTitle = createSectionLabel(FishToucherBundle.message("cultivation.section.artifacts"));
        row = addFullWidthRow(panel, gbc, row, artifactTitle);

        for (int i = 0; i < 2; i++) {
            JComboBox<ArtifactOption> artifactComboBox = new JComboBox<>();
            artifactComboBox.addActionListener(e -> updateArtifactDescription());
            artifactSlotComboBoxes.add(artifactComboBox);
            addLabelRow(panel, gbc, row++, FishToucherBundle.message("cultivation.label.artifactSlot", i + 1), artifactComboBox);
        }
        row = addFullWidthRow(panel, gbc, row, artifactDescriptionLabel);

        saveArtifactSetupButton.setFocusable(false);
        saveArtifactSetupButton.addActionListener(e -> saveArtifactSetup());
        row = addActionRow(panel, gbc, row, saveArtifactSetupButton);

        techniqueComboBox.addActionListener(e -> updateTechniqueDescription());
        pillComboBox.addActionListener(e -> updatePillDescription());
        addBottomGlue(panel, gbc, row);
        return createScrollableTab(panel);
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

    private void reloadSpellOptions(IdleCultivationManager manager, NovelReaderSettings settings) {
        List<String> equippedSpellIds = settings.getEquippedSpellIds();
        for (int i = 0; i < spellSlotComboBoxes.size(); i++) {
            JComboBox<SpellOption> comboBox = spellSlotComboBoxes.get(i);
            String selectedId = i < equippedSpellIds.size() ? equippedSpellIds.get(i) : "";
            comboBox.removeAllItems();
            comboBox.addItem(SpellOption.none());
            for (IdleCultivationManager.SpellDefinition spell : manager.getSpellDefinitions()) {
                boolean unlocked = settings.isSpellUnlocked(spell.id());
                SpellOption option = new SpellOption(spell, false, unlocked);
                comboBox.addItem(option);
                if (spell.id().equals(selectedId)) {
                    comboBox.setSelectedItem(option);
                }
            }
        }
    }

    private void reloadArtifactOptions(IdleCultivationManager manager, NovelReaderSettings settings) {
        List<String> equippedArtifactIds = settings.getEquippedArtifactIds();
        for (int i = 0; i < artifactSlotComboBoxes.size(); i++) {
            JComboBox<ArtifactOption> comboBox = artifactSlotComboBoxes.get(i);
            String selectedId = i < equippedArtifactIds.size() ? equippedArtifactIds.get(i) : "";
            comboBox.removeAllItems();
            comboBox.addItem(ArtifactOption.none());
            for (IdleCultivationManager.ArtifactDefinition artifact : manager.getArtifactDefinitions()) {
                boolean unlocked = settings.isArtifactUnlocked(artifact.id());
                boolean equipped = equippedArtifactIds.contains(artifact.id());
                ArtifactOption option = new ArtifactOption(artifact, false, unlocked, equipped);
                comboBox.addItem(option);
                if (artifact.id().equals(selectedId)) {
                    comboBox.setSelectedItem(option);
                }
            }
        }
    }

    private void saveSpellSetup() {
        List<String> spellIds = new ArrayList<>();
        for (JComboBox<SpellOption> comboBox : spellSlotComboBoxes) {
            SpellOption option = (SpellOption) comboBox.getSelectedItem();
            if (option != null && !option.empty && option.unlocked && option.spell != null) {
                spellIds.add(option.spell.id());
            }
        }
        IdleCultivationManager.getInstance().equipSpells(spellIds);
    }

    private void saveArtifactSetup() {
        List<String> artifactIds = new ArrayList<>();
        Set<String> selectedArtifactIds = new HashSet<>();
        for (JComboBox<ArtifactOption> comboBox : artifactSlotComboBoxes) {
            ArtifactOption option = (ArtifactOption) comboBox.getSelectedItem();
            if (option != null
                    && !option.empty
                    && option.unlocked
                    && option.artifact != null
                    && selectedArtifactIds.add(option.artifact.id())) {
                artifactIds.add(option.artifact.id());
            }
        }
        IdleCultivationManager.getInstance().equipArtifacts(artifactIds);
    }

    private void updateTechniqueDescription() {
        TechniqueOption option = (TechniqueOption) techniqueComboBox.getSelectedItem();
        if (option == null) return;
        IdleCultivationManager.TechniqueDefinition technique = option.technique;
        setWrappingText(techniqueDescriptionLabel, technique.description());
        equipTechniqueButton.setEnabled(option.unlocked && !technique.id().equals(NovelReaderSettings.getInstance().getEquippedTechniqueId()));
    }

    private void updatePillDescription() {
        PillOption option = (PillOption) pillComboBox.getSelectedItem();
        if (option == null) return;
        setWrappingText(pillDescriptionLabel, option.pill.description());
        usePillButton.setEnabled(option.count > 0);
    }

    private void updateSpellDescription() {
        if (refreshing) return;
        for (int i = 0; i < spellSlotComboBoxes.size(); i++) {
            JComboBox<SpellOption> comboBox = spellSlotComboBoxes.get(i);
            JTextArea descriptionLabel = spellDescriptionLabels.get(i);
            SpellOption option = (SpellOption) comboBox.getSelectedItem();
            if (option != null && !option.empty && option.spell != null) {
                setWrappingText(descriptionLabel, option.spell.name() + " - " + option.spell.description());
            } else {
                setWrappingText(descriptionLabel, "");
            }
        }
    }

    private void updateArtifactDescription() {
        if (refreshing) return;
        normalizeArtifactSelection();
        List<String> names = new ArrayList<>();
        for (JComboBox<ArtifactOption> comboBox : artifactSlotComboBoxes) {
            ArtifactOption option = (ArtifactOption) comboBox.getSelectedItem();
            if (option != null && !option.empty && option.artifact != null) {
                names.add(option.artifact.name() + " - " + option.artifact.description());
            }
        }
        setWrappingText(
                artifactDescriptionLabel,
                names.isEmpty()
                        ? FishToucherBundle.message("cultivation.status.noArtifactEquipped")
                        : String.join("  |  ", names)
        );
    }

    private void normalizeArtifactSelection() {
        if (adjustingArtifactSelection) {
            return;
        }
        adjustingArtifactSelection = true;
        try {
            Set<String> selectedArtifactIds = new HashSet<>();
            for (JComboBox<ArtifactOption> comboBox : artifactSlotComboBoxes) {
                ArtifactOption option = (ArtifactOption) comboBox.getSelectedItem();
                if (option == null || option.empty || option.artifact == null) {
                    continue;
                }
                if (!selectedArtifactIds.add(option.artifact.id())) {
                    comboBox.setSelectedIndex(0);
                }
            }
        } finally {
            adjustingArtifactSelection = false;
        }
    }

    record TechniqueOption(IdleCultivationManager.TechniqueDefinition technique, boolean unlocked) {
        @Override
        public String toString() {
            String name = technique.name();
            return unlocked ? name : name + " (" + FishToucherBundle.message("cultivation.status.locked") + ")";
        }
    }

    record PillOption(IdleCultivationManager.PillDefinition pill, int count) {
        @Override
        public String toString() {
            return pill.name() + " x" + count;
        }
    }

    record SpellOption(IdleCultivationManager.SpellDefinition spell, boolean empty, boolean unlocked) {
        private static SpellOption none() {
            return new SpellOption(null, true, true);
        }

        @Override
        public String toString() {
            if (empty || spell == null) {
                return FishToucherBundle.message("cultivation.spell.none");
            }
            return unlocked
                    ? spell.name()
                    : spell.name() + " (" + FishToucherBundle.message("cultivation.status.locked") + ")";
        }
    }

    record ArtifactOption(IdleCultivationManager.ArtifactDefinition artifact,
                                  boolean empty,
                                  boolean unlocked,
                                  boolean equipped) {
        private static ArtifactOption none() {
            return new ArtifactOption(null, true, true, false);
        }

        @Override
        public String toString() {
            if (empty || artifact == null) {
                return FishToucherBundle.message("cultivation.artifact.none");
            }
            if (!unlocked) {
                return artifact.name() + " (" + FishToucherBundle.message("cultivation.status.locked") + ")";
            }
            return equipped
                    ? artifact.name() + " (" + FishToucherBundle.message("cultivation.status.equipped") + ")"
                    : artifact.name();
        }
    }
}
