![Upstream VerveDo UI](https://s2.loli.net/2026/02/09/zUrZdykbWN68lYM.png)

# VerveTwoDo

VerveTwoDo is a fork of [VerveDo](https://github.com/Super12138/VerveDo) by Super12138 and contributors, licensed under GPL-3.0-only. It installs alongside upstream VerveDo as a separate app.

A simple to-do app that follows Material 3 Expressive, using Jetpack Compose.

[简体中文](https://github.com/CamilleHbp/VerveTwoDo/blob/main/README.md) | **English**

[![Android CI](https://github.com/CamilleHbp/VerveTwoDo/actions/workflows/android_ci.yml/badge.svg)](https://github.com/CamilleHbp/VerveTwoDo/actions/workflows/android_ci.yml)
![GitHub latest release](https://img.shields.io/github/v/release/CamilleHbp/VerveTwoDo?style=flat-square)
![GitHub all releases download](https://img.shields.io/github/downloads/CamilleHbp/VerveTwoDo/total?style=flat-square)

# 📦 Supported Versions

From `Android 8.0 (Oreo)` to `Android 17.0 (Cinnamon Bun)`

# 📃 License

[GPL-3.0-only](https://github.com/CamilleHbp/VerveTwoDo/blob/main/LICENSE)

# ✨ Features

- [x] Jetpack Compose
- [x] Material 3 **Expressive** Design
- [x] Task Categorization
- [x] Task Priority
- [x] Data Backup
- [x] Time Segmentation
- [x] Upcoming Tasks Widget with Tag Sorting and Filtering
- ...

Add **Upcoming tasks** from the VerveTwoDo section of your home-screen widget picker. It shows tasks due today or later, with unfinished tasks from earlier days at the top in **À reprogrammer**. Missed due dates remain visible; completing or rescheduling a task removes it from that section. The selected tag filters and sort order also apply within this section. Use the separate Tags and Sort controls to select one or more categories and order by due date, creation date, priority, or title. The header chevron hides or shows the controls without clearing your selections. Tags correspond to task categories, and each widget saves its own settings. Tap **+** to create a task, with today and a single selected tag prefilled. Saving returns to the widget; a category is optional for quick creation. Tap a task's circle to complete it. Completed tasks due today or later stay in their group, shown in grey with a strikethrough below unfinished tasks and following the same sort order. Tap the checked circle to mark a task unfinished again.

# ⬇️ Download

Check [VerveTwoDo releases](https://github.com/CamilleHbp/VerveTwoDo/releases) for published builds, or build a debug APK with `./gradlew :app:assembleDebug`. APKs are written to `app/build/outputs/apk/debug/` with the `vervetwodo-` prefix.

The [F-Droid listing for VerveDo](https://f-droid.org/packages/cn.super12138.todo) distributes the upstream app.

# Import from VerveDo

1. In VerveDo, open **Settings → Data Management → Backup** and save a ZIP file to Downloads.
2. Install VerveTwoDo alongside VerveDo. There is no need to uninstall the upstream app.
3. In VerveTwoDo, open **Settings → Data Management → Restore**, select that ZIP, and restart when prompted.

ZIP backups include tasks and in-app preferences. CSV exports contain task data only. Restoring replaces VerveTwoDo's data; it does not merge tasks or change the upstream app. Home-screen widgets and Android permissions need to be configured separately.

# 📸 Upstream VerveDo screenshots

| ![Overview page (Light)](https://s2.loli.net/2026/02/09/p7L3vcZ4KnOHdol.png) | ![Overview page (Dark)](https://s2.loli.net/2026/02/09/gU9x3lpYvJXLfDs.png) |
| ---------------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| Overview page (Light)                                                        | Overview page (Dark)                                                        |
| ![Task list](https://s2.loli.net/2026/02/09/klB2e4XRYI98zdG.png)             | ![Add new task](https://s2.loli.net/2026/02/09/KXEGDWc1lfSRyCP.png)         |
| Task list                                                                    | Add new task                                                                |

## 🤝 Contributing
You can contribute to VerveTwoDo by submitting code or translations. To contribute code, simply submit a Pull Request. Shared translations are maintained in [the upstream VerveDo Crowdin project](https://crowdin.com/project/vervedo). If you want to propose a new language, please submit an Issue.

Upstream VerveDo translations:

<a href="https://crowdin.com/project/vervedo">
    <img style="width:140; height:40px" src="./art/localization-at-white-rounded-bordered@1x.png" srcset="./art/localization-at-white-rounded-bordered@1x.png 1x,./art/localization-at-white-rounded-bordered@2x.png 2x" alt="VerveDo Crowdin Project" />
</a>
