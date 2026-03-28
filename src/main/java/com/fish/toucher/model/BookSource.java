package com.fish.toucher.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class BookSource {

    @SerializedName("name")
    private String name;

    @SerializedName("url")
    private String url;

    @SerializedName("enabled")
    private boolean enabled = true;

    @SerializedName("header")
    private Map<String, String> header;

    @SerializedName("search")
    private SearchRule searchRule;

    @SerializedName("chapter")
    private ChapterRule chapterRule;

    @SerializedName("content")
    private ContentRule contentRule;

    // --- Getters and Setters ---

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Map<String, String> getHeader() { return header; }
    public void setHeader(Map<String, String> header) { this.header = header; }

    /** Alias used by OnlineBookFetcher */
    public Map<String, String> getHeaders() { return header; }

    public SearchRule getSearchRule() { return searchRule; }
    public void setSearchRule(SearchRule searchRule) { this.searchRule = searchRule; }

    public ChapterRule getChapterRule() { return chapterRule; }
    public void setChapterRule(ChapterRule chapterRule) { this.chapterRule = chapterRule; }

    public ContentRule getContentRule() { return contentRule; }
    public void setContentRule(ContentRule contentRule) { this.contentRule = contentRule; }

    // --- Inner Classes ---

    public static class SearchRule {

        @SerializedName("url")
        private String url;

        @SerializedName("method")
        private String method = "GET";

        @SerializedName("type")
        private String type = "html";

        @SerializedName("list")
        private String list;

        @SerializedName("name")
        private String name;

        @SerializedName("author")
        private String author;

        @SerializedName("bookUrl")
        private String bookUrl;

        @SerializedName("coverUrl")
        private String coverUrl;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        /** Alias used by OnlineBookFetcher */
        public String getRuleType() { return type; }

        public String getList() { return list; }
        public void setList(String list) { this.list = list; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }

        public String getBookUrl() { return bookUrl; }
        public void setBookUrl(String bookUrl) { this.bookUrl = bookUrl; }

        public String getCoverUrl() { return coverUrl; }
        public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    }

    public static class ChapterRule {

        @SerializedName("url")
        private String url;

        @SerializedName("method")
        private String method = "GET";

        @SerializedName("type")
        private String type = "html";

        @SerializedName("list")
        private String list;

        @SerializedName("name")
        private String name;

        @SerializedName("chapterUrl")
        private String chapterUrl;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        /** Alias used by OnlineBookFetcher */
        public String getRuleType() { return type; }

        public String getList() { return list; }
        public void setList(String list) { this.list = list; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getChapterUrl() { return chapterUrl; }
        public void setChapterUrl(String chapterUrl) { this.chapterUrl = chapterUrl; }
    }

    public static class ContentRule {

        @SerializedName("url")
        private String url;

        @SerializedName("method")
        private String method = "GET";

        @SerializedName("type")
        private String type = "html";

        @SerializedName("content")
        private String content;

        @SerializedName("purify")
        private List<String> purify;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        /** Alias used by OnlineBookFetcher */
        public String getRuleType() { return type; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        /** Alias: the content field serves as the selector */
        public String getSelector() { return content; }
        public void setSelector(String selector) { this.content = selector; }

        public List<String> getPurify() { return purify; }
        public void setPurify(List<String> purify) { this.purify = purify; }
    }
}
