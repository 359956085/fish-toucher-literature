package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;

import javax.swing.*;

public final class LanguageSelector {

    public static final String[] LANGUAGE_VALUES = {
            NovelReaderSettings.LANGUAGE_AUTO,
            "en",
            "zh",
            "de",
            "fr",
            "it",
            "ja",
            "ko",
            "ru"
    };

    private LanguageSelector() {}

    public static String[] getLanguageLabels() {
        return new String[]{
                FishToucherBundle.message("settings.language.idea"),
                "English",
                "中文",
                "Deutsch",
                "Français",
                "Italiano",
                "日本語",
                "한국어",
                "Русский"
        };
    }

    public static JComboBox<String> createCombo(String selectedLanguage) {
        JComboBox<String> comboBox = new JComboBox<>(getLanguageLabels());
        comboBox.setSelectedIndex(getLanguageIndex(selectedLanguage));
        comboBox.setFocusable(false);
        comboBox.setToolTipText(FishToucherBundle.message("toolbar.language.tooltip"));
        return comboBox;
    }

    public static String getSelectedLanguage(JComboBox<String> comboBox) {
        int index = comboBox.getSelectedIndex();
        if (index >= 0 && index < LANGUAGE_VALUES.length) {
            return LANGUAGE_VALUES[index];
        }
        return NovelReaderSettings.LANGUAGE_AUTO;
    }

    public static int getLanguageIndex(String language) {
        for (int i = 0; i < LANGUAGE_VALUES.length; i++) {
            if (LANGUAGE_VALUES[i].equals(language)) {
                return i;
            }
        }
        return 0;
    }
}
