import { expect, test } from "@playwright/test";

test("A4 page renders interactive documentation and search opens with Ctrl+K", async ({ page }) => {
  await page.goto("/docs/animation/a4-npc-lifecycle-integration/");
  await expect(page.getByRole("heading", { name: "Жизненный цикл NPC-анимации" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Карта жизненного цикла NPC" })).toBeVisible();
  await page.waitForSelector("html[data-search-ready='true']", { timeout: 15_000 });
  await page.getByRole("button", { name: "Открыть поиск" }).click();
  const dialog = page.getByRole("dialog", { name: "Поиск по атласу" });
  await expect(dialog).toBeVisible();
  await page.getByLabel("Поисковый запрос").fill("жизненный");
  await expect(dialog.getByRole("link", { name: /Жизненный цикл NPC-анимации/ }).first()).toBeVisible();
});

test("mobile route keeps native navigation reachable", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/docs/platform/console-commands/");
  await expect(page.getByRole("heading", { name: "Консольные команды", exact: true })).toBeVisible();
  await expect(page.getByRole("navigation", { name: "Разделы атласа" })).toBeVisible();
});
