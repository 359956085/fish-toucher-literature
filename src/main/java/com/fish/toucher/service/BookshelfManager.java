package com.fish.toucher.service;

import com.fish.toucher.model.BookshelfItem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BookshelfManager {

    private static final Logger LOG = Logger.getInstance(BookshelfManager.class);
    private static final BookshelfManager INSTANCE = new BookshelfManager();

    private static final Type BOOK_LIST_TYPE = new TypeToken<List<BookshelfItem>>() {}.getType();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path bookshelfFile;
    private final List<BookshelfItem> books = new ArrayList<>();

    private BookshelfManager() {
        Path configDir = Paths.get(System.getProperty("user.home"), ".config", "fish-toucher");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            LOG.error("Failed to create config directory: " + configDir, e);
        }
        bookshelfFile = configDir.resolve("bookshelf.json");
        load();
    }

    public static BookshelfManager getInstance() {
        return INSTANCE;
    }

    public void load() {
        books.clear();
        if (!Files.exists(bookshelfFile)) {
            return;
        }
        try {
            String json = Files.readString(bookshelfFile, StandardCharsets.UTF_8);
            List<BookshelfItem> loaded = gson.fromJson(json, BOOK_LIST_TYPE);
            if (loaded != null) {
                books.addAll(loaded);
            }
            LOG.info("Loaded " + books.size() + " books from bookshelf");
        } catch (Exception e) {
            LOG.error("Failed to load bookshelf", e);
        }
    }

    public void save() {
        try {
            String json = gson.toJson(books, BOOK_LIST_TYPE);
            Files.writeString(bookshelfFile, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Failed to save bookshelf", e);
        }
    }

    public void addBook(BookshelfItem item) {
        books.removeIf(b -> b.getBookUrl() != null && b.getBookUrl().equals(item.getBookUrl()));
        books.add(0, item);
        save();
    }

    public void removeBook(BookshelfItem item) {
        books.removeIf(b -> b.getBookUrl() != null && b.getBookUrl().equals(item.getBookUrl()));
        save();
    }

    public void updateProgress(BookshelfItem item, int chapterIndex, String chapterName) {
        for (BookshelfItem book : books) {
            if (book.getBookUrl() != null && book.getBookUrl().equals(item.getBookUrl())) {
                book.setLastReadChapter(chapterIndex);
                book.setLastChapterName(chapterName);
                book.setLastReadTime(System.currentTimeMillis());
                save();
                return;
            }
        }
    }

    public BookshelfItem findByBookUrl(String bookUrl) {
        for (BookshelfItem book : books) {
            if (book.getBookUrl() != null && book.getBookUrl().equals(bookUrl)) {
                return book;
            }
        }
        return null;
    }

    public List<BookshelfItem> getBooks() {
        return new ArrayList<>(books);
    }
}
