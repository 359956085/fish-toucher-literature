package com.fish.toucher.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.fish.toucher.ui.NovelReaderManager;
import org.jetbrains.annotations.NotNull;

public class OpenNovelAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(OpenNovelAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        LOG.info("actionPerformed: OpenNovelAction triggered");
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withFileFilter(file -> {
                    String ext = file.getExtension();
                    return ext != null && (ext.equalsIgnoreCase("txt") || ext.equalsIgnoreCase("text"));
                })
                .withTitle("Select File")
                .withDescription("Choose a .txt file to read");

        VirtualFile[] files = FileChooser.chooseFiles(descriptor, e.getProject(), null);
        if (files.length > 0) {
            LOG.info("actionPerformed: user selected file: " + files[0].getPath());
            String filePath = files[0].getPath();
            NovelReaderManager.getInstance().loadFileAsync(e.getProject(), filePath, result -> {
                if (!result.isSuccess()
                        && result.status() != NovelReaderManager.LoadStatus.CANCELLED) {
                    LOG.warn("加载小说失败: " + filePath + ", " + result.message());
                    Messages.showErrorDialog(
                            e.getProject(),
                            "加载文件失败：" + result.message(),
                            "Fish Toucher"
                    );
                }
            });
        } else {
            LOG.info("actionPerformed: user cancelled file selection");
        }
    }
}
