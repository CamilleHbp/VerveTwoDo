---
version: 1
slug: "2138-todo-ui-widget-upcoming-upcomingtaskwidget-kt"
primary_target: "app/src/main/kotlin/cn/super12138/todo/ui/widget/upcoming/UpcomingTaskWidget.kt"
related_targets: ["app/src/main/kotlin/cn/super12138/todo/ui/widget/upcoming/UpcomingTasks.kt"]
---

# Upcoming Android widget

Mode: Operate. Extend the existing compact Material 3 widget.

## Direction contract

THESIS: Keep missed tasks visible and actionable through a first section named “À reprogrammer”. The section contains unfinished tasks due before the current local day.

OWN-WORLD: Preserve the widget’s Material 3 palette, system typography, compact rows, 48 dp task targets, separate Tags/Sort controls, and floating 56 dp add button. Use errorContainer/onErrorContainer for a restrained section header and error for missed-date metadata.

STORY: Notice the missed date, then tap the task to reschedule or check it off. Tag selection applies to every section; the chosen sort applies within the overdue section. Completed past tasks leave this section, with Undo available.

FIRST VIEWPORT: Existing header and compact controls, then the tinted “À reprogrammer” strip and overdue tasks with dates visible, followed by normal upcoming groups. Keep the add action at the bottom right.

FORM: Local extension of the incumbent native widget; no form tournament or seed applies. Preserve individually scrolling rows, completed-task strikethrough, and today/future grouping.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

This extension preserves the incumbent design context; it does not establish a replacement visual identity or introduce shipping raster assets.
