# Android application
All paths below are relative to app/src/main/kotlin/cn/super12138/todo/.
- VerveDoApp is the manifest Application. It starts Koin with VerveDoDI.allModules and installs the custom uncaught-exception CrashHandler. CrashActivity is the non-exported error UI.
- di/VerveDoDI.kt owns database, DataStore, repository, singleton, ViewModel and activity-retained navigation bindings. New constructor dependencies must be wired there.
- Data flows: Room TaskDao -> TaskRepository -> feature ViewModel -> Compose page; preference DataStore -> DataStoreManager -> SettingsRepository -> ViewModels/theme.
- ui/activities/MainActivity hosts adaptive navigation, app theme, secure-screen flag, haptic setting and shared confetti. MainViewModel combines appearance settings; it is not the task business-logic hub.
- ui/pages/overview computes dashboard counts and date groups; tasks owns sorting/search/selection; editor handles both add and edit; settings contains appearance, interaction, category presets, backup/restore, about/licences and developer options.
- ui/widget/ implements launcher widgets using Glance. Widgets share the repository and database but have their own composition/components, registration and update lifecycle.
- For persisted formats, migrations, category semantics, backup compatibility and date traps read `mem:app/persistence`.
- For Navigation 3, per-entry ViewModels, theming and widget integration read `mem:app/ui`.