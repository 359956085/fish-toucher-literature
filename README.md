# Fish Toucher — IntelliJ IDEA Plugin

A slacking-off plugin for IntelliJ IDEA — read novels or browse hot search trends while pretending to work.

一款适用于 IntelliJ IDEA 的摸鱼插件 — 在假装工作的同时看小说或浏览热搜。

**Supported IDEs:** IntelliJ IDEA 2024.2+ (Build 242 ~ 253.*)

## Features / 功能特性

### Novel Reading Mode / 小说阅读模式

| Feature | Description |
|---------|-------------|
| **Stealth Mode** | One line of text in the status bar — looks like a normal status message. Click to advance. |
| **Normal Mode** | Multi-line display in tool window, disguised as build log output, with page navigation and progress slider. |
| **Unified Progress** | Both modes share the same reading position per file. |
| **Boss Key** | Instantly hide all content with a keyboard shortcut. |
| **Auto Charset** | Automatically detects UTF-8 / GBK / UTF-16 encoding. |
| **Customizable** | Font, font size, chars per line, lines per page — all configurable. |

| 功能 | 说明 |
|------|------|
| **隐秘模式** | 状态栏显示一行文本，外观与普通状态信息无异，点击翻页 |
| **普通模式** | 工具窗口多行显示，伪装成构建日志，支持翻页和进度滑块 |
| **统一进度** | 两种模式共用统一阅读进度 |
| **老板键** | 快捷键一键隐藏所有阅读内容 |
| **编码自适应** | 自动识别 UTF-8 / GBK / UTF-16 编码 |
| **可配置** | 字体、字号、每行字数、每页行数均可自定义 |

### Hot Search Carousel Mode / 热搜轮播模式

| Feature | Description |
|---------|-------------|
| **Multiple Sources** | Baidu, Toutiao, Zhihu, Douyin, Kuaishou — switchable in settings or tool window. |
| **Real-time Data** | Auto-refreshing hot search trends displayed in both tool window and status bar. |
| **Click to Open** | Click any hot search title to open it in the default browser. |
| **Carousel** | Automatic rotation with configurable interval. |

| 功能 | 说明 |
|------|------|
| **多热搜源** | 百度、今日头条、知乎、抖音、快手，可在设置或工具窗口中切换 |
| **实时数据** | 自动刷新热搜榜单，同时显示在工具窗口和状态栏 |
| **点击打开** | 点击热搜标题直接在浏览器中打开 |
| **轮播** | 自动轮播，间隔可自定义 |

### i18n / 国际化

Settings UI automatically follows IntelliJ IDEA's language setting (Chinese / English).

设置界面自动跟随 IDEA 语言设置（中文 / 英文）。

## Keyboard Shortcuts / 快捷键

| Shortcut | Action |
|----------|--------|
| `Ctrl + Shift + Alt + M` | Open novel file / 打开小说文件 |
| `Alt + Shift + Right` | Next page / 下一页 |
| `Alt + Shift + Left` | Previous page / 上一页 |
| `Alt + Shift + H` | Toggle visibility (Boss key) / 显示/隐藏 (老板键) |

All shortcuts are customizable in Settings → Tools → Fish Toucher.

所有快捷键均可在 Settings → Tools → Fish Toucher 中自定义。

## Installation / 安装

### From Disk / 本地安装

1. Download or build the plugin `.zip` file
2. Open IDEA → `Settings` → `Plugins` → gear icon → `Install Plugin from Disk...`
3. Select the `.zip` file and restart IDEA

### Build from Source / 从源码构建

**Requirements:** JDK 21+, Gradle 8.13+

```bash
./gradlew buildPlugin
```

The plugin zip will be at `build/distributions/`.

## Settings / 设置

`Settings → Tools → Fish Toucher`

**Plugin Mode / 插件模式**
- Switch between Novel Reading and Hot Search Carousel modes
- 在小说阅读和热搜轮播模式之间切换

**Novel Settings / 小说设置**
- Stealth mode: chars per line, status bar toggle
- Normal mode: lines per page, chars per line
- Font family and size
- Keyboard shortcuts

**Hot Search Settings / 热搜设置**
- Source: Baidu / Toutiao / Zhihu / Douyin / Kuaishou
- Carousel interval (3–120 sec)
- Data refresh interval (1–120 min)

## Project Structure / 项目结构

```
src/main/
├── java/com/fish/toucher/
│   ├── FishToucherBundle.java              # i18n bundle
│   ├── ShortcutInitializer.java            # Auto-start on project open
│   ├── actions/
│   │   ├── OpenNovelAction.java            # Open novel file
│   │   ├── NextPageAction.java             # Next page
│   │   ├── PrevPageAction.java             # Previous page
│   │   └── ToggleVisibilityAction.java     # Boss key toggle
│   ├── settings/
│   │   ├── NovelReaderSettings.java        # Persistent settings
│   │   ├── NovelReaderConfigurable.java    # Settings UI
│   │   └── ShortcutKeyField.java           # Custom shortcut input
│   └── ui/
│       ├── NovelReaderManager.java         # Novel reading core
│       ├── NovelReaderPanel.java           # Novel tool window panel
│       ├── HotSearchManager.java           # Hot search data & carousel
│       ├── HotSearchPanel.java             # Hot search tool window panel
│       ├── NovelReaderToolWindowFactory.java
│       ├── NovelReaderStatusBarWidget.java
│       └── NovelReaderWidgetFactory.java
└── resources/
    ├── META-INF/
    │   ├── plugin.xml                      # Plugin descriptor
    │   ├── pluginIcon.svg                  # Plugin icon (40x40)
    │   └── book_icon.svg                   # Tool window icon (13x13)
    └── messages/
        ├── FishToucherBundle.properties        # English
        └── FishToucherBundle_zh.properties     # Chinese
```

## License

MIT
