// Desktop widget companions. Browsers cannot register desktop widgets themselves, so the
// app offers the native companion that matches the visitor's operating system.
export type WidgetPlatform = "macos" | "windows";

export const widgetDownloads: Record<WidgetPlatform, string> = {
  macos:
    "https://download.camille.studio/apps/vervetwodo-widgets/latest/macos-universal.zip",
  windows:
    "https://download.camille.studio/apps/vervetwodo-widgets/latest/windows-x64.exe",
};

// curl downloads carry no quarantine flag, so this installs without a Gatekeeper approval.
export const widgetInstallCommand =
  "curl -fsSL https://todo.camille.studio/install-widgets-macos.sh | sh";

type Hints = {
  platform?: string;
  mobile?: boolean;
  userAgent: string;
  touchPoints: number;
  platformVersion?: string;
};

/** Windows widgets need Windows 11 (platformVersion major ≥ 13); an unknown version still qualifies. */
export function detectWidgetPlatform(h: Hints): WidgetPlatform | null {
  if (h.mobile || /Android|iPhone|iPad|iPod|Mobile/i.test(h.userAgent)) return null;
  const name = (h.platform || "").toLowerCase();
  const mac = name ? name === "macos" : /Macintosh|Mac OS X/.test(h.userAgent);
  // iPadOS reports a Mac user agent; touch input distinguishes it.
  if (mac) return h.touchPoints > 1 ? null : "macos";
  const windows = name ? name === "windows" : /Windows NT/.test(h.userAgent);
  if (!windows) return null;
  const major = Number((h.platformVersion || "").split(".")[0]);
  return h.platformVersion && Number.isFinite(major) && major > 0 && major < 13
    ? null
    : "windows";
}

export async function widgetPlatform(): Promise<WidgetPlatform | null> {
  const data = (navigator as any).userAgentData;
  let platformVersion: string | undefined;
  if (data?.platform === "Windows" && data.getHighEntropyValues) {
    try {
      platformVersion = (await data.getHighEntropyValues(["platformVersion"]))
        .platformVersion;
    } catch {
      platformVersion = undefined;
    }
  }
  return detectWidgetPlatform({
    platform: data?.platform,
    mobile: data?.mobile,
    userAgent: navigator.userAgent,
    touchPoints: navigator.maxTouchPoints || 0,
    platformVersion,
  });
}

// Per-browser conveniences only: storage may be unavailable, and the offer still works then.
function flag(key: string, set?: boolean) {
  try {
    if (set) localStorage.setItem(key, "1");
    return localStorage.getItem(key) === "1";
  } catch {
    return false;
  }
}
export const widgetInstalled = (set?: boolean) =>
  flag("vervetwodo.widget.installed", set);
export const widgetOfferDismissed = (set?: boolean) =>
  flag("vervetwodo.widget.dismissed", set);

// Widget companions open the installed web app through this protocol handler, which the
// browser registers with the operating system when the app is installed.
export const widgetProtocol = "web+vervetwodo";

export type WidgetLink = { task?: string; add?: boolean; fromWidget: boolean };

/** Reads a widget link from the page query: `?protocol=web+vervetwodo://task?id=…` or `?task=…&widget=1`. */
export function widgetLink(search: string): WidgetLink | null {
  const params = new URLSearchParams(search);
  const raw = params.get("protocol");
  if (raw !== null) {
    const match = /^web\+vervetwodo:(?:\/\/)?([a-z]+)\/?(?:\?(.*))?$/i.exec(raw);
    if (!match) return null;
    const action = match[1].toLowerCase(),
      id = new URLSearchParams(match[2] || "").get("id") || "";
    if (action === "task")
      return /^[A-Za-z0-9-]{16,128}$/.test(id) ? { task: id, fromWidget: true } : null;
    if (action === "add") return { add: true, fromWidget: true };
    return action === "open" ? { fromWidget: true } : null;
  }
  if (!params.has("task") && !params.has("new") && !params.has("widget")) return null;
  return {
    task: params.get("task") || undefined,
    add: params.get("new") === "1",
    fromWidget: params.get("widget") === "1",
  };
}
