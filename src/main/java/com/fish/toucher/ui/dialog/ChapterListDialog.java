package com.fish.toucher.ui.dialog;

import com.fish.toucher.model.BookSource;
import com.fish.toucher.model.BookshelfItem;
import com.fish.toucher.model.ChapterInfo;
import com.fish.toucher.service.BookSourceManager;
import com.fish.toucher.service.BookshelfManager;
import com.fish.toucher.service.ChapterCacheManager;
import com.fish.toucher.service.OnlineBookFetcher;
import com.fish.toucher.ui.NovelReaderManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class ChapterListDialog extends DialogWrapper {

    private static final Logger LOG = Logger.getInstance(ChapterListDialog.class);

    private final BookshelfItem book;
    private final BookSource bookSource;

    private JList<ChapterInfo> chapterList;
    private DefaultListModel<ChapterInfo> listModel;
    private JLabel statusLabel;

    public ChapterListDialog(@NotNull Project project, @NotNull BookshelfItem book) {
        super(project, true);
        this.book = book;
        this.bookSource = findBookSource(book.getSourceName());

        setTitle(book.getName() + " - \u7ae0\u8282\u76ee\u5f55");
        setOKButtonText("Close");
        init();

        loadChapters();
    }

    @Nullable
    private BookSource findBookSource(String sourceName) {
        if (sourceName == null) {
            return null;
        }
        for (BookSource source : BookSourceManager.getInstance().getSources()) {
            if (sourceName.equals(source.getName())) {
                return source;
            }
        }
        return null;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction()};
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(400, 500));

        listModel = new DefaultListModel<>();
        chapterList = new JList<>(listModel);
        chapterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chapterList.setCellRenderer(new ChapterCellRenderer());

        chapterList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ChapterInfo selected = chapterList.getSelectedValue();
                if (selected != null) {
                    loadChapterContent(selected);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(chapterList);
        panel.add(scrollPane, BorderLayout.CENTER);

        statusLabel = new JLabel("Loading...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panel.add(statusLabel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadChapters() {
        if (bookSource == null) {
            statusLabel.setText("Book source not found: " + book.getSourceName());
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                String bookUrl = book.getBookUrl();
                ChapterCacheManager cacheManager = ChapterCacheManager.getInstance();

                List<ChapterInfo> chapters = cacheManager.getCachedChapterList(bookUrl);
                if (chapters == null) {
                    updateStatus("Fetching chapter list...");
                    OnlineBookFetcher fetcher = new OnlineBookFetcher();
                    chapters = fetcher.fetchChapterList(bookSource, bookUrl);
                    if (chapters != null && !chapters.isEmpty()) {
                        cacheManager.cacheChapterList(bookUrl, chapters);
                    }
                }

                final List<ChapterInfo> finalChapters = chapters;
                ApplicationManager.getApplication().invokeLater(() -> {
                    listModel.clear();
                    if (finalChapters != null) {
                        for (ChapterInfo chapter : finalChapters) {
                            listModel.addElement(chapter);
                        }
                    }

                    int total = listModel.getSize();
                    if (total == 0) {
                        statusLabel.setText("\u672a\u83b7\u53d6\u5230\u7ae0\u8282\u5217\u8868\uff0c\u8bf7\u68c0\u67e5\uff1a\u2460\u4e66\u6e90\u89c4\u5219\u662f\u5426\u6b63\u786e \u2461\u662f\u5426\u9700\u8981 Cookie \u2462\u7f51\u7edc\u662f\u5426\u6b63\u5e38");
                    } else {
                        statusLabel.setText("\u5171 " + total + " \u7ae0");
                    }

                    // Scroll to last read chapter
                    int lastRead = book.getLastReadChapter();
                    if (lastRead >= 0 && lastRead < total) {
                        chapterList.ensureIndexIsVisible(lastRead);
                        chapterList.setSelectedIndex(lastRead);
                        // Clear selection so it doesn't trigger loading immediately
                        chapterList.clearSelection();
                        chapterList.ensureIndexIsVisible(lastRead);
                    }
                });
            } catch (Exception e) {
                LOG.warn("Failed to load chapters", e);
                updateStatus("Failed to load chapters: " + e.getMessage());
            }
        });
    }

    private void loadChapterContent(@NotNull ChapterInfo chapter) {
        if (bookSource == null) {
            statusLabel.setText("Book source not found");
            return;
        }

        statusLabel.setText("Loading: " + chapter.getName() + "...");

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                String bookUrl = book.getBookUrl();
                ChapterCacheManager cacheManager = ChapterCacheManager.getInstance();

                // Try cache first
                String cachedContent = cacheManager.getCachedContent(bookUrl, chapter.getIndex());
                List<String> lines;
                if (cachedContent != null) {
                    lines = Arrays.asList(cachedContent.split("\n"));
                } else {
                    updateStatus("Fetching content...");
                    OnlineBookFetcher fetcher = new OnlineBookFetcher();
                    lines = fetcher.fetchContent(bookSource, chapter.getChapterUrl());
                    if (lines != null && !lines.isEmpty()) {
                        cacheManager.cacheContent(bookUrl, chapter.getIndex(), String.join("\n", lines));
                    }
                }

                if (lines == null || lines.isEmpty()) {
                    updateStatus("No content found for: " + chapter.getName());
                    return;
                }

                String virtualPath = "online://" + book.getSourceName() + "/" + book.getName() + "/" + chapter.getName();

                ApplicationManager.getApplication().invokeLater(() -> {
                    NovelReaderManager.getInstance().loadFromLines(virtualPath, lines);
                    BookshelfManager.getInstance().updateProgress(book, chapter.getIndex(), chapter.getName());
                    statusLabel.setText("Loaded: " + chapter.getName());
                    chapterList.repaint();
                });
            } catch (Exception e) {
                LOG.warn("Failed to load chapter content", e);
                updateStatus("Failed to load: " + e.getMessage());
            }
        });
    }

    private void updateStatus(@NotNull String text) {
        ApplicationManager.getApplication().invokeLater(() -> statusLabel.setText(text));
    }

    /**
     * Custom cell renderer that bolds the last-read chapter.
     */
    private class ChapterCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof ChapterInfo chapter) {
                setText(chapter.getName());
                if (chapter.getIndex() == book.getLastReadChapter()) {
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
            }

            return this;
        }
    }
}
