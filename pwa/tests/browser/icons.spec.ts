import { test, expect } from "@playwright/test";
import { iconIds, iconLabels } from "../../src/shared-icons";

test("tag icon picker offers the whole shared Camille Icons library", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: "New tag", exact: true }).click();
  const grid = page.locator(".icon-grid button");
  await expect(grid).toHaveCount(iconIds.length);
  for (const id of ["book", "tools", "calendar", "folder", "web", "computer", "keys", "clock", "lightbulb", "train", "headphones", "backpack"]) {
    const button = page.getByRole("button", { name: iconLabels.en[id], exact: true });
    await button.click();
    await expect(button).toHaveAttribute("aria-pressed", "true");
    await expect.poll(() => button.locator("img").evaluate((img: HTMLImageElement) => img.naturalWidth)).toBeGreaterThan(0);
    if (id === "book" || id === "folder") {
      const opacity = await button.locator("img").evaluate((img: HTMLImageElement) => {
        const canvas = document.createElement("canvas");
        canvas.width = img.naturalWidth;
        canvas.height = img.naturalHeight;
        const ctx = canvas.getContext("2d")!;
        ctx.drawImage(img, 0, 0);
        return [32, 64, 96].map(x => ctx.getImageData(x, 64, 1, 1).data[3]);
      });
      expect(opacity).toEqual([255, 255, 255]);
    }
  }
  await page.locator(".icon-grid").screenshot({ path: "test-results/icon-grid.png" });
});
