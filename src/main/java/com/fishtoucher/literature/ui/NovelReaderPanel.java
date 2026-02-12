package com.fishtoucher.literature.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.fishtoucher.literature.settings.NovelReaderSettings;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A reading panel disguised as a build/log console output.
 * The novel content is displayed in a monospaced font with line-number-like prefixes
 * to make it look like log output.
 */
public class NovelReaderPanel extends JPanel {

    private static final Logger LOG = Logger.getInstance(NovelReaderPanel.class);
    private final Project project;
    private final JTextArea textArea;
    private final JLabel statusLabel;
    private final JSlider progressSlider;

    public NovelReaderPanel(Project project) {
        LOG.info("NovelReaderPanel: initializing for project " + project.getName());
        this.project = project;
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

        // --- Bottom bar with controls ---
        JPanel bottomBar = new JPanel(new BorderLayout(5, 0));
        bottomBar.setBorder(new EmptyBorder(3, 8, 3, 8));

        // Left: navigation buttons (styled as tool buttons)
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));

        JButton openBtn = createSmallButton("📂");
        openBtn.setToolTipText("Open novel file (Alt+Shift+N)");
        openBtn.addActionListener(e -> openFile());

        JButton prevBtn = createSmallButton("◀");
        prevBtn.setToolTipText("Previous page (Alt+Shift+←)");
        prevBtn.addActionListener(e -> {
            NovelReaderManager.getInstance().prevPage();
        });

        JButton nextBtn = createSmallButton("▶");
        nextBtn.setToolTipText("Next page (Alt+Shift+→)");
        nextBtn.addActionListener(e -> {
            NovelReaderManager.getInstance().nextPage();
        });

        navPanel.add(openBtn);
        navPanel.add(prevBtn);
        navPanel.add(nextBtn);
        bottomBar.add(navPanel, BorderLayout.WEST);

        // Center: progress slider
        progressSlider = new JSlider(0, 100, 0);
        progressSlider.setPreferredSize(new Dimension(200, 20));
        progressSlider.setToolTipText("Drag to jump to position");
        progressSlider.addChangeListener(e -> {
            if (progressSlider.getValueIsAdjusting()) return;
            NovelReaderManager manager = NovelReaderManager.getInstance();
            if (manager.hasContent()) {
                manager.jumpToPercent(progressSlider.getValue());
            }
        });
        bottomBar.add(progressSlider, BorderLayout.CENTER);

        // Right: status
        statusLabel = new JLabel("");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        statusLabel.setForeground(JBColor.GRAY);
        bottomBar.add(statusLabel, BorderLayout.EAST);

        add(bottomBar, BorderLayout.SOUTH);

        // --- Double-click to open file ---
        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !NovelReaderManager.getInstance().hasContent()) {
                    openFile();
                }
            }
        });

        // Listen for changes
        NovelReaderManager.getInstance().addChangeListener(this::refreshContent);

        // Initial content
        refreshContent();
    }

    private JButton createSmallButton(String text) {
        JButton btn = new JButton(text);
        btn.setMargin(new Insets(1, 4, 1, 4));
        btn.setFont(btn.getFont().deriveFont(12f));
        btn.setFocusable(false);
        return btn;
    }

    private void updateFont() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        textArea.setFont(new Font(settings.getFontFamily(), Font.PLAIN, settings.getFontSize()));
    }

    private void refreshContent() {
        LOG.debug("refreshContent: updating panel content");
        NovelReaderManager manager = NovelReaderManager.getInstance();

        if (!manager.hasContent()) {
            textArea.setText("  [INFO] Waiting for input... Double-click or press Alt+Shift+N to load file.\n");
            statusLabel.setText("");
            progressSlider.setValue(0);
            return;
        }

        if (!manager.isVisible()) {
            textArea.setText("  [INFO] Build completed successfully.\n  [INFO] Process finished with exit code 0\n");
            statusLabel.setText("Hidden");
            return;
        }

        updateFont();

        // Format content to look like log/build output
        StringBuilder sb = new StringBuilder();
        java.util.List<String> pageLines = manager.getCurrentPageDisplayLines();
        int lineNum = manager.getCurrentLine();
        for (int i = 0; i < pageLines.size(); i++) {
            // Prefix with fake timestamp + line number to look like log output
            sb.append(String.format("  %s\n", pageLines.get(i)));
            if (i < pageLines.size() - 1) {
                sb.append("\n");
            }
        }

        textArea.setText(sb.toString());
        textArea.setCaretPosition(0);

        statusLabel.setText(manager.getStatusText());

        // Update slider without triggering listener
        int percent = manager.getTotalLines() > 0
                ? (int) ((long) manager.getCurrentLine() * 100 / manager.getTotalLines())
                : 0;
        progressSlider.setValue(percent);
    }

    private void openFile() {
        LOG.info("openFile: opening file chooser dialog");
        VirtualFile[] files = FileChooser.chooseFiles(
                FileChooserDescriptorFactory.createSingleFileDescriptor("txt"),
                project, null);
        if (files.length > 0) {
            LOG.info("openFile: user selected file: " + files[0].getPath());
            NovelReaderManager.getInstance().loadFile(files[0].getPath());
        } else {
            LOG.info("openFile: user cancelled file selection");
        }
    }
}
