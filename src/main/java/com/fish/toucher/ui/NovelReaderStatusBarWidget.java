package com.fish.toucher.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import com.fish.toucher.settings.NovelReaderSettings;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.net.URI;

/**
 * Stealth mode: status bar widget that shows novel content in a single line.
 * Clicking advances to the next line. Has its own independent reading progress.
 */
public class NovelReaderStatusBarWidget implements StatusBarWidget, StatusBarWidget.TextPresentation {

    private static final Logger LOG = Logger.getInstance(NovelReaderStatusBarWidget.class);
    private final Project project;
    private StatusBar statusBar;
    private final Runnable changeListener;

    private final Runnable hotSearchChangeListener;
    private final Runnable cultivationChangeListener;

    public NovelReaderStatusBarWidget(@NotNull Project project) {
        LOG.info("NovelReaderStatusBarWidget: creating for project " + project.getName());
        this.project = project;
        this.changeListener = () -> {
            if (statusBar != null) {
                statusBar.updateWidget(ID());
            }
        };
        this.hotSearchChangeListener = () -> {
            if (statusBar != null) {
                statusBar.updateWidget(ID());
            }
        };
        this.cultivationChangeListener = () -> {
            if (statusBar != null) {
                statusBar.updateWidget(ID());
            }
        };
        NovelReaderManager.getInstance().addChangeListener(changeListener);
        HotSearchManager.getInstance().addChangeListener(hotSearchChangeListener);
        IdleCultivationManager.getInstance().addChangeListener(cultivationChangeListener);
    }

    @Override
    public @NonNls @NotNull String ID() {
        return "NovelReaderWidget";
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        LOG.info("install: status bar widget installed");
        this.statusBar = statusBar;
        // Force an initial update so the widget shows current state immediately
        statusBar.updateWidget(ID());
    }

    @Override
    public void dispose() {
        LOG.info("dispose: status bar widget disposed");
        NovelReaderManager.getInstance().removeChangeListener(changeListener);
        HotSearchManager.getInstance().removeChangeListener(hotSearchChangeListener);
        IdleCultivationManager.getInstance().removeChangeListener(cultivationChangeListener);
    }

    @Override
    public @NotNull StatusBarWidget.WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    public @NotNull String getText() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();

        if (settings.isHotSearchMode()) {
            HotSearchManager manager = HotSearchManager.getInstance();
            if (!manager.hasContent()) return "\uD83D\uDD25 [Loading...]";
            String title = manager.getCurrentTitle();
            String status = manager.getCurrentStatusText();
            return "\uD83D\uDD25 " + title + "  " + status;
        }

        if (settings.isCultivationMode()) {
            return "\u262F " + IdleCultivationManager.getInstance().getStatusLine();
        }

        if (!settings.isShowInStatusBar()) return "";

        NovelReaderManager manager = NovelReaderManager.getInstance();
        if (!manager.hasContent() || !manager.isVisible()) return "";

        String content = manager.getStealthText();
        String status = manager.getStealthStatusText();
        return "\uD83D\uDCD6 " + content + "  " + status;
    }

    @Override
    public float getAlignment() {
        return 0f; // LEFT
    }

    @Override
    public @Nullable String getTooltipText() {
        if (NovelReaderSettings.getInstance().isHotSearchMode()) {
            return "Hot search carousel | Click to details | Fish Toucher";
        }
        if (NovelReaderSettings.getInstance().isCultivationMode()) {
            return "Idle cultivation | Click to meditate | Fish Toucher";
        }
        return "Stealth mode | Click to next line | Fish Toucher";
    }

    @Override
    public @Nullable Consumer<MouseEvent> getClickConsumer() {
        return mouseEvent -> {
            if (NovelReaderSettings.getInstance().isHotSearchMode()) {
                String url = HotSearchManager.getInstance().getCurrentUrl();
                if (url != null && !url.isEmpty()) {
                    try {
                        Desktop.getDesktop().browse(new URI(url));
                    } catch (Exception e) {
                        LOG.warn("getClickConsumer: failed to open URL: " + e.getMessage());
                    }
                }
                return;
            }
            if (NovelReaderSettings.getInstance().isCultivationMode()) {
                IdleCultivationManager.getInstance().meditateOnce();
                return;
            }
            NovelReaderManager manager = NovelReaderManager.getInstance();
            if (manager.hasContent()) {
                manager.stealthNextPage();
            }
        };
    }
}
