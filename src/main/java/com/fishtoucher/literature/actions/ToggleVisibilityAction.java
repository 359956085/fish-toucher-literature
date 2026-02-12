package com.fishtoucher.literature.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.fishtoucher.literature.ui.NovelReaderManager;
import org.jetbrains.annotations.NotNull;

public class ToggleVisibilityAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(ToggleVisibilityAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        LOG.info("actionPerformed: ToggleVisibilityAction triggered");
        NovelReaderManager.getInstance().toggleVisibility();
    }
}
