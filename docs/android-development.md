# Android development on macOS

From the repository root:

```sh
./script/android-dev.sh run       # Start the emulator, build, install and open VerveTwoDo
./script/android-dev.sh build     # Build a debug APK
./script/android-dev.sh test      # Run JVM tests and Android lint
./script/android-dev.sh start     # Start the emulator without rebuilding
./script/android-dev.sh stop      # Stop this emulator
./script/android-dev.sh devices   # List connected devices
```

Debug APKs are written to `app/build/outputs/apk/debug/` with the `vervetwodo-` prefix. The app installs as `studio.camille.vervetwodo`, separately from upstream VerveDo.

The launcher uses `~/Library/Android/sdk` and the `VerveDo_API_37` virtual device. It selects an available JDK 21, including a Gradle-provisioned JDK, without changing shell startup files. Gradle pins its own daemon to Java 21. Override `ANDROID_HOME`, `JAVA_HOME`, `VERVEDO_AVD` or `VERVEDO_EMULATOR_PORT` for a different environment. The default emulator port is 5554.

The virtual device uses Android 17 (API 37.0), the ARM64 Google APIs image and 2 GB of memory. Emulator output is saved to `$TMPDIR/vervedo-emulator.log` (or `/tmp/vervedo-emulator.log`).

## Tools and locations

| Component | Manager / location |
| --- | --- |
| Android command-line tools | Homebrew cask `android-commandlinetools` |
| Kotlin language server | Homebrew formula `JetBrains/utils/kotlin-lsp` |
| Android SDK packages | `sdkmanager`; `~/Library/Android/sdk` |
| Android CLI cache | `~/.android/cli` |
| Command-line tools inside the SDK | Symlink to Homebrew's `share/android-commandlinetools/cmdline-tools/latest` |
| Virtual device and its app data | `~/.android/avd/VerveDo_API_37.avd` and matching `.ini` |
| Project SDK path | Ignored `local.properties` |
| Gradle / Kotlin compiler | Checked-in Gradle wrapper and project dependencies |
| Java | Existing JDK 21 in Gradle's managed cache; system Java is preserved |

The separately installed Homebrew `android-platform-tools` cask also provides `adb`; it predates this setup. The project launcher uses the SDK's copy for consistency with the emulator. The app compiles against API 37.0 using Build Tools 36.0.0, as selected by its Android Gradle plugin.

List or manage SDK packages using the same environment as the launcher:

```sh
./script/android-dev.sh sdk --list_installed
./script/android-dev.sh avd list avd
```

To recreate the virtual device after deleting it:

```sh
printf 'no\n' | ./script/android-dev.sh avd create avd \
  -n VerveDo_API_37 \
  -k 'system-images;android-37.0;google_apis;arm64-v8a' \
  -d pixel_9a
```

## Remove the setup

First stop and delete this virtual device. Deleting it removes its installed apps and emulator data:

```sh
./script/android-dev.sh stop
./script/android-dev.sh avd delete avd -n VerveDo_API_37
```

If no other project needs these SDK packages, remove them through the SDK manager:

```sh
./script/android-dev.sh sdk --uninstall \
  'system-images;android-37.0;google_apis;arm64-v8a' \
  'platforms;android-37.0' \
  'build-tools;36.0.0' \
  'emulator' 'platform-tools'
brew uninstall --cask android-commandlinetools
```

To remove Kotlin editor support as well:

```sh
brew uninstall JetBrains/utils/kotlin-lsp
```

If no other installed package uses the JetBrains tap, `brew untap JetBrains/utils` removes its package definitions too.

The remaining `~/Library/Android/sdk` directory contains local SDK metadata, licenses and the command-line tools symlink; remove it in Finder if the SDK is no longer used. The `~/.android/cli` download cache can also be removed if the Android CLI is no longer used. Delete this project's `local.properties` if its SDK path is no longer valid. No shell configuration needs undoing.

Keep the pre-existing Java installation, Homebrew `android-platform-tools`, shared Gradle caches and any other virtual devices. Avoid `brew uninstall --zap`, which can remove shared Android settings.

## Serena

Project context is stored in `.serena/memories/`, starting with `core.md`. Run `serena memories check` from the repository root to check memory references.

Machine-specific Kotlin language-server settings are in ignored `.serena/project.local.yml` and `.serena/kotlin-lsp.local.sh`; the launcher uses Homebrew's `kotlin-lsp`. Caches are in ignored `.serena/cache/`. Reconnect Serena sessions opened before these settings changed. Removing those local files removes the project-specific runtime override. Project memories and the portable `.serena/project.yml` can be retained independently of Android tooling.

References: [Android command-line tools](https://developer.android.com/tools), [SDK manager](https://developer.android.com/tools/sdkmanager), [Android Emulator commands](https://developer.android.com/studio/run/emulator-commandline), [JetBrains Kotlin language server](https://github.com/Kotlin/kotlin-lsp).
