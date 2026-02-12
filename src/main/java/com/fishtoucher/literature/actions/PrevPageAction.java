package com.fishtoucher.literature.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.fishtoucher.literature.ui.NovelReaderManager;
import org.jetbrains.annotations.NotNull;

public class PrevPageAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(PrevPageAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        LOG.debug("actionPerformed: PrevPageAction triggered");
        NovelReaderManager.getInstance().prevPage();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(NovelReaderManager.getInstance().hasContent());
    }
}
