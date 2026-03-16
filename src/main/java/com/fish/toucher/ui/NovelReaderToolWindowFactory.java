package com.fish.toucher.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.fish.toucher.settings.NovelReaderSettings;
import org.jetbrains.annotations.NotNull;

public class NovelReaderToolWindowFactory implements ToolWindowFactory, DumbAware {

    private static final Logger LOG = Logger.getInstance(NovelReaderToolWindowFactory.class);

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        LOG.info("createToolWindowContent: creating tool window for project " + project.getName());
        NovelReaderSettings settings = NovelReaderSettings.getInstance();

        if (settings.isHotSearchMode()) {
            HotSearchPanel hotSearchPanel = new HotSearchPanel(project);
            Content content = ContentFactory.getInstance().createContent(hotSearchPanel, "", false);
            toolWindow.getContentManager().addContent(content);
            Disposer.register(toolWindow.getDisposable(), (Disposable) hotSearchPanel::dispose);
        } else {
            NovelReaderPanel panel = new NovelReaderPanel(project);
            Content content = ContentFactory.getInstance().createContent(panel, "", false);
            toolWindow.getContentManager().addContent(content);
        }

        LOG.info("createToolWindowContent: tool window created successfully");
    }
}
