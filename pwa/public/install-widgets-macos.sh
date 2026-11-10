#!/bin/sh
# Installs or updates VerveTwoDo Widgets for macOS:
#   curl -fsSL https://todo.camille.studio/install-widgets-macos.sh | sh
# The latest release is read from the download catalogue and checked against its SHA-256 and
# signature before anything is replaced. No administrator rights are needed.
set -eu

server="https://download.camille.studio"
bundle_id="studio.camille.vervetwodo.widgets"
app_name="VerveTwoDo Widgets.app"

case "$(defaults read -g AppleLanguages 2>/dev/null | sed -n 2p)" in
  *\"fr*|*" fr"*) french=1 ;;
  *) french=0 ;;
esac
say() { if [ "$french" = 1 ]; then printf '%s\n' "$2"; else printf '%s\n' "$1"; fi; }
fail() { say "$1" "$2" >&2; exit 1; }

[ "$(uname -s)" = Darwin ] || fail "VerveTwoDo Widgets requires macOS." "VerveTwoDo Widgets nécessite macOS."
major=$(sw_vers -productVersion | cut -d. -f1)
[ "$major" -ge 14 ] || fail "VerveTwoDo Widgets requires macOS 14 or later." "VerveTwoDo Widgets nécessite macOS 14 ou une version ultérieure."

work=$(mktemp -d -t vervetwodo-widgets)
trap 'rm -rf "$work"' EXIT

say "Downloading VerveTwoDo Widgets…" "Téléchargement de VerveTwoDo Widgets…"
curl -fsSL --retry 3 "$server/api/v1/vervetwodo-widgets.json" -o "$work/catalogue.json" ||
  fail "Could not reach the download server. Check your connection and try again." "Impossible de joindre le serveur de téléchargement. Vérifiez votre connexion et réessayez."
release=$(osascript -l JavaScript - "$work/catalogue.json" <<'JS'
function run(argv) {
  const text = $.NSString.stringWithContentsOfFileEncodingError(argv[0], $.NSUTF8StringEncoding, null).js;
  const catalogue = JSON.parse(text);
  const id = catalogue.latestByPlatform && catalogue.latestByPlatform.macos;
  const release = catalogue.releases.find((r) => r.id === id);
  const asset = release && release.assets.find((a) => a.platform === "macos" && a.primary && a.channel === "release");
  if (!asset || !/^\/apps\/[A-Za-z0-9._\/-]+$/.test(asset.url) || !/^[0-9a-f]{64}$/.test(asset.sha256)) return "";
  return [asset.url, asset.sha256, asset.size, release.version].join(" ");
}
JS
)
[ -n "$release" ] || fail "No macOS release is available yet." "Aucune version macOS n’est encore disponible."
set -- $release
url=$1 sha=$2 size=$3 version=$4

curl -fL --retry 3 --progress-bar "$server$url" -o "$work/widgets.zip" ||
  fail "The download failed. Try again." "Le téléchargement a échoué. Réessayez."
[ "$(stat -f %z "$work/widgets.zip")" = "$size" ] && [ "$(shasum -a 256 "$work/widgets.zip" | cut -d' ' -f1)" = "$sha" ] ||
  fail "The download could not be verified and was not installed." "Le téléchargement n’a pas pu être vérifié et n’a pas été installé."
ditto -x -k "$work/widgets.zip" "$work/app"
new="$work/app/$app_name"
[ "$(/usr/libexec/PlistBuddy -c 'Print CFBundleIdentifier' "$new/Contents/Info.plist" 2>/dev/null)" = "$bundle_id" ] &&
  codesign --verify --deep --strict "$new" 2>/dev/null ||
  fail "The download could not be verified and was not installed." "Le téléchargement n’a pas pu être vérifié et n’a pas été installé."

# Keep an existing copy in /Applications there; otherwise install for this user only.
if [ -d "/Applications/$app_name" ] && [ -w /Applications ]; then target="/Applications/$app_name"; else
  mkdir -p "$HOME/Applications"; target="$HOME/Applications/$app_name"; fi

say "Installing VerveTwoDo Widgets $version…" "Installation de VerveTwoDo Widgets $version…"
osascript -e "tell application id \"$bundle_id\" to quit" >/dev/null 2>&1 || true
for _ in 1 2 3 4 5 6 7 8 9 10; do pgrep -f "$app_name/Contents/MacOS/" >/dev/null || break; sleep 1; done
pkill -f "$app_name/Contents/MacOS/" 2>/dev/null || true
if [ -e "$target" ]; then mv "$target" "$work/previous.app"; fi
if ! ditto "$new" "$target"; then
  rm -rf "$target"
  [ -e "$work/previous.app" ] && mv "$work/previous.app" "$target"
  fail "VerveTwoDo Widgets could not be installed." "VerveTwoDo Widgets n’a pas pu être installé."
fi
for extension in "$target"/Contents/PlugIns/*.appex; do pluginkit -a "$extension" 2>/dev/null || true; done
open "$target"
say "VerveTwoDo Widgets is installed. Connect with your access code, then add VerveTwoDo from Edit Widgets." \
    "VerveTwoDo Widgets est installé. Connectez-vous avec votre code d’accès, puis ajoutez VerveTwoDo depuis Modifier les widgets."
