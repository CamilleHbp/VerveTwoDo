## verdict

- **Resolved — selection accessibility.** `TaskCard.kt:148` now makes each selection row a `toggleable` with `Role.Checkbox` and its selected value; the checkbox at line 169 is passive. The task title and checked state therefore belong to the row's single action. `TaskDetailUiTest.kt:150` asserts two toggleable nodes for two tasks, identifies a node by task title, and verifies its unchecked-to-checked transition; line 155 verifies selection does not complete the task. The parent reports these emulator tests passed. The recaptured `phone-selection.png` retains legible task labels, visible checked states, the selection toolbar, and hidden completion controls despite tighter checkbox spacing.

## remaining

Clear. All seven required captures were reopened at their original paths under `.impeccable/review/task-detail/detail-review/` and remain valid. No regression from the fix batch was observed. TalkBack speech and hardware behavior were not exercised in this review. This ship verdict covers the scored selection accessibility fix.

disposition: ship
