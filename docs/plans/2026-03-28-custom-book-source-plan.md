# Custom Book Source (自定义书源) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add custom online book source support to the Fish Toucher IntelliJ plugin, allowing users to search, read, and cache novels from configurable web sources.

**Architecture:** Extend the existing novel mode with online book source capabilities. Book source rules stored as JSON files in `~/.config/fish-toucher/sources/`. Fetched content injected into `NovelReaderManager.rawLines` to reuse existing pagination/display. New dependencies: Jsoup (HTML parsing), JsonPath (JSON parsing), Gson (serialization).

**Tech Stack:** Java 21, IntelliJ Platform SDK, Jsoup, JsonPath, Gson, Swing UI (DialogWrapper)

**Design Doc:** `docs/plans/2026-03-28-custom-book-source-design.md`

---

## Task 1: Add Dependencies

**Files:**
- Modify: `build.gradle.kts`

**Step 1: Add Jsoup, JsonPath, and Gson dependencies**

In `build.gradle.kts`, add to the `dependencies` block (outside `intellijPlatform`):

```kotlin
dependencies {
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("com.jayway.jsonpath:json-path:2.9.0")
    implementation("com.google.code.gson:gson:2.12.1")

    intellijPlatform {
        // ... existing
    }
}
```

**Step 2: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add build.gradle.kts
git commit -m "feat: add Jsoup, JsonPath, and Gson dependencies for online book source"
```

---

## Task 2: Create Data Model POJOs

**Files:**
- Create: `src/main/java/com/fish/toucher/model/BookSource.java`
- Create: `src/main/java/com/fish/toucher/model/BookshelfItem.java`
- Create: `src/main/java/com/fish/toucher/model/SearchResult.java`
- Create: `src/main/java/com/fish/toucher/model/ChapterInfo.java`

**Step 1: Create BookSource POJO**

```java
package com.fish.toucher.model;

import java.util.List;
import java.util.Map;

public class BookSource {
    private String name = "";
    private String url = "";
    private boolean enabled = true;
    private Map<String, String> header;

    private SearchRule search;
    private ChapterRule chapter;
    private ContentRule content;

    public static class SearchRule {
        private String url = "";
        private String method = "GET";
        private String type = "html";   // "html" or "json"
        private String list = "";       // CSS selector or JSONPath for item list
        private String name = "";       // selector for book name
        private String author = "";     // selector for author
        private String bookUrl = "";    // selector for book detail URL
        private String coverUrl = "";   // selector for cover image URL

        // getters and setters for all fields
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
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
        private String url = "";
        private String type = "html";
        private String list = "";
        private String name = "";
        private String chapterUrl = "";

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getList() { return list; }
        public void setList(String list) { this.list = list; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getChapterUrl() { return chapterUrl; }
        public void setChapterUrl(String chapterUrl) { this.chapterUrl = chapterUrl; }
    }

    public static class ContentRule {
        private String url = "";
        private String type = "html";
        private String selector = "";
        private List<String> purify;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSelector() { return selector; }
        public void setSelector(String selector) { this.selector = selector; }
        public List<String> getPurify() { return purify; }
        public void setPurify(List<String> purify) { this.purify = purify; }
    }

    // BookSource getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Map<String, String> getHeader() { return header; }
    public void setHeader(Map<String, String> header) { this.header = header; }
    public SearchRule getSearch() { return search; }
    public void setSearch(SearchRule search) { this.search = search; }
    public ChapterRule getChapter() { return chapter; }
    public void setChapter(ChapterRule chapter) { this.chapter = chapter; }
    public ContentRule getContent() { return content; }
    public void setContent(ContentRule content) { this.content = content; }
}
```

**Step 2: Create BookshelfItem POJO**

```java
package com.fish.toucher.model;

public class BookshelfItem {
    private String name = "";
    private String author = "";
    private String sourceName = "";
    private String bookUrl = "";
    private String coverUrl = "";
    private String lastChapterName = "";
    private int lastReadChapter = 0;
    private long lastReadTime = 0;
    private int totalChapters = 0;

    // getters and setters for all fields
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
```

**Step 3: Create SearchResult POJO**

```java
package com.fish.toucher.model;

public class SearchResult {
    private final String name;
    private final String author;
    private final String bookUrl;
    private final String coverUrl;
    private final String sourceName;

    public SearchResult(String name, String author, String bookUrl, String coverUrl, String sourceName) {
        this.name = name;
        this.author = author;
        this.bookUrl = bookUrl;
        this.coverUrl = coverUrl;
        this.sourceName = sourceName;
    }

    public String getName() { return name; }
    public String getAuthor() { return author; }
    public String getBookUrl() { return bookUrl; }
    public String getCoverUrl() { return coverUrl; }
    public String getSourceName() { return sourceName; }

    @Override
    public String toString() { return name + " - " + author; }
}
```

**Step 4: Create ChapterInfo POJO**

```java
package com.fish.toucher.model;

public class ChapterInfo {
    private final int index;
    private final String name;
    private final String chapterUrl;

    public ChapterInfo(int index, String name, String chapterUrl) {
        this.index = index;
        this.name = name;
        this.chapterUrl = chapterUrl;
    }

    public int getIndex() { return index; }
    public String getName() { return name; }
    public String getChapterUrl() { return chapterUrl; }

    @Override
    public String toString() { return name; }
}
```

**Step 5: Verify build**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add src/main/java/com/fish/toucher/model/
git commit -m "feat: add data model POJOs for book source, bookshelf, search result, chapter"
```

---

## Task 3: Create BookSourceManager (Source File CRUD)

**Files:**
- Create: `src/main/java/com/fish/toucher/service/BookSourceManager.java`

**Step 1: Implement BookSourceManager**

This manager handles loading/saving/CRUD for book source JSON files in `~/.config/fish-toucher/sources/`.

```java
package com.fish.toucher.service;

import com.fish.toucher.model.BookSource;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class BookSourceManager {

    private static final Logger LOG = Logger.getInstance(BookSourceManager.class);
    private static final BookSourceManager INSTANCE = new BookSourceManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path sourcesDir;
    private final List<BookSource> sources = new ArrayList<>();

    public static BookSourceManager getInstance() { return INSTANCE; }

    private BookSourceManager() {
        sourcesDir = Paths.get(System.getProperty("user.home"), ".config", "fish-toucher", "sources");
        try {
            Files.createDirectories(sourcesDir);
        } catch (IOException e) {
            LOG.error("Failed to create sources directory: " + sourcesDir, e);
        }
        loadAll();
    }

    public Path getSourcesDir() { return sourcesDir; }

    public List<BookSource> getSources() { return new ArrayList<>(sources); }

    public List<BookSource> getEnabledSources() {
        return sources.stream().filter(BookSource::isEnabled).toList();
    }

    public void loadAll() {
        sources.clear();
        if (!Files.isDirectory(sourcesDir)) return;
        try (Stream<Path> files = Files.list(sourcesDir)) {
            files.filter(p -> p.toString().endsWith(".json")).sorted().forEach(p -> {
                try {
                    String json = Files.readString(p, StandardCharsets.UTF_8);
                    BookSource source = GSON.fromJson(json, BookSource.class);
                    if (source != null && source.getName() != null && !source.getName().isEmpty()) {
                        sources.add(source);
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to load source file: " + p, e);
                }
            });
        } catch (IOException e) {
            LOG.error("Failed to list sources directory", e);
        }
        LOG.info("Loaded " + sources.size() + " book sources");
    }

    public void save(BookSource source) {
        String fileName = source.getName().replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fff_-]", "_") + ".json";
        Path file = sourcesDir.resolve(fileName);
        try {
            Files.writeString(file, GSON.toJson(source), StandardCharsets.UTF_8);
            LOG.info("Saved book source: " + file);
        } catch (IOException e) {
            LOG.error("Failed to save book source: " + file, e);
        }
        loadAll();
    }

    public void delete(BookSource source) {
        String fileName = source.getName().replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fff_-]", "_") + ".json";
        Path file = sourcesDir.resolve(fileName);
        try {
            Files.deleteIfExists(file);
            LOG.info("Deleted book source: " + file);
        } catch (IOException e) {
            LOG.error("Failed to delete book source: " + file, e);
        }
        loadAll();
    }

    public BookSource importFromJson(String json) {
        BookSource source = GSON.fromJson(json, BookSource.class);
        if (source == null || source.getName() == null || source.getName().isEmpty()) {
            throw new IllegalArgumentException("Invalid book source JSON: missing name");
        }
        if (source.getSearch() == null || source.getChapter() == null || source.getContent() == null) {
            throw new IllegalArgumentException("Invalid book source JSON: missing search/chapter/content rules");
        }
        save(source);
        return source;
    }

    public String exportToJson(BookSource source) {
        return GSON.toJson(source);
    }

    public String exportAllToJson() {
        return GSON.toJson(sources);
    }
}
```

**Step 2: Verify build**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/com/fish/toucher/service/BookSourceManager.java
git commit -m "feat: add BookSourceManager for book source file CRUD"
```

---

## Task 4: Create BookshelfManager

**Files:**
- Create: `src/main/java/com/fish/toucher/service/BookshelfManager.java`

**Step 1: Implement BookshelfManager**

```java
package com.fish.toucher.service;

import com.fish.toucher.model.BookshelfItem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class BookshelfManager {

    private static final Logger LOG = Logger.getInstance(BookshelfManager.class);
    private static final BookshelfManager INSTANCE = new BookshelfManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path bookshelfFile;
    private final List<BookshelfItem> books = new ArrayList<>();

    public static BookshelfManager getInstance() { return INSTANCE; }

    private BookshelfManager() {
        Path configDir = Paths.get(System.getProperty("user.home"), ".config", "fish-toucher");
        try { Files.createDirectories(configDir); } catch (IOException e) { LOG.error("Failed to create config dir", e); }
        bookshelfFile = configDir.resolve("bookshelf.json");
        load();
    }

    public List<BookshelfItem> getBooks() { return new ArrayList<>(books); }

    public void load() {
        books.clear();
        if (!Files.exists(bookshelfFile)) return;
        try {
            String json = Files.readString(bookshelfFile, StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<BookshelfItem>>() {}.getType();
            List<BookshelfItem> loaded = GSON.fromJson(json, listType);
            if (loaded != null) books.addAll(loaded);
            LOG.info("Loaded " + books.size() + " bookshelf items");
        } catch (Exception e) {
            LOG.error("Failed to load bookshelf", e);
        }
    }

    private void save() {
        try {
            Files.writeString(bookshelfFile, GSON.toJson(books), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Failed to save bookshelf", e);
        }
    }

    public void addBook(BookshelfItem item) {
        // Avoid duplicates by bookUrl
        books.removeIf(b -> b.getBookUrl().equals(item.getBookUrl()));
        books.addFirst(item);
        save();
    }

    public void removeBook(BookshelfItem item) {
        books.removeIf(b -> b.getBookUrl().equals(item.getBookUrl()));
        save();
    }

    public void updateProgress(BookshelfItem item, int chapterIndex, String chapterName) {
        for (BookshelfItem b : books) {
            if (b.getBookUrl().equals(item.getBookUrl())) {
                b.setLastReadChapter(chapterIndex);
                b.setLastChapterName(chapterName);
                b.setLastReadTime(System.currentTimeMillis());
                break;
            }
        }
        save();
    }

    public BookshelfItem findByBookUrl(String bookUrl) {
        return books.stream().filter(b -> b.getBookUrl().equals(bookUrl)).findFirst().orElse(null);
    }
}
```

**Step 2: Verify build**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/com/fish/toucher/service/BookshelfManager.java
git commit -m "feat: add BookshelfManager for bookshelf persistence"
```

---

## Task 5: Create ChapterCacheManager

**Files:**
- Create: `src/main/java/com/fish/toucher/service/ChapterCacheManager.java`

**Step 1: Implement ChapterCacheManager**

```java
package com.fish.toucher.service;

import com.fish.toucher.model.ChapterInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.List;

public class ChapterCacheManager {

    private static final Logger LOG = Logger.getInstance(ChapterCacheManager.class);
    private static final ChapterCacheManager INSTANCE = new ChapterCacheManager();
    private static final Gson GSON = new Gson();
    private static final long CHAPTER_LIST_TTL_MS = 24 * 60 * 60 * 1000L; // 24 hours

    private final Path cacheDir;

    public static ChapterCacheManager getInstance() { return INSTANCE; }

    private ChapterCacheManager() {
        cacheDir = Paths.get(System.getProperty("user.home"), ".config", "fish-toucher", "cache");
        try { Files.createDirectories(cacheDir); } catch (IOException e) { LOG.error("Failed to create cache dir", e); }
    }

    private Path getBookDir(String bookUrl) {
        String hash = md5(bookUrl);
        Path dir = cacheDir.resolve(hash);
        try { Files.createDirectories(dir); } catch (IOException e) { LOG.error("Failed to create book cache dir", e); }
        return dir;
    }

    // --- Chapter list cache ---

    public List<ChapterInfo> getCachedChapterList(String bookUrl) {
        Path file = getBookDir(bookUrl).resolve("chapters.json");
        if (!Files.exists(file)) return null;
        try {
            long lastModified = Files.getLastModifiedTime(file).toMillis();
            if (System.currentTimeMillis() - lastModified > CHAPTER_LIST_TTL_MS) {
                LOG.info("Chapter list cache expired for: " + bookUrl);
                return null;
            }
            String json = Files.readString(file, StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<ChapterInfo>>() {}.getType();
            return GSON.fromJson(json, listType);
        } catch (Exception e) {
            LOG.warn("Failed to read chapter list cache", e);
            return null;
        }
    }

    public void cacheChapterList(String bookUrl, List<ChapterInfo> chapters) {
        Path file = getBookDir(bookUrl).resolve("chapters.json");
        try {
            Files.writeString(file, GSON.toJson(chapters), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Failed to cache chapter list", e);
        }
    }

    // --- Content cache ---

    public String getCachedContent(String bookUrl, int chapterIndex) {
        Path file = getBookDir(bookUrl).resolve(chapterIndex + ".txt");
        if (!Files.exists(file)) return null;
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Failed to read content cache", e);
            return null;
        }
    }

    public void cacheContent(String bookUrl, int chapterIndex, String content) {
        Path file = getBookDir(bookUrl).resolve(chapterIndex + ".txt");
        try {
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Failed to cache content", e);
        }
    }

    // --- Clear cache for a book ---

    public void clearCache(String bookUrl) {
        Path dir = getBookDir(bookUrl);
        try (var stream = Files.list(dir)) {
            stream.forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
            Files.deleteIfExists(dir);
            LOG.info("Cleared cache for: " + bookUrl);
        } catch (IOException e) {
            LOG.warn("Failed to clear cache", e);
        }
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return String.format("%032x", new BigInteger(1, digest));
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
```

**Step 2: Verify build**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/com/fish/toucher/service/ChapterCacheManager.java
git commit -m "feat: add ChapterCacheManager for chapter content caching"
```

---

## Task 6: Create OnlineBookFetcher (HTTP + Parsing Engine)

**Files:**
- Create: `src/main/java/com/fish/toucher/service/OnlineBookFetcher.java`

**Step 1: Implement OnlineBookFetcher**

This is the core parsing engine. Supports HTML (Jsoup CSS selectors) and JSON (JsonPath).

```java
package com.fish.toucher.service;

import com.fish.toucher.model.BookSource;
import com.fish.toucher.model.ChapterInfo;
import com.fish.toucher.model.SearchResult;
import com.intellij.openapi.diagnostic.Logger;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.ProxySelector;
import java.net.URI;
import java.net.Proxy;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OnlineBookFetcher {

    private static final Logger LOG = Logger.getInstance(OnlineBookFetcher.class);
    private static final String DEFAULT_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    private final HttpClient httpClient;

    public OnlineBookFetcher() {
        this.httpClient = buildHttpClient();
    }

    // --- Search ---

    public List<SearchResult> search(BookSource source, String keyword) throws Exception {
        String url = source.getSearch().getUrl().replace("{{keyword}}", java.net.URLEncoder.encode(keyword, "UTF-8"));
        String body = fetch(url, source.getSearch().getMethod(), source.getHeader());
        String baseUrl = source.getUrl();

        List<SearchResult> results = new ArrayList<>();
        BookSource.SearchRule rule = source.getSearch();

        if ("json".equalsIgnoreCase(rule.getType())) {
            List<Map<String, Object>> items = JsonPath.read(body, rule.getList());
            for (Map<String, Object> item : items) {
                results.add(new SearchResult(
                        extractJsonValue(item, rule.getName()),
                        extractJsonValue(item, rule.getAuthor()),
                        resolveUrl(baseUrl, extractJsonValue(item, rule.getBookUrl())),
                        extractJsonValue(item, rule.getCoverUrl()),
                        source.getName()
                ));
            }
        } else {
            Document doc = Jsoup.parse(body, baseUrl);
            Elements elements = doc.select(rule.getList());
            for (Element el : elements) {
                results.add(new SearchResult(
                        extractHtml(el, rule.getName()),
                        extractHtml(el, rule.getAuthor()),
                        resolveUrl(baseUrl, extractHtml(el, rule.getBookUrl())),
                        extractHtml(el, rule.getCoverUrl()),
                        source.getName()
                ));
            }
        }
        return results;
    }

    // --- Chapter list ---

    public List<ChapterInfo> fetchChapterList(BookSource source, String bookUrl) throws Exception {
        String url = source.getChapter().getUrl().replace("{{bookUrl}}", bookUrl);
        String body = fetch(url, "GET", source.getHeader());
        String baseUrl = source.getUrl();

        List<ChapterInfo> chapters = new ArrayList<>();
        BookSource.ChapterRule rule = source.getChapter();

        if ("json".equalsIgnoreCase(rule.getType())) {
            List<Map<String, Object>> items = JsonPath.read(body, rule.getList());
            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> item = items.get(i);
                chapters.add(new ChapterInfo(
                        i,
                        extractJsonValue(item, rule.getName()),
                        resolveUrl(baseUrl, extractJsonValue(item, rule.getChapterUrl()))
                ));
            }
        } else {
            Document doc = Jsoup.parse(body, baseUrl);
            Elements elements = doc.select(rule.getList());
            for (int i = 0; i < elements.size(); i++) {
                Element el = elements.get(i);
                chapters.add(new ChapterInfo(
                        i,
                        extractHtml(el, rule.getName()),
                        resolveUrl(baseUrl, extractHtml(el, rule.getChapterUrl()))
                ));
            }
        }
        return chapters;
    }

    // --- Chapter content ---

    public List<String> fetchContent(BookSource source, String chapterUrl) throws Exception {
        String url = source.getContent().getUrl().replace("{{chapterUrl}}", chapterUrl);
        String body = fetch(url, "GET", source.getHeader());
        BookSource.ContentRule rule = source.getContent();

        List<String> lines = new ArrayList<>();

        if ("json".equalsIgnoreCase(rule.getType())) {
            String text = JsonPath.read(body, rule.getSelector());
            // Strip HTML tags if present in JSON content
            text = Jsoup.parse(text).text();
            for (String line : text.split("\\r?\\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) lines.add(trimmed);
            }
        } else {
            Document doc = Jsoup.parse(body);
            // Purify: remove unwanted elements
            if (rule.getPurify() != null) {
                for (String purifySelector : rule.getPurify()) {
                    doc.select(purifySelector).remove();
                }
            }
            Elements content = doc.select(rule.getSelector());
            // Convert <br> and <p> to newlines, then extract text
            String html = content.html();
            html = html.replaceAll("<br\\s*/?>", "\n");
            html = html.replaceAll("</p>", "\n");
            String text = Jsoup.parse(html).text();
            for (String line : text.split("\\r?\\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) lines.add(trimmed);
            }
        }
        return lines;
    }

    // --- HTTP fetch ---

    private String fetch(String url, String method, Map<String, String> headers) throws Exception {
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", DEFAULT_UA);

        if (headers != null) {
            headers.forEach(reqBuilder::header);
        }

        if ("POST".equalsIgnoreCase(method)) {
            reqBuilder.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            reqBuilder.GET();
        }

        HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
        LOG.info("fetch: " + url + " -> " + response.statusCode());
        return response.body();
    }

    private HttpClient buildHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL);
        try {
            ProxySelector proxySelector = ProxySelector.getDefault();
            if (proxySelector != null) {
                var proxies = proxySelector.select(URI.create("https://www.google.com"));
                boolean hasDirect = proxies.size() == 1 && proxies.getFirst().type() == Proxy.Type.DIRECT;
                if (!hasDirect) builder.proxy(proxySelector);
            }
        } catch (Exception e) {
            LOG.warn("Failed to configure proxy", e);
        }
        return builder.build();
    }

    // --- HTML extraction helpers ---

    private String extractHtml(Element parent, String rule) {
        if (rule == null || rule.isEmpty()) return "";
        // Rule format: "selector@attr" or "@attr" (use parent itself)
        String selector;
        String attr;
        int atIdx = rule.lastIndexOf('@');
        if (atIdx < 0) {
            // No @ — treat as CSS selector, get text
            Element el = parent.selectFirst(rule);
            return el != null ? el.text().trim() : "";
        }
        selector = rule.substring(0, atIdx);
        attr = rule.substring(atIdx + 1);
        Element el = selector.isEmpty() ? parent : parent.selectFirst(selector);
        if (el == null) return "";
        return switch (attr) {
            case "text" -> el.text().trim();
            case "href" -> el.absUrl("href");
            case "src" -> el.absUrl("src");
            default -> el.attr(attr).trim();
        };
    }

    // --- JSON extraction helpers ---

    private String extractJsonValue(Map<String, Object> item, String key) {
        if (key == null || key.isEmpty()) return "";
        Object val = item.get(key);
        return val != null ? val.toString() : "";
    }

    // --- URL resolution ---

    private String resolveUrl(String baseUrl, String url) {
        if (url == null || url.isEmpty()) return "";
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        if (url.startsWith("//")) return "https:" + url;
        if (url.startsWith("/")) return baseUrl.replaceAll("(https?://[^/]+).*", "$1") + url;
        return baseUrl + (baseUrl.endsWith("/") ? "" : "/") + url;
    }
}
```

**Step 2: Verify build**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/com/fish/toucher/service/OnlineBookFetcher.java
git commit -m "feat: add OnlineBookFetcher with HTML/JSON parsing engine"
```

---

## Task 7: Add loadFromLines to NovelReaderManager

**Files:**
- Modify: `src/main/java/com/fish/toucher/ui/NovelReaderManager.java`

**Step 1: Add loadFromLines method**

Add after the `loadFile` method (after line 147):

```java
/**
 * Load content from lines directly (for online sources).
 * @param virtualPath a virtual identifier like "online://sourceName/bookName/chapter"
 * @param lines the content lines to display
 */
public void loadFromLines(String virtualPath, List<String> lines) {
    rawLines.clear();
    rawLines.addAll(lines);
    currentFilePath = virtualPath;
    currentLine = 0;
    visible = true;
    LOG.info("loadFromLines: loaded " + rawLines.size() + " lines from " + virtualPath);
    fireChange();
}
```

**Step 2: Verify build**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/com/fish/toucher/ui/NovelReaderManager.java
git commit -m "feat: add loadFromLines method for online content injection"
```

---

## Task 8: Create ChapterListDialog

**Files:**
- Create: `src/main/java/com/fish/toucher/ui/dialog/ChapterListDialog.java`

**Step 1: Implement ChapterListDialog**

```java
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
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class ChapterListDialog extends DialogWrapper {

    private static final Logger LOG = Logger.getInstance(ChapterListDialog.class);

    private final BookshelfItem book;
    private final BookSource source;
    private JList<ChapterInfo> chapterList;
    private DefaultListModel<ChapterInfo> listModel;
    private JLabel statusLabel;

    public ChapterListDialog(@Nullable Project project, BookshelfItem book) {
        super(project, false);
        this.book = book;
        this.source = BookSourceManager.getInstance().getSources().stream()
                .filter(s -> s.getName().equals(book.getSourceName()))
                .findFirst().orElse(null);
        setTitle(book.getName() + " - 章节目录");
        setSize(400, 500);
        init();
        loadChapters();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        listModel = new DefaultListModel<>();
        chapterList = new JList<>(listModel);
        chapterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chapterList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value.getName());
            label.setBorder(new EmptyBorder(3, 8, 3, 8));
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
                label.setOpaque(true);
            }
            // Highlight last read chapter
            if (index == book.getLastReadChapter()) {
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }
            return label;
        });
        chapterList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && chapterList.getSelectedValue() != null) {
                loadChapterContent(chapterList.getSelectedValue());
            }
        });

        JScrollPane scrollPane = new JScrollPane(chapterList);
        panel.add(scrollPane, BorderLayout.CENTER);

        statusLabel = new JLabel("Loading...");
        statusLabel.setBorder(new EmptyBorder(5, 8, 5, 8));
        panel.add(statusLabel, BorderLayout.SOUTH);

        return panel;
    }

    @Override
    protected Action @[] createActions() {
        return new Action[]{getOKAction()};
    }

    private void loadChapters() {
        if (source == null) {
            statusLabel.setText("Book source not found: " + book.getSourceName());
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // Try cache first
                ChapterCacheManager cache = ChapterCacheManager.getInstance();
                List<ChapterInfo> chapters = cache.getCachedChapterList(book.getBookUrl());
                if (chapters == null) {
                    OnlineBookFetcher fetcher = new OnlineBookFetcher();
                    chapters = fetcher.fetchChapterList(source, book.getBookUrl());
                    cache.cacheChapterList(book.getBookUrl(), chapters);
                }
                List<ChapterInfo> finalChapters = chapters;
                ApplicationManager.getApplication().invokeLater(() -> {
                    listModel.clear();
                    for (ChapterInfo ch : finalChapters) listModel.addElement(ch);
                    statusLabel.setText(finalChapters.size() + " chapters");
                    // Update bookshelf total chapters
                    book.setTotalChapters(finalChapters.size());
                    BookshelfManager.getInstance().addBook(book);
                    // Scroll to last read position
                    if (book.getLastReadChapter() > 0 && book.getLastReadChapter() < finalChapters.size()) {
                        chapterList.ensureIndexIsVisible(book.getLastReadChapter());
                    }
                });
            } catch (Exception e) {
                LOG.error("Failed to fetch chapter list", e);
                ApplicationManager.getApplication().invokeLater(() ->
                        statusLabel.setText("Failed: " + e.getMessage()));
            }
        });
    }

    private void loadChapterContent(ChapterInfo chapter) {
        if (source == null) return;
        statusLabel.setText("Loading: " + chapter.getName() + "...");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ChapterCacheManager cache = ChapterCacheManager.getInstance();
                String cachedContent = cache.getCachedContent(book.getBookUrl(), chapter.getIndex());
                List<String> lines;
                if (cachedContent != null) {
                    lines = List.of(cachedContent.split("\\r?\\n"));
                } else {
                    OnlineBookFetcher fetcher = new OnlineBookFetcher();
                    lines = fetcher.fetchContent(source, chapter.getChapterUrl());
                    cache.cacheContent(book.getBookUrl(), chapter.getIndex(), String.join("\n", lines));
                }
                ApplicationManager.getApplication().invokeLater(() -> {
                    String virtualPath = "online://" + book.getSourceName() + "/" + book.getName() + "/" + chapter.getName();
                    NovelReaderManager.getInstance().loadFromLines(virtualPath, lines);
                    BookshelfManager.getInstance().updateProgress(book, chapter.getIndex(), chapter.getName());
                    statusLabel.setText("Loaded: " + chapter.getName());
                });
            } catch (Exception e) {
                LOG.error("Failed to fetch chapter content", e);
                ApplicationManager.getApplication().invokeLater(() ->
                        statusLabel.setText("Failed: " + e.getMessage()));
            }
        });
    }
}
```

**Step 2: Verify build**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/com/fish/toucher/ui/dialog/ChapterListDialog.java
git commit -m "feat: add ChapterListDialog for chapter selection and content loading"
```

---

## Task 9: Create OnlineBookDialog (Bookshelf + Search)

**Files:**
- Create: `src/main/java/com/fish/toucher/ui/dialog/OnlineBookDialog.java`

**Step 1: Implement OnlineBookDialog**

```java
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
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class OnlineBookDialog extends DialogWrapper {

    private static final Logger LOG = Logger.getInstance(OnlineBookDialog.class);
    private final Project project;

    // Bookshelf tab
    private DefaultListModel<BookshelfItem> bookshelfModel;
    private JList<BookshelfItem> bookshelfList;

    // Search tab
    private JComboBox<String> sourceSelector;
    private JTextField searchField;
    private DefaultListModel<SearchResult> searchResultModel;
    private JList<SearchResult> searchResultList;
    private JLabel searchStatusLabel;

    public OnlineBookDialog(@Nullable Project project) {
        super(project, true);
        this.project = project;
        setTitle("Online Book Source");
        setSize(500, 600);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("书架", createBookshelfPanel());
        tabbedPane.addTab("搜索", createSearchPanel());
        return tabbedPane;
    }

    @Override
    protected Action @[] createActions() {
        return new Action[]{getOKAction()};
    }

    // ========== Bookshelf Tab ==========

    private JPanel createBookshelfPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        bookshelfModel = new DefaultListModel<>();
        bookshelfList = new JList<>(bookshelfModel);
        bookshelfList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JPanel cell = new JPanel(new BorderLayout());
            cell.setBorder(new EmptyBorder(5, 8, 5, 8));
            JLabel nameLabel = new JLabel(value.getName() + " - " + value.getAuthor());
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
            JLabel progressLabel = new JLabel(
                    "Source: " + value.getSourceName()
                            + " | Chapter " + value.getLastReadChapter() + "/" + value.getTotalChapters()
                            + (value.getLastChapterName().isEmpty() ? "" : " | " + value.getLastChapterName()));
            progressLabel.setFont(progressLabel.getFont().deriveFont(Font.PLAIN, 11f));
            progressLabel.setForeground(JBColor.GRAY);
            cell.add(nameLabel, BorderLayout.NORTH);
            cell.add(progressLabel, BorderLayout.SOUTH);
            if (isSelected) {
                cell.setBackground(list.getSelectionBackground());
                nameLabel.setForeground(list.getSelectionForeground());
                cell.setOpaque(true);
            }
            return cell;
        });

        refreshBookshelf();

        JScrollPane scrollPane = new JScrollPane(bookshelfList);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Bottom buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JButton readBtn = new JButton("继续阅读");
        readBtn.addActionListener(e -> openSelectedBook());
        JButton removeBtn = new JButton("移除");
        removeBtn.addActionListener(e -> removeSelectedBook());
        JButton clearCacheBtn = new JButton("清除缓存");
        clearCacheBtn.addActionListener(e -> clearSelectedBookCache());
        buttonPanel.add(readBtn);
        buttonPanel.add(removeBtn);
        buttonPanel.add(clearCacheBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshBookshelf() {
        bookshelfModel.clear();
        for (BookshelfItem item : BookshelfManager.getInstance().getBooks()) {
            bookshelfModel.addElement(item);
        }
    }

    private void openSelectedBook() {
        BookshelfItem selected = bookshelfList.getSelectedValue();
        if (selected == null) return;
        new ChapterListDialog(project, selected).show();
    }

    private void removeSelectedBook() {
        BookshelfItem selected = bookshelfList.getSelectedValue();
        if (selected == null) return;
        BookshelfManager.getInstance().removeBook(selected);
        refreshBookshelf();
    }

    private void clearSelectedBookCache() {
        BookshelfItem selected = bookshelfList.getSelectedValue();
        if (selected == null) return;
        ChapterCacheManager.getInstance().clearCache(selected.getBookUrl());
        JOptionPane.showMessageDialog(getContentPanel(), "Cache cleared for: " + selected.getName());
    }

    // ========== Search Tab ==========

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Top: source selector + search field
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));
        topPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        List<BookSource> sources = BookSourceManager.getInstance().getEnabledSources();
        String[] sourceNames = sources.stream().map(BookSource::getName).toArray(String[]::new);
        sourceSelector = new JComboBox<>(sourceNames);
        sourceSelector.setPreferredSize(new Dimension(120, 28));
        topPanel.add(sourceSelector, BorderLayout.WEST);

        searchField = new JTextField();
        searchField.addActionListener(e -> doSearch());
        topPanel.add(searchField, BorderLayout.CENTER);

        JButton searchBtn = new JButton("搜索");
        searchBtn.addActionListener(e -> doSearch());
        topPanel.add(searchBtn, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.NORTH);

        // Center: search results
        searchResultModel = new DefaultListModel<>();
        searchResultList = new JList<>(searchResultModel);
        searchResultList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value.getName() + " - " + value.getAuthor());
            label.setBorder(new EmptyBorder(5, 8, 5, 8));
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
                label.setOpaque(true);
            }
            return label;
        });

        JScrollPane scrollPane = new JScrollPane(searchResultList);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Bottom: add to bookshelf + status
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 0));
        bottomPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JButton addBtn = new JButton("加入书架");
        addBtn.addActionListener(e -> addToBookshelf());
        bottomPanel.add(addBtn, BorderLayout.WEST);
        searchStatusLabel = new JLabel("");
        searchStatusLabel.setForeground(JBColor.GRAY);
        bottomPanel.add(searchStatusLabel, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void doSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) return;
        int sourceIdx = sourceSelector.getSelectedIndex();
        if (sourceIdx < 0) {
            searchStatusLabel.setText("No book source available");
            return;
        }
        List<BookSource> sources = BookSourceManager.getInstance().getEnabledSources();
        if (sourceIdx >= sources.size()) return;
        BookSource source = sources.get(sourceIdx);

        searchStatusLabel.setText("Searching...");
        searchResultModel.clear();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                OnlineBookFetcher fetcher = new OnlineBookFetcher();
                List<SearchResult> results = fetcher.search(source, keyword);
                ApplicationManager.getApplication().invokeLater(() -> {
                    searchResultModel.clear();
                    for (SearchResult r : results) searchResultModel.addElement(r);
                    searchStatusLabel.setText(results.size() + " results");
                });
            } catch (Exception e) {
                LOG.error("Search failed", e);
                ApplicationManager.getApplication().invokeLater(() ->
                        searchStatusLabel.setText("Search failed: " + e.getMessage()));
            }
        });
    }

    private void addToBookshelf() {
        SearchResult selected = searchResultList.getSelectedValue();
        if (selected == null) return;

        BookshelfItem item = new BookshelfItem();
        item.setName(selected.getName());
        item.setAuthor(selected.getAuthor());
        item.setSourceName(selected.getSourceName());
        item.setBookUrl(selected.getBookUrl());
        item.setCoverUrl(selected.getCoverUrl());
        item.setLastReadTime(System.currentTimeMillis());

        BookshelfManager.getInstance().addBook(item);
        refreshBookshelf();
        searchStatusLabel.setText("Added to bookshelf: " + selected.getName());
    }
}
```

**Step 2: Verify build**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/com/fish/toucher/ui/dialog/OnlineBookDialog.java
git commit -m "feat: add OnlineBookDialog with bookshelf and search tabs"
```

---

## Task 10: Create BookSourceEditDialog

**Files:**
- Create: `src/main/java/com/fish/toucher/ui/dialog/BookSourceEditDialog.java`

**Step 1: Implement BookSourceEditDialog**

A form dialog for editing book source rules, with fields matching the BookSource JSON structure.

```java
package com.fish.toucher.ui.dialog;

import com.fish.toucher.model.BookSource;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class BookSourceEditDialog extends DialogWrapper {

    // Basic fields
    private JTextField nameField;
    private JTextField urlField;
    private JCheckBox enabledCheckBox;
    private JTextField headersField;

    // Search rule fields
    private JTextField searchUrlField;
    private JComboBox<String> searchMethodCombo;
    private JComboBox<String> searchTypeCombo;
    private JTextField searchListField;
    private JTextField searchNameField;
    private JTextField searchAuthorField;
    private JTextField searchBookUrlField;
    private JTextField searchCoverUrlField;

    // Chapter rule fields
    private JTextField chapterUrlField;
    private JComboBox<String> chapterTypeCombo;
    private JTextField chapterListField;
    private JTextField chapterNameField;
    private JTextField chapterChapterUrlField;

    // Content rule fields
    private JTextField contentUrlField;
    private JComboBox<String> contentTypeCombo;
    private JTextField contentSelectorField;
    private JTextField contentPurifyField;

    private BookSource result;

    public BookSourceEditDialog(@Nullable BookSource existing) {
        super(true);
        setTitle(existing != null ? "Edit Book Source" : "New Book Source");
        init();
        if (existing != null) populateFrom(existing);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(3);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        // --- Basic ---
        row = addSectionTitle(panel, gbc, row, "Basic");
        nameField = new JTextField(30);
        row = addField(panel, gbc, row, "Name:", nameField);
        urlField = new JTextField(30);
        row = addField(panel, gbc, row, "Base URL:", urlField);
        enabledCheckBox = new JCheckBox("Enabled", true);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(enabledCheckBox, gbc);
        headersField = new JTextField(30);
        headersField.setToolTipText("Format: Key1:Value1, Key2:Value2");
        gbc.gridwidth = 1;
        row = addField(panel, gbc, row, "Headers:", headersField);

        // --- Search Rule ---
        row = addSectionTitle(panel, gbc, row, "Search Rule");
        searchUrlField = new JTextField(30);
        row = addField(panel, gbc, row, "Search URL:", searchUrlField);
        searchMethodCombo = new JComboBox<>(new String[]{"GET", "POST"});
        row = addField(panel, gbc, row, "Method:", searchMethodCombo);
        searchTypeCombo = new JComboBox<>(new String[]{"html", "json"});
        row = addField(panel, gbc, row, "Type:", searchTypeCombo);
        searchListField = new JTextField(30);
        row = addField(panel, gbc, row, "List Selector:", searchListField);
        searchNameField = new JTextField(30);
        row = addField(panel, gbc, row, "Name Selector:", searchNameField);
        searchAuthorField = new JTextField(30);
        row = addField(panel, gbc, row, "Author Selector:", searchAuthorField);
        searchBookUrlField = new JTextField(30);
        row = addField(panel, gbc, row, "Book URL Selector:", searchBookUrlField);
        searchCoverUrlField = new JTextField(30);
        row = addField(panel, gbc, row, "Cover URL Selector:", searchCoverUrlField);

        // --- Chapter Rule ---
        row = addSectionTitle(panel, gbc, row, "Chapter Rule");
        chapterUrlField = new JTextField(30);
        row = addField(panel, gbc, row, "Chapter URL:", chapterUrlField);
        chapterTypeCombo = new JComboBox<>(new String[]{"html", "json"});
        row = addField(panel, gbc, row, "Type:", chapterTypeCombo);
        chapterListField = new JTextField(30);
        row = addField(panel, gbc, row, "List Selector:", chapterListField);
        chapterNameField = new JTextField(30);
        row = addField(panel, gbc, row, "Name Selector:", chapterNameField);
        chapterChapterUrlField = new JTextField(30);
        row = addField(panel, gbc, row, "Chapter URL Selector:", chapterChapterUrlField);

        // --- Content Rule ---
        row = addSectionTitle(panel, gbc, row, "Content Rule");
        contentUrlField = new JTextField(30);
        row = addField(panel, gbc, row, "Content URL:", contentUrlField);
        contentTypeCombo = new JComboBox<>(new String[]{"html", "json"});
        row = addField(panel, gbc, row, "Type:", contentTypeCombo);
        contentSelectorField = new JTextField(30);
        row = addField(panel, gbc, row, "Content Selector:", contentSelectorField);
        contentPurifyField = new JTextField(30);
        contentPurifyField.setToolTipText("Comma-separated CSS selectors to remove, e.g.: script, div.ad");
        addField(panel, gbc, row, "Purify:", contentPurifyField);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(new Dimension(500, 600));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    private int addSectionTitle(JPanel panel, GridBagConstraints gbc, int row, String title) {
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setBorder(JBUI.Borders.emptyTop(8));
        panel.add(label, gbc);
        gbc.gridwidth = 1;
        return row;
    }

    private int addField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.gridy = row++;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
        gbc.weightx = 0;
        return row;
    }

    private void populateFrom(BookSource s) {
        nameField.setText(s.getName());
        urlField.setText(s.getUrl());
        enabledCheckBox.setSelected(s.isEnabled());
        if (s.getHeader() != null) {
            headersField.setText(s.getHeader().entrySet().stream()
                    .map(e -> e.getKey() + ":" + e.getValue())
                    .collect(Collectors.joining(", ")));
        }
        if (s.getSearch() != null) {
            searchUrlField.setText(s.getSearch().getUrl());
            searchMethodCombo.setSelectedItem(s.getSearch().getMethod());
            searchTypeCombo.setSelectedItem(s.getSearch().getType());
            searchListField.setText(s.getSearch().getList());
            searchNameField.setText(s.getSearch().getName());
            searchAuthorField.setText(s.getSearch().getAuthor());
            searchBookUrlField.setText(s.getSearch().getBookUrl());
            searchCoverUrlField.setText(s.getSearch().getCoverUrl());
        }
        if (s.getChapter() != null) {
            chapterUrlField.setText(s.getChapter().getUrl());
            chapterTypeCombo.setSelectedItem(s.getChapter().getType());
            chapterListField.setText(s.getChapter().getList());
            chapterNameField.setText(s.getChapter().getName());
            chapterChapterUrlField.setText(s.getChapter().getChapterUrl());
        }
        if (s.getContent() != null) {
            contentUrlField.setText(s.getContent().getUrl());
            contentTypeCombo.setSelectedItem(s.getContent().getType());
            contentSelectorField.setText(s.getContent().getSelector());
            if (s.getContent().getPurify() != null) {
                contentPurifyField.setText(String.join(", ", s.getContent().getPurify()));
            }
        }
    }

    @Override
    protected void doOKAction() {
        result = new BookSource();
        result.setName(nameField.getText().trim());
        result.setUrl(urlField.getText().trim());
        result.setEnabled(enabledCheckBox.isSelected());

        // Parse headers
        String headersText = headersField.getText().trim();
        if (!headersText.isEmpty()) {
            Map<String, String> headers = new LinkedHashMap<>();
            for (String pair : headersText.split(",")) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) headers.put(kv[0].trim(), kv[1].trim());
            }
            result.setHeader(headers);
        }

        BookSource.SearchRule search = new BookSource.SearchRule();
        search.setUrl(searchUrlField.getText().trim());
        search.setMethod((String) searchMethodCombo.getSelectedItem());
        search.setType((String) searchTypeCombo.getSelectedItem());
        search.setList(searchListField.getText().trim());
        search.setName(searchNameField.getText().trim());
        search.setAuthor(searchAuthorField.getText().trim());
        search.setBookUrl(searchBookUrlField.getText().trim());
        search.setCoverUrl(searchCoverUrlField.getText().trim());
        result.setSearch(search);

        BookSource.ChapterRule chapter = new BookSource.ChapterRule();
        chapter.setUrl(chapterUrlField.getText().trim());
        chapter.setType((String) chapterTypeCombo.getSelectedItem());
        chapter.setList(chapterListField.getText().trim());
        chapter.setName(chapterNameField.getText().trim());
        chapter.setChapterUrl(chapterChapterUrlField.getText().trim());
        result.setChapter(chapter);

        BookSource.ContentRule content = new BookSource.ContentRule();
        content.setUrl(contentUrlField.getText().trim());
        content.setType((String) contentTypeCombo.getSelectedItem());
        content.setSelector(contentSelectorField.getText().trim());
        String purifyText = contentPurifyField.getText().trim();
        if (!purifyText.isEmpty()) {
            content.setPurify(Arrays.stream(purifyText.split(",")).map(String::trim).toList());
        }
        result.setContent(content);

        super.doOKAction();
    }

    public BookSource getResult() { return result; }
}
```

**Step 2: Verify build**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/com/fish/toucher/ui/dialog/BookSourceEditDialog.java
git commit -m "feat: add BookSourceEditDialog for visual book source rule editing"
```

---

## Task 11: Add Online Button to NovelReaderPanel

**Files:**
- Modify: `src/main/java/com/fish/toucher/ui/NovelReaderPanel.java`

**Step 1: Add import and online button**

Add import at top:
```java
import com.fish.toucher.ui.dialog.OnlineBookDialog;
```

After line 69 (`navPanel.add(nextBtn);`), add the online button:

```java
JButton onlineBtn = createSmallButton("📡");
onlineBtn.setToolTipText("Online book source");
onlineBtn.addActionListener(e -> {
    OnlineBookDialog dialog = new OnlineBookDialog(project);
    dialog.show();
});
navPanel.add(onlineBtn);
```

**Step 2: Verify build**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/java/com/fish/toucher/ui/NovelReaderPanel.java
git commit -m "feat: add online book source button to NovelReaderPanel toolbar"
```

---

## Task 12: Add Book Source Management to Settings UI

**Files:**
- Modify: `src/main/java/com/fish/toucher/settings/NovelReaderConfigurable.java`

**Step 1: Add book source management section**

Add imports at top:
```java
import com.fish.toucher.model.BookSource;
import com.fish.toucher.service.BookSourceManager;
import com.fish.toucher.ui.dialog.BookSourceEditDialog;
import java.io.File;
import java.nio.file.Files;
```

Add a field:
```java
private JList<String> sourceList;
private DefaultListModel<String> sourceListModel;
```

In `createComponent()`, after the shortcuts section (after line 303, `novelSettingsPanel.add(shortcutToggleField, ngbc);`), add the book source management section:

```java
ngbc.gridx = 0; ngbc.gridy = nrow++; ngbc.gridwidth = 2;
novelSettingsPanel.add(new JSeparator(), ngbc);

// Online Book Sources section
ngbc.gridx = 0; ngbc.gridy = nrow++; ngbc.gridwidth = 2;
JLabel sourcesTitle = new JLabel("Online Book Sources");
sourcesTitle.setFont(sourcesTitle.getFont().deriveFont(Font.BOLD, 12f));
novelSettingsPanel.add(sourcesTitle, ngbc);

ngbc.gridx = 0; ngbc.gridy = nrow++; ngbc.gridwidth = 2;
JPanel sourceBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
JButton importBtn = new JButton("Import JSON");
importBtn.addActionListener(e -> importBookSource());
JButton exportBtn = new JButton("Export JSON");
exportBtn.addActionListener(e -> exportBookSources());
JButton newSourceBtn = new JButton("New Source");
newSourceBtn.addActionListener(e -> newBookSource());
sourceBtnPanel.add(importBtn);
sourceBtnPanel.add(exportBtn);
sourceBtnPanel.add(newSourceBtn);
novelSettingsPanel.add(sourceBtnPanel, ngbc);

ngbc.gridx = 0; ngbc.gridy = nrow++; ngbc.gridwidth = 2;
sourceListModel = new DefaultListModel<>();
sourceList = new JList<>(sourceListModel);
sourceList.setVisibleRowCount(4);
refreshSourceList();
JScrollPane sourceScroll = new JScrollPane(sourceList);
sourceScroll.setPreferredSize(new Dimension(300, 100));
novelSettingsPanel.add(sourceScroll, ngbc);

ngbc.gridx = 0; ngbc.gridy = nrow++; ngbc.gridwidth = 2;
JPanel sourceActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
JButton editSourceBtn = new JButton("Edit");
editSourceBtn.addActionListener(e -> editSelectedSource());
JButton deleteSourceBtn = new JButton("Delete");
deleteSourceBtn.addActionListener(e -> deleteSelectedSource());
sourceActionPanel.add(editSourceBtn);
sourceActionPanel.add(deleteSourceBtn);
novelSettingsPanel.add(sourceActionPanel, ngbc);
```

**Step 2: Add helper methods**

Add these methods to the class:

```java
private void refreshSourceList() {
    if (sourceListModel == null) return;
    sourceListModel.clear();
    for (BookSource source : BookSourceManager.getInstance().getSources()) {
        String prefix = source.isEnabled() ? "☑ " : "☐ ";
        sourceListModel.addElement(prefix + source.getName() + "  " + source.getUrl());
    }
}

private void importBookSource() {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        try {
            String json = Files.readString(chooser.getSelectedFile().toPath());
            BookSourceManager.getInstance().importFromJson(json);
            refreshSourceList();
            Messages.showInfoMessage("Book source imported successfully", "Fish Toucher");
        } catch (Exception e) {
            Messages.showErrorDialog("Import failed: " + e.getMessage(), "Fish Toucher");
        }
    }
}

private void exportBookSources() {
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new File("book_sources.json"));
    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        try {
            String json = BookSourceManager.getInstance().exportAllToJson();
            Files.writeString(chooser.getSelectedFile().toPath(), json);
            Messages.showInfoMessage("Exported successfully", "Fish Toucher");
        } catch (Exception e) {
            Messages.showErrorDialog("Export failed: " + e.getMessage(), "Fish Toucher");
        }
    }
}

private void newBookSource() {
    BookSourceEditDialog dialog = new BookSourceEditDialog(null);
    if (dialog.showAndGet()) {
        BookSource source = dialog.getResult();
        if (source != null) {
            BookSourceManager.getInstance().save(source);
            refreshSourceList();
        }
    }
}

private void editSelectedSource() {
    int idx = sourceList.getSelectedIndex();
    if (idx < 0) return;
    java.util.List<BookSource> sources = BookSourceManager.getInstance().getSources();
    if (idx >= sources.size()) return;
    BookSource existing = sources.get(idx);
    BookSourceEditDialog dialog = new BookSourceEditDialog(existing);
    if (dialog.showAndGet()) {
        BookSource updated = dialog.getResult();
        if (updated != null) {
            // Delete old if name changed
            if (!existing.getName().equals(updated.getName())) {
                BookSourceManager.getInstance().delete(existing);
            }
            BookSourceManager.getInstance().save(updated);
            refreshSourceList();
        }
    }
}

private void deleteSelectedSource() {
    int idx = sourceList.getSelectedIndex();
    if (idx < 0) return;
    java.util.List<BookSource> sources = BookSourceManager.getInstance().getSources();
    if (idx >= sources.size()) return;
    BookSource source = sources.get(idx);
    int confirm = Messages.showYesNoDialog("Delete source: " + source.getName() + "?", "Fish Toucher", Messages.getQuestionIcon());
    if (confirm == Messages.YES) {
        BookSourceManager.getInstance().delete(source);
        refreshSourceList();
    }
}
```

**Step 3: Verify build**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/main/java/com/fish/toucher/settings/NovelReaderConfigurable.java
git commit -m "feat: add book source management section to settings UI"
```

---

## Task 13: Add i18n Messages

**Files:**
- Modify: `src/main/resources/messages/FishToucherBundle.properties`
- Modify: `src/main/resources/messages/FishToucherBundle_zh.properties`

**Step 1: Add English messages**

Append to `FishToucherBundle.properties`:

```properties
# Online Book Source
settings.section.onlineBookSource=Online Book Sources
settings.button.importSource=Import JSON
settings.button.exportSource=Export JSON
settings.button.newSource=New Source
settings.button.editSource=Edit
settings.button.deleteSource=Delete
dialog.onlineBook.title=Online Book Source
dialog.onlineBook.bookshelfTab=Bookshelf
dialog.onlineBook.searchTab=Search
dialog.onlineBook.continueReading=Continue Reading
dialog.onlineBook.remove=Remove
dialog.onlineBook.clearCache=Clear Cache
dialog.onlineBook.search=Search
dialog.onlineBook.addToBookshelf=Add to Bookshelf
dialog.onlineBook.searching=Searching...
dialog.onlineBook.loading=Loading...
dialog.onlineBook.noSource=No book source available
```

**Step 2: Add Chinese messages**

Append to `FishToucherBundle_zh.properties`:

```properties
# Online Book Source
settings.section.onlineBookSource=\u5728\u7ebf\u4e66\u6e90
settings.button.importSource=\u5bfc\u5165JSON
settings.button.exportSource=\u5bfc\u51faJSON
settings.button.newSource=\u65b0\u5efa\u4e66\u6e90
settings.button.editSource=\u7f16\u8f91
settings.button.deleteSource=\u5220\u9664
dialog.onlineBook.title=\u5728\u7ebf\u4e66\u6e90
dialog.onlineBook.bookshelfTab=\u4e66\u67b6
dialog.onlineBook.searchTab=\u641c\u7d22
dialog.onlineBook.continueReading=\u7ee7\u7eed\u9605\u8bfb
dialog.onlineBook.remove=\u79fb\u9664
dialog.onlineBook.clearCache=\u6e05\u9664\u7f13\u5b58
dialog.onlineBook.search=\u641c\u7d22
dialog.onlineBook.addToBookshelf=\u52a0\u5165\u4e66\u67b6
dialog.onlineBook.searching=\u641c\u7d22\u4e2d...
dialog.onlineBook.loading=\u52a0\u8f7d\u4e2d...
dialog.onlineBook.noSource=\u6ca1\u6709\u53ef\u7528\u4e66\u6e90
```

**Step 3: Commit**

```bash
git add src/main/resources/messages/
git commit -m "feat: add i18n messages for online book source feature"
```

---

## Task 14: Update plugin.xml Version and Change Notes

**Files:**
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: Update version to 2.4.0 and add change notes**

Update `<version>` from `2.3.1` to `2.4.0`.

Add change notes entry:

```xml
<b>2.4.0</b>
<ul>
    <li>New: Custom online book source support with search, bookshelf, and chapter caching<br/>
        新增：自定义在线书源，支持搜索、书架、章节缓存</li>
    <li>New: Book source rule editor with JSON import/export<br/>
        新增：书源规则编辑器，支持JSON导入导出</li>
    <li>New: CSS selector + JSONPath parsing for HTML and API sources<br/>
        新增：CSS选择器 + JSONPath解析，支持HTML和API书源</li>
</ul>
```

Also update `build.gradle.kts` version from `"2.3.1"` to `"2.4.0"`.

**Step 2: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/resources/META-INF/plugin.xml build.gradle.kts
git commit -m "feat: bump version to 2.4.0, add online book source change notes"
```

---

## Task 15: Full Build and Smoke Test

**Step 1: Full clean build**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL

**Step 2: Verify plugin artifact**

Run: `ls -la build/distributions/`
Expected: `.zip` file present with updated name

**Step 3: Final commit if needed**

Fix any build issues and commit.
