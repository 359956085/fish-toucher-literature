# 📖 Fish Toucher Literature — IntelliJ IDEA Plugin

在 IntelliJ IDEA 中隐秘阅读本地小说 (TXT) 文件的插件，支持 IDEA **2025.1 (Build 253.x)** 版本。

## ✨ 功能特性

| 功能 | 说明 |
|------|------|
| **隐蔽阅读** | 底部 Tool Window 伪装成构建日志输出，不引人注目 |
| **状态栏阅读** | 小说内容显示在底部状态栏，极度隐蔽 |
| **一键隐藏** | `Alt+Shift+H` 瞬间隐藏内容，显示伪造的 "Build successful" 信息 |
| **快捷翻页** | `Alt+Shift+←/→` 上一页/下一页 |
| **阅读进度** | 自动保存每本书的阅读位置，下次打开自动恢复 |
| **进度条** | 拖动滑块快速跳转到任意位置 |
| **编码自适应** | 自动识别 UTF-8 / GBK 编码，完美支持中文小说 |
| **可配置** | 每页行数、字体、字号均可自定义 |

## ⌨️ 快捷键

| 快捷键 | 功能 |
|--------|------|
| `Alt + Shift + N` | 打开小说文件 |
| `Alt + Shift + →` | 下一页 |
| `Alt + Shift + ←` | 上一页 |
| `Alt + Shift + H` | 隐藏/显示内容 (老板键) |

## 🛠️ 构建方式

### 前置条件
- JDK 21+
- Gradle 8.13+ (项目包含 wrapper 配置)
- 使用 **IntelliJ Platform Gradle Plugin 2.11.0** (新版 2.x)
- 目标平台: **IntelliJ IDEA 2025.3** (Build 253.x)

> ⚠️ 从 2025.3 起，IntelliJ IDEA Community Edition (IC) 构建不再可用，
> 本项目使用 `intellijIdea("2025.3")` 作为统一平台依赖。

### 构建插件

**方式一：使用 Gradle Wrapper (推荐)**
```bash
cd fish-toucher-literature
# 如果没有 gradlew，先生成:
gradle wrapper --gradle-version 8.13
# 然后构建:
./gradlew buildPlugin
```

**方式二：直接使用本地 Gradle**
```bash
cd fish-toucher-literature
gradle buildPlugin
```

构建完成后，插件 zip 包位于:
```
build/distributions/fish-toucher-literature-1.0.0.zip
```

### 安装插件
1. 打开 IDEA → `Settings` → `Plugins`
2. 点击齿轮图标 ⚙ → `Install Plugin from Disk...`
3. 选择构建生成的 `.zip` 文件
4. 重启 IDEA

## 📖 使用方法

1. **打开小说**: `Alt+Shift+N` 或 菜单 `Tools → Novel Reader → Open Novel File`
2. **翻页**: 使用快捷键 `Alt+Shift+←/→`，或在底部面板点击 ◀ ▶ 按钮
3. **快速跳转**: 拖动进度条滑块
4. **老板来了**: 按 `Alt+Shift+H` 立即隐藏内容，面板会显示 "Build completed successfully"
5. **状态栏阅读**: 也可以在状态栏看到当前内容，点击状态栏文字可翻页

## ⚙️ 设置

`Settings → Tools → Novel Reader` 中可以调整:
- 每页显示行数 (1-50)
- 字体名称 (推荐: Microsoft YaHei, SimSun, Source Han Sans)
- 字号大小
- 是否在状态栏显示

## 📁 项目结构

```
fish-toucher-literature/
├── build.gradle.kts                 # Gradle 构建配置 (Platform Plugin 2.x)
├── settings.gradle.kts              # 含 pluginManagement
├── gradle.properties
├── gradle/wrapper/
│   └── gradle-wrapper.properties    # Gradle 8.13
└── src/main/
    ├── java/com/novelreader/
    │   ├── actions/
    │   │   ├── OpenNovelAction.java      # 打开文件
    │   │   ├── NextPageAction.java       # 下一页
    │   │   ├── PrevPageAction.java       # 上一页
    │   │   └── ToggleVisibilityAction.java # 隐藏/显示
    │   ├── settings/
    │   │   ├── NovelReaderSettings.java  # 持久化设置
    │   │   └── NovelReaderConfigurable.java # 设置界面
    │   └── ui/
    │       ├── NovelReaderManager.java   # 核心管理器
    │       ├── NovelReaderPanel.java     # 阅读面板 (伪装成日志)
    │       ├── NovelReaderToolWindowFactory.java
    │       ├── NovelReaderStatusBarWidget.java
    │       └── NovelReaderWidgetFactory.java
    └── resources/META-INF/
        ├── plugin.xml                    # 插件描述文件
        └── book_icon.svg                 # 图标
```

## 💡 隐蔽技巧

- Tool Window 标题为 "Novel Reader"，可以右键改名为 "Build Output" 等更隐蔽的名字
- 隐藏状态下显示虚假的构建成功信息
- 状态栏模式下内容自动截断为 80 字符，不会太显眼
- 可在设置中关闭状态栏显示，仅在底部面板中阅读
