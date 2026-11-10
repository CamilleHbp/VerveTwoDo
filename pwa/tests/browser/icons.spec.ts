import { test, expect } from "@playwright/test";
import { iconIds, iconLabels } from "../../src/shared-icons";

test("tag icon picker offers the whole shared Camille Icons library", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: "New tag", exact: true }).click();
  const grid = page.locator(".icon-grid button");
  await expect(grid).toHaveCount(iconIds.length);
  // An icon the web app did not carry before the library export.
  const added = iconIds.find((id) => id === "groceries")!;
  await page.getByRole("button", { name: iconLabels.en[added], exact: true }).click();
  const image = page.locator(`.icon-grid img[src="/category-icons/${added}.png"]`);
  await expect(image).toBeVisible();
  expect(await image.evaluate((img: HTMLImageElement) => img.naturalWidth)).toBeGreaterThan(0);
  await page.locator(".icon-grid").screenshot({ path: "test-results/icon-grid.png" });
});
