import { test, expect } from "@playwright/test";
import path from "node:path";
const fixture = path.resolve(__dirname, "../../../scripts/fixtures/sample.png");
test("desktop demo dashboard and mobile navigation", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: /You don’t have to/ }),
  ).toBeVisible();
  await page.screenshot({
    path: "../../docs/screenshots/landing.png",
    fullPage: true,
  });
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
  await page.screenshot({
    path: "../../docs/screenshots/dashboard.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(
    page.getByRole("button", { name: "Open navigation" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Open navigation" }).click();
  await page
    .getByRole("navigation", { name: "Case navigation" })
    .getByRole("link", { name: "Document vault" })
    .click();
  await expect(
    page.getByRole("heading", { name: "Your document vault." }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Upload document" }),
  ).toBeVisible();
  await page.screenshot({
    path: "../../docs/screenshots/mobile-vault.png",
    fullPage: true,
  });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
  expect(errors).toEqual([]);
});
test("family can create a case and complete a simulated organization request", async ({
  page,
  request,
}) => {
  const email = `browser-${Date.now()}@example.test`;
  const registration = await request.post("/api/v1/auth/register", {
    data: {
      name: "Browser test family",
      email,
      password: "Browser-test-2026!",
    },
  });
  expect(registration.ok()).toBeTruthy();
  await page.goto("/login");
  await page.getByLabel("Email address").fill(email);
  await page.getByLabel("Password", { exact: true }).fill("Browser-test-2026!");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page).toHaveURL(/\/app$/);
  await page.getByRole("link", { name: "Create a case", exact: true }).click();
  await page
    .getByLabel("Your family member’s full name")
    .fill("Browser Sample Person");
  await page.getByLabel("Your relationship to them").selectOption("Child");
  await page.getByRole("button", { name: "Continue", exact: true }).click();
  await page.getByLabel("Date of death", { exact: true }).fill("2025-01-01");
  await page.getByLabel("City", { exact: true }).fill("Pune");
  await page.getByLabel("State / union territory").fill("Maharashtra");
  await page.getByRole("button", { name: "Continue", exact: true }).click();
  await page.getByRole("button", { name: "Continue", exact: true }).click();
  await page.getByRole("button", { name: "Continue", exact: true }).click();
  await page
    .getByRole("button", { name: "Create your case", exact: true })
    .click();
  await expect(page).toHaveURL(/\/app\/cases\/[a-f0-9-]+$/);
  const caseUrl = page.url();
  await page
    .getByRole("link", { name: "Add an organization", exact: true })
    .click();
  await page
    .locator(".registry-card")
    .filter({ has: page.getByRole("heading", { name: "SBI", exact: true }) })
    .getByRole("button", { name: "Add to checklist" })
    .click();
  await expect(
    page
      .locator(".registry-card")
      .filter({ has: page.getByRole("heading", { name: "SBI", exact: true }) })
      .getByRole("button", { name: "Added" }),
  ).toBeVisible();
  await page.goto(`${caseUrl}/documents`);
  async function upload(category: string) {
    await page.getByLabel("Document type").selectOption(category);
    await page.getByLabel("Choose a document").setInputFiles(fixture);
    await page
      .getByRole("button", { name: "Upload document", exact: true })
      .click();
    await expect(
      page.getByRole("status").filter({ hasText: "Document added" }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Upload document", exact: true }),
    ).toBeEnabled();
  }
  await upload("DEATH_CERTIFICATE");
  await upload("USER_IDENTITY");
  await expect(page.locator(".document-row")).toHaveCount(2);
  await page
    .getByRole("button", { name: "Preview sample.png" })
    .first()
    .click();
  const preview = page.getByRole("dialog", { name: "Document preview" });
  await expect(preview).toBeVisible();
  await expect(preview.getByRole("img", { name: "sample.png" })).toBeVisible();
  await preview.getByRole("button", { name: "Close", exact: true }).click();
  await expect(preview).not.toBeVisible();
  await page.goto(`${caseUrl}/organizations`);
  await page
    .locator(".organization-card")
    .filter({ has: page.getByRole("heading", { name: "SBI", exact: true }) })
    .click();
  await expect(page).toHaveURL(/\/organizations\/[a-f0-9-]+$/);
  const workflowUrl = page.url();
  async function approve() {
    await page.getByRole("button", { name: "Check & prepare request" }).click();
    await expect(
      page.getByRole("heading", { name: "Review & approve sharing" }),
    ).toBeVisible();
    for (const checkbox of await page.locator(".consent-docs input").all())
      await checkbox.check();
    await page.getByLabel("I have reviewed these details").check();
    await page
      .getByRole("button", {
        name: "Approve & simulate submission",
        exact: true,
      })
      .click();
    await expect(
      page.getByRole("button", { name: "Simulate next response" }),
    ).toBeEnabled();
  }
  await approve();
  for (const state of [
    "Provider acknowledged",
    "Under review",
    "Additional documents required",
  ]) {
    await page.getByRole("button", { name: "Simulate next response" }).click();
    await expect(page.locator(".page-heading .status")).toHaveText(state);
  }
  await page.goto(`${caseUrl}/documents`);
  await upload("NOMINEE_DOCUMENT");
  await expect(page.locator(".document-row")).toHaveCount(3);
  await page.goto(workflowUrl);
  await approve();
  for (const state of ["Under review", "Approved", "Completed"]) {
    await page.getByRole("button", { name: "Simulate next response" }).click();
    await expect(page.locator(".page-heading .status")).toHaveText(state);
  }
  const dl = page.waitForEvent("download");
  await page.getByRole("button", { name: "Request package" }).click();
  expect((await dl).suggestedFilename()).toBe("provider-package.zip");
  await page.screenshot({
    path: "../../docs/screenshots/workflow.png",
    fullPage: true,
  });
});
