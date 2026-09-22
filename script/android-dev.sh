#!/usr/bin/env bash
set -euo pipefail

project_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
avd_name="${VERVEDO_AVD:-VerveDo_API_37}"
emulator_port="${VERVEDO_EMULATOR_PORT:-5554}"
serial="emulator-$emulator_port"
log_file="${TMPDIR:-/tmp}/vervedo-emulator.log"

if [[ -z "${JAVA_HOME:-}" ]]; then
    java21=$(/usr/libexec/java_home -v 21 2>/dev/null || true)
    if [[ -z "$java21" ]]; then
        for candidate in "$HOME"/.gradle/jdks/*/jdk-21*/Contents/Home; do
            if [[ -x "$candidate/bin/java" ]]; then
                java21="$candidate"
                break
            fi
        done
    fi
    if [[ -n "$java21" ]]; then export JAVA_HOME="$java21"; fi
fi

start_emulator() {
    local active_name state
    active_name=$(adb -s "$serial" emu avd name 2>/dev/null | tr -d '\r' | head -n 1 || true)
    if [[ -n "$active_name" && "$active_name" != "$avd_name" ]]; then
        printf 'Port %s belongs to %s. Set VERVEDO_EMULATOR_PORT to another even port.\n' "$emulator_port" "$active_name" >&2
        return 1
    fi
    state=$(adb -s "$serial" get-state 2>/dev/null || true)
    if [[ -z "$state" ]]; then
        if ! emulator -list-avds | grep -Fxq "$avd_name"; then
            printf 'Missing emulator %s. See docs/android-development.md.\n' "$avd_name" >&2
            return 1
        fi
        # Detach the emulator so it survives a terminal or agent command ending.
        python3 - "$log_file" "$avd_name" "$emulator_port" <<'PY'
import subprocess
import sys

with open(sys.argv[1], "w") as log:
    subprocess.Popen(
        ["emulator", "-avd", sys.argv[2], "-port", sys.argv[3],
         "-memory", "2048", "-no-boot-anim"],
        stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT,
        start_new_session=True,
    )
PY
        printf 'Starting %s; log: %s\n' "$avd_name" "$log_file"
    fi
    for ((attempt = 0; attempt < 180; attempt++)); do
        if [[ "$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; then
            adb -s "$serial" shell input keyevent 82
            return 0
        fi
        sleep 2
    done
    printf 'Emulator did not finish booting; inspect %s\n' "$log_file" >&2
    return 1
}

cd "$project_root"
case "${1:-help}" in
    build)
        bash ./gradlew :app:assembleDebug
        python3 - "$project_root/app/build/outputs/apk/debug" <<'PY'
import json
import sys
from pathlib import Path

output_dir = Path(sys.argv[1])
metadata = json.loads((output_dir / "output-metadata.json").read_text())
apks = [output_dir / element["outputFile"] for element in metadata["elements"]]
if not apks or any(not apk.is_file() for apk in apks):
    sys.exit("Build finished, but the debug APK output could not be found.")
for apk in apks:
    print(f"\nBuilt APK: {apk}")
PY
        ;;
    test)
        exec bash ./gradlew :app:testDebugUnitTest :app:lintDebug
        ;;
    start)
        start_emulator
        ;;
    run)
        start_emulator
        bash ./gradlew :app:assembleDebug
        apk=$(find app/build/outputs/apk/debug -maxdepth 1 -name 'vervetwodo-*.apk' -print -quit)
        [[ -n "$apk" ]] || { printf 'Debug APK was not found.\n' >&2; exit 1; }
        adb -s "$serial" install -r "$apk"
        seed_result=$(adb -s "$serial" shell am broadcast --include-stopped-packages \
            -n studio.camille.vervetwodo/cn.super12138.todo.debug.SampleTasksReceiver)
        printf '%s\n' "$seed_result"
        if [[ "$seed_result" != *'Broadcast completed: result=0'* ]] ||
            [[ "$seed_result" != *'Sample tasks added'* && "$seed_result" != *'Existing tasks preserved'* ]]; then
            printf 'Sample task initialization failed.\n' >&2
            exit 1
        fi
        exec adb -s "$serial" shell am start -n studio.camille.vervetwodo/cn.super12138.todo.ui.activities.MainActivity
        ;;
    stop)
        active_name=$(adb -s "$serial" emu avd name 2>/dev/null | tr -d '\r' | head -n 1 || true)
        [[ "$active_name" == "$avd_name" ]] || { printf '%s is not running on %s.\n' "$avd_name" "$serial"; exit 0; }
        exec adb -s "$serial" emu kill
        ;;
    devices)
        exec adb devices -l
        ;;
    sdk)
        shift
        exec "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_HOME" "$@"
        ;;
    avd)
        shift
        # avdmanager canonicalizes the Homebrew symlink and otherwise selects
        # Homebrew's SDK root. An SDK-local anchor keeps package discovery here.
        export AVDMANAGER_OPTS="${AVDMANAGER_OPTS:-} -Dcom.android.sdkmanager.toolsdir=\"$ANDROID_HOME/cmdline-tools/avdmanager-root\""
        exec "$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" "$@"
        ;;
    *)
        printf 'Usage: %s {build|test|start|run|stop|devices|sdk|avd}\n' "$0"
        printf 'run starts the emulator, builds, installs, seeds tasks if empty, and opens VerveTwoDo.\n'
        ;;
esac
