package com.fishtoucher.literature.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.fishtoucher.literature.ui.NovelReaderManager;
import org.jetbrains.annotations.NotNull;

public class NextPageAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(NextPageAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        LOG.debug("actionPerformed: NextPageAction triggered");
        NovelReaderManager.getInstance().nextPage();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(NovelReaderManager.getInstance().hasContent());
    }
}
