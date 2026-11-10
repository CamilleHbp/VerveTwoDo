import { test, expect } from "@playwright/test";

test("time fields accept typed hours and fall back to the default reminder time", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: "Settings", exact: true }).click();
  const hour = page.getByLabel("Default reminder time, Hour", { exact: true });
  const minute = page.getByLabel("Default reminder time, Minute", { exact: true });
  await expect(hour).toHaveValue("09");
  // Click leaves the caret wherever it lands; typing must still overwrite the field.
  await hour.click();
  await page.keyboard.press("End");
  await page.keyboard.type("10");
  await page.keyboard.press("Tab");
  await page.keyboard.type("15");
  await page.keyboard.press("Tab");
  await expect(hour).toHaveValue("10");
  await expect(minute).toHaveValue("15");
  await page.getByRole("button", { name: "Done", exact: true }).click();
  await page.getByRole("button", { name: "Settings", exact: true }).click();
  await expect(hour).toHaveValue("10");
  await expect(minute).toHaveValue("15");
  await page.getByRole("button", { name: "Done", exact: true }).click();

  await page.locator(".new-task").click();
  await page.getByLabel("Title", { exact: true }).fill("Timed task");
  await page.getByLabel("Due date", { exact: true }).fill("2026-11-07");
  await page.getByRole("switch", {name:"Set a time"}).check();
  const taskHour = page.getByLabel("Time (24-hour), Hour", { exact: true });
  const taskMinute = page.getByLabel("Time (24-hour), Minute", { exact: true });
  await expect(taskHour).toHaveAttribute("placeholder", "10");
  await expect(taskMinute).toHaveAttribute("placeholder", "15");
  // Only the hour is typed: the minute comes from the default reminder time.
  await taskHour.click();
  await page.keyboard.type("14");
  await page.getByRole("switch", {name:"Repeat"}).check();
  await page.getByLabel("Every", {exact:true}).fill("2");
  await page.getByLabel("Repeat unit").selectOption("monthly");
  await expect(page.getByText("Next occurrence: Jan 7, 2027")).toBeVisible();
  await page.screenshot({path:"test-results/schedule-desktop.png"});
  await page.setViewportSize({width:390,height:844});
  await page.getByText("Next occurrence: Jan 7, 2027").scrollIntoViewIfNeeded();
  await page.screenshot({path:"test-results/schedule-phone.png"});
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(page.getByText("14:15", { exact: false }).first()).toBeVisible();
  await page.screenshot({ path: "test-results/time-input.png" });
});
