package com.fish.toucher.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.fish.toucher.settings.NovelReaderSettings;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

public class NovelReaderToolWindowFactory implements ToolWindowFactory, DumbAware {

    private static final Logger LOG = Logger.getInstance(NovelReaderToolWindowFactory.class);

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        LOG.info("createToolWindowContent: creating tool window for project " + project.getName());
        buildContent(project, toolWindow);
        LOG.info("createToolWindowContent: tool window created successfully");
    }

    private static void buildContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        ContentManager cm = toolWindow.getContentManager();
        cm.removeAllContents(true);

        if (settings.isHotSearchMode()) {
            HotSearchPanel hotSearchPanel = new HotSearchPanel(project);
            Content content = ContentFactory.getInstance().createContent(hotSearchPanel, "", false);
            content.setDisposer(hotSearchPanel);
            cm.addContent(content);
        } else if (settings.isCultivationMode()) {
            IdleCultivationPanel cultivationPanel = new IdleCultivationPanel();
            Content content = ContentFactory.getInstance().createContent(cultivationPanel, "", false);
            content.setDisposer(cultivationPanel);
            cm.addContent(content);
        } else {
            NovelReaderManager.getInstance().loadMostRecentFileIfNeeded();
            NovelReaderPanel panel = new NovelReaderPanel(project);
            Content content = ContentFactory.getInstance().createContent(panel, "", false);
            content.setDisposer(panel);
            cm.addContent(content);
        }
    }

    /**
     * Switch plugin mode and rebuild all tool windows. Safe to call from any thread.
     */
    public static void switchMode(@NotNull String newMode) {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        settings.setPluginMode(newMode);
        newMode = settings.getPluginMode();
        if (NovelReaderSettings.MODE_HOT_SEARCH.equals(newMode)) {
            IdleCultivationManager.getInstance().stop();
            if (!HotSearchManager.getInstance().isRunning()) {
                HotSearchManager.getInstance().start();
            }
        } else if (NovelReaderSettings.MODE_CULTIVATION.equals(newMode)) {
            if (HotSearchManager.getInstance().isRunning()) {
                HotSearchManager.getInstance().stop();
            }
            IdleCultivationManager.getInstance().start();
        } else {
            if (HotSearchManager.getInstance().isRunning()) {
                HotSearchManager.getInstance().stop();
            }
            IdleCultivationManager.getInstance().stop();
            NovelReaderManager.getInstance().loadMostRecentFileIfNeeded();
        }
        ApplicationManager.getApplication().invokeLater(NovelReaderToolWindowFactory::rebuildAllToolWindows);
    }

    /**
     * Switch UI language and rebuild all tool windows. Safe to call from any thread.
     */
    public static void switchLanguage(@NotNull String language) {
        NovelReaderSettings.getInstance().setUiLanguage(language);
        ApplicationManager.getApplication().invokeLater(() -> {
            rebuildAllToolWindows();
            refreshAllStatusBarWidgets();
        });
    }

    /**
     * Rebuild tool window content for all open projects (called after mode switch).
     */
    public static void rebuildAllToolWindows() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (project.isDisposed()) continue;
            ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow("Fish Toucher");
            if (tw != null) {
                buildContent(project, tw);
                LOG.info("rebuildAllToolWindows: rebuilt for project " + project.getName());
            }
        }
    }

    public static void refreshAllStatusBarWidgets() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (project.isDisposed()) continue;
            StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
            if (statusBar != null) {
                statusBar.updateWidget("NovelReaderWidget");
            }
        }
    }
}
