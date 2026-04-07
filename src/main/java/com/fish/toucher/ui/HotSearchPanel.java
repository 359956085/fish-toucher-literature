package com.fish.toucher.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.fish.toucher.FishToucherBundle;
import com.fish.toucher.settings.NovelReaderSettings;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.List;

/**
 * Tool window panel for hot search mode.
 * Displays all Baidu hot search titles in a list format.
 * Clicking a title opens it in the default browser.
 */
public class HotSearchPanel extends JPanel {

    private static final Logger LOG = Logger.getInstance(HotSearchPanel.class);
    private final DefaultListModel<HotSearchManager.HotSearchItem> listModel;
    private final JList<HotSearchManager.HotSearchItem> list;
    private final JLabel statusLabel;
    private final Runnable changeListener;
    private int currentIndex = -1;

    public HotSearchPanel(Project project) {
        LOG.info("HotSearchPanel: initializing for project " + project.getName());
        setLayout(new BorderLayout());

        // --- List displaying hot search items ---
        listModel = new DefaultListModel<>();
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(JBColor.background());
        list.setForeground(JBColor.foreground());
        list.setCellRenderer(new HotSearchCellRenderer());
        updateFont();

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int idx = list.locationToIndex(e.getPoint());
                if (idx >= 0 && idx < listModel.size()) {
                    String url = listModel.get(idx).url();
                    openInBrowser(url);
                }
            }
        });

        // Hand cursor on hover
        list.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int idx = list.locationToIndex(e.getPoint());
                if (idx >= 0 && idx < listModel.size() && !listModel.get(idx).url().isEmpty()) {
                    list.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    list.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        // --- Top toolbar: mode switch ---
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()));
        JButton switchModeBtn = new JButton("\uD83D\uDCDA \u9605\u8BFB");
        switchModeBtn.setMargin(new Insets(1, 4, 1, 4));
        switchModeBtn.setFont(switchModeBtn.getFont().deriveFont(12f));
        switchModeBtn.setFocusable(false);
        switchModeBtn.setToolTipText("Switch to Novel Reading mode");
        switchModeBtn.addActionListener(e -> NovelReaderToolWindowFactory.switchMode("novel"));
        topBar.add(switchModeBtn);
        add(topBar, BorderLayout.NORTH);

        // --- Bottom bar ---
        JPanel bottomBar = new JPanel(new BorderLayout(5, 0));
        bottomBar.setBorder(new EmptyBorder(3, 8, 3, 8));

        // Left: source selector + refresh button
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));

        JComboBox<String> sourceCombo = new JComboBox<>(HotSearchManager.getSourceLabels());
        String currentSource = NovelReaderSettings.getInstance().getHotSearchSource();
        for (int i = 0; i < HotSearchManager.SOURCE_VALUES.length; i++) {
            if (HotSearchManager.SOURCE_VALUES[i].equals(currentSource)) {
                sourceCombo.setSelectedIndex(i);
                break;
            }
        }
        sourceCombo.setFocusable(false);
        sourceCombo.setToolTipText(FishToucherBundle.message("hotSearch.tooltip.switchSource"));
        sourceCombo.addActionListener(e -> {
            int idx = sourceCombo.getSelectedIndex();
            if (idx >= 0 && idx < HotSearchManager.SOURCE_VALUES.length) {
                String newSource = HotSearchManager.SOURCE_VALUES[idx];
                NovelReaderSettings.getInstance().setHotSearchSource(newSource);
                HotSearchManager.getInstance().switchSource();
            }
        });
        navPanel.add(sourceCombo);

        JButton refreshBtn = new JButton("\uD83D\uDD04");
        refreshBtn.setMargin(new Insets(1, 4, 1, 4));
        refreshBtn.setFont(refreshBtn.getFont().deriveFont(12f));
        refreshBtn.setFocusable(false);
        refreshBtn.setToolTipText(FishToucherBundle.message("hotSearch.tooltip.refresh"));
        refreshBtn.addActionListener(e -> HotSearchManager.getInstance().manualRefresh());
        navPanel.add(refreshBtn);
        bottomBar.add(navPanel, BorderLayout.WEST);

        // Right: status
        statusLabel = new JLabel("");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        statusLabel.setForeground(JBColor.GRAY);
        bottomBar.add(statusLabel, BorderLayout.EAST);

        add(bottomBar, BorderLayout.SOUTH);

        // Listen for changes
        changeListener = this::refreshContent;
        HotSearchManager manager = HotSearchManager.getInstance();
        manager.addChangeListener(changeListener);

        // Ensure manager is running and trigger a fresh fetch
        if (!manager.isRunning()) {
            manager.start();
        } else if (!manager.hasContent()) {
            manager.manualRefresh();
        }

        refreshContent();
    }

    public void dispose() {
        HotSearchManager.getInstance().removeChangeListener(changeListener);
    }

    private void updateFont() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        list.setFont(new Font(settings.getFontFamily(), Font.PLAIN, settings.getFontSize()));
    }

    private void refreshContent() {
        HotSearchManager manager = HotSearchManager.getInstance();

        if (!manager.hasContent()) {
            listModel.clear();
            statusLabel.setText(FishToucherBundle.message("hotSearch.status.loading"));
            return;
        }

        updateFont();

        List<HotSearchManager.HotSearchItem> items = manager.getAllItems();
        currentIndex = manager.getCurrentIndex();

        listModel.clear();
        for (HotSearchManager.HotSearchItem item : items) {
            listModel.addElement(item);
        }

        list.repaint();

        String refreshTime = manager.getLastRefreshTime();
        String sourceLabel = HotSearchManager.getSourceLabel(manager.getCurrentSource());
        statusLabel.setText(FishToucherBundle.message("hotSearch.status.format", sourceLabel, items.size(), refreshTime));
    }

    private void openInBrowser(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            LOG.warn("openInBrowser: failed to open URL: " + e.getMessage());
        }
    }

    /**
     * Custom cell renderer that highlights the current carousel item.
     */
    private class HotSearchCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, false, false);
            if (value instanceof HotSearchManager.HotSearchItem item) {
                String prefix = (index == currentIndex) ? "\u25B6 " : "   ";
                String rankStr = item.rank() > 0 ? String.valueOf(item.rank()) : "TOP";
                setText(String.format("%s[%3s] %s", prefix, rankStr, item.word()));
                setBorder(new EmptyBorder(4, 8, 4, 8));
                setBackground(JBColor.background());
                setForeground(JBColor.foreground());
                if (isSelected) {
                    setBackground(JBColor.background().brighter());
                }
            }
            return this;
        }
    }
}
