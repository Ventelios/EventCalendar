# 循记 PulseTempo 📅

一个简洁优雅的 Android 日程记录应用，帮助你轻松记录和管理每日事件。

![License](https://img.shields.io/github/license/Ventelios/EventCalendar)
![Android](https://img.shields.io/badge/Android-8.0%2B-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-brightgreen)

## ✨ 功能特性

- **日历视图** — 直观的月历显示，滑动翻页，事件点标记一目了然
- **快速记录** — 一键添加事件，支持滚轮式时间选择器、事件类型、备注
- **事件类型** — 自定义类型名称与颜色，灵活管理
- **历史时间线** — 按时间倒序查看所有事件，支持按类型筛选与内联编辑
- **数据统计** — 双模式统计看板：累计模式（今日/本周/本月）与近期模式（过去7天/30天）
- **趋势图** — 多粒度折线图（日/周/月），Y轴刻度标签，事件类型分色对比
- **数据管理** — JSON 格式导出/导入，数据备份与迁移
- **更新检查** — 自动检测 GitHub / Gitee 最新版本，网络延迟测速
- **暗色模式** — 跟随系统自动切换暖色调主题

## 📊 统计功能

| 模式 | 概览指标 | 趋势图粒度 |
|------|---------|-----------|
| 近期模式（默认） | 过去 N 天总计 + 日均 + 环比变化 | 每日 |
| 累计模式 · 7天 | 今日 / 本周 / 本月 | 每日（本周） |
| 累计模式 · 30天 | 今日 / 本周 / 本月 | 每周 |
| 累计模式 · 365天 | 今日 / 本周 / 本月 | 每月 |

## 🛠 技术栈

| 技术 | 说明 |
|------|------|
| Kotlin 2.0 | 开发语言 |
| Jetpack Compose | UI 框架 |
| Material Design 3 | 设计系统 |
| MVVM + Clean Architecture | 架构模式 |
| Room | 本地数据库 |
| Hilt | 依赖注入 |
| Coroutines + Flow | 异步编程 |
| Navigation Compose | 页面导航 |
| OkHttp + Gson | 网络请求与序列化 |

## 🚀 快速开始

### 环境要求

- Android Studio Ladybug 或更高版本
- JDK 21
- Android SDK 36

### 运行项目

```bash
git clone https://github.com/Ventelios/EventCalendar.git
cd EventCalendar
```

在 Android Studio 中打开项目，等待 Gradle 同步完成后点击 Run。

### 构建 APK

```bash
# 调试版
./gradlew assembleDebug

# 发布版（需配置签名）
./gradlew assembleRelease
```

## 📂 项目结构

```
app/src/main/java/com/eventcalendar/app/
├── data/
│   ├── local/
│   │   ├── dao/              # Room DAO
│   │   ├── entity/           # 数据实体
│   │   └── PreferencesManager.kt  # 偏好管理
│   ├── model/                # 数据传输模型
│   ├── repository/           # 数据仓库
│   └── service/              # 网络服务（GitHub/Gitee）
├── di/                       # Hilt 依赖注入模块
├── ui/
│   ├── navigation/           # 底部导航 + 路由
│   ├── screens/
│   │   ├── calendar/         # 日历页
│   │   ├── eventtypes/       # 事件类型管理
│   │   ├── history/          # 历史时间线
│   │   ├── menu/             # 设置 / 关于 / 更新检查
│   │   ├── statistics/       # 统计看板
│   │   └── datamanagement/   # 数据导入导出
│   └── theme/                # Material3 暖色调主题
├── EventCalendarApp.kt       # Application
└── MainActivity.kt           # 单 Activity 入口
```

## 🙏 鸣谢

感谢以下 AI 模型与工具提供的技术支持：

- **DeepSeek** · **GLM** · **Kimi** · **Qwen** · **MiniMax**
- **Trae** · **Codex** · **Claude Code**

## 📄 许可证

本项目仅供学习交流使用。

---

Made with ❤️ by Ventelios
