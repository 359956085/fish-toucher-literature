package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.File;
import java.util.List;

public class RecentFileSelector extends JPanel {

    private final @Nullable Project project;
    private final Runnable onChange;
    private final JButton dropdownButton;
    private final JPopupMenu popupMenu;

    public RecentFileSelector(@Nullable Project project, Runnable onChange) {
        super(new FlowLayout(FlowLayout.LEFT, 2, 0));
        this.project = project;
        this.onChange = onChange != null ? onChange : () -> {};

        dropdownButton = new JButton();
        dropdownButton.setFocusable(false);
        dropdownButton.setHorizontalAlignment(SwingConstants.LEFT);
        dropdownButton.setHorizontalTextPosition(SwingConstants.LEFT);
        dropdownButton.setIcon(new DropdownArrowIcon());
        dropdownButton.setIconTextGap(8);
        dropdownButton.setMargin(new Insets(1, 6, 1, 6));
        Border comboBoxBorder = UIManager.getBorder("ComboBox.border");
        Border innerBorder = comboBoxBorder != null ? comboBoxBorder : dropdownButton.getBorder();
        Color comboBoxBackground = UIManager.getColor("ComboBox.background");
        Color comboBoxForeground = UIManager.getColor("ComboBox.foreground");
        dropdownButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border(), 1),
                innerBorder
        ));
        if (comboBoxBackground != null) {
            dropdownButton.setBackground(comboBoxBackground);
        }
        if (comboBoxForeground != null) {
            dropdownButton.setForeground(comboBoxForeground);
        }
        dropdownButton.setOpaque(true);
        dropdownButton.setContentAreaFilled(true);
        dropdownButton.putClientProperty("JButton.buttonType", "combobox");
        dropdownButton.setToolTipText(FishToucherBundle.message("settings.tooltip.recentFiles"));
        dropdownButton.addActionListener(e -> showRecentFilesPopup());

        popupMenu = new JPopupMenu();

        add(dropdownButton);
        refresh();
    }

    public void refresh() {
        List<String> paths = NovelReaderSettings.getInstance().getRecentFilePaths();
        if (paths.isEmpty()) {
            RecentFileItem placeholder = RecentFileItem.placeholderItem();
            dropdownButton.setText(placeholder.label());
            dropdownButton.setEnabled(false);
            dropdownButton.setToolTipText(FishToucherBundle.message("settings.tooltip.recentFiles"));
            return;
        }

        RecentFileItem selectedItem = RecentFileItem.of(paths.get(0));
        String currentPath = NovelReaderManager.getInstance().getCurrentFilePath();
        if (!currentPath.isEmpty()) {
            for (String path : paths) {
                if (currentPath.equals(path)) {
                    selectedItem = RecentFileItem.of(path);
                    break;
                }
            }
        }

        dropdownButton.setText(selectedItem.label());
        dropdownButton.setEnabled(true);
        dropdownButton.setToolTipText(selectedItem.path());
    }

    private void showRecentFilesPopup() {
        popupMenu.removeAll();
        List<String> paths = NovelReaderSettings.getInstance().getRecentFilePaths();
        if (paths.isEmpty()) {
            return;
        }

        for (String path : paths) {
            popupMenu.add(createRecentFileRow(RecentFileItem.of(path)));
        }

        Dimension popupSize = popupMenu.getPreferredSize();
        popupMenu.setPopupSize(Math.max(dropdownButton.getWidth(), popupSize.width), popupSize.height);
        popupMenu.show(dropdownButton, 0, dropdownButton.getHeight());
    }

    private JComponent createRecentFileRow(RecentFileItem item) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));

        JButton openButton = new JButton(item.label());
        openButton.setHorizontalAlignment(SwingConstants.LEFT);
        openButton.setBorderPainted(false);
        openButton.setContentAreaFilled(false);
        openButton.setFocusable(false);
        openButton.setToolTipText(item.path());
        openButton.addActionListener(e -> loadRecentFile(item));

        JButton removeButton = new JButton("x");
        removeButton.setMargin(new Insets(0, 3, 0, 3));
        removeButton.setFocusable(false);
        removeButton.setToolTipText(FishToucherBundle.message("settings.tooltip.removeRecentFile"));
        removeButton.addActionListener(e -> removeRecentFile(item));

        row.add(openButton, BorderLayout.CENTER);
        row.add(removeButton, BorderLayout.EAST);
        row.setToolTipText(item.path());
        return row;
    }

    private void loadRecentFile(RecentFileItem item) {
        if (item == null || item.placeholder()) return;

        popupMenu.setVisible(false);
        loadFile(item.path());
        onChange.run();
        refresh();
    }

    private void removeRecentFile(RecentFileItem item) {
        if (item == null || item.placeholder()) return;

        popupMenu.setVisible(false);
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        List<String> pathsBeforeRemove = settings.getRecentFilePaths();
        boolean removedLatest = !pathsBeforeRemove.isEmpty() && item.path().equals(pathsBeforeRemove.get(0));

        settings.removeRecentFilePath(item.path());
        if (removedLatest) {
            loadNewLatestFile();
        }

        onChange.run();
        refresh();
    }

    private void loadNewLatestFile() {
        List<String> paths = NovelReaderSettings.getInstance().getRecentFilePaths();
        if (paths.isEmpty()) {
            return;
        }

        loadFile(paths.get(0));
    }

    private void loadFile(String path) {
        boolean success = NovelReaderManager.getInstance().loadFile(path);
        if (!success) {
            Messages.showErrorDialog(project,
                    FishToucherBundle.message("settings.dialog.recentFileLoadFailed", path),
                    "Fish Toucher");
        }
    }

    private record RecentFileItem(String path, String label, boolean placeholder) {
        private static RecentFileItem of(String path) {
            String name = new File(path).getName();
            return new RecentFileItem(path, name.isEmpty() ? path : name, false);
        }

        private static RecentFileItem placeholderItem() {
            return new RecentFileItem("", FishToucherBundle.message("settings.recentFiles.empty"), true);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class DropdownArrowIcon implements Icon {
        private static final int WIDTH = 8;
        private static final int HEIGHT = 5;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                Color color = c.isEnabled()
                        ? UIManager.getColor("ComboBox.foreground")
                        : UIManager.getColor("Label.disabledForeground");
                if (color == null) {
                    color = c.getForeground();
                }
                g2.setColor(color != null ? color : Color.DARK_GRAY);

                int centerX = x + WIDTH / 2;
                int centerY = y + HEIGHT / 2;
                Polygon arrow = new Polygon(
                        new int[]{centerX - 4, centerX + 4, centerX},
                        new int[]{centerY - 2, centerY - 2, centerY + 3},
                        3
                );
                g2.fillPolygon(arrow);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return WIDTH;
        }

        @Override
        public int getIconHeight() {
            return HEIGHT;
        }
    }

}
