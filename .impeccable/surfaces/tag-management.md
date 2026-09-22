---
version: 1
slug: tag-management
primary_target: app/src/main/kotlin/cn/super12138/todo/ui/pages/settings/SettingsDataCategory.kt
related_targets: [app/src/main/kotlin/cn/super12138/todo/ui/components/TagManagementDialog.kt, app/src/main/kotlin/cn/super12138/todo/ui/components/TagColorPicker.kt, app/src/main/kotlin/cn/super12138/todo/ui/components/EditableTag.kt, app/src/main/kotlin/cn/super12138/todo/ui/components/TagChip.kt]
---

# Tag management

## Direction contract

THESIS: Maintain a shared tag wherever it appears, with its effect on all tasks made clear before saving or deleting.

OWN-WORLD: Use the app's native Material 3 Expressive theme, typography, rounded surfaces, and dialog controls. Tag colors identify tags; theme roles keep names, counts, actions, and errors legible in light and dark mode.

STORY: Open Manage tags from Tasks or Settings → Data Management, or tap or long press a tag on a task surface. Edit its name and color in the shared dialog. Keep the task editor's assignment-removal control separate from global deletion. Global deletion opens a confirmation that states that tasks are retained and, for a tag in use, gives the affected task count.

FIRST VIEWPORT: Back navigation and the Manage tags title lead into a short scope description and a compact grouped list. Each row contains a color dot, name, task count, and edit icon. New tag remains available as a floating action. The empty state explains what tags are for and retains the creation action.

FORM: Bound the list to 720 dp on wide screens. Rows have a minimum height of 56 dp, 16 dp horizontal padding, and a 14 dp color dot separated from the name by 16 dp. Names can occupy two lines. Use the low surface-container role, 16 dp outer group corners, and inset dividers. Task counts and edit icons share the trailing row area.

## Editing and interaction

- Use the same scrollable Material dialog for creation and editing. Editing identifies the global scope before the name field. Save and Cancel stay in the dialog's action area; Delete tag is a separate error-colored action within the edit content.
- Offer a 180 dp hue-and-saturation wheel, a brightness slider with an accessible progress action, and a hexadecimal field with a live swatch. Keep precise text entry available alongside touch controls.
- Preserve the Material type hierarchy: body-large names and label-medium counts in the manager, body-medium supporting text, and label-large tag names on task surfaces.
- Keep inline editable tags at least 48 dp in each touch-target dimension. Both tap and long press open editing, and the accessibility action names the tag being edited.
- Keep task-editor tag chips in the secondary-container role. Their separate close icon removes the assignment from the current task; it does not delete the shared tag.
- Show duplicate-name and invalid-color feedback beside the relevant field, and disable Save until inputs are valid. Keep dialog controls disabled while saving to prevent repeated changes.
