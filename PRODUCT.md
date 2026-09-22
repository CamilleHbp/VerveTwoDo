# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

The primary user is the app's owner, managing their own personal and work tasks.

## Product Purpose

VerveTwoDo helps its owner capture, organize, review, and complete tasks. Success means being able to identify the relevant work, read it, and act on it with little navigation, while adapting the interface to the owner's workflow.

## Operating Context

- A native Android app built with Kotlin and Jetpack Compose, using Material 3 Expressive.
- The main destinations are Overview, Tasks, and Settings.
- Tasks are stored locally on the device. Android launcher widgets provide access to the same task data outside the app.
- Personal and work tasks belong to the same owner's workflow; categories, priorities, and due dates support their organization.

## Capabilities and Constraints

### Existing capabilities

- Create, edit, complete, and organize tasks with categories, priorities, and optional due dates.
- Review all, completed, and pending counts, today's progress, and upcoming tasks in Overview.
- Use All Incomplete, Today's Task, and Upcoming tasks launcher widgets. The Upcoming widget supports category filtering, sorting, and settings saved independently for each widget.
- Back up and restore local app data and export tasks to CSV.
- Adjust appearance and interaction preferences, including dynamic color, dark mode, contrast, and haptic feedback.
- Support localized content and Android right-to-left layout behavior.

### Required product direction

- Overview must support useful interaction with the tasks it displays, including reading task content and changing completion state.
- All, Pending, and Completed must provide meaningful access to the corresponding tasks.
- Overview cards must support personalization, including hiding, moving, and locking their arrangement.
- Task content must remain readable within its available space; task recognition and action take priority over decorative summaries.
- Treat in-app Overview cards and Android launcher widgets as distinct surfaces that operate on the same task data.

### Future synchronization

Future changes may add task synchronization through a private sync service already created by the owner. Synchronization is a potential integration, not an existing app capability. Local-only operation is not a permanent product constraint.

The service contract, authentication, synchronized fields, conflict handling, deletion behavior, and offline behavior during synchronization remain open decisions.

### Overview behavior

- The default order is Overdue, Today, Next 7 days, then All, Pending, and Completed summaries. Overdue appears when unfinished tasks have a due date before today; Next 7 days covers tomorrow through seven days ahead.
- Task rows open View Task, which displays the full title, metadata, details, and a checklist. Long press or the detail screen’s Edit action opens the editor. Overview rows provide a separate completion control with Undo. Status cards and section links open a named task collection that can return to Overview or show all tasks.
- Subtasks are ordered checklist items with text and completion state. They belong to their parent task, never appear as independent tasks or in launcher widgets, and do not automatically complete their parent. They can be checked in View Task, and added, renamed, removed, or checked in the editor.
- A visible Select action in the Tasks toolbar enters multi-selection, including while searching. Rows show selection checkboxes, completion controls are hidden, and the toolbar offers Select all, Delete, and Close. Selection remains active until explicitly closed, including when no rows are selected.
- Edit overview supports showing or hiding each card, dragging or using move buttons, Compact and Expanded sizes, and individual layout locks. Changes take effect when saved; unsaved changes can be discarded.
- Compact task sections preview three tasks; expanded sections preview six. Phone task sections retain full readable width. Compact sections can share a row on wider screens, while expanded cards use the available width.
- A lock protects a card's position, size, and visibility without disabling task actions. Other cards can move around its fixed position. Hidden cards remain available in the editor, and Reset layout restores the default arrangement and clears locks without changing tasks.

### Tag behavior

- Tags have a shared name and color across tasks. Manage tags is available from the Tasks toolbar and Settings → Data Management, and includes saved tags and tags already used by tasks, including imported categories. Each tag shows its task count, including completed tasks.
- Tap or long press a tag in Tasks, Overview, View Task, or the task editor to edit its shared name and color. The task editor's separate remove control only removes that tag from the current task.
- Create and rename accept nonblank names, trim surrounding whitespace, and prevent duplicate names without regard to letter case. Changing only a tag's color preserves its existing name.
- Choose a color with a hue-and-saturation wheel and brightness control, or enter a six-digit hexadecimal color. A swatch previews the selected color; invalid color values prevent saving.
- Renaming or deleting a tag updates its use across pending and completed tasks, saved tags, open task drafts, and launcher-widget tag filters. Color changes apply wherever the shared tag is displayed.
- Deleting a tag requires a separate confirmation that makes clear that tasks are kept and, for a tag in use, states the affected task count. Deletion removes the tag's assignments and saved identity.

## Identity and Attribution

The app is named VerveTwoDo, a fork of VerveDo by Super12138 and contributors. Its Android application ID is `studio.camille.vervetwodo`, allowing it to be installed alongside upstream VerveDo. Upstream ZIP backups can be restored through Settings → Data Management → Restore.

Preserve the existing upstream attribution and license information in `README.md`, `README_EN.md`, `LICENSE`, and `app/licences/`.

## Evidence on Hand

- Product description and existing feature documentation: `README_EN.md` and `README.md`.
- Android app and task workflows: `app/src/main/`.
- App icons and drawable assets: `app/src/main/res/`.
- Store artwork and screenshots: `fastlane/metadata/android/`.

## Product Principles

1. **Act where the task is visible.** Reading a task and acting on it should form one coherent workflow.
2. **Give the owner control.** The interface should adapt to personal priorities through meaningful, persistent customization.
3. **Make interactions trustworthy.** Controls must perform the actions they imply, and task state must remain consistent across app screens and launcher widgets.
4. **Keep daily work straightforward.** Personalization should support quick task capture, review, and completion without making those actions harder to find.
