let token = "";
let refreshing: Promise<boolean> | null = null;
export const setToken = (value: string) => {
  token = value;
};
export async function refresh(): Promise<boolean> {
  if (!refreshing)
    refreshing = fetch("/api/v1/auth/refresh", {
      method: "POST",
      credentials: "same-origin",
    })
      .then(async (r) => {
        if (!r.ok) {
          token = "";
          return false;
        }
        const data = await r.json();
        token = data.accessToken;
        return true;
      })
      .catch(() => false)
      .finally(() => {
        refreshing = null;
      });
  return refreshing;
}
export async function api<T = any>(
  path: string,
  options: RequestInit = {},
  retry = true,
): Promise<T> {
  const headers = new Headers(options.headers);
  if (token && (!path.startsWith("/auth/") || path === "/auth/me"))
    headers.set("Authorization", `Bearer ${token}`);
  if (options.body && !(options.body instanceof FormData))
    headers.set("Content-Type", "application/json");
  const response = await fetch(`/api/v1${path}`, {
    ...options,
    headers,
    credentials: "same-origin",
  });
  if (response.status === 401 && retry && !path.startsWith("/auth/")) {
    if (await refresh()) return api(path, options, false);
  }
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || `Request failed (${response.status})`);
  }
  if (response.headers.get("content-type")?.includes("application/json"))
    return response.json();
  return (await response.blob()) as T;
}
export const post = <T = any>(path: string, body?: unknown) =>
  api<T>(path, {
    method: "POST",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
export function download(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
export const label = (value: string = "") =>
  value
    .toLowerCase()
    .replaceAll("_", " ")
    .replace(/^./, (x) => x.toUpperCase());
export const date = (value?: string) =>
  value
    ? new Date(value).toLocaleDateString("en-IN", {
        day: "numeric",
        month: "short",
        year: "numeric",
      })
    : "Not set";
export const categories = [
  "DEATH_CERTIFICATE",
  "USER_IDENTITY",
  "DECEASED_IDENTITY",
  "NOMINEE_DOCUMENT",
  "LEGAL_HEIR_CERTIFICATE",
  "SUCCESSION_CERTIFICATE",
  "MARRIAGE_CERTIFICATE",
  "ACCOUNT_DOCUMENT",
  "POLICY_DOCUMENT",
  "BANK_STATEMENT",
  "INVESTMENT_STATEMENT",
  "UTILITY_BILL",
  "EMPLOYER_DOCUMENT",
  "CORRESPONDENCE",
  "OTHER",
];

export const recordCaseCreation = (started: number) =>
  post("/onboarding/timing", { seconds: (Date.now() - started) / 1000 });
