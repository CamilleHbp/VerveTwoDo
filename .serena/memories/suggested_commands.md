# Commands
Run Gradle commands from the repository root; use the wrapper, not a globally installed Gradle.
- Discover tasks: ./gradlew :app:tasks --all
- One-command local emulator/build/install/launch: ./script/android-dev.sh run. Separate build/test/start/stop/devices subcommands are available. Usage/removal: docs/android-development.md.
- Debug APK: ./gradlew :app:assembleDebug
- Install debug to connected emulator/device: ./gradlew :app:installDebug
- Run launcher: adb shell am start -n studio.camille.vervetwodo/cn.super12138.todo.ui.activities.MainActivity
- JVM tests: ./gradlew :app:testDebugUnitTest
- Android lint: ./gradlew :app:lintDebug
- Instrumented tests (device required): ./gradlew :app:connectedDebugAndroidTest
- Release APK / CI equivalent: ./gradlew :app:assembleRelease
- APK outputs: app/build/outputs/apk/debug/ and release/. Archive basename is vervetwodo-<versionName>. Release enables R8 shrinking/resource shrinking.
- Signing: releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword Gradle properties are optional; absent releaseStoreFile, the build uses debug signing, including release. Do not mistake this for a publishable official release or persist credentials in source/memories.
- CI installs NDK 28.2.13676358, but the app has no declared external native build. Do not install the NDK solely to match CI unless a build actually requires it.
- Script dependencies/build: pnpm --dir script install --frozen-lockfile; pnpm --dir script run build. Script version mutation and CWD rules: `mem:script/core`.
- Development environment paths and emulator launch details: `mem:local_environment`.
- Memory-reference validation: serena memories check from repository root. Serena symbolic tool line numbers are zero-based.
