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
 * Status bar widget that shows novel content in a single line at the bottom of the IDE.
 * Clicking advances to the next page.
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

        String content = manager.getCurrentPageText();
        String status = manager.getStatusText();
        // Truncate for status bar — use charsPerLine if set, otherwise default 80
        int maxChars = settings.getCharsPerLine() > 0 ? settings.getCharsPerLine() : 80;
        if (content.length() > maxChars) {
            content = content.substring(0, maxChars - 3) + "...";
        }
        return "📖 " + content + "  " + status;
    }

    @Override
    public float getAlignment() {
        return 0f; // LEFT
    }

    @Override
    public @Nullable String getTooltipText() {
        return "Click to go to next page | Fish Toucher Literature";
    }

    @Override
    public @Nullable Consumer<MouseEvent> getClickConsumer() {
        return mouseEvent -> {
            NovelReaderManager manager = NovelReaderManager.getInstance();
            if (manager.hasContent()) {
                manager.nextPage();
            }
        };
    }
}
