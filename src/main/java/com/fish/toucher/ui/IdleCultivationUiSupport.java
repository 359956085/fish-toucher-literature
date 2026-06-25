package com.fish.toucher.ui;

import com.fish.toucher.FishToucherBundle;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicHTML;
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
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
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
        allowHorizontalShrink(label);
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
        allowHorizontalShrink(textArea);
        return textArea;
    }

    static JLabel createGuideHtmlLabel(String text) {
        return new GuideHtmlLabel(text);
    }

    static void addLabelRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent value) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0; gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel labelComponent = new JLabel(label);
        labelComponent.setForeground(JBColor.GRAY);
        allowHorizontalShrink(labelComponent);
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
        JLabel title = createSectionLabel(FishToucherBundle.message("cultivation.guide." + sectionId + ".title"));
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(title, gbc);

        JLabel text = createGuideHtmlLabel(FishToucherBundle.message("cultivation.guide." + sectionId + ".desc"));
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(text, gbc);
        return row;
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
        Dimension minimumSize = component.getMinimumSize();
        Dimension preferredSize = component.getPreferredSize();
        component.setMinimumSize(new Dimension(0, Math.max(minimumSize.height, preferredSize.height)));
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
            int width = resolvePreferredWidth();
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
            return new Dimension(0, getPreferredSize().height);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            int oldWidth = getWidth();
            super.setBounds(x, y, width, height);
            if (oldWidth != width) {
                revalidate();
            }
        }

        private int resolvePreferredWidth() {
            int width = getWidth();
            if (width >= MIN_EFFECTIVE_WRAP_WIDTH) {
                return width;
            }

            Container parent = getParent();
            while (parent != null) {
                int parentWidth = parent.getWidth();
                if (parentWidth > 0) {
                    Insets insets = parent.getInsets();
                    int availableWidth = parentWidth - Math.max(0, getX()) - insets.right;
                    if (availableWidth >= MIN_EFFECTIVE_WRAP_WIDTH) {
                        return availableWidth;
                    }
                }
                parent = parent.getParent();
            }
            return CULTIVATION_MIN_WIDTH;
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
            int width = resolvePreferredWidth();
            if (!getLineWrap()) {
                Dimension preferredSize = super.getPreferredSize();
                return new Dimension(width, preferredSize.height);
            }
            return new Dimension(width, calculateWrappedHeight(width));
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(0, getPreferredSize().height);
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

        private int resolvePreferredWidth() {
            int width = getWidth();
            if (width >= MIN_EFFECTIVE_WRAP_WIDTH) {
                return width;
            }

            int availableWidth = resolveAvailableWidth();
            if (availableWidth >= MIN_EFFECTIVE_WRAP_WIDTH) {
                return availableWidth;
            }

            return CULTIVATION_MIN_WIDTH;
        }

        private int resolveAvailableWidth() {
            Container parent = getParent();
            while (parent != null) {
                int parentWidth = parent.getWidth();
                if (parentWidth > 0) {
                    Insets insets = parent.getInsets();
                    int availableWidth = parentWidth - Math.max(0, getX()) - insets.right;
                    return Math.max(1, availableWidth);
                }
                parent = parent.getParent();
            }
            return 0;
        }
    }
}
