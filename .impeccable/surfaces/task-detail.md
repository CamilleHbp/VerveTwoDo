---
version: 1
slug: task-detail
primary_target: app/src/main/kotlin/cn/super12138/todo/ui/pages/detail/TaskDetailPage.kt
related_targets: [app/src/main/kotlin/cn/super12138/todo/ui/pages/editor/TaskEditorPage.kt, app/src/main/kotlin/cn/super12138/todo/ui/pages/tasks/TasksPage.kt]
---

# Task detail and checklist

Mode: Operate. Extend the existing Android Material 3 Expressive world.

## Direction contract

THESIS: Read the whole task and act on its checklist without entering a form. The user chose simple checklist items and a dedicated View Task screen.

OWN-WORLD: Inherit Material theme roles, system typography, rounded surfaces, and existing back navigation. Restrained color supports state and actions. Respect the owner's light/dark preference in everyday indoor and outdoor use.

STORY: Tap a task to read it, tick steps directly, and explicitly complete the parent. Edit is visible in the app bar; long press edits from task lists. Select beside Search opens the existing bulk action toolbar.

FIRST VIEWPORT: Back and Edit frame a readable task title and completion control. Compact metadata follows, then full details and a checklist with a completed/total count. On wide screens the reading column stays bounded. Empty checklists offer Add subtasks.

FORM: A precisely requested detail screen inside the incumbent system; no concept tournament. Ordered checklist rows retain their position when checked. Editing supports adding, renaming, removing, and checking items. Widgets display parent tasks only.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
