package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class RecentFileSelector extends JPanel {

    private final @Nullable Project project;
    private final Runnable onChange;
    private final JComboBox<RecentFileItem> comboBox;
    private final JButton deleteButton;
    private boolean refreshing;

    public RecentFileSelector(@Nullable Project project, Runnable onChange) {
        super(new FlowLayout(FlowLayout.LEFT, 2, 0));
        this.project = project;
        this.onChange = onChange != null ? onChange : () -> {};

        comboBox = new JComboBox<>();
        comboBox.setFocusable(false);
        comboBox.setRenderer(new RecentFileRenderer());
        comboBox.setToolTipText(FishToucherBundle.message("settings.tooltip.recentFiles"));
        comboBox.addActionListener(e -> loadSelectedRecentFile());

        deleteButton = new JButton("x");
        deleteButton.setMargin(new Insets(1, 4, 1, 4));
        deleteButton.setFont(deleteButton.getFont().deriveFont(12f));
        deleteButton.setFocusable(false);
        deleteButton.setToolTipText(FishToucherBundle.message("settings.tooltip.removeRecentFile"));
        deleteButton.addActionListener(e -> removeSelectedRecentFile());

        add(comboBox);
        add(deleteButton);
        refresh();
    }

    public void refresh() {
        refreshing = true;
        try {
            comboBox.removeAllItems();
            List<String> paths = NovelReaderSettings.getInstance().getRecentFilePaths();
            if (paths.isEmpty()) {
                comboBox.addItem(RecentFileItem.placeholder());
                comboBox.setEnabled(false);
                deleteButton.setEnabled(false);
                comboBox.setToolTipText(FishToucherBundle.message("settings.tooltip.recentFiles"));
                return;
            }

            for (String path : paths) {
                comboBox.addItem(RecentFileItem.of(path));
            }

            String currentPath = NovelReaderManager.getInstance().getCurrentFilePath();
            if (!currentPath.isEmpty()) {
                for (int i = 0; i < comboBox.getItemCount(); i++) {
                    RecentFileItem item = comboBox.getItemAt(i);
                    if (currentPath.equals(item.path())) {
                        comboBox.setSelectedIndex(i);
                        break;
                    }
                }
            }
            comboBox.setEnabled(true);
            deleteButton.setEnabled(comboBox.getSelectedItem() instanceof RecentFileItem item && !item.placeholder());
            updateTooltip();
        } finally {
            refreshing = false;
        }
    }

    private void loadSelectedRecentFile() {
        if (refreshing) return;
        RecentFileItem item = getSelectedItem();
        updateTooltip();
        if (item == null || item.placeholder()) return;

        boolean success = NovelReaderManager.getInstance().loadFile(item.path());
        if (!success) {
            Messages.showErrorDialog(project,
                    FishToucherBundle.message("settings.dialog.recentFileLoadFailed", item.path()),
                    "Fish Toucher");
        }
        onChange.run();
        refresh();
    }

    private void removeSelectedRecentFile() {
        RecentFileItem item = getSelectedItem();
        if (item == null || item.placeholder()) return;

        NovelReaderSettings.getInstance().removeRecentFilePath(item.path());
        onChange.run();
        refresh();
    }

    private RecentFileItem getSelectedItem() {
        Object selected = comboBox.getSelectedItem();
        return selected instanceof RecentFileItem item ? item : null;
    }

    private void updateTooltip() {
        RecentFileItem item = getSelectedItem();
        comboBox.setToolTipText(item != null && !item.placeholder()
                ? item.path()
                : FishToucherBundle.message("settings.tooltip.recentFiles"));
    }

    private record RecentFileItem(String path, String label, boolean placeholder) {
        private static RecentFileItem of(String path) {
            String name = new File(path).getName();
            return new RecentFileItem(path, name.isEmpty() ? path : name, false);
        }

        private static RecentFileItem placeholder() {
            return new RecentFileItem("", FishToucherBundle.message("settings.recentFiles.empty"), true);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class RecentFileRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof RecentFileItem item) {
                setText(item.label());
                if (list != null && !item.placeholder()) {
                    list.setToolTipText(item.path());
                }
            }
            return this;
        }
    }
}
