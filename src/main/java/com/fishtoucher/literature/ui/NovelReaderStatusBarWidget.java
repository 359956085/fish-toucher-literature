package com.fishtoucher.literature.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import com.fishtoucher.literature.settings.NovelReaderSettings;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;

/**
 * Stealth mode: status bar widget that shows novel content in a single line.
 * Clicking advances to the next line. Has its own independent reading progress.
 */
public class NovelReaderStatusBarWidget implements StatusBarWidget, StatusBarWidget.TextPresentation {

    private static final Logger LOG = Logger.getInstance(NovelReaderStatusBarWidget.class);
    private final Project project;
    private StatusBar statusBar;
    private final Runnable changeListener;

    public NovelReaderStatusBarWidget(@NotNull Project project) {
        LOG.info("NovelReaderStatusBarWidget: creating for project " + project.getName());
        this.project = project;
        this.changeListener = () -> {
            if (statusBar != null) {
                statusBar.updateWidget(ID());
            }
        };
        NovelReaderManager.getInstance().addChangeListener(changeListener);
    }

    @Override
    public @NonNls @NotNull String ID() {
        return "NovelReaderWidget";
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        LOG.info("install: status bar widget installed");
        this.statusBar = statusBar;
    }

    @Override
    public void dispose() {
        LOG.info("dispose: status bar widget disposed");
        NovelReaderManager.getInstance().removeChangeListener(changeListener);
    }

    @Override
    public @NotNull StatusBarWidget.WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    public @NotNull String getText() {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
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
        return "Stealth mode | Click to next line | Fish Toucher Literature";
    }

    @Override
    public @Nullable Consumer<MouseEvent> getClickConsumer() {
        return mouseEvent -> {
            NovelReaderManager manager = NovelReaderManager.getInstance();
            if (manager.hasContent()) {
                manager.stealthNextPage();
            }
        };
    }
}
