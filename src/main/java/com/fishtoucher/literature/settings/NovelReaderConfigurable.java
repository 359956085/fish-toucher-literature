package com.fishtoucher.literature.settings;

import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class NovelReaderConfigurable implements Configurable {

    private static final Logger LOG = Logger.getInstance(NovelReaderConfigurable.class);
    private JPanel mainPanel;
    private JSpinner linesPerPageSpinner;
    private JSpinner charsPerLineSpinner;
    private JSpinner fontSizeSpinner;
    private JTextField fontFamilyField;
    private JCheckBox showInStatusBarCheckBox;
    private ShortcutKeyField shortcutOpenField;
    private ShortcutKeyField shortcutNextPageField;
    private ShortcutKeyField shortcutPrevPageField;
    private ShortcutKeyField shortcutToggleField;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Fish Toucher Literature";
    }

    @Override
    public @Nullable JComponent createComponent() {
        mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        NovelReaderSettings settings = NovelReaderSettings.getInstance();

        // Lines per page
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Lines per page:"), gbc);
        gbc.gridx = 1;
        linesPerPageSpinner = new JSpinner(new SpinnerNumberModel(settings.getLinesPerPage(), 1, 50, 1));
        mainPanel.add(linesPerPageSpinner, gbc);

        // Chars per line
        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(new JLabel("Chars per line (0=unlimited):"), gbc);
        gbc.gridx = 1;
        charsPerLineSpinner = new JSpinner(new SpinnerNumberModel(settings.getCharsPerLine(), 10, 500, 10));
        mainPanel.add(charsPerLineSpinner, gbc);

        // Font family
        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(new JLabel("Font family:"), gbc);
        gbc.gridx = 1;
        fontFamilyField = new JTextField(settings.getFontFamily(), 20);
        mainPanel.add(fontFamilyField, gbc);

        // Font size
        gbc.gridx = 0; gbc.gridy = 3;
        mainPanel.add(new JLabel("Font size:"), gbc);
        gbc.gridx = 1;
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(settings.getFontSize(), 8, 30, 1));
        mainPanel.add(fontSizeSpinner, gbc);

        // Show in status bar
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        showInStatusBarCheckBox = new JCheckBox("Show reading content in status bar", settings.isShowInStatusBar());
        mainPanel.add(showInStatusBarCheckBox, gbc);

        // --- Keyboard shortcuts section ---
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        JLabel shortcutTitle = new JLabel("Keyboard Shortcuts (click field and press key combination, Esc to clear):");
        shortcutTitle.setFont(shortcutTitle.getFont().deriveFont(Font.BOLD, 12f));
        mainPanel.add(shortcutTitle, gbc);

        gbc.gridwidth = 1;

        // Open novel file
        gbc.gridx = 0; gbc.gridy = 6;
        mainPanel.add(new JLabel("Open novel file:"), gbc);
        gbc.gridx = 1;
        shortcutOpenField = new ShortcutKeyField(settings.getShortcutOpen());
        mainPanel.add(shortcutOpenField, gbc);

        // Next page
        gbc.gridx = 0; gbc.gridy = 7;
        mainPanel.add(new JLabel("Next page:"), gbc);
        gbc.gridx = 1;
        shortcutNextPageField = new ShortcutKeyField(settings.getShortcutNextPage());
        mainPanel.add(shortcutNextPageField, gbc);

        // Previous page
        gbc.gridx = 0; gbc.gridy = 8;
        mainPanel.add(new JLabel("Previous page:"), gbc);
        gbc.gridx = 1;
        shortcutPrevPageField = new ShortcutKeyField(settings.getShortcutPrevPage());
        mainPanel.add(shortcutPrevPageField, gbc);

        // Toggle visibility
        gbc.gridx = 0; gbc.gridy = 9;
        mainPanel.add(new JLabel("Toggle visibility:"), gbc);
        gbc.gridx = 1;
        shortcutToggleField = new ShortcutKeyField(settings.getShortcutToggle());
        mainPanel.add(shortcutToggleField, gbc);

        return mainPanel;
    }

    @Override
    public boolean isModified() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        return (int) linesPerPageSpinner.getValue() != settings.getLinesPerPage()
                || (int) charsPerLineSpinner.getValue() != settings.getCharsPerLine()
                || (int) fontSizeSpinner.getValue() != settings.getFontSize()
                || !fontFamilyField.getText().equals(settings.getFontFamily())
                || showInStatusBarCheckBox.isSelected() != settings.isShowInStatusBar()
                || !shortcutOpenField.getKeystrokeString().equals(settings.getShortcutOpen())
                || !shortcutNextPageField.getKeystrokeString().equals(settings.getShortcutNextPage())
                || !shortcutPrevPageField.getKeystrokeString().equals(settings.getShortcutPrevPage())
                || !shortcutToggleField.getKeystrokeString().equals(settings.getShortcutToggle());
    }

    @Override
    public void apply() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        LOG.info("apply: saving settings - linesPerPage=" + linesPerPageSpinner.getValue()
                + ", charsPerLine=" + charsPerLineSpinner.getValue()
                + ", fontSize=" + fontSizeSpinner.getValue()
                + ", fontFamily=" + fontFamilyField.getText()
                + ", showInStatusBar=" + showInStatusBarCheckBox.isSelected());
        settings.setLinesPerPage((int) linesPerPageSpinner.getValue());
        settings.setCharsPerLine((int) charsPerLineSpinner.getValue());
        settings.setFontSize((int) fontSizeSpinner.getValue());
        settings.setFontFamily(fontFamilyField.getText());
        settings.setShowInStatusBar(showInStatusBarCheckBox.isSelected());

        // Save and apply shortcuts
        String oldOpen = settings.getShortcutOpen();
        String oldNext = settings.getShortcutNextPage();
        String oldPrev = settings.getShortcutPrevPage();
        String oldToggle = settings.getShortcutToggle();

        settings.setShortcutOpen(shortcutOpenField.getKeystrokeString());
        settings.setShortcutNextPage(shortcutNextPageField.getKeystrokeString());
        settings.setShortcutPrevPage(shortcutPrevPageField.getKeystrokeString());
        settings.setShortcutToggle(shortcutToggleField.getKeystrokeString());

        LOG.info("apply: shortcuts - open=" + settings.getShortcutOpen()
                + ", next=" + settings.getShortcutNextPage()
                + ", prev=" + settings.getShortcutPrevPage()
                + ", toggle=" + settings.getShortcutToggle());

        // Apply to active keymap
        applyShortcutToKeymap("NovelReader.Open", oldOpen, settings.getShortcutOpen());
        applyShortcutToKeymap("NovelReader.NextPage", oldNext, settings.getShortcutNextPage());
        applyShortcutToKeymap("NovelReader.PrevPage", oldPrev, settings.getShortcutPrevPage());
        applyShortcutToKeymap("NovelReader.Toggle", oldToggle, settings.getShortcutToggle());
    }

    private void applyShortcutToKeymap(String actionId, String oldKeystroke, String newKeystroke) {
        Keymap keymap = KeymapManager.getInstance().getActiveKeymap();

        // Remove old shortcut set by plugin settings
        if (oldKeystroke != null && !oldKeystroke.isEmpty()) {
            KeyStroke oldKs = KeyStroke.getKeyStroke(oldKeystroke.replace("ctrl", "control"));
            if (oldKs != null) {
                KeyboardShortcut oldShortcut = new KeyboardShortcut(oldKs, null);
                try {
                    keymap.removeShortcut(actionId, oldShortcut);
                } catch (Exception e) {
                    LOG.debug("applyShortcutToKeymap: could not remove old shortcut for " + actionId + ": " + e.getMessage());
                }
            }
        }

        // Also remove all existing shortcuts for this action to avoid duplicates
        Shortcut[] existingShortcuts = keymap.getShortcuts(actionId);
        for (Shortcut s : existingShortcuts) {
            try {
                keymap.removeShortcut(actionId, s);
            } catch (Exception e) {
                LOG.debug("applyShortcutToKeymap: could not remove existing shortcut for " + actionId + ": " + e.getMessage());
            }
        }

        // Add new shortcut
        if (newKeystroke != null && !newKeystroke.isEmpty()) {
            KeyStroke newKs = KeyStroke.getKeyStroke(newKeystroke.replace("ctrl", "control"));
            if (newKs != null) {
                keymap.addShortcut(actionId, new KeyboardShortcut(newKs, null));
                LOG.info("applyShortcutToKeymap: set " + actionId + " -> " + newKeystroke);
            } else {
                LOG.warn("applyShortcutToKeymap: failed to parse keystroke '" + newKeystroke + "' for " + actionId);
            }
        }
    }

    @Override
    public void reset() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        linesPerPageSpinner.setValue(settings.getLinesPerPage());
        charsPerLineSpinner.setValue(settings.getCharsPerLine());
        fontSizeSpinner.setValue(settings.getFontSize());
        fontFamilyField.setText(settings.getFontFamily());
        showInStatusBarCheckBox.setSelected(settings.isShowInStatusBar());
        shortcutOpenField.setKeystrokeString(settings.getShortcutOpen());
        shortcutNextPageField.setKeystrokeString(settings.getShortcutNextPage());
        shortcutPrevPageField.setKeystrokeString(settings.getShortcutPrevPage());
        shortcutToggleField.setKeystrokeString(settings.getShortcutToggle());
    }
}
