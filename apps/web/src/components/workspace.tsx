"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import {
  Sprout,
  LayoutDashboard,
  Building2,
  ScanSearch,
  FolderLock,
  ListChecks,
  Users,
  MessagesSquare,
  History,
  Settings,
  CircleHelp,
  Bell,
  Search,
  Menu,
  X,
  LogOut,
  ChevronDown,
  ShieldCheck,
  ArrowUpRight,
} from "lucide-react";
import { api } from "@/lib/api";
import { useSession } from "./providers";
import { Loading, ErrorBox } from "./ui/shared";
const items = [
  ["overview", LayoutDashboard],
  ["organizations", Building2],
  ["discovery", ScanSearch],
  ["documents", FolderLock],
  ["tasks", ListChecks],
  ["family", Users],
  ["correspondence", MessagesSquare],
  ["timeline", History],
  ["settings", Settings],
] as const;
export function Workspace({
  caseId,
  section,
  children,
}: {
  caseId?: string;
  section: string;
  children: React.ReactNode;
}) {
  const { user, loading, logout, t, locale, setLocale } = useSession();
  const router = useRouter();
  const [menu, setMenu] = useState(false);
  const [search, setSearch] = useState("");
  const [term, setTerm] = useState("");
  useEffect(() => {
    if (!loading && !user) router.replace("/login");
  }, [user, loading, router]);
  useEffect(() => {
    const timer = setTimeout(() => setTerm(search), 300);
    return () => clearTimeout(timer);
  }, [search]);
  const cases = useQuery({
    queryKey: ["cases"],
    queryFn: () => api<any[]>("/cases"),
    enabled: !!user,
  });
  const current = cases.data?.find((c) => c.id === caseId);
  const notifications = useQuery({
    queryKey: ["notifications"],
    queryFn: () => api<any[]>("/notifications"),
    enabled: !!user,
    refetchInterval: 30000,
  });
  const results = useQuery({
    queryKey: ["search", caseId, term],
    queryFn: () =>
      api<any[]>(`/search?caseId=${caseId}&q=${encodeURIComponent(term)}`),
    enabled: !!caseId && term.length > 1,
  });
  if (loading || !user) return <Loading />;
  return (
    <div className="workspace">
      <aside className={`sidebar ${menu ? "open" : ""}`}>
        <Link className="brand" href="/app">
          <span className="brand-icon">
            <Sprout size={24} />
          </span>
          OneNotify<span className="brand-dot">.</span>
        </Link>
        <button
          className="mobile-close icon-button"
          onClick={() => setMenu(false)}
          aria-label="Close menu"
        >
          <X />
        </button>
        <div className="workspace-caption">YOUR FAMILY WORKSPACE</div>
        <div className="case-switch">
          <span className="avatar initials">
            {current?.full_name?.slice(0, 1) || "F"}
          </span>
          <div>
            <strong>{current?.full_name || "Your cases"}</strong>
            <small>{caseId ? "Family case" : "A place to begin"}</small>
          </div>
          <ChevronDown size={14} />
          <select
            aria-label="Switch case"
            value={caseId || ""}
            onChange={(e) =>
              router.push(
                e.target.value ? `/app/cases/${e.target.value}` : "/app",
              )
            }
          >
            <option value="">All cases</option>
            {cases.data?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.full_name}
              </option>
            ))}
          </select>
        </div>
        <nav className="side-nav" aria-label="Case navigation">
          {caseId ? (
            items.map(([key, Icon]) => (
              <Link
                key={key}
                onClick={() => setMenu(false)}
                className={section === key ? "active" : ""}
                href={`/app/cases/${caseId}${key === "overview" ? "" : "/" + key}`}
              >
                <Icon size={19} />
                {t[key]}
              </Link>
            ))
          ) : (
            <Link className="active" href="/app">
              <LayoutDashboard size={19} />
              {t.allCases}
            </Link>
          )}
        </nav>
        <div className="sidebar-bottom">
          <div className="support-card">
            <span className="support-leaf">
              <Sprout size={24} />
            </span>
            <strong>You can take your time.</strong>
            <p>
              A small step today is enough.
              <br />
              We’ll keep everything here.
            </p>
            <Link href="/help">
              A little guidance <ArrowUpRight size={14} />
            </Link>
          </div>
          <Link className="help-link" href="/help">
            <CircleHelp size={18} />
            {t.help}
          </Link>
          <button
            className="user-card"
            onClick={() => router.push("/app/settings")}
          >
            <span className="avatar">{user.name.slice(0, 1)}</span>
            <div>
              <strong>{user.name}</strong>
              <small>Your account</small>
            </div>
            <Settings size={16} />
          </button>
        </div>
      </aside>
      {menu && (
        <button
          className="menu-backdrop"
          aria-label="Close menu"
          onClick={() => setMenu(false)}
        />
      )}
      <div className="workspace-body">
        <header className="topbar">
          <button
            className="icon-button mobile-menu"
            onClick={() => setMenu(true)}
            aria-label="Open navigation"
          >
            <Menu />
          </button>
          <div className="breadcrumbs">
            Your workspace<span>/</span>
            <strong>
              {caseId
                ? t[section as keyof typeof t] || "Organization checklist"
                : "Overview"}
            </strong>
          </div>
          <div className="topbar-tools">
            {caseId && (
              <div className="search-wrap">
                <Search size={17} />
                <input
                  aria-label="Search this case"
                  placeholder="Search your case…"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
                {term.length > 1 && (
                  <div className="search-results">
                    <ErrorBox error={results.error} />
                    {results.data?.length === 0 && <p>No matching items.</p>}
                    {results.data?.map((r) => (
                      <Link
                        key={r.id}
                        onClick={() => {
                          setSearch("");
                          setTerm("");
                        }}
                        href={`/app/cases/${caseId}/${r.type}${r.type === "organizations" ? "/" + r.id : ""}`}
                      >
                        <small>{r.type}</small>
                        {r.title}
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            )}
            <select
              className="language-select"
              aria-label="Language"
              value={locale}
              onChange={(e) => setLocale(e.target.value as "en" | "hi")}
            >
              <option value="en">EN</option>
              <option value="hi">हिंदी</option>
            </select>
            <Link
              className="icon-button notification-bell"
              href="/app/notifications"
              aria-label="Notifications"
            >
              <Bell size={19} />
              {notifications.data?.some((n) => !n.read_at) && <span />}
            </Link>
            <button
              className="icon-button"
              aria-label="Sign out"
              onClick={async () => {
                await logout();
                router.push("/login");
              }}
            >
              <LogOut size={17} />
            </button>
          </div>
        </header>
        <div className="demo-banner">
          <span className="demo-pill">DEMO WORKSPACE</span>Provider requirements
          and responses are examples. No organizations are contacted.
        </div>
        <main id="main" className="main-content">
          {children}
        </main>
        <footer className="workspace-footer">
          <span>
            <ShieldCheck size={14} />
            Your family’s information, handled with care.
          </span>
          <Link href="/privacy">Privacy & your data</Link>
          {user.system_role === "ADMIN" && (
            <Link href="/app/admin">Demo administration</Link>
          )}
        </footer>
      </div>
    </div>
  );
}
