---
target: Upcoming Android widget
total_score: 23
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 1
target_identity: "file:/Users/camille/Dev/Personal/VerveTwoDo/app/src/main/kotlin/cn/super12138/todo/ui/widget/upcoming/UpcomingTaskWidget.kt"
target_fingerprint: "sha256:cc4218183c01721b13baae12a7fba7a512d9120a6b4d15a4c3e552dece474389"
target_path: /Users/camille/Dev/Personal/VerveTwoDo/app/src/main/kotlin/cn/super12138/todo/ui/widget/upcoming/UpcomingTaskWidget.kt
timestamp: 2026-09-21T08-47-12Z
slug: 2138-todo-ui-widget-upcoming-upcomingtaskwidget-kt
---
# Upcoming widget design critique

The main problem is hierarchy: configuration takes visual priority over the tasks. The widget has useful behavior, but its composition feels like a settings panel with a task list attached. The strongest direction is a compact header, two distinct optional selectors, and a list whose task titles lead.

The visual language is broadly Material and appropriate for an Android utility. It needs more deliberate composition and clearer task states, rather than additional decoration.

## Priority issues

1. **[P1] The controls consume too much of the widget.** The two full-width rows use 96dp before the task list begins. In the expanded view, the first task starts around halfway down the widget. The sort picker initially exposes four date variants while Priority and Alphabetical remain below the viewport. **Fix:** put Tags and Sort side by side in one optional row. Present four sort methods, with direction as a separate control. Adapt the header to compact widget sizes. Suggested command: `$impeccable adapt`.

2. **[P2] The completion button dominates and suggests an already-completed state.** A saturated green circle with a white check is the strongest visual object beside an unfinished task. Its actual target is only 40dp, smaller than the secondary header controls. **Fix:** use a restrained outlined completion circle beside the task title, inside a 48dp touch area. Reserve the filled check for completion feedback and provide Undo. Suggested command: `$impeccable harden`.

3. **[P2] Task metadata adds noise without enough hierarchy.** “Working”, “Default”, and “Sep 21, 2026” compete beneath a title limited to one line. Routine priority information takes space that could help identify the task. **Fix:** hide default priority, use relative dates such as Today, and combine essential metadata into a quiet line. Allow a second title line when space permits, and make the text open the task. Suggested command: `$impeccable distill`.

4. **[P2] The controls communicate their meaning poorly.** Sliders and a gear look like two settings entrances. “1 task · 2 selected” never identifies the selected objects, and hiding controls conceals both tag identities and sorting. “Tags · Done” merges a panel title with its exit action. **Fix:** show a concise tag summary, keep the task count separate, use an explicit expanded/collapsed cue, and separate picker headings from Done. Suggested command: `$impeccable clarify`.

## Design health

Scores use a 0–4 heuristic scale. Functional usability is acceptable; visual hierarchy needs substantial work.

| Heuristic | Score | Main issue |
|---|---:|---|
| System status | 2 | Collapsed view hides sort and tag identities |
| Real-world language | 3 | “Selected” and “Default” need interpretation |
| User control | 2 | No immediate widget undo |
| Consistency | 3 | Completion target is smaller than secondary controls |
| Error prevention | 2 | Completion applies immediately |
| Recognition | 2 | Filter scope and additional sort methods need discovery |
| Efficiency | 3 | Useful independent, persistent controls |
| Minimalist design | 2 | Configuration and routine metadata dominate |
| Error recovery | 2 | Empty results offer no direct recovery action |
| Help | 2 | Inline control behavior is under-explained |
| **Total** | **23/40** | **Acceptable** |

## What works

- Independent filtering and sorting, saved per widget, support different task views.
- The collapsed layout gives the task a much clearer position and is a useful foundation.
- Theme-derived colors, readable text, and generous main control rows provide a workable native foundation.

## Cognitive load and emotional journey

Cognitive load is moderate: the hierarchy competes with task reading, and users must remember the hidden filter and sort state. The photographed picker shows four choices at once; its problem is discovering the remaining choices. The collapsed view feels calm, opening a picker replaces all task content, and accidental completion lacks immediate recovery.

## Persona red flags

- **First-time user:** the sliders/gear distinction and “2 selected” require experimentation.
- **Power user:** Priority and Alphabetical are absent from the first picker viewport; collapsed sorting cannot be verified at a glance.
- **User with accessibility needs:** completion has a smaller target than settings; repeated completion descriptions omit the task title. Large-text and TalkBack behavior need native verification.

## Smaller issues

Long task titles cannot be opened from their rows. Empty results need an appropriate Show all tags or Open tasks action. Multi-select tags and single-select sorting use the same check treatment. Small-size adaptation is a source-based concern that still needs visual verification; sparse sample data explains some of the unused space in the current screenshots.

## Design choices

1. Density: compact rows that show more tasks, or roomier rows with more title space?
2. Controls at rest: a visible compact Tags/Sort row, or controls hidden behind an explicit expand button?
