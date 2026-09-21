# UI, navigation and widgets
Paths relative to app/src/main/kotlin/cn/super12138/todo/.
- Actual navigation wiring is MainActivity + activity-retained Koin TopLevelBackStack<NavKey> + TopNavigation's NavDisplay. VerveDoNavigator exists as a wrapper but is not the main activity's navigation entrypoint.
- Routes are kotlinx @Serializable VerveDoScreen subclasses implementing Navigation 3 NavKey. Top-level destinations are Overview, Tasks, Settings.Main; editors/settings detail routes are nested destinations.
- TopLevelBackStack tracks a SnapshotStateList per top-level route and exposes a flattened stack. Re-selecting a tab reorders its stack while retaining detail history. Preserve this behavior when changing navigation.
- TopNavigation adds saveable-state and ViewModel-store entry decorators; ViewModels are scoped per screen entry. Editor.Edit carries a TaskEntity; TaskEditorPage supplies it to EditorViewModel through Koin parametersOf.
- MainActivity hides adaptive navigation controls on non-top-level routes. Preserve normal and predictive back handling and shared editor transitions.
- VerveDoTheme wraps MaterialExpressiveTheme using Kyant m3color; it supports dynamic color, palette/contrast, pure black and dark-mode choices. Theme helpers live in ui/theme/ and layout/color constants in ui/VerveDoDefaults.kt.
- Secure mode sets WindowManager.FLAG_SECURE; haptics use VibrationUtils; confetti is a shared injected controller.
- Glance widgets live under ui/widget/, use TaskRepository, and render separate Glance components. Review widget queries and updateTaskWidgets/updateAll call sites when changing task writes or widget lifecycle. MainActivity.onStop also triggers refresh.
- New widget variants need a receiver registration in AndroidManifest.xml, provider metadata in res/xml/, localized labels, previews/layout resources as used by existing widgets, and inclusion in update paths. Consult the current tree for all variants rather than assuming a fixed list.