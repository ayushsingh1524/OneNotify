"use client";
import { useState } from "react";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api, post, label, date } from "@/lib/api";
import { Button } from "./ui/button";
import { Heading, ErrorBox, Loading, Empty, Status } from "./ui/shared";
import {
  Plus,
  Search,
  ArrowRight,
  Mail,
  Check,
  Mic,
  Compass,
} from "lucide-react";
import { BrowserVoiceAssistant, parseVoiceIntent } from "@/lib/voice";
import { useSession } from "./providers";
export function Organizations({ caseId }: { caseId: string }) {
  const [filter, setFilter] = useState("");
  const q = useQuery({
    queryKey: ["providers", caseId],
    queryFn: () => api<any[]>(`/cases/${caseId}/providers`),
  });
  return (
    <>
      <Heading
        eyebrow="ONE SHARED CHECKLIST"
        title="Your organizations."
        description="Know what’s moving, what’s waiting, and where you can help."
      >
        <Button asChild>
          <Link href={`/app/cases/${caseId}/discovery`}>
            <Plus size={17} />
            Add an organization
          </Link>
        </Button>
      </Heading>
      <div className="filter-bar">
        <Search size={18} />
        <input
          aria-label="Filter organizations"
          placeholder="Find an organization…"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        />
      </div>
      <ErrorBox error={q.error} />
      {q.isLoading ? (
        <Loading />
      ) : !q.data?.length ? (
        <Empty
          title="Start with an organization you know."
          body="You don’t need account numbers to begin a checklist."
          href={`/app/cases/${caseId}/discovery`}
          action="Find organizations"
        />
      ) : (
        <div className="organization-grid">
          {q.data
            ?.filter((p) => p.name.toLowerCase().includes(filter.toLowerCase()))
            .map((p) => (
              <Link
                className="panel organization-card"
                key={p.id}
                href={`/app/cases/${caseId}/organizations/${p.id}`}
              >
                <div className="organization-card-top">
                  <span className="provider-icon">
                    {p.name.slice(0, 2).toUpperCase()}
                  </span>
                  <span className="demo-pill">
                    {p.adapter_type === "SIMULATED" ? "SIMULATED" : "MANUAL"}
                  </span>
                </div>
                <h2>{p.name}</h2>
                <p>
                  {p.category} · {label(p.action)}
                </p>
                <Status state={p.state} />
                <div className="organization-card-bottom">
                  <span>
                    {p.assigned_name || "Not assigned"}
                    {p.due_at && <small>Estimate: {date(p.due_at)}</small>}
                  </span>
                  <ArrowRight size={17} />
                </div>
              </Link>
            ))}
        </div>
      )}
    </>
  );
}
export function Discovery({ caseId }: { caseId: string }) {
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("All");
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState("");
  const [guide, setGuide] = useState("");
  const [voice, setVoice] = useState("");
  const { locale } = useSession();
  const client = useQueryClient();
  const registry = useQuery({
    queryKey: ["registry"],
    queryFn: () => api<any[]>("/providers"),
  });
  const known = useQuery({
    queryKey: ["providers", caseId],
    queryFn: () => api<any[]>(`/cases/${caseId}/providers`),
  });
  const discoveries = useQuery({
    queryKey: ["discovery", caseId],
    queryFn: () => api<any[]>(`/cases/${caseId}/discovery`),
  });
  const act = async (key: string, fn: () => Promise<unknown>) => {
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
  return (
    <>
      <Heading
        eyebrow="START WITH WHAT YOU KNOW"
        title="Let’s find the right organizations."
        description="Add known services, or use a little guidance to find possible accounts."
      />
      <section className="guided-card">
        <span className="guide-icon">
          <Compass size={27} />
        </span>
        <div>
          <h2>Do you know where they had a bank account?</h2>
          <p>There’s no wrong answer. We can help you work it out.</p>
          <div className="button-row">
            {["Yes", "No", "Not sure"].map((x) => (
              <Button
                key={x}
                variant={guide === x ? "default" : "secondary"}
                onClick={() => {
                  setGuide(x);
                  if (x === "Yes") setFilter("Banking");
                }}
              >
                {x}
              </Button>
            ))}
          </div>
          {guide && (
            <p className="guide-answer">
              {guide === "Yes"
                ? "Choose their bank below. You can start without an account number."
                : "Look for passbooks, statements, policy folders, or recurring bills. Ask family members what they remember. The sample email tool below shows how assisted discovery can help."}
            </p>
          )}
        </div>
      </section>
      <ErrorBox error={error || registry.error} />
      <div className="discovery-tools">
        <div className="panel compact-panel">
          <Mail size={23} />
          <h3>Explore sample email discovery</h3>
          <p>Try simulated messages. No email account is connected.</p>
          <Button
            variant="secondary"
            disabled={!!busy}
            onClick={() =>
              act("sample", () =>
                post(`/cases/${caseId}/discovery/sample-email`),
              )
            }
          >
            {busy === "sample" ? "Checking…" : "Import sample messages"}
          </Button>
        </div>
        <div className="panel compact-panel">
          <Mic size={23} />
          <h3>Tell us what you remember</h3>
          <p>
            Optional browser voice input. Your browser may use its speech
            service.
          </p>
          <Button
            variant="secondary"
            onClick={async () => {
              setError(null);
              try {
                const text = await new BrowserVoiceAssistant().listen(
                  locale === "hi" ? "hi-IN" : "en-IN",
                );
                setVoice(text);
                const intent = parseVoiceIntent(text);
                if (intent.provider) setSearch(intent.provider);
              } catch (e) {
                setError(e);
              }
            }}
          >
            Start voice input
          </Button>
          {voice && (
            <p>
              {voice} · {parseVoiceIntent(voice).guidance}
            </p>
          )}
        </div>
      </div>
      {discoveries.data?.some((d) => d.status === "POSSIBLE") && (
        <section className="panel">
          <div className="panel-heading">
            <h2>Possible organizations</h2>
            <span className="demo-pill">OWNERSHIP UNCONFIRMED</span>
          </div>
          {discoveries.data
            .filter((d) => d.status === "POSSIBLE")
            .map((d) => (
              <div className="list-row" key={d.id}>
                <span className="provider-icon">{d.name.slice(0, 2)}</span>
                <div>
                  <strong>{d.name}</strong>
                  <p>{d.hint}</p>
                  <small>{label(d.source)}</small>
                </div>
                <div className="row-actions">
                  <Button
                    size="sm"
                    disabled={!!busy}
                    onClick={() =>
                      act(d.id, () =>
                        api(`/cases/${caseId}/discovery/${d.id}`, {
                          method: "PUT",
                          body: JSON.stringify({ status: "CONFIRMED" }),
                        }),
                      )
                    }
                  >
                    Confirm & add
                  </Button>
                  <Button
                    variant="ghost"
                    disabled={!!busy}
                    onClick={() =>
                      act(d.id, () =>
                        api(`/cases/${caseId}/discovery/${d.id}`, {
                          method: "PUT",
                          body: JSON.stringify({ status: "IGNORED" }),
                        }),
                      )
                    }
                  >
                    Ignore
                  </Button>
                </div>
              </div>
            ))}
        </section>
      )}
      <div className="section-heading">
        <h2>Browse organizations</h2>
        <span className="muted">All requirements below are demo examples</span>
      </div>
      <div className="filter-bar">
        <Search size={18} />
        <input
          aria-label="Search registry"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Bank, insurer, mobile provider…"
        />
        <select
          aria-label="Filter category"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        >
          {[
            "All",
            ...Array.from(new Set(registry.data?.map((p) => p.category) || [])),
          ].map((c) => (
            <option key={c}>{c}</option>
          ))}
        </select>
      </div>
      <div className="registry-grid">
        {registry.data
          ?.filter(
            (p) =>
              p.name.toLowerCase().includes(search.toLowerCase()) &&
              (filter === "All" || p.category === filter),
          )
          .map((p) => {
            const added = known.data?.some((k) => k.provider_id === p.id);
            return (
              <div className="panel registry-card" key={p.id}>
                <span className="provider-icon">
                  {p.name.slice(0, 2).toUpperCase()}
                </span>
                <h3>{p.name}</h3>
                <p>{p.category}</p>
                <span className="demo-pill">DEMO · {p.adapter_type}</span>
                <Button
                  variant="secondary"
                  disabled={added || !!busy}
                  onClick={() =>
                    act(p.id, () =>
                      post(`/cases/${caseId}/providers`, {
                        providerId: p.id,
                        action: "NOTIFY_DEATH",
                      }),
                    )
                  }
                >
                  {added ? (
                    <>
                      <Check size={16} />
                      Added
                    </>
                  ) : (
                    <>
                      <Plus size={16} />
                      Add to checklist
                    </>
                  )}
                </Button>
              </div>
            );
          })}
      </div>
    </>
  );
}
