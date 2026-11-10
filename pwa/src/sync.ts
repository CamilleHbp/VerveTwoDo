import { Operation, project } from "./model";
import { read, mutate } from "./storage";
import { updatePush } from "./push";
export type SyncStatus =
  "local" | "syncing" | "synced" | "pending" | "offline" | "error" | "signIn";
let busy = false;
let stream: EventSource | undefined;
let listener: (s: SyncStatus) => void = () => {};
export function onSync(fn: typeof listener) {
  listener = fn;
}
export async function api(path: string, body: unknown = {}) {
  const response = await fetch(`/api/${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "same-origin",
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(70000),
  });
  if (response.status === 401) throw Error("signIn");
  if (!response.ok)
    throw Error(response.status === 429 ? "rateLimited" : "syncError");
  const result = await response.json();
  if (result.error) throw Error("syncError");
  return result;
}
function batch(ops: Operation[]) {
  const result: Operation[] = [];
  let bytes = 256;
  for (const op of ops.slice(0, 200)) {
    const size = new TextEncoder().encode(JSON.stringify(op)).length + 1;
    if (size >= 198000) throw Error("storageLimit");
    if (bytes + size >= 198000) break;
    result.push(op);
    bytes += size;
  }
  return result;
}
export async function sync() {
  if (busy) return;
  const d = await read();
  if (!d.connected) {
    listener("local");
    return;
  }
  busy = true;
  listener("syncing");
  try {
    // A cross-tab lock prevents an older response from replacing a newer projection.
    const run = async () => {
      let current = await read();
      while (current.outbox.length) {
        const ops = batch(current.outbox);
        const reply = await api("apply", { operations: ops });
        await mutate((d) => {
          const ids = new Set(ops.map((o) => o.id));
          d.outbox = d.outbox.filter((o) => !ids.has(o.id));
          project(d, reply.state.records || []);
        });
        current = await read();
      }
      const reply = await api("check");
      await mutate((d) => project(d, reply.state.records || []));
      listener(reply.telemetry?.confirmed ? "synced" : "pending");
    };
    if (navigator.locks) await navigator.locks.request("vervetwodo-sync", run);
    else await run();
  } catch (e) {
    listener(
      !navigator.onLine
        ? "offline"
        : e instanceof Error && e.message === "signIn"
          ? "signIn"
          : "error",
    );
  } finally {
    busy = false;
  }
}
export async function connect(code: string) {
  await api("login", { code });
  await mutate((d) => {
    d.connected = true;
  });
  watch();
  await sync();
  const current = await read();
  if (current.preferences.push) await updatePush(current);
}
export async function disconnect() {
  stream?.close();
  stream = undefined;
  await api("logout");
  await mutate((d) => {
    d.connected = false;
    d.preferences.push = false;
  });
  listener("local");
}
function watch() {
  if (stream) return;
  stream = new EventSource("/api/events");
  stream.onmessage = () => void sync();
  stream.addEventListener("ready", () => void sync());
  stream.onerror = () => {
    if (!navigator.onLine) listener("offline");
  };
}
export function startSync() {
  void read().then((d) => {
    if (d.connected) watch();
  });
  const online = () => void sync();
  window.addEventListener("online", online);
  const visible = () => {
    if (document.visibilityState === "visible") void sync();
  };
  document.addEventListener("visibilitychange", visible);
  void sync();
  return () => {
    stream?.close();
    stream = undefined;
    window.removeEventListener("online", online);
    document.removeEventListener("visibilitychange", visible);
  };
}
