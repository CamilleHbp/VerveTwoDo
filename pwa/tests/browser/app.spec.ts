import { test, expect } from "@playwright/test";
test('desktop collections, favorites, filtering, dark appearance and keyboard navigation',async({page})=>{
 await page.goto('/');await create(page,'Review the garden plan');await create(page,'Book the weekend train');await create(page,'Finish the reading notes');
 await page.locator('.favorite').first().click();await page.getByRole('button',{name:'Favorites',exact:true}).click();await expect(page.locator('.task-row')).toHaveCount(1);
 await page.getByRole('button',{name:'My collection',exact:true}).click();await page.getByRole('button',{name:'View options',exact:true}).click();await page.getByLabel('Dates',{exact:true}).selectOption('noDate');await page.getByRole('button',{name:'Done',exact:true}).click();await expect(page.locator('.task-row')).toHaveCount(3);
 await page.getByLabel('Search tasks',{exact:true}).fill('garden');await expect(page.locator('.task-row')).toHaveCount(1);await page.getByLabel('Search tasks',{exact:true}).fill('');
 await page.getByRole('button',{name:'Settings',exact:true}).click();await page.getByLabel('Appearance',{exact:true}).selectOption('dark');await page.getByRole('button',{name:'Done',exact:true}).click();await expect(page.locator('html')).toHaveAttribute('data-theme','dark');await page.screenshot({path:'test-results/desktop-dark.png',fullPage:true});
 await page.getByRole('button',{name:'Settings',exact:true}).click();await page.getByLabel('Appearance',{exact:true}).selectOption('light');await page.getByRole('button',{name:'Done',exact:true}).click();await page.screenshot({path:'test-results/desktop.png',fullPage:true});
 await page.keyboard.press('Control+n');await expect(page.getByRole('dialog',{name:'New task',exact:true})).toBeVisible();await page.getByLabel('Title',{exact:true}).fill('Draft');await page.keyboard.press('Escape');await expect(page.getByRole('dialog',{name:'Discard this draft?',exact:true})).toBeVisible();await page.getByRole('button',{name:'Discard',exact:true}).click();await expect(page.getByRole('dialog')).toHaveCount(0);
});
async function create(page: any, title: string) {
  await page.locator(".new-task").click();
  await page.getByLabel("Title", { exact: true }).fill(title);
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: title, exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Close", exact: true }).last().click();
}
test("create, markdown, checklist, metadata, recurrence, undo and offline reload", async ({
  page,
  context,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/");
  await page.locator(".new-task").click();
  await page.getByLabel("Title", { exact: true }).fill("Plan the weekend");
  await page
    .getByLabel("Notes", { exact: true })
    .fill("**Trail map**\n<script>alert(1)</script>");
  const entry = page.getByRole("textbox", {name:"Add a step"});
  await entry.fill("Pack water");
  await entry.press("Enter");
  await expect(entry).toBeFocused();
  await expect(entry).toHaveValue("");
  await page.locator(".step-composer").screenshot({path:"test-results/checklist-input-desktop.png"});
  await page.getByLabel("Due date", { exact: true }).fill("2026-11-07");
  await page.getByRole("switch", { name: "Repeat" }).check();
  await page.getByRole("radio", { name: "High", exact: true }).click();
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(page.locator(".markdown strong")).toHaveText("Trail map");
  await expect(page.locator(".markdown script")).toHaveCount(0);
  await page.getByRole("button", { name: "Close", exact: true }).last().click();
  await page
    .getByRole("button", {
      name: "Complete task: Plan the weekend",
      exact: true,
    })
    .click();
  await expect(page.locator(".task-row")).toHaveCount(1);
  await expect(page.locator(".metadata")).toContainText("Nov 14");
  await page.getByRole("button", { name: "Undo last task change" }).click();
  await expect(page.locator(".metadata")).toContainText("Nov 7");
  await page.evaluate(() => navigator.serviceWorker.ready);
  await page.reload();
  await context.setOffline(true);
  await page.reload();
  await expect(
    page.getByText("Plan the weekend", { exact: true }),
  ).toBeVisible();
  await create(page, "Offline task");
  await page.reload();
  await expect(page.getByText("Offline task", { exact: true })).toBeVisible();
  expect(errors).toEqual([]);
});
test("tags, edit rejection after another tab, and French preferences", async ({
  page,
  context,
}) => {
  await page.goto("/");
  await page.getByRole("button", { name: "New tag", exact: true }).click();
  await page.getByLabel("Name", { exact: true }).fill("Projects");
  await page.getByRole("button", { name: "Work", exact: true }).click();
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await create(page, "Shared draft");
  await page.getByText("Shared draft", { exact: true }).click();
  await page.getByRole("button", { name: "Edit · Title", exact: true }).click();
  await page.getByLabel("Title", { exact: true }).fill("Unsaved draft");
  const other = await context.newPage();
  await other.goto("/");
  await other.getByText("Shared draft", { exact: true }).click();
  await other.getByRole("button", { name: "Edit · Title", exact: true }).click();
  await other.getByLabel("Title", { exact: true }).fill("Saved elsewhere");
  await other.getByRole("button", { name: "Save", exact: true }).click();
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText(
    "changed while you were editing",
  );
  await expect(page.getByLabel("Title", { exact: true })).toHaveValue(
    "Unsaved draft",
  );
  await page.getByRole("button", { name: "Cancel", exact: true }).click();
  await page.getByRole("button", { name: "Close", exact: true }).last().click();
  await page.getByRole("button", { name: "Settings", exact: true }).click();
  await page.getByLabel("Language", { exact: true }).selectOption("fr");
  await expect(page.locator("html")).toHaveAttribute("lang", "fr");
  await page.reload();
  await expect(page.locator("html")).toHaveAttribute("lang", "fr");
  await expect(
    page.getByRole("button", { name: "Réglages", exact: true }),
  ).toBeVisible();
});
test("backup export/import, bulk editing and deletion undo", async ({
  page,
}) => {
  await page.goto("/");
  await create(page, "First");
  await create(page, "Second");
  await page.getByRole("button", { name: "Select", exact: true }).click();
  await page.getByRole("button", { name: "Select all", exact: true }).click();
  await page
    .getByRole("button", { name: "Edit selected tasks", exact: true })
    .click();
  await page.getByRole("radio", { name: "Urgent", exact: true }).click();
  await page.getByRole("button", { name: "Apply", exact: true }).click();
  await expect(page.locator(".metadata").first()).toContainText("Urgent");
  await page.getByRole("button", { name: "Delete", exact: true }).click();
  await page.getByRole("button", { name: "Apply", exact: true }).click();
  await expect(page.locator(".task-row")).toHaveCount(0);
  await page.getByRole("button", { name: "Undo last task change" }).click();
  await expect(page.locator(".task-row")).toHaveCount(2);
  await page.getByRole("button", { name: "Settings", exact: true }).click();
  const event = page.waitForEvent("download");
  await page.getByRole("button", { name: "Save backup…" }).click();
  const download = await event;
  await download.saveAs("test-results/backup.json");
  await page
    .locator("input[type=file]")
    .setInputFiles("test-results/backup.json");
  await page.getByRole("button", { name: "Apply", exact: true }).click();
  await page.getByRole("button", { name: "Done", exact: true }).click();
  await expect(page.locator(".task-row")).toHaveCount(4);
});
test("mobile layout and installable manifest", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: "Agenda", exact: true }),
  ).toBeVisible();
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth),
  ).toBeLessThanOrEqual(390);
  const manifest = await (
    await page.request.get("/manifest.webmanifest")
  ).json();
  for (const icon of manifest.icons) {
    expect((await page.request.get(icon.src)).status()).toBe(200);
  }
  await page.locator(".mobile-add").click();
  await page.getByLabel("Title", { exact: true }).fill("Mobile task");
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Mobile task", exact: true }),
  ).toBeVisible();
  await page.screenshot({ path: "test-results/mobile.png", fullPage: true });
});
function fakePlatform(platform: string, userAgent: string, mobile = false, platformVersion = "15.0.0") {
  return ({ platform, userAgent, mobile, platformVersion }: any) => {
    Object.defineProperty(navigator, "userAgent", { get: () => userAgent });
    Object.defineProperty(navigator, "userAgentData", {
      get: () => ({ platform, mobile, getHighEntropyValues: async () => ({ platformVersion }) }),
    });
  };
}
const widgetCases = [
  ["macOS", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)", "Download for macOS", "macos-universal.zip"],
  ["Windows", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)", "Download for Windows 11", "windows-x64.exe"],
] as const;
for (const [platform, userAgent, label, file] of widgetCases) {
  test(`offers the ${platform} widget companion and remembers its installation`, async ({ page }) => {
    await page.addInitScript(fakePlatform(platform, userAgent), { platform, userAgent, mobile: false, platformVersion: "15.0.0" });
    await page.goto("/");
    await page.getByRole("button", { name: "Settings", exact: true }).click();
    await expect(page.getByRole("heading", { name: "Desktop widget", exact: true })).toBeVisible();
    await expect(page.getByRole("link", { name: label, exact: true })).toHaveAttribute("href", new RegExp(`/vervetwodo-widgets/latest/${file}$`));
    await page.goto("/?widget=1&new=1");
    await expect(page.getByRole("dialog", { name: "New task", exact: true })).toBeVisible();
    await expect(page).toHaveURL(/\/$/);
    await page.getByRole("button", { name: "Cancel", exact: true }).or(page.getByRole("button", { name: "Close", exact: true })).first().click();
    await page.getByRole("button", { name: "Settings", exact: true }).click();
    await expect(page.getByText("VerveTwoDo Widgets is installed.", { exact: false })).toBeVisible();
  });
}
test("offers a one-command macOS install", async ({ page, context }) => {
  const userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)";
  await context.grantPermissions(["clipboard-read", "clipboard-write"]);
  await page.addInitScript(fakePlatform("macOS", userAgent), { platform: "macOS", userAgent, mobile: false, platformVersion: "15.0.0" });
  await page.goto("/");
  await page.getByRole("button", { name: "Settings", exact: true }).click();
  const command = "curl -fsSL https://todo.camille.studio/install-widgets-macos.sh | sh";
  await expect(page.getByText(command, { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Copy command", exact: true }).click();
  await expect(page.getByText("Command copied. Paste it in Terminal.", { exact: true })).toBeVisible();
  expect(await page.evaluate(() => navigator.clipboard.readText())).toBe(command);
});
test("opens widget links delivered by the protocol handler", async ({ page }) => {
  await page.goto("/?protocol=" + encodeURIComponent("web+vervetwodo://add"));
  await expect(page.getByRole("dialog", { name: "New task", exact: true })).toBeVisible();
  await expect(page).toHaveURL(/\/$/);
  const manifest = await (await page.request.get("/manifest.webmanifest")).json();
  expect(manifest.protocol_handlers).toEqual([{ protocol: "web+vervetwodo", url: "/?protocol=%s" }]);
});
test("serves the macOS install script", async ({ request }) => {
  const script = await (await request.get("/install-widgets-macos.sh")).text();
  expect(script.startsWith("#!/bin/sh")).toBe(true);
  expect(script).toContain("/api/v1/vervetwodo-widgets.json");
});
test("offers no widget companion on phones", async ({ page }) => {
  const userAgent = "Mozilla/5.0 (Linux; Android 15) Mobile";
  await page.addInitScript(fakePlatform("Android", userAgent, true), { platform: "Android", userAgent, mobile: true, platformVersion: "15.0.0" });
  await page.goto("/");
  await page.getByRole("button", { name: "Settings", exact: true }).click();
  await expect(page.getByRole("heading", { name: "About", exact: true })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Desktop widget", exact: true })).toHaveCount(0);
});

test("edit individual fields on a phone and keep their values after reopening", async ({page}) => {
  await page.setViewportSize({width: 390, height: 844});
  await page.goto("/");
  await page.locator(".mobile-add").click();
  await page.getByLabel("Title", {exact:true}).fill("Plan the route");
  await page.getByRole("button", {name:"Save", exact:true}).click();
  const dialog = page.getByRole("dialog", {name:"Task details"});
  await dialog.getByRole("button", {name:"Edit · Title"}).click();
  await expect(dialog.getByLabel("Title", {exact:true})).toBeFocused();
  await dialog.getByLabel("Title", {exact:true}).fill("Walk the coast");
  await dialog.getByRole("button", {name:"Add notes"}).click();
  await dialog.getByLabel("Notes", {exact:true}).fill("Bring the **map**.");
  await dialog.getByRole("radio", {name:"High",exact:true}).click();
  await expect(dialog.locator(".markdown strong")).toHaveText("map");
  const step = dialog.getByRole("textbox", {name:"Add a step"});
  await step.fill("Pack water");
  await step.press("Enter");
  await expect(step).toBeFocused();
  await expect(step).toHaveValue("");
  await dialog.locator(".step-composer").screenshot({path:"test-results/checklist-input-phone.png"});
  await step.fill("Pack lunch");
  await step.press("Enter");
  await expect(dialog.getByRole("checkbox",{name:"Pack lunch"})).toBeVisible();
  await expect(step).toBeFocused();
  await step.fill("Discard me");
  await step.press("Escape");
  await expect(step).toHaveValue("");
  await expect(step).not.toBeFocused();
  await expect(dialog.getByRole("checkbox",{name:"Discard me"})).toHaveCount(0);
  await dialog.getByRole("checkbox", {name:"Pack water"}).click();
  await expect(dialog.getByRole("checkbox", {name:"Pack water"})).toBeChecked();
  await dialog.getByRole("button", {name:"Edit · Due date"}).click();
  await dialog.getByLabel("Due date", {exact:true}).fill("2027-01-09");
  await dialog.getByRole("button", {name:"Save", exact:true}).click();
  await dialog.getByRole("button", {name:"Edit · Title"}).click();
  await dialog.getByLabel("Title", {exact:true}).fill("Discard me");
  await dialog.getByLabel("Title", {exact:true}).press("Escape");
  await expect(dialog.getByRole("heading", {name:"Walk the coast"})).toBeVisible();
  await dialog.getByRole("button", {name:"Close", exact:true}).last().click();
  await page.reload();
  await page.getByText("Walk the coast", {exact:true}).click();
  await expect(dialog.getByRole("radio", {name:"High",exact:true})).toBeChecked();
  await expect(dialog.getByRole("checkbox", {name:"Pack water"})).toBeChecked();
  await expect(dialog.getByRole("button", {name:"Edit · Due date"})).toContainText("Jan 9, 2027");
  await expect(dialog.locator(".markdown strong")).toHaveText("map");
  await page.screenshot({path: "test-results/task-inline-mobile.png"});
});
