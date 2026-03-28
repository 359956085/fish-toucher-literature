package com.fish.toucher.ui.dialog;

import com.fish.toucher.model.BookSource;
import com.fish.toucher.model.BookshelfItem;
import com.fish.toucher.model.SearchResult;
import com.fish.toucher.service.BookSourceManager;
import com.fish.toucher.service.BookshelfManager;
import com.fish.toucher.service.ChapterCacheManager;
import com.fish.toucher.service.OnlineBookFetcher;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

public class OnlineBookDialog extends DialogWrapper {

    private static final Logger LOG = Logger.getInstance(OnlineBookDialog.class);

    private final Project project;

    // Bookshelf tab components
    private JList<BookshelfItem> bookshelfList;
    private DefaultListModel<BookshelfItem> bookshelfModel;

    // Search tab components
    private JComboBox<String> sourceComboBox;
    private JTextField searchField;
    private JList<SearchResult> searchResultList;
    private DefaultListModel<SearchResult> searchResultModel;
    private JLabel searchStatusLabel;

    public OnlineBookDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        setTitle("\u5728\u7ebf\u4e66\u6e90");
        init();
        refreshBookshelf();
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction()};
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setPreferredSize(new Dimension(500, 600));

        tabbedPane.addTab("\u4e66\u67b6", createBookshelfPanel());
        tabbedPane.addTab("\u641c\u7d22", createSearchPanel());

        return tabbedPane;
    }

    // -------------------------------------------------------------------------
    // Bookshelf tab
    // -------------------------------------------------------------------------

    private JPanel createBookshelfPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        bookshelfModel = new DefaultListModel<>();
        bookshelfList = new JList<>(bookshelfModel);
        bookshelfList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookshelfList.setCellRenderer(new BookshelfCellRenderer());

        JScrollPane scrollPane = new JScrollPane(bookshelfList);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Button bar at the bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton readButton = new JButton("\u7ee7\u7eed\u9605\u8bfb");
        JButton removeButton = new JButton("\u79fb\u9664");
        JButton clearCacheButton = new JButton("\u6e05\u9664\u7f13\u5b58");

        readButton.addActionListener(e -> continueReading());
        removeButton.addActionListener(e -> removeFromBookshelf());
        clearCacheButton.addActionListener(e -> clearBookCache());

        buttonPanel.add(readButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(clearCacheButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshBookshelf() {
        bookshelfModel.clear();
        for (BookshelfItem item : BookshelfManager.getInstance().getBooks()) {
            bookshelfModel.addElement(item);
        }
    }

    private void continueReading() {
        BookshelfItem selected = bookshelfList.getSelectedValue();
        if (selected == null) {
            Messages.showInfoMessage(project, "\u8bf7\u5148\u9009\u62e9\u4e00\u672c\u4e66", "\u63d0\u793a");
            return;
        }
        if (project == null) {
            LOG.warn("Cannot open ChapterListDialog: project is null");
            return;
        }
        new ChapterListDialog(project, selected).show();
    }

    private void removeFromBookshelf() {
        BookshelfItem selected = bookshelfList.getSelectedValue();
        if (selected == null) {
            Messages.showInfoMessage(project, "\u8bf7\u5148\u9009\u62e9\u4e00\u672c\u4e66", "\u63d0\u793a");
            return;
        }
        BookshelfManager.getInstance().removeBook(selected);
        refreshBookshelf();
    }

    private void clearBookCache() {
        BookshelfItem selected = bookshelfList.getSelectedValue();
        if (selected == null) {
            Messages.showInfoMessage(project, "\u8bf7\u5148\u9009\u62e9\u4e00\u672c\u4e66", "\u63d0\u793a");
            return;
        }
        ChapterCacheManager.getInstance().clearCache(selected.getBookUrl());
        Messages.showInfoMessage(project,
                "\u5df2\u6e05\u9664\u300c" + selected.getName() + "\u300d\u7684\u7f13\u5b58",
                "\u6e05\u9664\u7f13\u5b58");
    }

    // -------------------------------------------------------------------------
    // Search tab
    // -------------------------------------------------------------------------

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Top search bar
        JPanel topBar = new JPanel(new BorderLayout(4, 0));
        topBar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // Build source combo
        List<BookSource> enabledSources = BookSourceManager.getInstance().getEnabledSources();
        String[] sourceNames = enabledSources.stream()
                .map(BookSource::getName)
                .toArray(String[]::new);
        sourceComboBox = new JComboBox<>(sourceNames);
        sourceComboBox.setPreferredSize(new Dimension(130, 28));

        searchField = new JTextField();
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performSearch();
                }
            }
        });

        JButton searchButton = new JButton("\u641c\u7d22");
        searchButton.addActionListener(e -> performSearch());

        topBar.add(sourceComboBox, BorderLayout.WEST);
        topBar.add(searchField, BorderLayout.CENTER);
        topBar.add(searchButton, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);

        // Result list
        searchResultModel = new DefaultListModel<>();
        searchResultList = new JList<>(searchResultModel);
        searchResultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchResultList.setCellRenderer(new SearchResultCellRenderer());

        JScrollPane scrollPane = new JScrollPane(searchResultList);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Bottom bar: status + add-to-shelf button
        JPanel bottomBar = new JPanel(new BorderLayout(4, 0));
        bottomBar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        searchStatusLabel = new JLabel(enabledSources.isEmpty() ? "\u6ca1\u6709\u53ef\u7528\u4e66\u6e90" : " ");
        searchStatusLabel.setForeground(JBColor.GRAY);

        JButton addToShelfButton = new JButton("\u52a0\u5165\u4e66\u67b6");
        addToShelfButton.addActionListener(e -> addToBookshelf());

        bottomBar.add(searchStatusLabel, BorderLayout.CENTER);
        bottomBar.add(addToShelfButton, BorderLayout.EAST);

        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    private void performSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            updateSearchStatus("\u8bf7\u8f93\u5165\u641c\u7d22\u5185\u5bb9");
            return;
        }

        List<BookSource> enabledSources = BookSourceManager.getInstance().getEnabledSources();
        if (enabledSources.isEmpty()) {
            updateSearchStatus("\u6ca1\u6709\u53ef\u7528\u4e66\u6e90");
            return;
        }

        String selectedSourceName = (String) sourceComboBox.getSelectedItem();
        BookSource targetSource = enabledSources.stream()
                .filter(s -> s.getName().equals(selectedSourceName))
                .findFirst()
                .orElse(null);

        if (targetSource == null) {
            updateSearchStatus("\u672a\u627e\u5230\u6240\u9009\u4e66\u6e90");
            return;
        }

        updateSearchStatus("\u641c\u7d22\u4e2d...");
        searchResultModel.clear();

        final BookSource source = targetSource;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                OnlineBookFetcher fetcher = new OnlineBookFetcher();
                List<SearchResult> results = fetcher.search(source, keyword);

                ApplicationManager.getApplication().invokeLater(() -> {
                    searchResultModel.clear();
                    if (results == null || results.isEmpty()) {
                        updateSearchStatus("\u672a\u627e\u5230\u76f8\u5173\u4e66\u7c4d");
                    } else {
                        for (SearchResult r : results) {
                            searchResultModel.addElement(r);
                        }
                        updateSearchStatus("\u627e\u5230 " + results.size() + " \u6761\u7ed3\u679c");
                    }
                });
            } catch (Exception e) {
                LOG.warn("Search failed", e);
                ApplicationManager.getApplication().invokeLater(() ->
                        updateSearchStatus("\u641c\u7d22\u5931\u8d25: " + e.getMessage()));
            }
        });
    }

    private void addToBookshelf() {
        SearchResult selected = searchResultList.getSelectedValue();
        if (selected == null) {
            Messages.showInfoMessage(project, "\u8bf7\u5148\u9009\u62e9\u4e00\u672c\u4e66", "\u63d0\u793a");
            return;
        }

        BookshelfItem item = new BookshelfItem();
        item.setName(selected.getName());
        item.setAuthor(selected.getAuthor());
        item.setBookUrl(selected.getBookUrl());
        item.setCoverUrl(selected.getCoverUrl());
        item.setSourceName(selected.getSourceName());
        item.setLastReadTime(System.currentTimeMillis());

        BookshelfManager.getInstance().addBook(item);
        refreshBookshelf();

        Messages.showInfoMessage(project,
                "\u300c" + selected.getName() + "\u300d\u5df2\u52a0\u5165\u4e66\u67b6",
                "\u52a0\u5165\u4e66\u67b6");
    }

    private void updateSearchStatus(String text) {
        searchStatusLabel.setText(text);
    }

    // -------------------------------------------------------------------------
    // Cell renderers
    // -------------------------------------------------------------------------

    private static class BookshelfCellRenderer extends JPanel implements ListCellRenderer<BookshelfItem> {

        private final JLabel titleLabel;
        private final JLabel detailLabel;

        BookshelfCellRenderer() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

            titleLabel = new JLabel();
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

            detailLabel = new JLabel();
            detailLabel.setFont(detailLabel.getFont().deriveFont(Font.PLAIN, 11f));
            detailLabel.setForeground(JBColor.GRAY);

            add(titleLabel, BorderLayout.NORTH);
            add(detailLabel, BorderLayout.SOUTH);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends BookshelfItem> list,
                                                      BookshelfItem value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            if (value != null) {
                String name = value.getName() != null ? value.getName() : "";
                String author = value.getAuthor() != null ? value.getAuthor() : "";
                titleLabel.setText(name + " - " + author);

                String source = value.getSourceName() != null ? value.getSourceName() : "";
                int lastChapter = value.getLastReadChapter();
                int total = value.getTotalChapters();
                String lastChapterName = value.getLastChapterName() != null ? value.getLastChapterName() : "";
                detailLabel.setText("Source: " + source + " | Chapter " + lastChapter + "/" + total + " | " + lastChapterName);
            }

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                titleLabel.setForeground(list.getSelectionForeground());
                detailLabel.setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
                titleLabel.setForeground(list.getForeground());
                detailLabel.setForeground(JBColor.GRAY);
            }

            setOpaque(true);
            return this;
        }
    }

    private static class SearchResultCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof SearchResult result) {
                String name = result.getName() != null ? result.getName() : "";
                String author = result.getAuthor() != null ? result.getAuthor() : "";
                setText(name + " - " + author);
            }
            return this;
        }
    }
}