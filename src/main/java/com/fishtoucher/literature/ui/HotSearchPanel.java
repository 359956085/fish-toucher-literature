package com.fishtoucher.literature.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.fishtoucher.literature.settings.NovelReaderSettings;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Tool window panel for hot search mode.
 * Displays all Baidu hot search titles in a list format.
 */
public class HotSearchPanel extends JPanel {

    private static final Logger LOG = Logger.getInstance(HotSearchPanel.class);
    private final JTextArea textArea;
    private final JLabel statusLabel;
    private final Runnable changeListener;

    public HotSearchPanel(Project project) {
        LOG.info("HotSearchPanel: initializing for project " + project.getName());
        setLayout(new BorderLayout());

        // --- Text area styled as console/log output ---
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(JBColor.background());
        textArea.setForeground(JBColor.foreground());
        textArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        updateFont();

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        // --- Bottom bar ---
        JPanel bottomBar = new JPanel(new BorderLayout(5, 0));
        bottomBar.setBorder(new EmptyBorder(3, 8, 3, 8));

        // Left: refresh button
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        JButton refreshBtn = new JButton("\uD83D\uDD04");
        refreshBtn.setMargin(new Insets(1, 4, 1, 4));
        refreshBtn.setFont(refreshBtn.getFont().deriveFont(12f));
        refreshBtn.setFocusable(false);
        refreshBtn.setToolTipText("Refresh hot search");
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
        HotSearchManager.getInstance().addChangeListener(changeListener);

        // Ensure manager is running
        HotSearchManager.getInstance().start();

        refreshContent();
    }

    public void dispose() {
        HotSearchManager.getInstance().removeChangeListener(changeListener);
    }

    private void updateFont() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        textArea.setFont(new Font(settings.getFontFamily(), Font.PLAIN, settings.getFontSize()));
    }

    private void refreshContent() {
        HotSearchManager manager = HotSearchManager.getInstance();

        if (!manager.hasContent()) {
            textArea.setText("  [INFO] Loading Baidu hot search...\n");
            statusLabel.setText("");
            return;
        }

        updateFont();

        List<HotSearchManager.HotSearchItem> items = manager.getAllItems();
        int currentIdx = manager.getCurrentIndex();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            HotSearchManager.HotSearchItem item = items.get(i);
            String prefix = (i == currentIdx) ? "  \u25B6 " : "    ";
            String rankStr = item.rank() > 0 ? String.valueOf(item.rank()) : "TOP";
            sb.append(String.format("%s[%3s] %s\n", prefix, rankStr, item.word()));
        }

        textArea.setText(sb.toString());

        String refreshTime = manager.getLastRefreshTime();
        statusLabel.setText(items.size() + " items | Updated " + refreshTime);
    }
}
