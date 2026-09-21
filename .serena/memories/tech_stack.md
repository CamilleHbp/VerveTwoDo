# Build and dependencies
- Gradle Kotlin DSL with checked-in wrapper. settings.gradle.kts includes only :app; repository resolution is centralized and FAIL_ON_PROJECT_REPOS is enabled. Google, Maven Central and JitPack are intentional.
- Pin sources: gradle/libs.versions.toml for libraries/plugins, gradle/wrapper/gradle-wrapper.properties for Gradle and its SHA-256, app/build.gradle.kts for SDK/JVM/app versions, gradle/gradle-daemon-jvm.properties for daemon JDK.
- Current baseline: Gradle 9.7.1, Android Gradle Plugin 9.4.1, Kotlin Compose/serialization plugins 2.4.20, KSP 2.3.12. AGP supplies Kotlin support; no separate org.jetbrains.kotlin.android plugin is applied.
- App compiles/targets API 37, supports API 26+, Java/Kotlin bytecode target 21. The effective AGP-selected Build Tools version is 36.0.0, independent of compile SDK 37. Daemon toolchain is pinned to JDK 21 with Foojay resolution. CI's Java setup step label says 21 but its configured installation is Java 25; trust actual settings over labels.
- Compose BOM 2026.09.00; Material 3 has a separate explicit 1.5.0-alpha28 pin. Navigation 3, adaptive navigation suite, lifecycle ViewModels/Flow, Koin, Preferences DataStore and Glance.
- Database is Room 3 (androidx.room3 imports/artifacts/plugin), with KSP and checked-in schema export. Do not introduce older Room 2 migration APIs by assumption.
- Other libraries: kotlinx serialization/coroutines, Kyant m3color, Konfetti, AboutLibraries, kotlin-csv. Gradle AboutLibraries configuration uses app/licences/.
- Kotlin build tools are supplied through Gradle; no separate global Kotlin compiler is required.
- script/ uses Node/pnpm, TypeScript and @types/node; its pnpm-lock.yaml controls resolved versions. Script CI runs Node 24.