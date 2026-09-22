# Task detail documentation audit

This development record covers the task-detail and simple-checklist extension. The direction contract is `.impeccable/surfaces/task-detail.md`; the product behavior is already recorded in `PRODUCT.md`.

## Disposition

No design-system deviation was found in the reviewed extension. The implementation extends the incumbent native Material 3 Expressive system. It does not establish a new identity or alter the shared theme, type ramp, or defaults.

`DESIGN.md` and `.impeccable/design.json` were absent before this pass and remain absent. The shipped Impeccable documenter protocol directs ordinary extensions to preserve the incumbent system and report evidence, reserving system-document creation for a new world or an approved system change. This pass therefore records the comparison here without inventing tokens, naming a new visual world, or promoting screen-specific dimensions into global rules.

Only this development audit was written. Source files, `PRODUCT.md`, existing critiques, design files, and unrelated Overview work were left untouched.

## Evidence checked

| Evidence | Observed implementation |
| --- | --- |
| `app/src/main/kotlin/cn/super12138/todo/ui/theme/Theme.kt` — `VerveDoTheme` | Existing dynamic color scheme supports light/dark mode, pure black, palette, and contrast preferences, then supplies `MaterialExpressiveTheme`. No theme diff was present. |
| `app/src/main/kotlin/cn/super12138/todo/ui/theme/Type.kt` — `Typography` | Existing Material `Typography()` remains the type source. No typography diff was present. |
| `app/src/main/kotlin/cn/super12138/todo/ui/VerveDoDefaults.kt` | Existing tonal containers, Material shapes, 8 dp content padding, 16 dp horizontal screen padding, and theme motion remain unchanged. |
| `app/src/main/kotlin/cn/super12138/todo/ui/pages/detail/TaskDetailPage.kt` — `TaskDetailScreen`, `TaskDetailContent`, `DetailHeading`, `DetailMetadata` | Reuses `TopAppBarScaffold`, a tonal filled Back button, text actions, Material text roles, secondary-container tags, checkboxes, progress indicator, and outline-variant dividers. The reading column is centered and capped at 720 dp. Title and details have no truncation limits. Metadata and tags wrap. |
| `app/src/main/kotlin/cn/super12138/todo/ui/pages/detail/TaskDetailViewModel.kt` — `setCompleted`, `setSubtaskCompleted` | Parent completion and checklist completion are separate operations. The detail screen exposes their controls separately. |
| `app/src/main/kotlin/cn/super12138/todo/ui/pages/editor/TaskEditorPage.kt` — checklist section | Uses the existing subtitle pattern, Material outlined fields, labeled checkbox and remove controls, and a text Add subtask action. Stable item keys preserve checklist identity. |
| `app/src/main/kotlin/cn/super12138/todo/ui/pages/tasks/components/TasksTopAppBar.kt` — `TasksTopAppBar` | Visible Select text action accompanies Search and remains available in search mode. Selection reuses the existing tonal toolbar and theme motion. |
| `app/src/main/kotlin/cn/super12138/todo/ui/pages/tasks/components/TaskCard.kt` — `TaskCard` | Keeps existing card colors, shape animation, typography, and tag presentation. Selection uses a row-level `toggleable` with `Role.Checkbox` and a passive checkbox; completion controls are hidden in selection mode. Normal rows retain separate click and labeled long-click actions. |

All seven supplied captures in `.impeccable/review/task-detail/detail-review/` were inspected:

- `phone-detail-light.png`: readable title/details, compact metadata, native actions, completed and incomplete checklist rows.
- `phone-detail-dark-large.png`: the same hierarchy in dark mode with larger text and wrapping checklist content.
- `tablet-detail-light.png`: centered bounded reading column rather than stretched body text.
- `phone-detail-empty.png`: restrained empty-checklist message and Add subtasks action.
- `phone-editor-checklist.png`: checklist fields inherit the editor's Material form controls.
- `phone-tasks.png`: visible Select beside Search and incumbent task-card styling.
- `phone-selection.png`: selected card treatment, checkbox affordances, and bulk toolbar.

The captures establish visual layout. The last selection accessibility correction was inspected in source; final build and runtime verification are handled by the implementation review. This documentation pass does not substitute screenshots for accessibility or persistence tests.

## Five-line system summary

1. Palette: existing dynamic Material color roles; primary actions, secondary-container tags/selection, and tonal surfaces adapt to the owner's theme.
2. Type ramp: incumbent Material typography; `headlineMedium` task title, `titleLarge` section headings, `bodyLarge` reading/checklist text, and smaller metadata/labels.
3. Theme inheritance: shared theme roles and native Material controls supply colors, shape, and interaction treatment without new global tokens.
4. Readable layout: full-width phone content and a centered 720 dp maximum detail column; spacing remains within the app's familiar 4/8 dp rhythm.
5. State clarity: checkbox state, text decoration, counts, and explicit actions communicate completion and selection without requiring color alone.

## Not canonized or repaired

No actual visual-system drift was identified in the reviewed extension. The prior selection accessibility defect is not a design rule; its source correction is recorded above while runtime verification remains with the implementation review. Native system typography is an incumbent platform decision. No replacement display face, fixed global palette, new token scale, or shipping raster asset was introduced by this pass. Review PNGs are development evidence, not product artwork.
