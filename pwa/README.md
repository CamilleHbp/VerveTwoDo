# VerveTwoDo Web

An installable, offline-capable browser edition of VerveTwoDo. The web gateway runs independently of the Android and desktop applications and joins their existing private workspace.

## Use

Open [VerveTwoDo Web](https://todo.camille.studio). Use your browser’s **Install app**, **Add to Dock**, or **Add to Home Screen** command. After the first successful load, the application works offline.

If you installed the earlier sslip.io edition, synchronize there first, then open and install this address and reconnect with your existing access code. Browser storage, preferences, and notification permissions are separate for each address. The earlier address remains available during the transition; export a backup there if you keep tasks local-only.

Tasks are stored in this browser’s IndexedDB database. In **Settings → Synchronization**, enter the same access code used by the Mac or Android app. Local tasks merge into the workspace; independent field edits merge, and competing values remain available for explicit resolution. **Disconnect** revokes the web session but retains this browser’s local tasks. Browser data clearing removes the local copy; export a backup for independent safekeeping.

The application supports:

- Agenda and tag collections, text search, completion filters, six sort orders, collapsible date groups, favorites, and bulk edits.
- Task details and editing, Markdown notes, ordered checklists, five priority levels, dates, explicit time zones, and daily/weekly/monthly/yearly recurrence with calendar anchors.
- Per-task rollover, a default for new tasks, completion/deletion Undo that preserves subsequent edits, and stale-draft protection across tabs and sync updates.
- Tag names, colors and the Mac category artwork; tag deletion preserves tasks.
- Desktop JSON backup import/export and CSV export. Imports create fresh task identities and never replace existing tasks. Android ZIP backups are not accepted.
- System/English/French language selection, system/light/dark appearance, compact rows, and keyboard shortcuts.
- A configurable **My collection** view with date scope, any/all/untagged matching, date grouping, sorting, and category icons.
- Foreground reminders and opt-in Web Push reminders for synchronized tasks, with default time and advance reminders. Completing, deleting, or rescheduling a task changes the server’s reminder eligibility.

Browser applications cannot register desktop widgets themselves. On macOS and Windows 11, **Settings → Desktop widget** offers **VerveTwoDo Widgets**, a small companion that syncs with the same access code, shows your tasks in the system widgets, and opens tasks back in this app. It updates itself. **My collection** supplies the configurable task view inside the PWA. Local-only reminders require the page to run. Background reminders require synchronization, an active web session, browser push support, and notification permission. Browser/OS restrictions, offline devices, Focus modes, and expired sessions may delay or prevent delivery. On iOS/iPadOS, install the application on the Home Screen before enabling background reminders. Reconnect when the application requests session renewal.

## Development

Prerequisites: Node.js 22.12 or newer and Go 1.27. Private Go dependencies require access to the same pinned `sync-core` module as the desktop app.

```sh
cd pwa
npm ci
npm run build
npm test
(cd server && GOWORK=off go test -race ./...)
npm run test:e2e
python3 scripts/test-sync.py
```

The browser tests start the local Go gateway and use isolated browser storage. The synchronization test creates temporary upstream and gateway instances and never reads or writes production tasks.

Run the production build locally:

```sh
cd pwa/server
GOWORK=off go run .
```

Open `http://127.0.0.1:8791`. For Vite development, run the gateway with `PUBLIC_ORIGIN=http://127.0.0.1:5173`, then run `npm run dev` in `pwa`. All browser API requests remain on the page’s own origin.

The gateway accepts `PUBLIC_ORIGIN`, `SYNC_ORIGIN`, `LISTEN_ADDR`, `DATA_DIR`, and `ASSET_DIR`. The upstream workspace URL is configured by the operator, not supplied by the browser. Authentication uses expiring, HttpOnly, SameSite cookies. Static caching excludes all API responses. Session hashes, sync credentials, VAPID keys and push subscriptions belong only in the private data directory.

## Deployment

```sh
python3 pwa/scripts/deploy.py
```

The deployment script builds and tests the PWA and gateway, creates an immutable staged release, verifies transferred hashes, and starts only the `vervetwodo-web` Compose project. It adds and validates a dedicated Caddy route when needed. Existing sync and native app services are preserved. Public JavaScript, styles, icons, manifest and service worker are checked by size and SHA-256 after deployment.

The service data is `/srv/homeserver/data/vervetwodo-web`; include it in server backups. Code releases live under `/srv/homeserver/builds/vervetwodo-web`, with `current` pointing at the active release. To roll back, run Docker Compose with project `vervetwodo-web` from a previous release directory, retaining the same data volume. The service worker offers updates for explicit reload after drafts are saved.

This edition is served over HTTPS rather than distributed as an APK or desktop package. The native-app download catalogue remains unchanged.

## Credits and license

VerveTwoDo builds on VerveDo by super12138. Category artwork and English/French application vocabulary are reused from this repository’s desktop resources. The application is distributed under the repository’s GPL-3.0 license. Third-party libraries retain their respective licenses in the dependency metadata.
