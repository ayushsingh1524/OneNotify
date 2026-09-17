import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
test("landing and authenticated workspace have no serious accessibility violations", async ({
  page,
}) => {
  await page.goto("/");
  let result = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
    .analyze();
  expect(
    result.violations.map((v) => ({
      id: v.id,
      impact: v.impact,
      nodes: v.nodes.map((n) => n.target),
    })),
  ).toEqual([]);
  await page.goto("/login");
  await page.getByLabel("Email address").fill("family@example.test");
  await page.getByLabel("Password", { exact: true }).fill("Family-demo-2026!");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page).toHaveURL(/\/app$/);
  await page.getByRole("link", { name: /Rajesh Sharma/ }).click();
  await expect(
    page.getByRole("heading", {
      name: "A little clarity, one step at a time.",
    }),
  ).toBeVisible();
  result = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
    .analyze();
  expect(
    result.violations.map((v) => ({
      id: v.id,
      impact: v.impact,
      nodes: v.nodes.map((n) => ({
        target: n.target,
        summary: n.failureSummary,
      })),
    })),
  ).toEqual([]);
});

test("public account and help pages pass automated accessibility checks", async ({
  page,
}) => {
  for (const route of [
    "/login",
    "/register",
    "/forgot-password",
    "/reset-password",
    "/verify-email",
    "/help",
    "/privacy",
  ]) {
    await page.goto(route);
    await expect(page.getByRole("heading", { level: 1 }).first()).toBeVisible();
    const result = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
      .analyze();
    expect
      .soft(
        result.violations.map((v) => ({
          route,
          id: v.id,
          nodes: v.nodes.map((n) => ({
            target: n.target,
            summary: n.failureSummary,
          })),
        })),
      )
      .toEqual([]);
  }
});

test("family workspace pages pass automated accessibility checks", async ({
  page,
}) => {
  await page.goto("/login");
  await page.getByLabel("Email address").fill("family@example.test");
  await page.getByLabel("Password", { exact: true }).fill("Family-demo-2026!");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page).toHaveURL(/\/app$/);
  await page.getByRole("link", { name: /Rajesh Sharma/ }).click();
  await expect(page).toHaveURL(/\/app\/cases\/[a-f0-9-]+$/);
  const base = page.url();
  for (const route of [
    "documents",
    "tasks",
    "family",
    "correspondence",
    "timeline",
    "organizations",
    "discovery",
    "settings",
  ]) {
    await page.goto(`${base}/${route}`);
    await expect(page.getByRole("heading", { level: 1 }).first()).toBeVisible();
    const result = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21aa"])
      .analyze();
    expect
      .soft(
        result.violations.map((v) => ({
          route,
          id: v.id,
          nodes: v.nodes.map((n) => ({
            target: n.target,
            summary: n.failureSummary,
          })),
        })),
      )
      .toEqual([]);
  }
});
