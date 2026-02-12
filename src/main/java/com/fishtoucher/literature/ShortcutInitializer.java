package com.fishtoucher.literature;

import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.fishtoucher.literature.settings.NovelReaderSettings;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Applies custom keyboard shortcuts from plugin settings to the active keymap on project open.
 */
public class ShortcutInitializer implements ProjectActivity {

    private static final Logger LOG = Logger.getInstance(ShortcutInitializer.class);

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        LOG.info("ShortcutInitializer: applying custom shortcuts on project open");
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        applyShortcut("NovelReader.Open", settings.getShortcutOpen());
        applyShortcut("NovelReader.NextPage", settings.getShortcutNextPage());
        applyShortcut("NovelReader.PrevPage", settings.getShortcutPrevPage());
        applyShortcut("NovelReader.Toggle", settings.getShortcutToggle());
        return Unit.INSTANCE;
    }

    private void applyShortcut(String actionId, String keystrokeStr) {
        if (keystrokeStr == null || keystrokeStr.isEmpty()) {
            LOG.debug("applyShortcut: no shortcut configured for " + actionId);
            return;
        }

        Keymap keymap = KeymapManager.getInstance().getActiveKeymap();

        // Remove all existing shortcuts for this action
        Shortcut[] existing = keymap.getShortcuts(actionId);
        for (Shortcut s : existing) {
            try {
                keymap.removeShortcut(actionId, s);
            } catch (Exception e) {
                LOG.debug("applyShortcut: could not remove existing shortcut for " + actionId + ": " + e.getMessage());
            }
        }

        // Parse and add the configured shortcut
        KeyStroke ks = KeyStroke.getKeyStroke(keystrokeStr.replace("ctrl", "control"));
        if (ks != null) {
            keymap.addShortcut(actionId, new KeyboardShortcut(ks, null));
            LOG.info("applyShortcut: set " + actionId + " -> " + keystrokeStr);
        } else {
            LOG.warn("applyShortcut: failed to parse keystroke '" + keystrokeStr + "' for " + actionId);
        }
    }
}
