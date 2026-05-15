# Fish Toucher — IntelliJ IDEA Plugin

Fish Toucher is a lightweight IntelliJ IDEA plugin for reading novels, browsing hot searches, and playing idle cultivation inside the IDE.

Fish Toucher 是一款轻量 IntelliJ IDEA 插件，可在 IDE 内阅读小说、浏览热搜和放置修仙。

**Supported IDEs:** IntelliJ IDEA 2024.2+ (Build 242 ~ 261.*)

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
| **Multiple Sources** | Baidu, Toutiao, Zhihu, Douyin, Kuaishou, X Trends, Google Trends — switchable in settings or tool window. |
| **Real-time Data** | Auto-refreshing hot search trends displayed in both tool window and status bar. |
| **Click to Open** | Click any hot search title to open it in the default browser. |
| **Carousel** | Automatic rotation with configurable interval. |

| 功能 | 说明 |
|------|------|
| **多热搜源** | 百度、今日头条、知乎、抖音、快手、X趋势、Google趋势，可在设置或工具窗口中切换 |
| **实时数据** | 自动刷新热搜榜单，同时显示在工具窗口和状态栏 |
| **点击打开** | 点击热搜标题直接在浏览器中打开 |
| **轮播** | 自动轮播，间隔可自定义 |

### Idle Cultivation Mode / 放置修仙模式

| Feature | Description |
|---------|-------------|
| **Idle Growth** | Gains qi over real elapsed time, including offline gains capped at 8 hours. |
| **Tabbed Panel** | Training, bag, travel, and abode tabs keep actions readable inside the tool window. |
| **Techniques** | Equip one main technique to tune qi gain, spirit stone gain, or breakthrough chance. |
| **Pills** | Use travel-earned pills for qi, spirit stones, breakthrough chance, or safer failures. |
| **Travel** | Dispatch one timed travel task and claim safe random rewards when it finishes. |
| **Abode** | Spend spirit stones on facilities that improve qi, produce stones or pills, and raise breakthrough chance. |
| **Status Bar** | Displays one-line cultivation progress; click to meditate once. |
| **Breakthrough** | Attempt breakthroughs when qi is full; failure never drops the realm and improves the next chance. |

| 功能 | 说明 |
|------|------|
| **放置成长** | 按真实经过时间获得修为和灵石，离线收益封顶 8 小时 |
| **标签页面板** | 修炼、背包、游历三页承载玩法，工具窗口内更清晰 |
| **功法** | 每次装备一本主修功法，调整修为、灵石或突破收益 |
| **丹药** | 使用游历获得的丹药，获得修为、灵石、突破加成或护脉效果 |
| **游历** | 派遣一个倒计时游历任务，完成后领取无惩罚随机奖励 |
| **状态栏** | 单行显示修炼进度，点击可吐纳一次 |
| **突破** | 修为满后可尝试突破，失败不掉境界并提高下次成功率 |

### i18n / 国际化

The plugin UI follows IntelliJ IDEA's language by default, and can also be switched manually in settings or the tool window.

插件界面默认跟随 IDEA 语言，也可在配置页或工具窗口手动切换。

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
- Switch between Novel Reading, Hot Search Carousel, and Idle Cultivation modes
- 在小说阅读、热搜轮播和放置修仙模式之间切换

**Novel Settings / 小说设置**
- Stealth mode: chars per line, status bar toggle
- Normal mode: lines per page, chars per line
- Font family and size
- Keyboard shortcuts

**Hot Search Settings / 热搜设置**
- Source: Baidu / Toutiao / Zhihu / Douyin / Kuaishou / X Trends / Google Trends
- Carousel interval (3–120 sec)
- Data refresh interval (1–120 min)

**Idle Cultivation / 放置修仙**
- Tool window tabs for training, bag, travel, and abode
- Equip techniques, use pills, dispatch timed travel, and upgrade abode facilities
- Real offline qi gains capped at 8 hours; spirit stones now mainly feed abode upgrades
- 工具窗口支持修炼、背包、游历三页
- 可装备功法、使用丹药、派遣倒计时游历
- 真实离线收益，封顶 8 小时

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
│       ├── IdleCultivationManager.java     # Idle cultivation game state
│       ├── IdleCultivationPanel.java       # Idle cultivation tool window panel
│       ├── PluginModeSelector.java         # Shared mode selector
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
