package com.fishtoucher.literature.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class NovelReaderWidgetFactory implements StatusBarWidgetFactory {

    private static final Logger LOG = Logger.getInstance(NovelReaderWidgetFactory.class);

    @Override
    public @NonNls @NotNull String getId() {
        return "FishToucherLiteratureWidget";
    }

    @Override
    public @Nls @NotNull String getDisplayName() {
        return "Fish Toucher Literature";
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return true;
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        LOG.info("createWidget: creating status bar widget for project " + project.getName());
        return new NovelReaderStatusBarWidget(project);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        LOG.info("disposeWidget: disposing status bar widget");
        // cleanup handled in widget dispose
    }

    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return true;
    }
}
