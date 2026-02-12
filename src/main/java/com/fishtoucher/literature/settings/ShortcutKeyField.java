package com.fishtoucher.literature.settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * A text field that captures keyboard shortcut input.
 * When focused and a key combination is pressed, it displays the shortcut description
 * (e.g., "Ctrl+Shift+Alt+M") and stores the corresponding KeyStroke string.
 */
public class ShortcutKeyField extends JTextField {

    private String keystrokeString = "";

    public ShortcutKeyField(String initialKeystroke) {
        super(20);
        setEditable(false);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

        if (initialKeystroke != null && !initialKeystroke.isEmpty()) {
            this.keystrokeString = initialKeystroke;
            setText(toDisplayString(initialKeystroke));
        }

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                // Ignore standalone modifier keys
                if (keyCode == KeyEvent.VK_CONTROL || keyCode == KeyEvent.VK_SHIFT
                        || keyCode == KeyEvent.VK_ALT || keyCode == KeyEvent.VK_META) {
                    return;
                }

                // Escape clears the shortcut
                if (keyCode == KeyEvent.VK_ESCAPE) {
                    keystrokeString = "";
                    setText("");
                    e.consume();
                    return;
                }

                int modifiers = e.getModifiersEx();
                KeyStroke ks = KeyStroke.getKeyStroke(keyCode, modifiers);
                keystrokeString = keystrokeToString(ks);
                setText(toDisplayString(keystrokeString));
                e.consume();
            }
        });
    }

    /**
     * Get the keystroke string in IntelliJ format (e.g., "ctrl shift alt M").
     */
    public String getKeystrokeString() {
        return keystrokeString;
    }

    /**
     * Set the keystroke string and update display.
     */
    public void setKeystrokeString(String keystroke) {
        this.keystrokeString = keystroke != null ? keystroke : "";
        setText(toDisplayString(this.keystrokeString));
    }

    /**
     * Convert a KeyStroke to IntelliJ-compatible string format.
     * Example: "ctrl shift alt M", "alt shift LEFT"
     */
    private static String keystrokeToString(KeyStroke ks) {
        StringBuilder sb = new StringBuilder();
        int modifiers = ks.getModifiers();
        if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) sb.append("ctrl ");
        if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) sb.append("shift ");
        if ((modifiers & KeyEvent.ALT_DOWN_MASK) != 0) sb.append("alt ");
        if ((modifiers & KeyEvent.META_DOWN_MASK) != 0) sb.append("meta ");
        sb.append(KeyEvent.getKeyText(ks.getKeyCode()).toUpperCase().replace(" ", "_"));
        return sb.toString().trim();
    }

    /**
     * Convert IntelliJ keystroke string to user-friendly display format.
     * Example: "ctrl shift alt M" -> "Ctrl+Shift+Alt+M"
     */
    static String toDisplayString(String keystrokeStr) {
        if (keystrokeStr == null || keystrokeStr.isEmpty()) return "";
        String[] parts = keystrokeStr.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append("+");
            // Capitalize first letter
            if (part.length() > 0) {
                sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}
