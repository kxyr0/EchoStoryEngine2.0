import { mkdirSync } from "node:fs";
import path from "node:path";
import { expect, test, type Page } from "@playwright/test";

const baselineDir = path.join(process.cwd(), "tests", "visual", "baselines");

async function capture(page: Page, project: string, scene: string) {
  mkdirSync(baselineDir, { recursive: true });
  await page.waitForTimeout(220);
  await page.evaluate(() => {
    document.documentElement.dataset.theme = "light";
    localStorage.setItem("echo-continuum-theme", "light");
  });
  await page.screenshot({
    path: path.join(baselineDir, `${project}-${scene}.png`),
    fullPage: false,
    animations: "disabled",
  });
  await page.evaluate(() => {
    document.documentElement.dataset.theme = "dark";
    localStorage.setItem("echo-continuum-theme", "dark");
  });
  await page.screenshot({
    path: path.join(baselineDir, `${project}-${scene}-dark.png`),
    fullPage: false,
    animations: "disabled",
  });
}

test("homepage-runtime-map", async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Платформа Echo" })).toBeVisible();
  await page.getByRole("link", { name: /Анимация/ }).first().hover();
  await capture(page, testInfo.project.name, "homepage-runtime-map");
});

test("dependency-field-selected", async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto("/docs/platform/dependency-rules/");
  await page.waitForSelector("astro-island[component-export='DependencyBoundary']:not([ssr])", { timeout: 15_000 });
  const forbiddenEdge = page.getByRole("button", { name: "Анимация -> NPC" });
  await forbiddenEdge.click();
  await expect(forbiddenEdge).toHaveAttribute("aria-pressed", "true");
  await expect(page.locator(".boundary-readout.is-blocked")).toContainText("Связь запрещена");
  await page.locator(".dependency-field").scrollIntoViewIfNeeded();
  await page.evaluate(() => window.scrollBy(0, -220));
  await capture(page, testInfo.project.name, "dependency-field-selected");
});

test("lifecycle-enter", async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto("/docs/animation/animation-overview/");
  await expect(page.getByRole("button", { name: /ENTER/ })).toBeVisible();
  await capture(page, testInfo.project.name, "lifecycle-enter");
});

test("lifecycle-main-stop-requested", async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto("/docs/animation/animation-overview/");
  await page.getByRole("button", { name: /MAIN/ }).click();
  await page.getByRole("button", { name: "Запрос остановки" }).click();
  await capture(page, testInfo.project.name, "lifecycle-main-stop-requested");
});

test("lifecycle-exit", async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto("/docs/animation/animation-overview/");
  await page.getByRole("button", { name: /EXIT/ }).click();
  await capture(page, testInfo.project.name, "lifecycle-exit");
});

test("syntax-line-selected", async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto("/docs/animation/animation-profiles/");
  await page.getByRole("button", { name: "Пояснить строку 3" }).click();
  await capture(page, testInfo.project.name, "syntax-line-selected");
});

test("npc-chunk-unloaded", async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto("/docs/animation/a4-npc-lifecycle-integration/");
  await page.getByRole("button", { name: /Чанк выгружен/ }).click();
  await capture(page, testInfo.project.name, "npc-chunk-unloaded");
});

test("npc-rematerialized", async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto("/docs/animation/a4-npc-lifecycle-integration/");
  await page.getByRole("button", { name: /Возвращён/ }).click();
  await capture(page, testInfo.project.name, "npc-rematerialized");
});

test("search-open", async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto("/docs/animation/a4-npc-lifecycle-integration/");
  await page.getByRole("button", { name: "Открыть поиск" }).click();
  await expect(page.getByRole("dialog", { name: "Поиск по атласу" })).toBeVisible();
  await capture(page, testInfo.project.name, "search-open");
});

test("focus-mode", async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto("/docs/animation/a4-npc-lifecycle-integration/");
  await page.getByRole("button", { name: "Включить режим чтения" }).click();
  await capture(page, testInfo.project.name, "focus-mode");
});

test("mobile-navigation", async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/docs/animation/a4-npc-lifecycle-integration/");
  await expect(page.getByRole("navigation", { name: "Разделы атласа" })).toBeVisible();
  await capture(page, testInfo.project.name, "mobile-navigation");
});

test("mobile-context-lens", async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/docs/animation/a4-npc-lifecycle-integration/");
  const actorCell = page.getByRole("button", { name: /Актёр/ }).first();
  await actorCell.scrollIntoViewIfNeeded();
  if (testInfo.project.name === "mobile") {
    await actorCell.tap({ force: true });
  } else {
    await actorCell.click();
  }
  await expect(page.getByRole("dialog", { name: "Контекст выбранного элемента" })).toBeVisible();
  await capture(page, testInfo.project.name, "mobile-context-lens");
});
