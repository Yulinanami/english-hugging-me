# English Hugging Me

**悬浮背词** — 一款跨平台桌面/移动端英语词汇悬浮窗应用，让你在使用电脑或手机的同时，随时随地沉浸式背单词。

## 功能特性

- **悬浮窗显示**：半透明圆角悬浮窗常驻屏幕最上层，不影响正常操作
- **定时轮播**：按照设定间隔自动切换显示下一个单词（默认 8 秒）
- **多种显示模式**：
  - 只显示单词
  - 单词 + 释义
  - 单词 + 释义 + 常用短语
- **三种悬浮行为**：
  - 可拖动 — 自由拖放悬浮窗位置
  - 锁定位置 — 固定不动
  - 点击穿透 — 鼠标/触摸完全穿透悬浮窗（Windows 通过 JNA 实现）
- **可定制**：透明度、颜色（单词 / 词性 / 释义 / 短语各自独立）、字号等可调节
- **跨平台**：同时支持 Windows 桌面端和 Android 移动端

## 项目架构

项目采用 Gradle 多模块结构，代码共享核心逻辑：

```
english-hugging-me/
├── core/                        # 核心模块（Java Library）
│   └── me.englishhugging.core
│       ├── WordEntry            # 单词数据模型（单词 + 释义 + 短语）
│       ├── Translation          # 释义模型（词性 + 中文释义）
│       ├── Phrase               # 短语模型（短语 + 中文释义）
│       ├── VocabularyJsonLoader # JSON 词库加载器（Gson）
│       ├── WordScheduler        # 定时轮播调度器
│       ├── AppSettings          # 应用设置（词库、显示模式、颜色等）
│       ├── DisplayMode          # 显示模式枚举
│       └── OverlayMode          # 悬浮行为枚举
│
├── desktop/                     # 桌面端模块（JavaFX）
│   └── me.englishhugging.desktop
│       ├── FloatingWordsDesktopApp  # 主应用（悬浮窗 + 设置面板 + 系统托盘）
│       ├── DesktopSettingsStore     # 桌面端设置持久化（Properties 文件）
│       └── WindowsClickThrough     # Windows 点击穿透（JNA + Win32 API）
│
├── android/                     # Android 端模块
│   └── me.englishhugging.android
│       ├── MainActivity             # 主界面（设置 + 启停控制）
│       ├── OverlayService           # 悬浮窗前台服务
│       └── AndroidSettingsStore     # Android 设置持久化（SharedPreferences）
│
└── vocabulary/                  # 应用使用的 JSON 词库
```

## 技术栈

| 层面         | 技术                                |
| ------------ | ----------------------------------- |
| 语言         | Java 17                             |
| 构建工具     | Gradle                              |
| 核心依赖     | Gson 2.11.0                         |
| 桌面端 UI    | JavaFX 21.0.5                       |
| Windows 集成 | JNA 5.15.0（点击穿透 / 隐藏任务栏） |
| Android      | compileSdk 36，minSdk 26            |
| 单元测试     | JUnit 5                             |

## 快速开始

### 环境要求

- **JDK 17** 或更高版本
- **Gradle**（项目自带 Gradle Wrapper）

### 运行桌面端

```bash
# Windows
gradlew.bat :desktop:run

# macOS / Linux
./gradlew :desktop:run
```

启动后，悬浮窗会显示在屏幕左上角，程序图标在系统托盘中。右键托盘图标可打开设置窗口。

### 构建 Android 端

```bash
# 生成 APK
gradlew.bat :android:assembleDebug
```

安装后打开应用，选择词库与设置，点击「启动悬浮背词」即可。需要授予**悬浮窗权限**。

### 运行单元测试

```bash
gradlew.bat :core:test
```

## 桌面端设置说明

设置保存在 `~/.english-hugging-me/desktop.properties`，可手动编辑或通过设置窗口调整：

| 设置项   | 说明                      | 默认值     |
| -------- | ------------------------- | ---------- |
| 词库路径 | JSON 词库文件路径         | 初中词库   |
| 显示内容 | 单词 / +释义 / +短语      | 单词+释义  |
| 悬浮行为 | 可拖动 / 锁定 / 点击穿透  | 可拖动     |
| 切换间隔 | 单词切换间隔（秒）        | 8 秒       |
| 透明度   | 悬浮窗透明度 (0.2 ~ 1.0)  | 0.85       |
| 颜色设置 | 单词 / 词性 / 释义 / 短语 | 各有默认色 |
| 字号设置 | 单词字号 / 释义字号       | 30 / 24    |

## License

本项目为个人学习项目，词库数据来源于开源社区。
