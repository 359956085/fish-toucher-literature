package com.fish.toucher;

import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.keymap.Keymap;

import javax.swing.*;
import java.util.Arrays;

/**
 * 插件托管快捷键更新逻辑。只替换已知旧绑定，不删除用户额外绑定。
 *
 * @author fengshi
 */
public final class ShortcutBindingSupport {

    private ShortcutBindingSupport() {}

    public static boolean apply(
            Keymap keymap,
            String actionId,
            String oldKeystroke,
            String newKeystroke
    ) {
        KeyboardShortcut newShortcut = parse(newKeystroke);
        if (newKeystroke != null && !newKeystroke.isEmpty() && newShortcut == null) {
            return false;
        }

        KeyboardShortcut oldShortcut = parse(oldKeystroke);
        if (oldShortcut != null && !oldShortcut.equals(newShortcut)) {
            keymap.removeShortcut(actionId, oldShortcut);
        }
        if (newShortcut != null && !contains(keymap.getShortcuts(actionId), newShortcut)) {
            keymap.addShortcut(actionId, newShortcut);
        }
        return true;
    }

    private static KeyboardShortcut parse(String keystroke) {
        if (keystroke == null || keystroke.isEmpty()) {
            return null;
        }
        KeyStroke parsed = KeyStroke.getKeyStroke(keystroke.replace("ctrl", "control"));
        return parsed == null ? null : new KeyboardShortcut(parsed, null);
    }

    private static boolean contains(Shortcut[] shortcuts, KeyboardShortcut expected) {
        return Arrays.asList(shortcuts).contains(expected);
    }
}
