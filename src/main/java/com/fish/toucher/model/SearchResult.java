package com.fish.toucher.model;

public class SearchResult {

    private String name;
    private String author;
    private String bookUrl;
    private String coverUrl;
    private String sourceName;

    public SearchResult() {
    }

    public SearchResult(String name, String author, String bookUrl, String coverUrl, String sourceName) {
        this.name = name;
        this.author = author;
        this.bookUrl = bookUrl;
        this.coverUrl = coverUrl;
        this.sourceName = sourceName;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getBookUrl() { return bookUrl; }
    public void setBookUrl(String bookUrl) { this.bookUrl = bookUrl; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    @Override
    public String toString() {
        return name + " - " + author;
    }
}
