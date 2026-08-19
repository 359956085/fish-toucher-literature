package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.text.View;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

final class IdleCultivationUiSupport {

    static final int CULTIVATION_MIN_WIDTH = 200;
    private static final int SCROLL_UNIT_INCREMENT = 16;
    private static final int MIN_EFFECTIVE_WRAP_WIDTH = 64;
    private static final String OUTER_SCROLL_PANE_PROPERTY = "cultivation.outerScrollPane";

    private IdleCultivationUiSupport() {
    }

    static JPanel createFormPanel() {
        JPanel panel = new CultivationFormPanel();
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        return panel;
    }

    static JPanel createScrollableTab(JPanel contentPanel) {
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.putClientProperty(OUTER_SCROLL_PANE_PROPERTY, Boolean.TRUE);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setMinimumSize(new Dimension(CULTIVATION_MIN_WIDTH, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JViewport viewport = scrollPane.getViewport();
        int[] lastViewportWidth = {-1};
        viewport.addChangeListener(e -> {
            int viewportWidth = viewport.getExtentSize().width;
            if (viewportWidth != lastViewportWidth[0]) {
                lastViewportWidth[0] = viewportWidth;
                contentPanel.revalidate();
                contentPanel.repaint();
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.setMinimumSize(new Dimension(CULTIVATION_MIN_WIDTH, 0));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    static JPanel createActionPanel() {
        return new FlowActionPanel(FlowLayout.LEFT);
    }

    static JPanel createRightActionPanel() {
        return new FlowActionPanel(FlowLayout.RIGHT);
    }

    /**
     * 流式操作面板。最小高度跟随内容（=首选高度）：
     * GridBagLayout 在容器比首选尺寸窄时会走 MINSIZE 分支、按各组件的“最小尺寸”布局，
     * 最小高度为 0 的行会被整体 setBounds(0,0,0,0) 卸载（弟子操作行曾因此消失）。
     * 保持最小高度 = 首选高度后，MINSIZE 分支与 PREFERRED 分支渲染一致，不再卸载。
     */
    private static class FlowActionPanel extends JPanel {
        private FlowActionPanel(int alignment) {
            super(new FlowLayout(alignment, 6, 0));
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(0, getPreferredSize().height);
        }
    }

    static JPanel createInlineActionPanel(JComponent content, JComponent actions) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        allowHorizontalShrink(content);
        allowHorizontalShrink(actions);
        panel.add(content, BorderLayout.CENTER);
        panel.add(actions, BorderLayout.EAST);
        allowHorizontalShrink(panel);
        return panel;
    }

    static GridBagConstraints createConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 4, 5, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    static int addFullWidthRow(JPanel panel, GridBagConstraints gbc, int row, JComponent component) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        allowHorizontalShrink(component);
        panel.add(component, gbc);
        return row + 1;
    }

    static int addSeparatorRow(JPanel panel, GridBagConstraints gbc, int row) {
        return addFullWidthRow(panel, gbc, row, new JSeparator());
    }

    static int addActionRow(JPanel panel, GridBagConstraints gbc, int row, JComponent actions) {
        return addFullWidthRow(panel, gbc, row, actions);
    }

    static JTextArea createHintTextArea() {
        return createGuideTextArea("");
    }

    static JTextArea createHintTextArea(String text) {
        JTextArea textArea = createHintTextArea();
        setWrappingText(textArea, text);
        return textArea;
    }

    static JTextArea createChallengeInfoTextArea(boolean hintStyle, int rows) {
        JTextArea textArea = createGuideTextArea("");
        textArea.setRows(rows);
        if (!hintStyle) {
            textArea.setForeground(UIManager.getColor("Label.foreground"));
        }
        Dimension minimumSize = textArea.getMinimumSize();
        textArea.setMinimumSize(new Dimension(0, minimumSize.height));
        return textArea;
    }

    static JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        return label;
    }

    static JTextArea createSectionTextArea(String text) {
        JTextArea textArea = createGuideTextArea(text);
        textArea.setForeground(UIManager.getColor("Label.foreground"));
        textArea.setFont(textArea.getFont().deriveFont(Font.BOLD, 12f));
        return textArea;
    }

    static JTextArea createGuideTextArea(String text) {
        JTextArea textArea = new WrappingTextArea(text);
        textArea.setForeground(UIManager.getColor("Label.foreground"));
        allowHorizontalShrink(textArea);
        return textArea;
    }

    static JLabel createGuideHtmlLabel(String text) {
        return new GuideHtmlLabel(text);
    }

    static JProgressBar createReadableProgressBar() {
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setUI(new ReadableProgressBarUi());
        return progressBar;
    }

    static void addLabelRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent value) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0; gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel labelComponent = new JLabel(label);
        labelComponent.setForeground(UIManager.getColor("Label.foreground"));
        panel.add(labelComponent, gbc);

        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1.0; gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        allowHorizontalShrink(value);
        panel.add(value, gbc);
        gbc.weightx = 0;
    }

    static void addBottomGlue(JPanel panel, GridBagConstraints gbc, int row) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);
        gbc.weighty = 0;
    }

    static int addGuideSection(JPanel panel, GridBagConstraints gbc, int row, String sectionId) {
        row = addFullWidthRow(panel, gbc, row, createSectionLabel(FishToucherBundle.message("cultivation.guide." + sectionId + ".title")));
        return addFullWidthRow(panel, gbc, row, createGuideHtmlLabel(FishToucherBundle.message("cultivation.guide." + sectionId + ".desc")));
    }

    static int addRealmDescriptionRows(JPanel panel, GridBagConstraints gbc, int row) {
        for (int i = 0; i <= 8; i++) {
            String label = FishToucherBundle.message("cultivation.realm." + i) + ":";
            JLabel description = createGuideHtmlLabel(FishToucherBundle.message("cultivation.realm." + i + ".desc"));
            addLabelRow(panel, gbc, row++, label, description);
        }
        return row;
    }

    static void allowHorizontalShrink(JComponent component) {
        if (component instanceof WrappingTextArea || component instanceof JLabel) {
            return;
        }
        if (component.isMinimumSizeSet()) {
            // 已显式设置最小尺寸（如子容器的最小宽度）时不覆盖，避免后续被 GridBagLayout 压成 0 宽而消失。
            return;
        }
        Dimension minimumSize = component.getMinimumSize();
        Dimension preferredSize = component.getPreferredSize();
        component.setMinimumSize(new Dimension(0, Math.max(minimumSize.height, preferredSize.height)));
    }

    static void setLabelTextIfChanged(JLabel label, String text) {
        String normalizedText = text != null ? text : "";
        if (!normalizedText.equals(label.getText())) {
            label.setText(normalizedText);
        }
    }

    static void setButtonTextIfChanged(AbstractButton button, String text) {
        String normalizedText = text != null ? text : "";
        if (!normalizedText.equals(button.getText())) {
            button.setText(normalizedText);
        }
    }

    static void setButtonEnabledWithReason(AbstractButton button, boolean enabled, String enabledTooltip, String disabledReason) {
        button.setEnabled(enabled);
        button.setToolTipText(enabled ? enabledTooltip : disabledReason);
    }

    static void setProgressTextIfChanged(JProgressBar progressBar, int value, String text) {
        String normalizedText = text != null ? text : "";
        if (progressBar.getValue() != value) {
            progressBar.setValue(value);
        }
        if (!normalizedText.equals(progressBar.getString())) {
            progressBar.setString(normalizedText);
        }
    }

    static void setWrappingText(JTextArea textArea, String text) {
        String normalizedText = text != null ? text : "";
        if (normalizedText.equals(textArea.getText())) {
            return;
        }
        textArea.setText(normalizedText);
        textArea.revalidate();
        textArea.repaint();
        Container parent = textArea.getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }

    static void preserveOuterScrollPositions(JComponent root, Runnable action) {
        List<ScrollPosition> positions = new ArrayList<>();
        collectOuterScrollPositions(root, positions);
        try {
            action.run();
        } finally {
            if (!positions.isEmpty()) {
                SwingUtilities.invokeLater(() -> restoreOuterScrollPositions(positions));
            }
        }
    }

    private static void collectOuterScrollPositions(Component component, List<ScrollPosition> positions) {
        if (component instanceof JScrollPane scrollPane
                && Boolean.TRUE.equals(scrollPane.getClientProperty(OUTER_SCROLL_PANE_PROPERTY))) {
            positions.add(new ScrollPosition(scrollPane, new Point(scrollPane.getViewport().getViewPosition())));
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectOuterScrollPositions(child, positions);
            }
        }
    }

    private static void restoreOuterScrollPositions(List<ScrollPosition> positions) {
        for (ScrollPosition position : positions) {
            JViewport viewport = position.scrollPane().getViewport();
            Component view = viewport.getView();
            if (view == null) {
                continue;
            }
            Dimension viewSize = view.getSize();
            Dimension extentSize = viewport.getExtentSize();
            int x = clamp(position.viewPosition().x, 0, Math.max(0, viewSize.width - extentSize.width));
            int y = clamp(position.viewPosition().y, 0, Math.max(0, viewSize.height - extentSize.height));
            viewport.setViewPosition(new Point(x, y));
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int resolvePreferredWidth(Component component) {
        int width = component.getWidth();
        if (width >= MIN_EFFECTIVE_WRAP_WIDTH) {
            return width;
        }
        // 未布局或宽度未知时用固定最小宽度，避免从祖先容器推算宽度导致首选宽度超宽、
        // 进而被 GridBagLayout 压缩塌缩（弟子列表曾因此整体 0x0）。
        return CULTIVATION_MIN_WIDTH;
    }

    private static String toGuideHtml(String text) {
        String escapedText = escapeHtml(text != null ? text : "")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\n", "<br>");
        return "<html><body style=\"margin:0; padding:0;\">" + escapedText + "</body></html>";
    }

    private static String escapeHtml(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '&' -> builder.append("&amp;");
                case '<' -> builder.append("&lt;");
                case '>' -> builder.append("&gt;");
                case '"' -> builder.append("&quot;");
                default -> builder.append(ch);
            }
        }
        return builder.toString();
    }

    private record ScrollPosition(JScrollPane scrollPane, Point viewPosition) {
    }

    private static class ReadableProgressBarUi extends BasicProgressBarUI {
        @Override
        protected void paintString(Graphics g,
                                   int x,
                                   int y,
                                   int width,
                                   int height,
                                   int amountFull,
                                   Insets b) {
            String text = progressBar.getString();
            if (text == null || text.isEmpty()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setFont(progressBar.getFont());
                FontMetrics metrics = g2.getFontMetrics();
                int textWidth = metrics.stringWidth(text);
                int textX = x + Math.round((width - textWidth) / 2f);
                int textY = y + Math.round((height + metrics.getAscent() - metrics.getDescent()) / 2f);
                if (progressBar.getOrientation() != JProgressBar.HORIZONTAL) {
                    g2.setColor(normalProgressTextColor());
                    g2.drawString(text, textX, textY);
                    return;
                }

                // 深色主题下原生进度条字符串可能落在低对比色上，这里按填充区分别绘制。
                Shape oldClip = g2.getClip();
                int filledWidth = Math.max(0, Math.min(width, amountFull));
                g2.setClip(x, y, filledWidth, height);
                g2.setColor(filledProgressTextColor());
                g2.drawString(text, textX, textY);

                g2.setClip(x + filledWidth, y, Math.max(0, width - filledWidth), height);
                g2.setColor(normalProgressTextColor());
                g2.drawString(text, textX, textY);
                g2.setClip(oldClip);
            } finally {
                g2.dispose();
            }
        }

        private Color normalProgressTextColor() {
            Color color = UIManager.getColor("Label.foreground");
            return color != null ? color : new JBColor(Color.BLACK, Color.WHITE);
        }

        private Color filledProgressTextColor() {
            Color color = UIManager.getColor("ProgressBar.selectionForeground");
            return color != null ? color : new JBColor(Color.WHITE, Color.WHITE);
        }
    }

    private static class CultivationFormPanel extends JPanel implements Scrollable {
        private CultivationFormPanel() {
            super(new GridBagLayout());
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension preferredSize = super.getPreferredSize();
            return new Dimension(CULTIVATION_MIN_WIDTH, preferredSize.height);
        }

        @Override
        public Dimension getMinimumSize() {
            Dimension minimumSize = super.getMinimumSize();
            return new Dimension(CULTIVATION_MIN_WIDTH, minimumSize.height);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return SCROLL_UNIT_INCREMENT;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(SCROLL_UNIT_INCREMENT, visibleRect.height - SCROLL_UNIT_INCREMENT);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static class GuideHtmlLabel extends JLabel {
        private GuideHtmlLabel(String text) {
            super(toGuideHtml(text));
            setForeground(JBColor.GRAY);
            setVerticalAlignment(SwingConstants.TOP);
        }

        @Override
        public Dimension getPreferredSize() {
            int width = IdleCultivationUiSupport.resolvePreferredWidth(this);
            View view = (View) getClientProperty(BasicHTML.propertyKey);
            if (view == null) {
                Dimension preferredSize = super.getPreferredSize();
                return new Dimension(width, preferredSize.height);
            }
            view.setSize(width, 0);
            int height = (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS));
            return new Dimension(width, height);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(CULTIVATION_MIN_WIDTH, getPreferredSize().height);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            int oldWidth = getWidth();
            super.setBounds(x, y, width, height);
            if (oldWidth != width) {
                revalidate();
            }
        }
    }

    private static class WrappingTextArea extends JTextArea {
        private WrappingTextArea(String text) {
            super(text != null ? text : "");
            setColumns(0);
            setEditable(false);
            setFocusable(false);
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(false);
            setForeground(JBColor.GRAY);
            setFont(UIManager.getFont("Label.font"));
            setBorder(BorderFactory.createEmptyBorder());
        }

        @Override
        public Dimension getPreferredSize() {
            int width = IdleCultivationUiSupport.resolvePreferredWidth(this);
            if (!getLineWrap()) {
                Dimension preferredSize = super.getPreferredSize();
                return new Dimension(width, preferredSize.height);
            }
            return new Dimension(width, calculateWrappedHeight(width));
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(CULTIVATION_MIN_WIDTH, getPreferredSize().height);
        }

        private int calculateWrappedHeight(int width) {
            Insets insets = getInsets();
            FontMetrics metrics = getFontMetrics(getFont());
            int contentWidth = Math.max(1, width - insets.left - insets.right);
            int rows = calculateWrappedRows(getText(), metrics, contentWidth);
            if (getRows() > 0) {
                rows = Math.max(rows, getRows());
            }
            return insets.top + insets.bottom + rows * metrics.getHeight();
        }

        private int calculateWrappedRows(String text, FontMetrics metrics, int contentWidth) {
            if (text == null || text.isEmpty()) {
                return getRows() > 0 ? getRows() : 0;
            }
            int rows = 0;
            for (String paragraph : text.split("\\R", -1)) {
                rows += Math.max(1, calculateParagraphRows(paragraph, metrics, contentWidth));
            }
            return rows;
        }

        private int calculateParagraphRows(String text, FontMetrics metrics, int contentWidth) {
            if (text.isEmpty()) {
                return 1;
            }
            int rows = 1;
            int lineWidth = 0;
            for (int offset = 0; offset < text.length(); ) {
                int codePoint = text.codePointAt(offset);
                int charWidth = metrics.stringWidth(new String(Character.toChars(codePoint)));
                if (lineWidth > 0 && lineWidth + charWidth > contentWidth) {
                    rows++;
                    lineWidth = 0;
                }
                lineWidth += charWidth;
                offset += Character.charCount(codePoint);
            }
            return rows;
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            int oldWidth = getWidth();
            super.setBounds(x, y, width, height);
            if (oldWidth != width) {
                revalidate();
            }
        }
    }
}
