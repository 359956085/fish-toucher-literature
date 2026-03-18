package com.fish.toucher.settings;

import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.ui.HotSearchManager;
import com.fish.toucher.ui.NovelReaderManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class NovelReaderConfigurable implements Configurable {

    private static final Logger LOG = Logger.getInstance(NovelReaderConfigurable.class);

    // Mode selector
    private JComboBox<String> modeComboBox;
    private static final String[] MODE_VALUES = {"novel", "hotsearch"};

    // Stealth mode
    private JSpinner stealthCharsPerLineSpinner;

    // Normal mode
    private JSpinner normalLinesPerPageSpinner;
    private JSpinner normalCharsPerLineSpinner;

    // Shared
    private JSpinner fontSizeSpinner;
    private JTextField fontFamilyField;
    private JCheckBox showInStatusBarCheckBox;
    private JLabel currentFileLabel;

    // Shortcuts
    private ShortcutKeyField shortcutOpenField;
    private ShortcutKeyField shortcutNextPageField;
    private ShortcutKeyField shortcutPrevPageField;
    private ShortcutKeyField shortcutToggleField;

    // Hot search settings
    private JComboBox<String> sourceComboBox;
    private JSpinner carouselIntervalSpinner;
    private JSpinner refreshIntervalSpinner;
    private JComboBox<String> xTranslateLanguageComboBox;
    private JPanel hotSearchSettingsPanel;

    private static final String[] TRANSLATE_LANG_CODES = {
            "en", "zh", "ja", "ko", "ru", "fr", "de", "it", "es", "pt", "ar", "hi", "th", "vi", "id"
    };
    private static final String[] TRANSLATE_LANG_LABELS = {
            "English", "\u4e2d\u6587", "\u65e5\u672c\u8a9e", "\ud55c\uad6d\uc5b4",
            "\u0420\u0443\u0441\u0441\u043a\u0438\u0439", "Fran\u00e7ais", "Deutsch", "Italiano",
            "Espa\u00f1ol", "Portugu\u00eas", "\u0627\u0644\u0639\u0631\u0628\u064a\u0629",
            "\u0939\u093f\u0928\u094d\u0926\u0940", "\u0e44\u0e17\u0e22",
            "Ti\u1ebfng Vi\u1ec7t", "Bahasa Indonesia"
    };

    // Novel-only settings panel (hidden in hot search mode)
    private JPanel novelSettingsPanel;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Fish Toucher";
    }

    @Override
    public @Nullable JComponent createComponent() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        int row = 0;

        // ========== Plugin Mode Section ==========
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JLabel modeTitle = new JLabel(FishToucherBundle.message("settings.section.pluginMode"));
        modeTitle.setFont(modeTitle.getFont().deriveFont(Font.BOLD, 12f));
        mainPanel.add(modeTitle, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel(FishToucherBundle.message("settings.label.mode")), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        String[] modeLabels = {FishToucherBundle.message("settings.mode.novel"), FishToucherBundle.message("settings.mode.hotSearch")};
        modeComboBox = new JComboBox<>(modeLabels);
        int modeIdx = "hotsearch".equals(settings.getPluginMode()) ? 1 : 0;
        modeComboBox.setSelectedIndex(modeIdx);
        modeComboBox.addActionListener(e -> updateNovelComponentsVisibility());
        mainPanel.add(modeComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JLabel modeHint = new JLabel(FishToucherBundle.message("settings.hint.modeChange"));
        modeHint.setForeground(com.intellij.ui.JBColor.GRAY);
        mainPanel.add(modeHint, gbc);

        // ========== Hot Search Settings (visible only in hot search mode) ==========
        hotSearchSettingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints hgbc = new GridBagConstraints();
        hgbc.insets = JBUI.insets(5);
        hgbc.anchor = GridBagConstraints.WEST;
        hgbc.fill = GridBagConstraints.HORIZONTAL;

        hgbc.gridx = 0; hgbc.gridy = 0; hgbc.gridwidth = 1;
        hotSearchSettingsPanel.add(new JLabel(FishToucherBundle.message("settings.label.source")), hgbc);
        hgbc.gridx = 1; hgbc.gridy = 0;
        sourceComboBox = new JComboBox<>(HotSearchManager.getSourceLabels());
        String currentSource = settings.getHotSearchSource();
        for (int i = 0; i < HotSearchManager.SOURCE_VALUES.length; i++) {
            if (HotSearchManager.SOURCE_VALUES[i].equals(currentSource)) {
                sourceComboBox.setSelectedIndex(i);
                break;
            }
        }
        hotSearchSettingsPanel.add(sourceComboBox, hgbc);

        hgbc.gridx = 0; hgbc.gridy = 1;
        hotSearchSettingsPanel.add(new JLabel(FishToucherBundle.message("settings.label.carouselInterval")), hgbc);
        hgbc.gridx = 1; hgbc.gridy = 1;
        carouselIntervalSpinner = new JSpinner(new SpinnerNumberModel(settings.getCarouselIntervalSeconds(), 3, 120, 1));
        hotSearchSettingsPanel.add(carouselIntervalSpinner, hgbc);

        hgbc.gridx = 0; hgbc.gridy = 2;
        hotSearchSettingsPanel.add(new JLabel(FishToucherBundle.message("settings.label.refreshInterval")), hgbc);
        hgbc.gridx = 1; hgbc.gridy = 2;
        refreshIntervalSpinner = new JSpinner(new SpinnerNumberModel(settings.getRefreshIntervalMinutes(), 1, 120, 1));
        hotSearchSettingsPanel.add(refreshIntervalSpinner, hgbc);

        hgbc.gridx = 0; hgbc.gridy = 3;
        hotSearchSettingsPanel.add(new JLabel(FishToucherBundle.message("settings.label.xTranslateLanguage")), hgbc);
        hgbc.gridx = 1; hgbc.gridy = 3;
        xTranslateLanguageComboBox = new JComboBox<>(TRANSLATE_LANG_LABELS);
        String currentLang = settings.getXTranslateLanguage();
        for (int i = 0; i < TRANSLATE_LANG_CODES.length; i++) {
            if (TRANSLATE_LANG_CODES[i].equals(currentLang)) {
                xTranslateLanguageComboBox.setSelectedIndex(i);
                break;
            }
        }
        hotSearchSettingsPanel.add(xTranslateLanguageComboBox, hgbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        mainPanel.add(hotSearchSettingsPanel, gbc);

        // ========== Separator ==========
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        mainPanel.add(new JSeparator(), gbc);

        // ========== Novel Settings (wrapped in a panel for show/hide) ==========
        novelSettingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints ngbc = new GridBagConstraints();
        ngbc.insets = JBUI.insets(5);
        ngbc.anchor = GridBagConstraints.WEST;
        ngbc.fill = GridBagConstraints.HORIZONTAL;
        int nrow = 0;

        // Stealth Mode
        ngbc.gridx = 0; ngbc.gridy = nrow++; ngbc.gridwidth = 2;
        JLabel stealthTitle = new JLabel(FishToucherBundle.message("settings.section.stealthMode"));
        stealthTitle.setFont(stealthTitle.getFont().deriveFont(Font.BOLD, 12f));
        novelSettingsPanel.add(stealthTitle, ngbc);

        ngbc.gridwidth = 1;
        ngbc.gridx = 0; ngbc.gridy = nrow;
        novelSettingsPanel.add(new JLabel(FishToucherBundle.message("settings.label.charsPerLine")), ngbc);
        ngbc.gridx = 1; ngbc.gridy = nrow++;
        stealthCharsPerLineSpinner = new JSpinner(new SpinnerNumberModel(settings.getStealthCharsPerLine(), 10, 500, 10));
        novelSettingsPanel.add(stealthCharsPerLineSpinner, ngbc);

        ngbc.gridx = 0; ngbc.gridy = nrow++; ngbc.gridwidth = 2;
        showInStatusBarCheckBox = new JCheckBox(FishToucherBundle.message("settings.label.enableStatusBar"), settings.isShowInStatusBar());
        novelSettingsPanel.add(showInStatusBarCheckBox, ngbc);

        ngbc.gridx = 0; ngbc.gridy = nrow++; ngbc.gridwidth = 2;
        novelSettingsPanel.add(new JSeparator(), ngbc);

        // Normal Mode
        ngbc.gridx = 0; ngbc.gridy = nrow++; ngbc.gridwidth = 2;
        JLabel normalTitle = new JLabel(FishToucherBundle.message("settings.section.normalMode"));
        normalTitle.setFont(normalTitle.getFont().deriveFont(Font.BOLD, 12f));
        novelSettingsPanel.add(normalTitle, ngbc);

        ngbc.gridwidth = 1;
        ngbc.gridx = 0; ngbc.gridy = nrow;
        novelSettingsPanel.add(new JLabel(FishToucherBundle.message("settings.label.linesPerPage")), ngbc);
        ngbc.gridx = 1; ngbc.gridy = nrow++;
        normalLinesPerPageSpinner = new JSpinner(new SpinnerNumberModel(settings.getNormalLinesPerPage(), 1, 50, 1));
        novelSettingsPanel.add(normalLinesPerPageSpinner, ngbc);

        ngbc.gridx = 0; ngbc.gridy = nrow;
        novelSettingsPanel.add(new JLabel(FishToucherBundle.message("settings.label.charsPerLine")), ngbc);
        ngbc.gridx = 1; ngbc.gridy = nrow++;
        normalCharsPerLineSpinner = new JSpinner(new SpinnerNumberModel(settings.getNormalCharsPerLine(), 10, 500, 10));
        novelSettingsPanel.add(normalCharsPerLineSpinner, ngbc);

        ngbc.gridx = 0; ngbc.gridy = nrow++; ngbc.gridwidth = 2;
        novelSettingsPanel.add(new JSeparator(), ngbc);

        // Shared Settings
        ngbc.gridx = 0; ngbc.gridy = nrow++; ngbc.gridwidth = 2;
        JLabel sharedTitle = new JLabel(FishToucherBundle.message("settings.section.shared"));
        sharedTitle.setFont(sharedTitle.getFont().deriveFont(Font.BOLD, 12f));
        novelSettingsPanel.add(sharedTitle, ngbc);

        ngbc.gridwidth = 1;
        ngbc.gridx = 0; ngbc.gridy = nrow;
        novelSettingsPanel.add(new JLabel(FishToucherBundle.message("settings.label.fontFamily")), ngbc);
        ngbc.gridx = 1; ngbc.gridy = nrow++;
        fontFamilyField = new JTextField(settings.getFontFamily(), 20);
        novelSettingsPanel.add(fontFamilyField, ngbc);

        ngbc.gridx = 0; ngbc.gridy = nrow;
        novelSettingsPanel.add(new JLabel(FishToucherBundle.message("settings.label.fontSize")), ngbc);
        ngbc.gridx = 1; ngbc.gridy = nrow++;
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(settings.getFontSize(), 8, 30, 1));
        novelSettingsPanel.add(fontSizeSpinner, ngbc);

        ngbc.gridx = 0; ngbc.gridy = nrow; ngbc.gridwidth = 1;
        novelSettingsPanel.add(new JLabel(FishToucherBundle.message("settings.label.currentFile")), ngbc);
        ngbc.gridx = 1; ngbc.gridy = nrow++; ngbc.weightx = 1.0;
        String filePath = NovelReaderManager.getInstance().getCurrentFilePath();
        currentFileLabel = new JLabel(filePath.isEmpty() ? FishToucherBundle.message("settings.label.noFileLoaded") : filePath);
        currentFileLabel.setForeground(filePath.isEmpty() ? com.intellij.ui.JBColor.GRAY : com.intellij.ui.JBColor.foreground());
        currentFileLabel.setToolTipText(filePath.isEmpty() ? null : filePath);
        novelSettingsPanel.add(currentFileLabel, ngbc);
        ngbc.weightx = 0;

        ngbc.gridx = 0; ngbc.gridy = nrow++; ngbc.gridwidth = 2;
        JPanel importPanel = getJPanel();
        novelSettingsPanel.add(importPanel, ngbc);

        ngbc.gridx = 0; ngbc.gridy = nrow++; ngbc.gridwidth = 2;
        novelSettingsPanel.add(new JSeparator(), ngbc);

        // Keyboard shortcuts
        ngbc.gridx = 0; ngbc.gridy = nrow++; ngbc.gridwidth = 2;
        JLabel shortcutTitle = new JLabel(FishToucherBundle.message("settings.section.shortcuts"));
        shortcutTitle.setFont(shortcutTitle.getFont().deriveFont(Font.BOLD, 12f));
        novelSettingsPanel.add(shortcutTitle, ngbc);

        ngbc.gridwidth = 1;
        ngbc.gridx = 0; ngbc.gridy = nrow;
        novelSettingsPanel.add(new JLabel(FishToucherBundle.message("settings.label.shortcutOpen")), ngbc);
        ngbc.gridx = 1; ngbc.gridy = nrow++;
        shortcutOpenField = new ShortcutKeyField(settings.getShortcutOpen());
        novelSettingsPanel.add(shortcutOpenField, ngbc);

        ngbc.gridx = 0; ngbc.gridy = nrow;
        novelSettingsPanel.add(new JLabel(FishToucherBundle.message("settings.label.shortcutNextPage")), ngbc);
        ngbc.gridx = 1; ngbc.gridy = nrow++;
        shortcutNextPageField = new ShortcutKeyField(settings.getShortcutNextPage());
        novelSettingsPanel.add(shortcutNextPageField, ngbc);

        ngbc.gridx = 0; ngbc.gridy = nrow;
        novelSettingsPanel.add(new JLabel(FishToucherBundle.message("settings.label.shortcutPrevPage")), ngbc);
        ngbc.gridx = 1; ngbc.gridy = nrow++;
        shortcutPrevPageField = new ShortcutKeyField(settings.getShortcutPrevPage());
        novelSettingsPanel.add(shortcutPrevPageField, ngbc);

        ngbc.gridx = 0; ngbc.gridy = nrow;
        novelSettingsPanel.add(new JLabel(FishToucherBundle.message("settings.label.shortcutToggle")), ngbc);
        ngbc.gridx = 1; ngbc.gridy = nrow++;
        shortcutToggleField = new ShortcutKeyField(settings.getShortcutToggle());
        novelSettingsPanel.add(shortcutToggleField, ngbc);

        // Add novel settings panel to main panel
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        mainPanel.add(novelSettingsPanel, gbc);

        // Set initial visibility
        updateNovelComponentsVisibility();

        return mainPanel;
    }

    private @NotNull JPanel getJPanel() {
        JPanel importPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton importFileButton = new JButton(FishToucherBundle.message("settings.button.importFile"));
        importFileButton.setToolTipText(FishToucherBundle.message("settings.tooltip.importFile"));
        importFileButton.addActionListener(e -> {
            FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                    .withFileFilter(file -> {
                        String ext = file.getExtension();
                        return ext != null && (ext.equalsIgnoreCase("txt") || ext.equalsIgnoreCase("text"));
                    })
                    .withTitle(FishToucherBundle.message("settings.dialog.selectFile"))
                    .withDescription(FishToucherBundle.message("settings.dialog.selectFileDesc"));
            VirtualFile[] files = FileChooser.chooseFiles(descriptor, null, null);
            if (files.length > 0) {
                boolean success = NovelReaderManager.getInstance().loadFile(files[0].getPath());
                if (success) {
                    Messages.showInfoMessage(FishToucherBundle.message("settings.dialog.fileLoadSuccess", files[0].getName()), "Fish Toucher");
                    updateCurrentFileLabel();
                } else {
                    Messages.showErrorDialog(FishToucherBundle.message("settings.dialog.fileLoadFailed"), "Fish Toucher");
                }
            }
        });
        importPanel.add(importFileButton);
        return importPanel;
    }

    private void updateNovelComponentsVisibility() {
        boolean isNovel = modeComboBox.getSelectedIndex() == 0;
        novelSettingsPanel.setVisible(isNovel);
        hotSearchSettingsPanel.setVisible(!isNovel);
    }

    private String getSelectedMode() {
        return MODE_VALUES[modeComboBox.getSelectedIndex()];
    }

    private String getSelectedSource() {
        int idx = sourceComboBox.getSelectedIndex();
        return (idx >= 0 && idx < HotSearchManager.SOURCE_VALUES.length)
                ? HotSearchManager.SOURCE_VALUES[idx] : "baidu";
    }

    private String getSelectedTranslateLanguage() {
        int idx = xTranslateLanguageComboBox.getSelectedIndex();
        return (idx >= 0 && idx < TRANSLATE_LANG_CODES.length)
                ? TRANSLATE_LANG_CODES[idx] : "en";
    }

    private void updateCurrentFileLabel() {
        if (currentFileLabel == null) return;
        String filePath = NovelReaderManager.getInstance().getCurrentFilePath();
        if (filePath.isEmpty()) {
            currentFileLabel.setText(FishToucherBundle.message("settings.label.noFileLoaded"));
            currentFileLabel.setForeground(com.intellij.ui.JBColor.GRAY);
            currentFileLabel.setToolTipText(null);
        } else {
            currentFileLabel.setText(filePath);
            currentFileLabel.setForeground(com.intellij.ui.JBColor.foreground());
            currentFileLabel.setToolTipText(filePath);
        }
    }

    @Override
    public boolean isModified() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        return !getSelectedMode().equals(settings.getPluginMode())
                || !getSelectedSource().equals(settings.getHotSearchSource())
                || (int) carouselIntervalSpinner.getValue() != settings.getCarouselIntervalSeconds()
                || (int) refreshIntervalSpinner.getValue() != settings.getRefreshIntervalMinutes()
                || !getSelectedTranslateLanguage().equals(settings.getXTranslateLanguage())
                || (int) stealthCharsPerLineSpinner.getValue() != settings.getStealthCharsPerLine()
                || showInStatusBarCheckBox.isSelected() != settings.isShowInStatusBar()
                || (int) normalLinesPerPageSpinner.getValue() != settings.getNormalLinesPerPage()
                || (int) normalCharsPerLineSpinner.getValue() != settings.getNormalCharsPerLine()
                || (int) fontSizeSpinner.getValue() != settings.getFontSize()
                || !fontFamilyField.getText().equals(settings.getFontFamily())
                || !shortcutOpenField.getKeystrokeString().equals(settings.getShortcutOpen())
                || !shortcutNextPageField.getKeystrokeString().equals(settings.getShortcutNextPage())
                || !shortcutPrevPageField.getKeystrokeString().equals(settings.getShortcutPrevPage())
                || !shortcutToggleField.getKeystrokeString().equals(settings.getShortcutToggle());
    }

    @Override
    public void apply() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        String newMode = getSelectedMode();
        LOG.info("apply: saving settings"
                + " pluginMode=" + newMode
                + ", stealthCharsPerLine=" + stealthCharsPerLineSpinner.getValue()
                + ", normalLinesPerPage=" + normalLinesPerPageSpinner.getValue()
                + ", normalCharsPerLine=" + normalCharsPerLineSpinner.getValue()
                + ", fontSize=" + fontSizeSpinner.getValue()
                + ", fontFamily=" + fontFamilyField.getText()
                + ", showInStatusBar=" + showInStatusBarCheckBox.isSelected());

        String oldMode = settings.getPluginMode();
        settings.setPluginMode(newMode);

        // Start/stop HotSearchManager based on mode change
        if ("hotsearch".equals(newMode) && !HotSearchManager.getInstance().isRunning()) {
            HotSearchManager.getInstance().start();
        } else if ("novel".equals(newMode) && HotSearchManager.getInstance().isRunning()) {
            HotSearchManager.getInstance().stop();
        }

        // Hot search source
        String oldSource = settings.getHotSearchSource();
        String newSource = getSelectedSource();
        settings.setHotSearchSource(newSource);
        if (!newSource.equals(oldSource) && HotSearchManager.getInstance().isRunning()) {
            HotSearchManager.getInstance().switchSource();
        }

        // Hot search timing
        int oldCarousel = settings.getCarouselIntervalSeconds();
        int oldRefresh = settings.getRefreshIntervalMinutes();
        settings.setCarouselIntervalSeconds((int) carouselIntervalSpinner.getValue());
        settings.setRefreshIntervalMinutes((int) refreshIntervalSpinner.getValue());
        if ((oldCarousel != settings.getCarouselIntervalSeconds() || oldRefresh != settings.getRefreshIntervalMinutes())
                && HotSearchManager.getInstance().isRunning()) {
            HotSearchManager.getInstance().applyTimingChanges();
        }

        // X translate language
        String oldLang = settings.getXTranslateLanguage();
        settings.setXTranslateLanguage(getSelectedTranslateLanguage());
        if (!oldLang.equals(settings.getXTranslateLanguage())
                && "x".equals(settings.getHotSearchSource())
                && HotSearchManager.getInstance().isRunning()) {
            HotSearchManager.getInstance().switchSource();
        }

        settings.setStealthCharsPerLine((int) stealthCharsPerLineSpinner.getValue());
        settings.setShowInStatusBar(showInStatusBarCheckBox.isSelected());
        settings.setNormalLinesPerPage((int) normalLinesPerPageSpinner.getValue());
        settings.setNormalCharsPerLine((int) normalCharsPerLineSpinner.getValue());
        settings.setFontSize((int) fontSizeSpinner.getValue());
        settings.setFontFamily(fontFamilyField.getText());

        // Save and apply shortcuts
        String oldOpen = settings.getShortcutOpen();
        String oldNext = settings.getShortcutNextPage();
        String oldPrev = settings.getShortcutPrevPage();
        String oldToggle = settings.getShortcutToggle();

        settings.setShortcutOpen(shortcutOpenField.getKeystrokeString());
        settings.setShortcutNextPage(shortcutNextPageField.getKeystrokeString());
        settings.setShortcutPrevPage(shortcutPrevPageField.getKeystrokeString());
        settings.setShortcutToggle(shortcutToggleField.getKeystrokeString());

        applyShortcutToKeymap("NovelReader.Open", oldOpen, settings.getShortcutOpen());
        applyShortcutToKeymap("NovelReader.NextPage", oldNext, settings.getShortcutNextPage());
        applyShortcutToKeymap("NovelReader.PrevPage", oldPrev, settings.getShortcutPrevPage());
        applyShortcutToKeymap("NovelReader.Toggle", oldToggle, settings.getShortcutToggle());
    }

    private void applyShortcutToKeymap(String actionId, String oldKeystroke, String newKeystroke) {
        Keymap keymap = KeymapManager.getInstance().getActiveKeymap();

        if (oldKeystroke != null && !oldKeystroke.isEmpty()) {
            KeyStroke oldKs = KeyStroke.getKeyStroke(oldKeystroke.replace("ctrl", "control"));
            if (oldKs != null) {
                try {
                    keymap.removeShortcut(actionId, new KeyboardShortcut(oldKs, null));
                } catch (Exception e) {
                    LOG.debug("applyShortcutToKeymap: could not remove old shortcut for " + actionId);
                }
            }
        }

        Shortcut[] existingShortcuts = keymap.getShortcuts(actionId);
        for (Shortcut s : existingShortcuts) {
            try {
                keymap.removeShortcut(actionId, s);
            } catch (Exception e) {
                LOG.debug("applyShortcutToKeymap: could not remove existing shortcut for " + actionId);
            }
        }

        if (newKeystroke != null && !newKeystroke.isEmpty()) {
            KeyStroke newKs = KeyStroke.getKeyStroke(newKeystroke.replace("ctrl", "control"));
            if (newKs != null) {
                keymap.addShortcut(actionId, new KeyboardShortcut(newKs, null));
                LOG.info("applyShortcutToKeymap: set " + actionId + " -> " + newKeystroke);
            }
        }
    }

    @Override
    public void reset() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        modeComboBox.setSelectedIndex("hotsearch".equals(settings.getPluginMode()) ? 1 : 0);
        String source = settings.getHotSearchSource();
        for (int i = 0; i < HotSearchManager.SOURCE_VALUES.length; i++) {
            if (HotSearchManager.SOURCE_VALUES[i].equals(source)) {
                sourceComboBox.setSelectedIndex(i);
                break;
            }
        }
        carouselIntervalSpinner.setValue(settings.getCarouselIntervalSeconds());
        refreshIntervalSpinner.setValue(settings.getRefreshIntervalMinutes());
        String lang = settings.getXTranslateLanguage();
        for (int i = 0; i < TRANSLATE_LANG_CODES.length; i++) {
            if (TRANSLATE_LANG_CODES[i].equals(lang)) {
                xTranslateLanguageComboBox.setSelectedIndex(i);
                break;
            }
        }
        updateNovelComponentsVisibility();
        stealthCharsPerLineSpinner.setValue(settings.getStealthCharsPerLine());
        showInStatusBarCheckBox.setSelected(settings.isShowInStatusBar());
        normalLinesPerPageSpinner.setValue(settings.getNormalLinesPerPage());
        normalCharsPerLineSpinner.setValue(settings.getNormalCharsPerLine());
        fontSizeSpinner.setValue(settings.getFontSize());
        fontFamilyField.setText(settings.getFontFamily());
        shortcutOpenField.setKeystrokeString(settings.getShortcutOpen());
        shortcutNextPageField.setKeystrokeString(settings.getShortcutNextPage());
        shortcutPrevPageField.setKeystrokeString(settings.getShortcutPrevPage());
        shortcutToggleField.setKeystrokeString(settings.getShortcutToggle());
    }
}
