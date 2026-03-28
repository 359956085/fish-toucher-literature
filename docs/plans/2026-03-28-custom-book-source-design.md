# Custom Book Source (自定义书源) Design

## Overview

在 novel 模式中内嵌在线书源功能，用户配置书源规则（URL模板 + CSS选择器/JSONPath）后，可搜索、浏览、阅读在线小说。抓取的正文转为本地 rawLines，复用现有分页/进度/显示系统。书源规则存储为独立 JSON 文件，支持导入/导出/分享。

## Requirements

1. 搜索小说 — 输入书名，从书源搜索返回结果列表
2. 获取章节目录 — 选定小说后拉取章节列表
3. 获取章节正文 — 选定章节后抓取正文，转为 rawLines 加载到阅读器
4. 自定义请求头/Cookie — 支持需要鉴权的书源
5. 章节缓存 — 已读章节本地缓存，避免重复请求
6. 书架管理 — 收藏小说、记录进度、继续阅读

## Data Model

### BookSource JSON Format

```json
{
  "name": "示例书源",
  "url": "https://www.example.com",
  "enabled": true,
  "header": {
    "User-Agent": "Mozilla/5.0 ...",
    "Cookie": "optional_auth_cookie"
  },
  "search": {
    "url": "https://www.example.com/search?q={{keyword}}",
    "method": "GET",
    "type": "html",
    "list": "div.search-result .book-item",
    "name": "a.book-title@text",
    "author": "span.author@text",
    "bookUrl": "a.book-title@href",
    "coverUrl": "img.cover@src"
  },
  "chapter": {
    "url": "{{bookUrl}}",
    "type": "html",
    "list": "ul.chapter-list li a",
    "name": "@text",
    "chapterUrl": "@href"
  },
  "content": {
    "url": "{{chapterUrl}}",
    "type": "html",
    "selector": "div.chapter-content",
    "purify": ["script", "div.ad"]
  }
}
```

- `type`: `"html"` uses CSS selectors (Jsoup), `"json"` uses JSONPath
- `@text` / `@href` / `@src`: extract element text/attribute
- `{{keyword}}`, `{{bookUrl}}`, `{{chapterUrl}}`: template variables
- `header`: optional custom headers for authentication
- `purify`: selectors of elements to remove from content

### Bookshelf JSON Format

```json
{
  "books": [
    {
      "name": "小说名",
      "author": "作者",
      "sourceName": "示例书源",
      "bookUrl": "https://www.example.com/book/123",
      "coverUrl": "https://www.example.com/cover/123.jpg",
      "lastChapterName": "第100章 最新章节",
      "lastReadChapter": 50,
      "lastReadTime": 1711612800000,
      "totalChapters": 100
    }
  ]
}
```

### File Storage Layout

```
~/.config/fish-toucher/
├── sources/              # Book source rules
│   ├── example.json
│   └── ...
├── bookshelf.json        # Bookshelf data
└── cache/                # Chapter cache
    └── {md5(bookUrl)}/
        ├── chapters.json     # Chapter list cache (24h TTL)
        └── {chapterIndex}.txt # Content cache (no expiry)
```

## Architecture

### New Classes

| Class | Responsibility |
|-------|---------------|
| `BookSource` | Book source rule POJO, maps to JSON |
| `BookshelfItem` | Bookshelf entry POJO |
| `BookSourceManager` | Source file CRUD, manages `~/.config/fish-toucher/sources/` |
| `BookshelfManager` | Bookshelf data CRUD, manages `bookshelf.json` |
| `OnlineBookFetcher` | HTTP requests + content parsing (CSS selector / JSONPath) |
| `ChapterCacheManager` | Chapter cache read/write, manages `cache/` directory |
| `OnlineBookDialog` | Online book source dialog (bookshelf + search tabs) |
| `ChapterListDialog` | Chapter list selection dialog |
| `BookSourceEditDialog` | Book source rule editor dialog |

### Class Relationships

```
NovelReaderPanel
  └─ [Online] button → OnlineBookDialog
       ├─ Bookshelf tab → BookshelfManager → ChapterListDialog
       └─ Search tab → BookSourceManager + OnlineBookFetcher
                                              ↓
                                    ChapterCacheManager (cache content)
                                              ↓
                                    NovelReaderManager.loadFromLines(virtualPath, lines)
                                    (reuse existing pagination system)
```

### NovelReaderManager Change

One new method, no changes to existing logic:

```java
public void loadFromLines(String virtualPath, List<String> lines) {
    this.rawLines = lines;
    this.currentFilePath = virtualPath;  // e.g. "online://sourceName/bookName/chapter1"
    this.currentLine = 0;
    fireChange();
}
```

### New Dependencies

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("com.jayway.jsonpath:json-path:2.9.0")
    implementation("com.google.code.gson:gson:2.12.1")
}
```

## UI Design

### NovelReaderPanel Toolbar

Add an "Online" button to existing toolbar: `[Open File] [Prev] [Next] [Online 📡]`

### OnlineBookDialog (DialogWrapper)

Two tabs:
- **Bookshelf tab**: List of saved books with name/author/progress, [Continue Reading] and [Remove] buttons
- **Search tab**: Source selector dropdown + search input, results list with [Add to Bookshelf] button

### ChapterListDialog

Displays chapter list for selected book, click to load chapter content.

### Settings UI Extension

Add "Online Book Sources" section at bottom of novel mode settings:
- [Import JSON] [Export JSON] [New Source] buttons
- Source list with enable/disable checkboxes
- [Edit] [Delete] per source
- Edit opens BookSourceEditDialog with form fields matching BookSource JSON structure

## Parsing Engine

```
type == "html" → Jsoup.parse(html) → CSS selector extraction
type == "json" → JsonPath.read(json) → JSONPath extraction
    ↓
Template variable substitution: {{keyword}} {{bookUrl}} {{chapterUrl}}
    ↓
Purify: remove interference elements
    ↓
Split by paragraphs → List<String>
```

## Error Handling

- Request timeout (15s) → notification "Request timeout, check network or source URL"
- Parse failure (selector matches nothing) → notification "Parse failed, check source rules"
- Import validation: reject JSON missing required fields (name, url, search, chapter, content)
- Reuse IDE proxy settings via `ProxySelector.getDefault()`

## Cache Strategy

- Cached chapters read locally, no re-request
- No expiry for content (novel text doesn't change); user can manually clear cache from bookshelf
- Chapter list cache expires after 24 hours (for following updates)

## Chapter Navigation

- Reaching end of current chapter → show "Load Next Chapter" button at bottom
- Click loads next chapter (from cache or network) → inject into rawLines → reset to line 0
- Update bookshelf `lastReadChapter` progress

## Threading Model

- All network requests on pooled threads (`executeOnPooledThread()`)
- UI updates via `invokeLater()` back to EDT
- Loading indicator shown in dialogs during requests
