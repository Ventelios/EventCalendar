# 循记 PulseTempo 📅

一个简洁优雅的 Android 日程记录应用，帮助你轻松记录和管理每日事件。

![License](https://img.shields.io/github/license/Ventelios/EventCalendar)
![Android](https://img.shields.io/badge/Android-8.0%2B-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.x-purple)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-OK-brightgreen)

## ✨ 功能特性

- **日历视图** - 直观的月历显示，事件标记一目了然
- **事件记录** - 快速记录每日事件，支持添加时间、备注
- **历史记录** - 按时间线查看所有历史事件，支持筛选
- **数据统计** - 统计事件发生频次，支持今日/本周/本月视图
- **事件类型** - 自定义事件类型及颜色
- **暗色模式** - 支持跟随系统切换明暗主题

## 🛠 技术栈

| 技术 | 说明 |
|------|------|
| Kotlin | 开发语言 |
| Jetpack Compose | UI 框架 |
| Material Design 3 | 设计系统 |
| MVVM + Clean Architecture | 架构模式 |
| Room | 本地数据库 |
| Hilt | 依赖注入 |
| Coroutines + Flow | 异步编程 |
| Navigation Compose | 页面导航 |

## 📱 预览

| 日历 | 历史 | 统计 |
|------|------|------|
| 📅 | 📋 | 📊 |

## 🚀 快速开始

### 环境要求

- Android Studio Arctic Fox 或更高版本
- JDK 21
- Android SDK 34

### 克隆项目

```bash
git clone https://github.com/Ventelios/EventCalendar.git
cd EventCalendar
```

### 运行项目

1. 在 Android Studio 中打开项目
2. 等待 Gradle 同步完成
3. 点击 Run 按钮或按 `Shift + F10`

### 构建 APK

```bash
# 调试版
./gradlew assembleDebug

# 发布版（需签名）
./gradlew assembleRelease
```

APK 文件位置：`app/build/outputs/apk/debug/app-debug.apk`

## 📂 项目结构

```
app/src/main/java/com/eventcalendar/app/
├── data/                  # 数据层
│   ├── local/            # 本地数据库
│   └── repository/       # 数据仓库
├── di/                   # 依赖注入模块
├── ui/                   # 界面层
│   ├── navigation/       # 导航配置
│   ├── screens/         # 页面组件
│   │   ├── calendar/    # 日历页面
│   │   ├── eventtypes/  # 事件类型页面
│   │   ├── history/     # 历史记录页面
│   │   ├── menu/        # 菜单页面
│   │   └── statistics/  # 统计页面
│   └── theme/           # 主题配置
├── EventCalendarApp.kt  # 应用入口
└── MainActivity.kt      # 主 Activity
```

## 🙏 鸣谢

感谢以下大模型的支持：

- MiniMax-M2.5
- GLM-5
- Kimi-K2.5
- Qwen3.5-Plus

特别感谢 [字节跳动 Trae](https://www.trae.com) AI 编程助手

## 📄 许可证

本项目仅供学习交流使用。

---

Made with ❤️ by Ventelios
