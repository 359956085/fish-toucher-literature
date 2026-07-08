package com.fish.toucher;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.fish.toucher.settings.NovelReaderSettings;
import com.fish.toucher.ui.HotSearchManager;
import com.fish.toucher.ui.IdleCultivationManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import javax.swing.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Applies custom keyboard shortcuts from plugin settings to the active keymap on project open.
 * Also shows a notification when the plugin is first installed or updated.
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

        checkFirstInstallOrUpdate(project, settings);

        // Auto-start HotSearchManager if in hot search mode
        if (settings.isHotSearchMode()) {
            HotSearchManager.getInstance().start();
        } else if (settings.isCultivationMode()) {
            IdleCultivationManager.getInstance().start();
        }

        return Unit.INSTANCE;
    }

    private void checkFirstInstallOrUpdate(@NotNull Project project, @NotNull NovelReaderSettings settings) {
        String currentVersion = getPluginVersion();
        if (currentVersion == null) {
            return;
        }

        String installedVersion = settings.getInstalledVersion();
        if (currentVersion.equals(installedVersion)) {
            return;
        }

        boolean isFirstInstall = installedVersion == null || installedVersion.isEmpty();
        settings.setInstalledVersion(currentVersion);

        ApplicationManager.getApplication().invokeLater(() -> {
            String title;
            String content;
            if (isFirstInstall) {
                title = "Fish Toucher Installed Successfully";
                content = "Plugin v" + currentVersion + " has been installed. Please restart the IDE for the best experience.\n"
                        + "Settings: Settings → Tools → Fish Toucher";
            } else {
                title = "Fish Toucher Updated Successfully";
                content = "Plugin has been updated to v" + currentVersion + ". Please restart the IDE to apply all changes.";
            }

            NotificationGroupManager.getInstance()
                    .getNotificationGroup("Fish Toucher")
                    .createNotification(title, content, NotificationType.INFORMATION)
                    .notify(project);
        });
    }

    @Nullable
    private String getPluginVersion() {
        try (InputStream inputStream = ShortcutInitializer.class.getResourceAsStream("/META-INF/plugin.xml")) {
            if (inputStream == null) {
                return null;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder().parse(inputStream);
            NodeList versions = document.getElementsByTagName("version");
            if (versions.getLength() == 0) {
                return null;
            }

            String version = versions.item(0).getTextContent().trim();
            return version.isEmpty() ? null : version;
        } catch (Exception e) {
            LOG.warn("getPluginVersion: failed to read plugin.xml version", e);
            return null;
        }
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
