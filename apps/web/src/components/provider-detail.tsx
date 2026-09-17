"use client";
import { ManualResponse } from "./manual-response";
import { useState } from "react";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api, post, label, date, download } from "@/lib/api";
import { useSession } from "./providers";
import { Button } from "./ui/button";
import { Heading, Field, ErrorBox, Loading, Status } from "./ui/shared";
import {
  ArrowLeft,
  ArrowRight,
  Check,
  FilePlus2,
  Download,
  ShieldCheck,
  Play,
  Clock3,
} from "lucide-react";
export function ProviderDetail({ caseId, id }: { caseId: string; id: string }) {
  const { t } = useSession();
  const client = useQueryClient();
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState("");
  const [approved, setApproved] = useState(false);
  const [selected, setSelected] = useState<string[]>([]);
  const [draft, setDraft] = useState("");
  const [draftType, setDraftType] = useState("NOTIFY_DEATH");
  const [source, setSource] = useState("");
  const [showCancel, setShowCancel] = useState(false);
  const [submission, setSubmission] = useState<{
    consentId: string;
    key: string;
  } | null>(null);
  const q = useQuery({
    queryKey: ["provider", id],
    queryFn: () => api(`/provider-cases/${id}`),
  });
  const docs = useQuery({
    queryKey: ["documents", caseId],
    queryFn: () => api<any[]>(`/cases/${caseId}/documents`),
  });
  const family = useQuery({
    queryKey: ["family", caseId],
    queryFn: () => api<any[]>(`/cases/${caseId}/members`),
  });
  const caseQ = useQuery({
    queryKey: ["case", caseId],
    queryFn: () => api(`/cases/${caseId}`),
  });
  const run = async (key: string, fn: () => Promise<unknown>) => {
    setBusy(key);
    setError(null);
    try {
      await fn();
      await client.invalidateQueries();
    } catch (e) {
      setError(e);
    } finally {
      setBusy("");
    }
  };
  if (q.isLoading) return <Loading />;
  if (!q.data) return <ErrorBox error={q.error} />;
  const p = q.data;
  const canApprove = ["CASE_OWNER", "FAMILY_ADMIN"].includes(caseQ.data?.role);
  const available = docs.data?.filter((d) => d.status === "AVAILABLE") || [];
  const missing = p.requirements.filter((r: any) => !r.available);
  const canPrepare = [
    "DRAFT",
    "DOCUMENTS_PENDING",
    "READY_TO_SUBMIT",
    "ADDITIONAL_DOCUMENTS_REQUIRED",
    "FAILED",
    "ESCALATION_REQUIRED",
  ].includes(p.state);
  const canSimulate = [
    "SUBMITTED",
    "PROVIDER_ACKNOWLEDGED",
    "UNDER_REVIEW",
    "RESUBMITTED",
    "APPROVED",
  ].includes(p.state);
  const next =
    p.state === "COMPLETED"
      ? "This organization request is complete. Your timeline and documents are kept together."
      : missing.length
        ? "Add the missing documents below, then check your request."
        : p.state === "USER_APPROVAL_REQUIRED"
          ? "Review the information and documents below. A case owner or family admin can approve the request."
          : canSimulate
            ? "You’re waiting for a provider response. In this demo, you can simulate the next response below."
            : p.state === "EXTERNAL_ACTION_REQUIRED"
              ? "Download the request package and follow the provider’s official process. OneNotify has not sent it."
              : "Review your checklist, then prepare your request.";
  return (
    <>
      <Link className="back-link" href={`/app/cases/${caseId}/organizations`}>
        <ArrowLeft size={16} />
        All organizations
      </Link>
      <Heading
        eyebrow={`${p.category.toUpperCase()} · ${p.adapter_type === "SIMULATED" ? "SIMULATED INTEGRATION" : "MANUAL PROCESS"}`}
        title={p.name}
        description={label(p.action)}
      >
        <Status state={p.state} />
        <Button
          variant="secondary"
          onClick={() =>
            run("package", async () =>
              download(
                await api(`/provider-cases/${id}/package`),
                "provider-package.zip",
              ),
            )
          }
        >
          <Download size={16} />
          Request package
        </Button>
      </Heading>
      <ErrorBox error={error || q.error} />
      <section className="next-step">
        <span className="next-step-icon">
          <ArrowRight size={24} />
        </span>
        <div>
          <span className="eyebrow">{t.next}</span>
          <h2>{next}</h2>
          {canPrepare && (
            <Button
              disabled={!!busy}
              onClick={() =>
                run("prepare", () => post(`/provider-cases/${id}/prepare`))
              }
            >
              {busy === "prepare" ? "Checking…" : "Check & prepare request"}
              <ArrowRight size={16} />
            </Button>
          )}
          {p.state === "PAUSED" && (
            <Button
              onClick={() =>
                run("resume", () =>
                  post(`/provider-cases/${id}/action`, { action: "resume" }),
                )
              }
            >
              Resume checklist
            </Button>
          )}
        </div>
      </section>
      <div className="detail-columns">
        <div>
          <section className="panel">
            <div className="panel-heading">
              <h2>Document checklist</h2>
              <span className="demo-pill">DEMO REQUIREMENTS</span>
            </div>
            {p.requirements.map((r: any) => (
              <div className="requirement-row" key={r.category}>
                <span
                  className={`requirement-icon ${r.available ? "available" : ""}`}
                >
                  {r.available ? <Check size={17} /> : <FilePlus2 size={17} />}
                </span>
                <div>
                  <strong>{label(r.category)}</strong>
                  <p>
                    {r.available
                      ? "Available in your vault"
                      : "Needed before this request can move forward"}
                  </p>
                </div>
                {!r.available && (
                  <Link
                    className="text-link"
                    href={`/app/cases/${caseId}/documents`}
                  >
                    Add document <ArrowRight size={15} />
                  </Link>
                )}
              </div>
            ))}
            <div className="panel-note">
              These are example requirements. Confirm the actual process with
              the organization.
            </div>
          </section>
          {p.state === "USER_APPROVAL_REQUIRED" && (
            <section className="panel consent-panel">
              <div className="panel-heading">
                <h2>Review & approve sharing</h2>
                <ShieldCheck size={21} />
              </div>
              <div className="panel-content">
                <p>
                  <strong>Recipient:</strong> {p.name}{" "}
                  {p.demo ? "(DEMO — no real transmission)" : ""}
                </p>
                <p>
                  <strong>Purpose:</strong> {label(p.action)} following a death
                </p>
                <p>
                  <strong>Information:</strong> Deceased full name, date of
                  death, requester name, requested action.
                </p>
                <p>
                  <strong>Approval expires:</strong> 30 minutes. Approval
                  applies only to the documents selected here.
                </p>
                <div className="consent-docs">
                  {available.map((d) => (
                    <label key={d.id} className="checkbox-row">
                      <input
                        type="checkbox"
                        checked={selected.includes(d.id)}
                        disabled={!!submission}
                        onChange={(e) =>
                          setSelected(
                            e.target.checked
                              ? [...selected, d.id]
                              : selected.filter((x) => x !== d.id),
                          )
                        }
                      />
                      <span>
                        {d.filename}
                        <small>
                          {label(d.category)} · version {d.version}
                        </small>
                      </span>
                    </label>
                  ))}
                </div>
                <label className="checkbox-row approval">
                  <input
                    type="checkbox"
                    checked={approved}
                    disabled={!!submission}
                    onChange={(e) => setApproved(e.target.checked)}
                  />
                  <span>
                    I have reviewed these details and authorize this specific
                    request and selected documents.
                  </span>
                </label>
                {!canApprove && (
                  <p className="notice">
                    Ask a case owner or family administrator to approve this
                    request.
                  </p>
                )}
                <Button
                  disabled={
                    !approved || !selected.length || !!busy || !canApprove
                  }
                  onClick={() =>
                    run("submit", async () => {
                      let pending = submission;
                      if (!pending) {
                        const consent = await post("/consents", {
                          providerCaseId: id,
                          documentIds: selected,
                          approved: true,
                        });
                        pending = {
                          consentId: consent.id,
                          key: crypto.randomUUID(),
                        };
                        setSubmission(pending);
                      }
                      await api(`/provider-cases/${id}/submit`, {
                        method: "POST",
                        headers: { "Idempotency-Key": pending.key },
                        body: JSON.stringify({ consentId: pending.consentId }),
                      });
                      setApproved(false);
                      setSelected([]);
                      setSubmission(null);
                    })
                  }
                >
                  {busy === "submit"
                    ? "Submitting…"
                    : submission
                      ? "Retry the same approved request"
                      : p.adapter_type === "SIMULATED"
                        ? "Approve & simulate submission"
                        : "Approve & prepare for manual submission"}
                </Button>
                {submission && (
                  <Button
                    variant="ghost"
                    onClick={() =>
                      run("revoke", async () => {
                        await api(`/consents/${submission.consentId}`, {
                          method: "DELETE",
                        });
                        setSubmission(null);
                        setApproved(false);
                      })
                    }
                  >
                    Withdraw pending approval
                  </Button>
                )}
              </div>
            </section>
          )}
          <section className="panel">
            <div className="panel-heading">
              <h2>Request timeline</h2>
            </div>
            <div className="workflow-timeline">
              {p.timeline.length ? (
                p.timeline.map((e: any) => (
                  <div className="timeline-entry" key={e.id}>
                    <span className="timeline-node" />
                    <div>
                      <strong>{label(e.to_state)}</strong>
                      <p>{e.reason}</p>
                      <small>
                        {e.actor || "Simulated provider / system"} ·{" "}
                        {date(e.created_at)}
                      </small>
                    </div>
                  </div>
                ))
              ) : (
                <p className="muted">
                  Your timeline will begin when you prepare this request.
                </p>
              )}
            </div>
          </section>
          <section className="panel">
            <div className="panel-heading">
              <h2>Provider correspondence</h2>
              <Link
                className="text-link"
                href={`/app/cases/${caseId}/correspondence`}
              >
                Add a note
              </Link>
            </div>
            <div className="panel-content">
              {p.correspondence.length ? (
                p.correspondence.map((m: any) => (
                  <article className="message" key={m.id}>
                    <strong>{m.subject}</strong>
                    <p>{m.body}</p>
                    <small>
                      {date(m.created_at)} · {label(m.kind)}
                    </small>
                  </article>
                ))
              ) : (
                <p className="muted">
                  Responses, call notes, and letters will appear here.
                </p>
              )}
            </div>
          </section>
        </div>
        <aside>
          {p.adapter_type !== "SIMULATED" && canApprove && (
            <ManualResponse id={id} state={p.state} />
          )}
          <section className="panel compact-panel">
            <h3>People & timing</h3>
            <Field label="Family member helping">
              <select
                value={p.assigned_to || ""}
                onChange={(e) =>
                  run("assign", () =>
                    api(`/provider-cases/${id}/assignee`, {
                      method: "PUT",
                      body: JSON.stringify({ userId: e.target.value || null }),
                    }),
                  )
                }
              >
                <option value="">Not assigned</option>
                {family.data
                  ?.filter((m) => m.status === "ACTIVE" && m.role !== "VIEWER")
                  .map((m) => (
                    <option key={m.id} value={m.user_id}>
                      {m.name}
                    </option>
                  ))}
              </select>
            </Field>
            {p.deadlines.map((d: any) => (
              <p key={d.id}>
                <Clock3 size={14} /> {label(d.kind)}: {date(d.due_at)}
                <small className="block">
                  {label(d.source)} · not a statutory deadline
                </small>
              </p>
            ))}
            <p className="muted">
              Example processing estimate: {p.estimated_days} days.
            </p>
            <a
              className="text-link"
              href={p.website}
              target="_blank"
              rel="noreferrer"
            >
              Provider website ↗
            </a>
          </section>
          {p.adapter_type === "SIMULATED" && (
            <section className="simulation-card">
              <span className="demo-pill">DEMO CONTROLS</span>
              <h3>See how a response works.</h3>
              <p>
                Simulate acknowledgement, review, an extra document request, and
                completion. This does not contact {p.name}.
              </p>
              <Button
                variant="secondary"
                disabled={!canSimulate || !!busy || !canApprove}
                onClick={() =>
                  run("simulate", () =>
                    post(`/provider-cases/${id}/simulate-response`),
                  )
                }
              >
                <Play size={15} />
                Simulate next response
              </Button>
            </section>
          )}
          <section className="panel compact-panel">
            <h3>A little help with the wording</h3>
            <p>Deterministic draft assistance. Always review before use.</p>
            <Field label="Draft type">
              <select
                value={draftType}
                onChange={(e) => setDraftType(e.target.value)}
              >
                {[
                  "NOTIFY_DEATH",
                  "CLOSE_ACCOUNT",
                  "TRANSFER_ACCOUNT",
                  "MISSING_DOCUMENT_RESPONSE",
                  "FOLLOW_UP",
                  "ESCALATION",
                ].map((s) => (
                  <option key={s} value={s}>
                    {label(s)}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Provider instructions (optional)">
              <textarea
                rows={3}
                value={source}
                onChange={(e) => setSource(e.target.value)}
                maxLength={10000}
              />
            </Field>
            <Button
              variant="secondary"
              disabled={!!busy}
              onClick={() =>
                run("draft", async () => {
                  const r = await post(`/provider-cases/${id}/assist`, {
                    type: draftType,
                    text: source,
                  });
                  setDraft(r.draft + "\n\n" + r.explanation);
                })
              }
            >
              Prepare a draft
            </Button>
            {draft && (
              <Field label="Review and edit draft">
                <textarea
                  rows={12}
                  value={draft}
                  onChange={(e) => setDraft(e.target.value)}
                />
              </Field>
            )}
          </section>
          {!["COMPLETED", "CANCELLED", "APPROVED"].includes(p.state) && (
            <section className="panel compact-panel">
              <h3>Need a little time?</h3>
              <div className="button-row">
                <Button
                  variant="ghost"
                  disabled={!!busy}
                  onClick={() =>
                    run("pause", () =>
                      post(`/provider-cases/${id}/action`, { action: "pause" }),
                    )
                  }
                >
                  Pause request
                </Button>
                <Button
                  variant="ghost"
                  onClick={() => setShowCancel(!showCancel)}
                >
                  Cancel request
                </Button>
              </div>
              {showCancel && (
                <>
                  <p>
                    Cancel this checklist? Its history will remain available.
                  </p>
                  <Button
                    variant="destructive"
                    onClick={() =>
                      run("cancel", () =>
                        post(`/provider-cases/${id}/action`, {
                          action: "cancel",
                        }),
                      )
                    }
                  >
                    Confirm cancellation
                  </Button>
                </>
              )}
            </section>
          )}
        </aside>
      </div>
    </>
  );
}
