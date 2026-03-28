package com.fish.toucher.model;

public class BookshelfItem {

    private String name;
    private String author;
    private String sourceName;
    private String bookUrl;
    private String coverUrl;
    private String lastChapterName;
    private int lastReadChapter;
    private long lastReadTime;
    private int totalChapters;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getBookUrl() { return bookUrl; }
    public void setBookUrl(String bookUrl) { this.bookUrl = bookUrl; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getLastChapterName() { return lastChapterName; }
    public void setLastChapterName(String lastChapterName) { this.lastChapterName = lastChapterName; }

    public int getLastReadChapter() { return lastReadChapter; }
    public void setLastReadChapter(int lastReadChapter) { this.lastReadChapter = lastReadChapter; }

    public long getLastReadTime() { return lastReadTime; }
    public void setLastReadTime(long lastReadTime) { this.lastReadTime = lastReadTime; }

    public int getTotalChapters() { return totalChapters; }
    public void setTotalChapters(int totalChapters) { this.totalChapters = totalChapters; }
}
