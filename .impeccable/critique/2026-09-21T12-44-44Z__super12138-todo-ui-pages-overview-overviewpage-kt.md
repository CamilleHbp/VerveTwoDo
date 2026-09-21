---
target: home Overview
total_score: 16
max_score: 32
na_heuristics: 5,9
p0_count: 0
p1_count: 3
target_identity: "file:/Users/camille/Dev/Personal/VerveTwoDo/app/src/main/kotlin/cn/super12138/todo/ui/pages/overview/OverviewPage.kt"
target_fingerprint: "sha256:884e64ca2c8493b5f4529f0b6a640191f71ac9a9cb5ae454343418a257bd2d87"
target_path: /Users/camille/Dev/Personal/VerveTwoDo/app/src/main/kotlin/cn/super12138/todo/ui/pages/overview/OverviewPage.kt
timestamp: 2026-09-21T12-44-44Z
slug: super12138-todo-ui-pages-overview-overviewpage-kt
---
**Overview needs to become a place where users can act on their tasks.** Its Material styling is coherent, but the hierarchy gives large summaries priority over the actual work.

The live screen makes the problem concrete: the only upcoming task reads “Review the pr…”, while its card contains substantial unused space and a neighboring progress card displays just “0 / 1”.

**Four priority issues**

1. **[P1] Upcoming cannot support reading or completing tasks.**  
   The grid allows columns as narrow as 160dp. Upcoming is fixed at 240dp high, while each title is restricted to one line beside its date. Rows have no open, expand, or completion action.  
   **Fix:** Make the task section full-width on phones, allow two-line titles, move secondary metadata below, and provide separate controls for opening and completing tasks. Completion should update counts and offer Undo. Use content-responsive height or a short preview with “View all.” Compress the large progress display.  
   Evidence: [ListCard.kt:60](/Users/camille/Dev/Personal/VerveTwoDo/app/src/main/kotlin/cn/super12138/todo/ui/pages/overview/components/ListCard.kt:60), [ListCard.kt:122](/Users/camille/Dev/Personal/VerveTwoDo/app/src/main/kotlin/cn/super12138/todo/ui/pages/overview/components/ListCard.kt:122). Suggested workflow: `$impeccable shape`, then `$impeccable layout`.

2. **[P1] All / Pending / Completed actively promise an interaction that does nothing.**  
   These cards have pressed-state animation, haptics, and accessibility Button semantics. Their click callback defaults to an empty function, and Overview never supplies an action. This makes users question whether their tap registered.  
   **Fix:** Open the corresponding filtered task list, with its scope clearly named and easy to clear. Make the callback mandatory. Present these summaries compactly by default.  
   Evidence: [StatusCard.kt:51](/Users/camille/Dev/Personal/VerveTwoDo/app/src/main/kotlin/cn/super12138/todo/ui/pages/overview/components/StatusCard.kt:51). Suggested workflow: `$impeccable shape`.

3. **[P2] Customization has no interface or persistent layout model.**  
   All five sections have fixed placement and sizing. Users cannot prioritize Upcoming, hide redundant statistics, or protect an arrangement.  
   **Fix:** Add an explicit “Edit overview” mode with show/hide, reorder, supported sizes, and layout locks. Provide drag handles plus accessible Move up/down commands, a way to restore hidden cards, and Reset layout. Save preferences across launches. A lock should protect placement while leaving task actions usable. Keep these controls out of normal task use.  
   Evidence: [OverviewPage.kt:33](/Users/camille/Dev/Personal/VerveTwoDo/app/src/main/kotlin/cn/super12138/todo/ui/pages/overview/OverviewPage.kt:33). Suggested workflow: `$impeccable shape`.

4. **[P1] The home screen hides overdue work from its task preview.**  
   Upcoming includes incomplete tasks from today through seven days ahead. Yesterday’s unfinished task disappears from both that list and Today’s progress, surviving only in the broad Pending count. Meanwhile, every pending task gets error coloring, even when nothing is overdue.  
   **Fix:** Surface Overdue before Today, followed by a clearly labeled future period. Reserve warning color for actual urgency. Distinguish “nothing due in this period” from “no tasks,” and give empty states a useful action.  
   Evidence: [OverviewViewModel.kt:23](/Users/camille/Dev/Personal/VerveTwoDo/app/src/main/kotlin/cn/super12138/todo/ui/pages/overview/OverviewViewModel.kt:23). Suggested workflow: `$impeccable shape`, then `$impeccable harden`.

**Design health: 16/32 — lower boundary of “Acceptable”; significant improvements needed.** Scores are 0–4 and apply to Overview.

| Heuristic | Score | Main finding |
|---|---:|---|
| System status | 2 | Useful counts; misleading button feedback |
| Real-world language | 3 | Familiar labels; unclear time scope |
| Control and freedom | 3 | Clear navigation out of Overview |
| Consistency | 2 | Buttons do nothing; task behavior differs across tabs |
| Error prevention | n/a | No functioning mutation or input flow to assess |
| Recognition | 2 | Truncated tasks must be found again elsewhere |
| Flexibility and efficiency | 1 | No direct task actions or personalization |
| Minimalist design | 2 | Summaries consume disproportionate space |
| Error recovery | n/a | No exposed recovery flow to assess |
| Contextual help | 1 | Card behavior and empty states lack guidance |

**Keep the visual foundation.** The rounded Material components are cohesive, the three labeled navigation destinations are clear, and due dates/categories provide useful context. The composition feels generic because task counts dominate; the improvement should come from task-specific behavior and hierarchy.

**Cognitive load is moderate:** focus, hierarchy, and working memory fail. Users identify an abbreviated task, switch tabs, and locate it again. Grouping and the small number of navigation choices work. This is a shortage of useful actions, rather than an excess of choices.

**The emotional journey starts calmly and stalls at the first tap.** Completing a task directly, seeing progress update, and having Undo would give the screen a satisfying outcome.

**Persona risks:** A power user repeatedly leaves Overview to act; a screen-reader user encounters buttons without results; a distracted mobile user cannot identify or complete the truncated task in place.

Smaller refinements: shorten “Today’s Task Progress” to “Today,” remove uninformative “Default” priority text, and check fixed heights with large text and long lists.

**Recommended default:** readable Overdue and Today tasks first, a compact progress summary, then upcoming work. Statistics remain optional, actionable cards. Normal mode supports daily work; edit mode supports personalization.

Which default would you prefer: **Today + overdue**, **next seven days**, or **balanced tasks and summaries**? For customization, should the scope be **hide/reorder/lock**, or **those controls plus card sizing**?
