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

### Open product decisions

- Overview's default emphasis: today's and overdue work, the coming days, or a balance of tasks and summaries.
- Whether card personalization includes user-adjustable sizes in addition to visibility, ordering, and locking.
- The exact behavior of layout locks and the available ways to restore hidden cards or reset the arrangement.

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
