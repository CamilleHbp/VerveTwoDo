import { test } from "node:test";
import assert from "node:assert/strict";
import { detectWidgetPlatform, widgetLink } from "../src/platform";

const mac = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/131.0 Safari/537.36";
const windows = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0 Safari/537.36";

test("offers the companion matching the desktop system", () => {
  assert.equal(detectWidgetPlatform({ userAgent: mac, touchPoints: 0 }), "macos");
  assert.equal(detectWidgetPlatform({ platform: "macOS", userAgent: mac, touchPoints: 0 }), "macos");
  assert.equal(detectWidgetPlatform({ platform: "Windows", userAgent: windows, touchPoints: 0, platformVersion: "15.0.0" }), "windows");
  assert.equal(detectWidgetPlatform({ userAgent: windows, touchPoints: 0 }), "windows");
});

test("offers nothing where the companion cannot run", () => {
  assert.equal(detectWidgetPlatform({ platform: "Windows", userAgent: windows, touchPoints: 0, platformVersion: "10.0.0" }), null);
  assert.equal(detectWidgetPlatform({ userAgent: mac, touchPoints: 5 }), null, "iPadOS");
  assert.equal(detectWidgetPlatform({ platform: "Android", mobile: true, userAgent: "Mozilla/5.0 (Linux; Android 15)", touchPoints: 5 }), null);
  assert.equal(detectWidgetPlatform({ platform: "Linux", userAgent: "Mozilla/5.0 (X11; Linux x86_64)", touchPoints: 0 }), null);
  assert.equal(detectWidgetPlatform({ userAgent: "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X)", touchPoints: 5 }), null);
});

test("reads widget links from the protocol handler and legacy parameters", () => {
  const id = "0f6d1c1e-5b0a-4c4e-9a59-7f2f3f1d2b11";
  const encoded = (value: string) => "?protocol=" + encodeURIComponent(value);
  assert.deepEqual(widgetLink(encoded(`web+vervetwodo://task?id=${id}`)), { task: id, fromWidget: true });
  assert.deepEqual(widgetLink(encoded("web+vervetwodo://add")), { add: true, fromWidget: true });
  assert.deepEqual(widgetLink(encoded("web+vervetwodo://open/")), { fromWidget: true });
  assert.equal(widgetLink(encoded("web+vervetwodo://task?id=short")), null);
  assert.equal(widgetLink(encoded("web+other://open")), null);
  assert.equal(widgetLink(encoded("web+vervetwodo://delete")), null);
  assert.deepEqual(widgetLink(`?task=${id}&widget=1`), { task: id, add: false, fromWidget: true });
  assert.deepEqual(widgetLink("?new=1"), { task: undefined, add: true, fromWidget: false });
  assert.equal(widgetLink("?q=1"), null);
});
