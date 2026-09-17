"use client";
import { useState } from "react";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api, post, date, label } from "@/lib/api";
import { useSession } from "./providers";
import { Button } from "./ui/button";
import { Heading, Field, ErrorBox, Loading, Empty } from "./ui/shared";
import { Plus, Mail, ShieldCheck } from "lucide-react";
function useAction() {
  const client = useQueryClient();
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  const run = async (fn: () => Promise<unknown>) => {
    setBusy(true);
    setError(null);
    try {
      await fn();
      await client.invalidateQueries();
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  };
  return { run, error, busy };
}
export function Family({ caseId }: { caseId: string }) {
  const q = useQuery({
    queryKey: ["family", caseId],
    queryFn: () => api<any[]>(`/cases/${caseId}/members`),
  });
  const { run, error, busy } = useAction();
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("CONTRIBUTOR");
  const [revoke, setRevoke] = useState("");
  return (
    <>
      <Heading
        eyebrow="YOU DON’T HAVE TO DO THIS ALONE"
        title="Your family, together."
        description="Invite people you trust and share the work at a comfortable pace."
      />
      <ErrorBox error={error || q.error} />
      <section className="panel compact-panel">
        <h2>Invite someone to help</h2>
        <p>
          Existing users get access immediately. New users gain access when they
          register with the invited email. Invitations are recorded locally; no
          invitation email is sent.
        </p>
        <form
          className="inline-form"
          onSubmit={(e) => {
            e.preventDefault();
            run(async () => {
              await post(`/cases/${caseId}/members`, { email, role });
              setEmail("");
            });
          }}
        >
          <Field label="Email address">
            <input
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              type="email"
              required
            />
          </Field>
          <Field label="Their role">
            <select value={role} onChange={(e) => setRole(e.target.value)}>
              {[
                "CONTRIBUTOR",
                "VIEWER",
                "FAMILY_ADMIN",
                "PROFESSIONAL_ADVISOR",
              ].map((r) => (
                <option key={r} value={r}>
                  {label(r)}
                </option>
              ))}
            </select>
          </Field>
          <Button disabled={busy}>
            <Plus size={16} />
            Invite member
          </Button>
        </form>
      </section>
      <div className="family-grid">
        {q.data?.map((m) => (
          <div className="panel member-card" key={m.id}>
            <span className="avatar large">
              {m.name.slice(0, 1).toUpperCase()}
            </span>
            <h3>{m.name}</h3>
            <p>{m.email}</p>
            <span className="role-badge">{label(m.role)}</span>
            <small>{label(m.status)}</small>
            {m.role !== "CASE_OWNER" && m.status !== "REVOKED" && (
              <>
                {revoke === m.id ? (
                  <div>
                    <p>Remove this person’s case access?</p>
                    <Button
                      variant="destructive"
                      disabled={busy}
                      onClick={() =>
                        run(() =>
                          api(`/cases/${caseId}/members/${m.id}`, {
                            method: "DELETE",
                          }),
                        )
                      }
                    >
                      Revoke access
                    </Button>
                    <Button variant="ghost" onClick={() => setRevoke("")}>
                      Keep access
                    </Button>
                  </div>
                ) : (
                  <Button variant="ghost" onClick={() => setRevoke(m.id)}>
                    Manage access
                  </Button>
                )}
              </>
            )}
          </div>
        ))}
      </div>
      <div className="notice">
        <ShieldCheck size={18} />
        <div>
          Viewers can read. Contributors and advisors can add documents and
          notes. Case owners and family admins can manage access and approve
          submissions. System admins do not automatically get access to family
          documents.
        </div>
      </div>
    </>
  );
}
export function Tasks({ caseId }: { caseId: string }) {
  const q = useQuery({
    queryKey: ["tasks", caseId],
    queryFn: () => api<any[]>(`/cases/${caseId}/tasks`),
  });
  const family = useQuery({
    queryKey: ["family", caseId],
    queryFn: () => api<any[]>(`/cases/${caseId}/members`),
  });
  const { run, error, busy } = useAction();
  const [title, setTitle] = useState("");
  const [assigned, setAssigned] = useState("");
  const [due, setDue] = useState("");
  return (
    <>
      <Heading
        eyebrow="SHARE THE LITTLE THINGS"
        title="Family tasks."
        description="Make space for one clear next step. Share a task or keep a reminder for yourself."
      />
      <ErrorBox error={error || q.error} />
      <section className="panel compact-panel">
        <form
          className="task-form"
          onSubmit={(e) => {
            e.preventDefault();
            run(async () => {
              await post(`/cases/${caseId}/tasks`, {
                title,
                assignedTo: assigned || null,
                dueAt: due ? new Date(due + "T18:00:00").toISOString() : null,
              });
              setTitle("");
            });
          }}
        >
          <Field label="What needs doing?">
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="For example: look for the policy folder"
              required
              maxLength={200}
            />
          </Field>
          <Field label="Who is helping?">
            <select
              value={assigned}
              onChange={(e) => setAssigned(e.target.value)}
            >
              <option value="">Unassigned</option>
              {family.data
                ?.filter((m) => m.status === "ACTIVE" && m.role !== "VIEWER")
                .map((m) => (
                  <option key={m.id} value={m.user_id}>
                    {m.name}
                  </option>
                ))}
            </select>
          </Field>
          <Field label="Reminder date (optional)">
            <input
              type="date"
              value={due}
              onChange={(e) => setDue(e.target.value)}
            />
          </Field>
          <Button disabled={busy}>
            <Plus size={16} />
            Add task
          </Button>
        </form>
      </section>
      {q.isLoading ? (
        <Loading />
      ) : q.data?.length ? (
        <div className="panel">
          {q.data.map((task) => (
            <div
              className={`task-row ${task.status === "COMPLETED" ? "task-done" : ""}`}
              key={task.id}
            >
              <input
                type="checkbox"
                aria-label={`Complete ${task.title}`}
                checked={task.status === "COMPLETED"}
                disabled={busy}
                onChange={(e) =>
                  run(() =>
                    api(`/tasks/${task.id}`, {
                      method: "PUT",
                      body: JSON.stringify({ done: e.target.checked }),
                    }),
                  )
                }
              />
              <div>
                <strong>{task.title}</strong>
                <p>
                  {task.assigned_name || "Unassigned"}
                  {task.due_at && ` · Reminder ${date(task.due_at)}`}
                </p>
              </div>
              <span className="push status neutral">{label(task.status)}</span>
            </div>
          ))}
        </div>
      ) : (
        <Empty
          title="No tasks yet."
          body="Add one small thing to do, and choose who can help."
        />
      )}
    </>
  );
}
export function Correspondence({ caseId }: { caseId: string }) {
  const q = useQuery({
    queryKey: ["correspondence", caseId],
    queryFn: () => api<any[]>(`/cases/${caseId}/correspondence`),
  });
  const providers = useQuery({
    queryKey: ["providers", caseId],
    queryFn: () => api<any[]>(`/cases/${caseId}/providers`),
  });
  const docs = useQuery({
    queryKey: ["documents", caseId],
    queryFn: () => api<any[]>(`/cases/${caseId}/documents`),
  });
  const family = useQuery({
    queryKey: ["family", caseId],
    queryFn: () => api<any[]>(`/cases/${caseId}/members`),
  });
  const { run, error, busy } = useAction();
  const [subject, setSubject] = useState("");
  const [body, setBody] = useState("");
  const [kind, setKind] = useState("NOTE");
  const [provider, setProvider] = useState("");
  const [reference, setReference] = useState("");
  const [attachment, setAttachment] = useState("");
  const [mention, setMention] = useState("");
  return (
    <>
      <Heading
        eyebrow="NOTHING LOST BETWEEN CONVERSATIONS"
        title="Correspondence."
        description="Keep messages, letters, call notes, and references with your case."
      />
      <ErrorBox error={error || q.error} />
      <details className="panel compose-panel" open>
        <summary>
          <Plus size={18} />
          Add a note or record a message
        </summary>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            run(async () => {
              await post(`/cases/${caseId}/correspondence`, {
                subject,
                body,
                kind,
                providerCaseId: provider || null,
                reference,
                attachments: attachment ? [attachment] : [],
                mentions: mention ? [mention] : [],
              });
              setSubject("");
              setBody("");
              setReference("");
            });
          }}
        >
          <div className="form-grid">
            <Field label="Organization">
              <select
                value={provider}
                onChange={(e) => setProvider(e.target.value)}
              >
                <option value="">General case note</option>
                {providers.data?.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Type">
              <select value={kind} onChange={(e) => setKind(e.target.value)}>
                {["NOTE", "CALL_NOTE", "EMAIL", "LETTER"].map((x) => (
                  <option key={x} value={x}>
                    {label(x)}
                  </option>
                ))}
              </select>
            </Field>
          </div>
          <Field label="Subject">
            <input
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              required
              maxLength={200}
            />
          </Field>
          <Field label="What would you like to record?">
            <textarea
              rows={4}
              value={body}
              onChange={(e) => setBody(e.target.value)}
              required
              maxLength={10000}
            />
          </Field>
          <div className="form-grid">
            <Field label="Support reference (optional)">
              <input
                value={reference}
                onChange={(e) => setReference(e.target.value)}
                maxLength={120}
              />
            </Field>
            <Field label="Attach a vault document">
              <select
                value={attachment}
                onChange={(e) => setAttachment(e.target.value)}
              >
                <option value="">No attachment</option>
                {docs.data
                  ?.filter((d) => d.status === "AVAILABLE")
                  .map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.filename}
                    </option>
                  ))}
              </select>
            </Field>
            <Field label="Mention a family member">
              <select
                value={mention}
                onChange={(e) => setMention(e.target.value)}
              >
                <option value="">No mention</option>
                {family.data
                  ?.filter((m) => m.status === "ACTIVE")
                  .map((m) => (
                    <option key={m.id} value={m.user_id}>
                      {m.name}
                    </option>
                  ))}
              </select>
            </Field>
          </div>
          <Button disabled={busy}>Save to case</Button>
          <p className="fine-print">
            This records correspondence. It does not send an email or letter.
          </p>
        </form>
      </details>
      {q.data?.map((m) => (
        <article className="panel correspondence-card" key={m.id}>
          <div className="message-meta">
            <span className="role-badge">{label(m.kind)}</span>
            <span>{m.provider_name || "General case"}</span>
            <time>{date(m.created_at)}</time>
          </div>
          <h3>{m.subject}</h3>
          <p className="preserve-lines">{m.body}</p>
          <small>
            {m.author || "Simulated provider"}
            {m.reference && ` · Reference: ${m.reference}`}
          </small>
        </article>
      ))}
    </>
  );
}
export function Timeline({ caseId }: { caseId: string }) {
  const q = useQuery({
    queryKey: ["timeline", caseId],
    queryFn: () => api<any[]>(`/cases/${caseId}/timeline`),
  });
  return (
    <>
      <Heading
        eyebrow="A SHARED RECORD OF EVERY STEP"
        title="Your case timeline."
        description="Important actions are recorded in an append-only history."
      />
      <ErrorBox error={q.error} />
      <section className="panel">
        <div className="workflow-timeline">
          {q.isLoading ? (
            <Loading />
          ) : (
            q.data?.map((e) => (
              <div className="timeline-entry" key={e.id}>
                <span className="timeline-node" />
                <div>
                  <strong>{label(e.action)}</strong>
                  <p>
                    {e.actor || "OneNotify system"} · {date(e.created_at)}
                  </p>
                  <small>Reference {e.id}</small>
                  {e.provider_case_id && (
                    <Link
                      className="text-link"
                      href={`/app/cases/${caseId}/organizations/${e.provider_case_id}`}
                    >
                      Open organization request
                    </Link>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      </section>
    </>
  );
}
export function Notifications() {
  const q = useQuery({
    queryKey: ["notifications"],
    queryFn: () => api<any[]>("/notifications"),
  });
  const { run, error } = useAction();
  return (
    <>
      <Heading
        eyebrow="STAY IN THE LOOP"
        title="Your updates."
        description="A quieter place to see what needs your attention."
      />
      <ErrorBox error={error || q.error} />
      <div className="panel">
        {q.data?.length ? (
          q.data.map((n) => (
            <div
              className={`notification-row ${n.read_at ? "" : "unread"}`}
              key={n.id}
            >
              <span className="notification-symbol">
                <Mail size={18} />
              </span>
              <div>
                <strong>{n.title}</strong>
                <p>{n.body}</p>
                <small>{date(n.created_at)}</small>
                {n.case_id && (
                  <Link className="text-link" href={`/app/cases/${n.case_id}`}>
                    Open case
                  </Link>
                )}
              </div>
              {!n.read_at && (
                <Button
                  variant="ghost"
                  onClick={() =>
                    run(() =>
                      api(`/notifications/${n.id}/read`, { method: "PUT" }),
                    )
                  }
                >
                  Mark read
                </Button>
              )}
            </div>
          ))
        ) : (
          <Empty
            title="You’re up to date."
            body="New case activity and reminders will appear here."
          />
        )}
      </div>
    </>
  );
}
export function CaseSettings({ caseId }: { caseId: string }) {
  const { run, error, busy } = useAction();
  const [confirm, setConfirm] = useState("");
  const [requested, setRequested] = useState(false);
  return (
    <>
      <Heading
        eyebrow="YOUR CASE, YOUR CONTROL"
        title="Case settings."
        description="Manage your family’s information and privacy."
      />
      <ErrorBox error={error} />
      <section className="panel compact-panel">
        <h2>Family member details</h2>
        <p>Update names, dates, and optional account hints.</p>
        <Button asChild variant="secondary">
          <Link href={`/app/cases/${caseId}/profile`}>Edit profile</Link>
        </Button>
      </section>
      <section className="panel compact-panel">
        <h2>Request case deletion</h2>
        <p>
          This creates a retention review request. It does not immediately erase
          documents or the audit trail. An operator must review obligations and
          confirm the erasure plan.
        </p>
        <Field label="Type DELETE to request a review">
          <input
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            autoComplete="off"
          />
        </Field>
        <Button
          variant="destructive"
          disabled={confirm !== "DELETE" || busy || requested}
          onClick={() =>
            run(async () => {
              await post(`/cases/${caseId}/deletion-request`);
              setRequested(true);
            })
          }
        >
          {requested ? "Deletion review requested" : "Request deletion review"}
        </Button>
      </section>
    </>
  );
}
export function UserSettings() {
  const { user, locale, setLocale } = useSession();
  return (
    <>
      <Heading eyebrow="YOUR ACCOUNT" title="Make yourself at home." />
      <section className="panel compact-panel">
        <h2>{user?.name}</h2>
        <p>{user?.email}</p>
        <Field label="Navigation language">
          <select
            value={locale}
            onChange={(e) => setLocale(e.target.value as "en" | "hi")}
          >
            <option value="en">English</option>
            <option value="hi">हिंदी</option>
          </select>
        </Field>
        <p className="notice">
          Hindi navigation is available. Detailed workflow content is currently
          in English; additional translations can be added through the message
          catalog.
        </p>
        <Button asChild variant="secondary">
          <Link href="/forgot-password">Change your password</Link>
        </Button>
      </section>
    </>
  );
}
