package com.fish.toucher.model;

public class ChapterInfo {

    private int index;
    private String name;
    private String chapterUrl;

    public ChapterInfo() {
    }

    public ChapterInfo(int index, String name, String chapterUrl) {
        this.index = index;
        this.name = name;
        this.chapterUrl = chapterUrl;
    }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    /** Alias used by OnlineBookFetcher */
    public void setTitle(String title) { this.name = title; }

    public String getChapterUrl() { return chapterUrl; }
    public void setChapterUrl(String chapterUrl) { this.chapterUrl = chapterUrl; }

    /** Alias used by OnlineBookFetcher */
    public String getUrl() { return chapterUrl; }
    public void setUrl(String url) { this.chapterUrl = url; }

    @Override
    public String toString() {
        return name;
    }
}
