package com.fishtoucher.literature.settings;

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
import com.fishtoucher.literature.ui.NovelReaderManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class NovelReaderConfigurable implements Configurable {

    private static final Logger LOG = Logger.getInstance(NovelReaderConfigurable.class);

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

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Fish Toucher Literature";
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

        // ========== Stealth Mode Section ==========
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JLabel stealthTitle = new JLabel("Stealth Mode (Status Bar) \u2014 1 line at a time");
        stealthTitle.setFont(stealthTitle.getFont().deriveFont(Font.BOLD, 12f));
        mainPanel.add(stealthTitle, gbc);

        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Chars per line:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        stealthCharsPerLineSpinner = new JSpinner(new SpinnerNumberModel(settings.getStealthCharsPerLine(), 10, 500, 10));
        mainPanel.add(stealthCharsPerLineSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        showInStatusBarCheckBox = new JCheckBox("Enable stealth mode in status bar", settings.isShowInStatusBar());
        mainPanel.add(showInStatusBarCheckBox, gbc);

        // ========== Separator ==========
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        mainPanel.add(new JSeparator(), gbc);

        // ========== Normal Mode Section ==========
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JLabel normalTitle = new JLabel("Normal Mode (Tool Window) \u2014 multi-line display");
        normalTitle.setFont(normalTitle.getFont().deriveFont(Font.BOLD, 12f));
        mainPanel.add(normalTitle, gbc);

        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Lines per page:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        normalLinesPerPageSpinner = new JSpinner(new SpinnerNumberModel(settings.getNormalLinesPerPage(), 1, 50, 1));
        mainPanel.add(normalLinesPerPageSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Chars per line:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        normalCharsPerLineSpinner = new JSpinner(new SpinnerNumberModel(settings.getNormalCharsPerLine(), 10, 500, 10));
        mainPanel.add(normalCharsPerLineSpinner, gbc);

        // ========== Separator ==========
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        mainPanel.add(new JSeparator(), gbc);

        // ========== Shared Settings ==========
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JLabel sharedTitle = new JLabel("Shared Settings");
        sharedTitle.setFont(sharedTitle.getFont().deriveFont(Font.BOLD, 12f));
        mainPanel.add(sharedTitle, gbc);

        gbc.gridwidth = 1;

        // Font family
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Font family:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        fontFamilyField = new JTextField(settings.getFontFamily(), 20);
        mainPanel.add(fontFamilyField, gbc);

        // Font size
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Font size:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(settings.getFontSize(), 8, 30, 1));
        mainPanel.add(fontSizeSpinner, gbc);

        // Current file path
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        mainPanel.add(new JLabel("Current file:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 1.0;
        String filePath = NovelReaderManager.getInstance().getCurrentFilePath();
        currentFileLabel = new JLabel(filePath.isEmpty() ? "No file loaded" : filePath);
        currentFileLabel.setForeground(filePath.isEmpty() ? com.intellij.ui.JBColor.GRAY : com.intellij.ui.JBColor.foreground());
        currentFileLabel.setToolTipText(filePath.isEmpty() ? null : filePath);
        mainPanel.add(currentFileLabel, gbc);
        gbc.weightx = 0;

        // Import file button
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JPanel importPanel = getJPanel();
        mainPanel.add(importPanel, gbc);

        // ========== Separator ==========
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        mainPanel.add(new JSeparator(), gbc);

        // ========== Keyboard shortcuts ==========
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JLabel shortcutTitle = new JLabel("Keyboard Shortcuts (click field and press key combination, Esc to clear):");
        shortcutTitle.setFont(shortcutTitle.getFont().deriveFont(Font.BOLD, 12f));
        mainPanel.add(shortcutTitle, gbc);

        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Open file:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        shortcutOpenField = new ShortcutKeyField(settings.getShortcutOpen());
        mainPanel.add(shortcutOpenField, gbc);

        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Next page:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        shortcutNextPageField = new ShortcutKeyField(settings.getShortcutNextPage());
        mainPanel.add(shortcutNextPageField, gbc);

        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Previous page:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        shortcutPrevPageField = new ShortcutKeyField(settings.getShortcutPrevPage());
        mainPanel.add(shortcutPrevPageField, gbc);

        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Toggle visibility:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        shortcutToggleField = new ShortcutKeyField(settings.getShortcutToggle());
        mainPanel.add(shortcutToggleField, gbc);

        return mainPanel;
    }

    private @NotNull JPanel getJPanel() {
        JPanel importPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton importFileButton = new JButton("Import File...");
        importFileButton.setToolTipText("Select a novel file (.txt/.text) to load");
        importFileButton.addActionListener(e -> {
            FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                    .withFileFilter(file -> {
                        String ext = file.getExtension();
                        return ext != null && (ext.equalsIgnoreCase("txt") || ext.equalsIgnoreCase("text"));
                    })
                    .withTitle("Select File")
                    .withDescription("Choose a .txt / .text file to read");
            VirtualFile[] files = FileChooser.chooseFiles(descriptor, null, null);
            if (files.length > 0) {
                boolean success = NovelReaderManager.getInstance().loadFile(files[0].getPath());
                if (success) {
                    Messages.showInfoMessage("File loaded successfully: " + files[0].getName(), "Fish Toucher Literature");
                    updateCurrentFileLabel();
                } else {
                    Messages.showErrorDialog("Failed to load the file. Please check if the file is a valid text file.", "Fish Toucher Literature");
                }
            }
        });
        importPanel.add(importFileButton);
        return importPanel;
    }

    private void updateCurrentFileLabel() {
        if (currentFileLabel == null) return;
        String filePath = NovelReaderManager.getInstance().getCurrentFilePath();
        if (filePath.isEmpty()) {
            currentFileLabel.setText("No file loaded");
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
        return (int) stealthCharsPerLineSpinner.getValue() != settings.getStealthCharsPerLine()
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
        LOG.info("apply: saving settings"
                + " stealthCharsPerLine=" + stealthCharsPerLineSpinner.getValue()
                + ", normalLinesPerPage=" + normalLinesPerPageSpinner.getValue()
                + ", normalCharsPerLine=" + normalCharsPerLineSpinner.getValue()
                + ", fontSize=" + fontSizeSpinner.getValue()
                + ", fontFamily=" + fontFamilyField.getText()
                + ", showInStatusBar=" + showInStatusBarCheckBox.isSelected());

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
