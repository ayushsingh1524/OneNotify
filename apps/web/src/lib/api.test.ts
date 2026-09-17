import { afterEach, expect, test, vi } from "vitest";
import { api, setToken } from "./api";

afterEach(() => {
  setToken("");
  vi.unstubAllGlobals();
});

test("an expired access token cannot prevent cookie-based logout", async () => {
  const fetch = vi.fn().mockResolvedValue(
    new Response('{"ok":true}', {
      headers: { "Content-Type": "application/json" },
    }),
  );
  vi.stubGlobal("fetch", fetch);
  setToken("expired-session-token");
  await api("/auth/logout", { method: "POST" });
  expect(fetch.mock.calls[0][1].headers.has("Authorization")).toBe(false);
  expect(fetch.mock.calls[0][1].credentials).toBe("same-origin");
});

test("authenticated profile requests still carry the access token", async () => {
  const fetch = vi.fn().mockResolvedValue(
    new Response('{"id":"sample"}', {
      headers: { "Content-Type": "application/json" },
    }),
  );
  vi.stubGlobal("fetch", fetch);
  setToken("current-session-token");
  await api("/auth/me");
  expect(fetch.mock.calls[0][1].headers.get("Authorization")).toBe(
    "Bearer current-session-token",
  );
});
