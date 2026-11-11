import { test, expect } from "@playwright/test";
import { iconIds, iconLabels } from "../../src/shared-icons";

test("tag icon picker offers the whole shared Camille Icons library", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: "New tag", exact: true }).click();
  const grid = page.locator(".icon-grid button");
  await expect(grid).toHaveCount(iconIds.length);
  for (const id of ["book", "tools", "calendar", "folder", "web", "computer"]) {
    const button = page.getByRole("button", { name: iconLabels.en[id], exact: true });
    await button.click();
    await expect(button).toHaveAttribute("aria-pressed", "true");
    await expect.poll(() => button.locator("img").evaluate((img: HTMLImageElement) => img.naturalWidth)).toBeGreaterThan(0);
  }
  await page.locator(".icon-grid").screenshot({ path: "test-results/icon-grid.png" });
});
