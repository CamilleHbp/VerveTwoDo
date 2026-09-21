![Upstream VerveDo UI](https://s2.loli.net/2026/02/09/EJS1HLOAvKyaRsl.png)

# VerveTwoDo

VerveTwoDo is a fork of [VerveDo](https://github.com/Super12138/VerveDo) by Super12138 and contributors, licensed under GPL-3.0-only. It installs alongside upstream VerveDo as a separate app.

一个简单的、遵循 Material 3 Expressive 的待办应用，使用 Jetpack Compose 编写

**简体中文** | [English](https://github.com/CamilleHbp/VerveTwoDo/blob/main/README_EN.md)

[![Android CI](https://github.com/CamilleHbp/VerveTwoDo/actions/workflows/android_ci.yml/badge.svg)](https://github.com/CamilleHbp/VerveTwoDo/actions/workflows/android_ci.yml)
![GitHub Release 最新版本](https://img.shields.io/github/v/release/CamilleHbp/VerveTwoDo?style=flat-square)
![GitHub Release 总下载数](https://img.shields.io/github/downloads/CamilleHbp/VerveTwoDo/total?style=flat-square)

## 📦 版本支持

支持 `Android 8.0 (Oreo)` 至 `Android 17.0 (Cinnamon Bun)`

## 📃 许可证

[GPL-3.0-only](https://github.com/CamilleHbp/VerveTwoDo/blob/main/LICENSE)

## ✨ 功能

- [x] Jetpack Compose
- [x] Material 3 **Expressive** 设计
- [x] 任务分类
- [x] 任务优先级
- [x] 数据备份
- [x] 时间划分功能
- ...

## ⬇️ Download

Check [VerveTwoDo releases](https://github.com/CamilleHbp/VerveTwoDo/releases) for published builds, or build a debug APK with `./gradlew :app:assembleDebug`. APKs are written to `app/build/outputs/apk/debug/` with the `vervetwodo-` prefix.

The [F-Droid listing for VerveDo](https://f-droid.org/packages/cn.super12138.todo) distributes the upstream app.

## Import from VerveDo

1. In VerveDo, open **Settings → Data Management → Backup** and save a ZIP file to Downloads.
2. Install VerveTwoDo alongside VerveDo. There is no need to uninstall the upstream app.
3. In VerveTwoDo, open **Settings → Data Management → Restore**, select that ZIP, and restart when prompted.

ZIP backups include tasks and in-app preferences. CSV exports contain task data only. Restoring replaces VerveTwoDo's data; it does not merge tasks or change the upstream app. Home-screen widgets and Android permissions need to be configured separately.

## 📸 Upstream VerveDo screenshots

| ![概览界面（浅色）](https://s2.loli.net/2026/02/09/nhuMmF8L7Oqk4dp.png) | ![概览界面（深色）](https://s2.loli.net/2026/02/09/Oari6zwC14gLPNl.png) |
| ----------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| 概览界面（浅色）                                                        | 概览界面（深色）                                                        |
| ![待办列表](https://s2.loli.net/2026/02/09/LbqGhyJXjke2gZ3.png)         | ![添加待办](https://s2.loli.net/2026/02/09/Da3h29rxFMmJQiV.png)         |
| 待办列表                                                                | 添加待办                                                                |

## 🤝 贡献
你可以为 VerveTwoDo 贡献代码和翻译。
贡献代码只需提交 Pull Request 即可，贡献翻译请[加入 Crowdin 项目](https://crowdin.com/project/vervedo)。若需提出新语言，请提交 Issue。

Upstream VerveDo translations:

<a href="https://crowdin.com/project/vervedo">
    <img style="width:140; height:40px" src="./art/localization-at-white-rounded-bordered@1x.png" srcset="./art/localization-at-white-rounded-bordered@1x.png 1x,./art/localization-at-white-rounded-bordered@2x.png 2x" alt="VerveDo Crowdin 项目" />
</a>
