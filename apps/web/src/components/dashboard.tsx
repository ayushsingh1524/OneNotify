"use client";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import {
  ArrowRight,
  Plus,
  ArrowUpRight,
  Check,
  Clock3,
  AlertCircle,
  FolderHeart,
  Users,
  Download,
  Sprout,
  Building2,
} from "lucide-react";
import { api, date, download, label } from "@/lib/api";
import { useSession } from "./providers";
import { Button } from "./ui/button";
import { Heading, Loading, ErrorBox, Empty, Status } from "./ui/shared";
import { useState } from "react";
export function AllCases() {
  const { user, t } = useSession();
  const q = useQuery({
    queryKey: ["cases"],
    queryFn: () => api<any[]>("/cases"),
  });
  return (
    <>
      <Heading
        eyebrow="YOUR FAMILY WORKSPACE"
        title={`Welcome, ${user?.name.split(" ")[0] || "there"}.`}
        description="Everything you’re helping with, together in one place."
      >
        <Button asChild>
          <Link href="/app/new">
            <Plus size={17} />
            {t.newCase}
          </Link>
        </Button>
      </Heading>
      <ErrorBox error={q.error} />
      {q.isLoading ? (
        <Loading />
      ) : q.data?.length ? (
        <div className="case-grid">
          {q.data.map((c) => (
            <Link className="case-card" key={c.id} href={`/app/cases/${c.id}`}>
              <div className="case-card-top">
                <span className="case-monogram">
                  {c.full_name
                    .split(" ")
                    .map((x: string) => x[0])
                    .slice(0, 2)
                    .join("")}
                </span>
                <ArrowUpRight size={21} />
              </div>
              <span className="eyebrow">FAMILY CASE</span>
              <h2>{c.full_name}</h2>
              <p>
                {c.city} · Created {date(c.created_at)}
              </p>
              <div className="case-card-bottom">
                <span className="small-dot" />
                Your shared checklist is ready <ArrowRight size={15} />
              </div>
            </Link>
          ))}
        </div>
      ) : (
        <Empty
          title="A place to start, when you’re ready."
          body="Create a case for your family member. You can add information a little at a time."
          href="/app/new"
          action="Create your first case"
        />
      )}
      <div className="gentle-note">
        <Sprout size={25} />
        <div>
          <strong>There’s no need to do everything today.</strong>
          <p>
            Start with what you know. Invite someone you trust to help with the
            rest.
          </p>
        </div>
      </div>
    </>
  );
}
export function CaseDashboard({ caseId }: { caseId: string }) {
  const { t } = useSession();
  const [error, setError] = useState<unknown>();
  const q = useQuery({
    queryKey: ["case", caseId],
    queryFn: () => api(`/cases/${caseId}`),
  });
  const orgs = useQuery({
    queryKey: ["providers", caseId],
    queryFn: () => api<any[]>(`/cases/${caseId}/providers`),
  });
  const docs = useQuery({
    queryKey: ["documents", caseId],
    queryFn: () => api<any[]>(`/cases/${caseId}/documents`),
  });
  const timeline = useQuery({
    queryKey: ["timeline", caseId],
    queryFn: () => api<any[]>(`/cases/${caseId}/timeline`),
  });
  if (q.isLoading) return <Loading />;
  if (!q.data) return <ErrorBox error={q.error} />;
  const c = q.data;
  const summary = c.summary;
  const path = `/app/cases/${caseId}`;
  const action =
    orgs.data
      ?.filter(
        (p) =>
          ![
            "COMPLETED",
            "CANCELLED",
            "SUBMITTED",
            "UNDER_REVIEW",
            "PROVIDER_ACKNOWLEDGED",
            "RESUBMITTED",
            "PAUSED",
          ].includes(p.state),
      )
      .slice(0, 3) || [];
  return (
    <>
      <Heading
        eyebrow={`${c.full_name.toUpperCase()} · FAMILY CASE`}
        title={t.welcome}
        description="Here’s where things stand. We’ll help you with what comes next."
      >
        <Button asChild variant="secondary">
          <Link href={`${path}/family`}>
            <Users size={16} />
            Invite family
          </Link>
        </Button>
        <Button asChild>
          <Link href={`${path}/discovery`}>
            <Plus size={17} />
            Add an organization
          </Link>
        </Button>
      </Heading>
      <ErrorBox error={error || q.error || orgs.error} />
      <div className="case-context">
        <span className="case-monogram small">
          {c.full_name
            .split(" ")
            .map((x: string) => x[0])
            .slice(0, 2)
            .join("")}
        </span>
        <div>
          <strong>Remembering {c.full_name}</strong>
          <span>
            {c.city}, {c.state} <span className="dot-divider">·</span> Died{" "}
            {date(c.date_of_death)}
          </span>
        </div>
        <Link href={`${path}/profile`}>
          View details <ArrowUpRight size={15} />
        </Link>
      </div>
      <div className="stats-grid case-stats">
        {[
          [Building2, summary.total, "Organizations", "teal"],
          [Check, summary.completed, t.completed, "green"],
          [Clock3, summary.waiting, t.waiting, "blue"],
          [AlertCircle, summary.attention, t.attention, "gold"],
          [Clock3, summary.not_started, t.notStarted, "teal"],
        ].map(([Icon, value, text, color]: any) => (
          <div className="stat-card" key={text}>
            <div className="stat-top">
              <span>{text}</span>
              <Icon size={18} className={`text-${color}`} />
            </div>
            <strong>{value}</strong>
            <small>
              {text === "Organizations"
                ? "Together in your checklist"
                : text === t.completed
                  ? "One less thing to carry"
                  : text === t.waiting
                    ? "We’re keeping track"
                    : "A clear next step below"}
            </small>
          </div>
        ))}
      </div>
      <div className="dashboard-columns">
        <div className="dashboard-main">
          <section className="panel attention-panel">
            <div className="panel-heading">
              <div>
                <span className="eyebrow">LET’S TAKE THE NEXT STEP</span>
                <h2>Your next steps</h2>
              </div>
              <span className="count-badge">{action.length}</span>
            </div>
            {action.length ? (
              action.map((p) => (
                <Link
                  className="attention-row"
                  key={p.id}
                  href={`${path}/organizations/${p.id}`}
                >
                  <span
                    className={`provider-icon ${p.category === "Insurance" ? "gold" : "teal"}`}
                  >
                    {p.name.slice(0, 2).toUpperCase()}
                  </span>
                  <div>
                    <strong>{p.name}</strong>
                    <p>
                      {p.state === "DRAFT"
                        ? "Review the checklist and gather your documents"
                        : p.state === "ADDITIONAL_DOCUMENTS_REQUIRED"
                          ? "An additional document has been requested"
                          : p.state === "USER_APPROVAL_REQUIRED"
                            ? "Your request is ready for review and approval"
                            : "Open the checklist to see what’s needed"}
                    </p>
                    <span className="mini-tag">
                      {p.state === "DRAFT" ? "Ready to begin" : label(p.state)}
                    </span>
                  </div>
                  <ArrowRight size={18} />
                </Link>
              ))
            ) : (
              <Empty
                title="Nothing needs your attention right now."
                body="You can check back later or add another organization."
              />
            )}
            <Link className="panel-footer-link" href={`${path}/organizations`}>
              View all organizations <ArrowRight size={16} />
            </Link>
          </section>
          <section className="panel">
            <div className="panel-heading">
              <h2>Your organizations</h2>
              <Link className="text-link" href={`${path}/organizations`}>
                View all <ArrowUpRight size={14} />
              </Link>
            </div>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Organization</th>
                    <th>Status</th>
                    <th>Helping with this</th>
                  </tr>
                </thead>
                <tbody>
                  {orgs.data?.slice(0, 5).map((p) => (
                    <tr key={p.id}>
                      <td>
                        <Link
                          className="organization-name"
                          href={`${path}/organizations/${p.id}`}
                        >
                          <span className="provider-icon tiny">
                            {p.name.slice(0, 2).toUpperCase()}
                          </span>
                          <span>
                            <strong>{p.name}</strong>
                            <small>{p.category}</small>
                          </span>
                        </Link>
                      </td>
                      <td>
                        <Status state={p.state} />
                      </td>
                      <td>
                        <span className="assignee-dot">
                          {p.assigned_name?.slice(0, 1) || "–"}
                        </span>
                        {p.assigned_name?.split(" ")[0] || "Unassigned"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {!orgs.data?.length && (
              <Empty
                title="Your checklist starts here."
                body="Add a bank, insurer, utility, or another organization."
                href={`${path}/discovery`}
                action="Find organizations"
              />
            )}
          </section>
        </div>
        <aside className="dashboard-aside">
          <section className="vault-card">
            <div className="vault-icon">
              <FolderHeart size={25} />
            </div>
            <h2>
              One safe place for
              <br />
              your documents.
            </h2>
            <p>
              Upload once. Reuse when you need to.
              <br />
              You’re always in control of sharing.
            </p>
            <div className="vault-count">
              <strong>
                {docs.data?.filter((d) => d.status === "AVAILABLE").length || 0}
              </strong>{" "}
              documents in your vault
            </div>
            <Button asChild variant="secondary">
              <Link href={`${path}/documents`}>
                Open document vault <ArrowRight size={16} />
              </Link>
            </Button>
            <small>
              <Check size={12} />
              Encrypted storage · Access recorded
            </small>
          </section>
          <section className="panel activity-panel">
            <div className="panel-heading">
              <h2>Recent activity</h2>
              <HistoryIcon />
            </div>
            <div className="activity-list">
              {timeline.data?.slice(0, 4).map((e) => (
                <div className="activity-item" key={e.id}>
                  <span className="activity-dot" />
                  <div>
                    <strong>{label(e.action)}</strong>
                    <p>
                      {e.actor || "OneNotify"} · {date(e.created_at)}
                    </p>
                  </div>
                </div>
              ))}
            </div>
            <Link className="panel-footer-link" href={`${path}/timeline`}>
              Full case timeline <ArrowRight size={15} />
            </Link>
          </section>
        </aside>
      </div>
      {c.deadlines.length > 0 && (
        <section className="panel">
          <div className="panel-heading">
            <h2>Dates to keep in mind</h2>
            <span className="muted">System estimates, not legal deadlines</span>
          </div>
          {c.deadlines.map((d: any) => (
            <Link
              className="list-row"
              key={d.id}
              href={`${path}/organizations/${d.provider_case_id}`}
            >
              <Clock3 size={18} />
              <strong>{d.name}</strong>
              <span>{label(d.kind)}</span>
              <span className="push">{date(d.due_at)}</span>
            </Link>
          ))}
        </section>
      )}
      <div className="dashboard-bottom">
        <p>
          <Sprout size={18} />
          Progress can be quiet. Every small step counts.
        </p>
        <button
          className="text-link"
          onClick={async () => {
            try {
              download(
                await api(`/cases/${caseId}/export`),
                "onenotify-summary.pdf",
              );
            } catch (e) {
              setError(e);
            }
          }}
        >
          <Download size={15} />
          Download case summary
        </button>
      </div>
    </>
  );
}
function HistoryIcon() {
  return <Clock3 size={17} className="muted" />;
}
