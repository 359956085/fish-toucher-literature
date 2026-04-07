package com.fish.toucher.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
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
            cm.addContent(content);
            Disposer.register(toolWindow.getDisposable(), (Disposable) hotSearchPanel::dispose);
        } else {
            NovelReaderPanel panel = new NovelReaderPanel(project);
            Content content = ContentFactory.getInstance().createContent(panel, "", false);
            cm.addContent(content);
        }
    }

    /**
     * Switch plugin mode and rebuild all tool windows. Safe to call from any thread.
     */
    public static void switchMode(@NotNull String newMode) {
        NovelReaderSettings settings = NovelReaderSettings.getInstance();
        settings.setPluginMode(newMode);
        if ("hotsearch".equals(newMode) && !HotSearchManager.getInstance().isRunning()) {
            HotSearchManager.getInstance().start();
        } else if ("novel".equals(newMode) && HotSearchManager.getInstance().isRunning()) {
            HotSearchManager.getInstance().stop();
        }
        ApplicationManager.getApplication().invokeLater(NovelReaderToolWindowFactory::rebuildAllToolWindows);
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
}
