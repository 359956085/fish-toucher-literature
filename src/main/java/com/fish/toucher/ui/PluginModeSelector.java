package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;

import javax.swing.*;

public final class PluginModeSelector {

    public static final String[] MODE_VALUES = {
            NovelReaderSettings.MODE_NOVEL,
            NovelReaderSettings.MODE_HOT_SEARCH,
            NovelReaderSettings.MODE_CULTIVATION
    };

    private PluginModeSelector() {}

    public static String[] getModeLabels() {
        return new String[]{
                FishToucherBundle.message("settings.mode.novel"),
                FishToucherBundle.message("settings.mode.hotSearch"),
                FishToucherBundle.message("settings.mode.cultivation")
        };
    }

    public static JComboBox<String> createCombo(String selectedMode) {
        JComboBox<String> comboBox = new JComboBox<>(getModeLabels());
        comboBox.setSelectedIndex(getModeIndex(selectedMode));
        comboBox.setFocusable(false);
        comboBox.setToolTipText(FishToucherBundle.message("toolbar.mode.tooltip"));
        return comboBox;
    }

    public static String getSelectedMode(JComboBox<String> comboBox) {
        int index = comboBox.getSelectedIndex();
        if (index >= 0 && index < MODE_VALUES.length) {
            return MODE_VALUES[index];
        }
        return NovelReaderSettings.MODE_NOVEL;
    }

    public static int getModeIndex(String mode) {
        for (int i = 0; i < MODE_VALUES.length; i++) {
            if (MODE_VALUES[i].equals(mode)) {
                return i;
            }
        }
        return 0;
    }
}
