"use client";
import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api, post, categories, label } from "@/lib/api";
import { useSession } from "./providers";
import { Button } from "./ui/button";
import { Heading, Field, ErrorBox } from "./ui/shared";
export function Admin() {
  const { user } = useSession();
  const client = useQueryClient();
  const q = useQuery({
    queryKey: ["admin"],
    queryFn: () => api("/admin"),
    enabled: user?.system_role === "ADMIN",
  });
  const metrics = useQuery({
    queryKey: ["metrics"],
    queryFn: () => api("/admin/metrics"),
    enabled: user?.system_role === "ADMIN",
  });
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  const [editing, setEditing] = useState("");
  const [form, setForm] = useState({
    name: "",
    category: "Banking",
    website: "https://example.com",
    adapterType: "SIMULATED",
    estimatedDays: 10,
    notes: "Example requirements only",
    requirements: ["DEATH_CERTIFICATE", "USER_IDENTITY"],
  });
  const [configKey, setConfigKey] = useState("translations.hi");
  const [config, setConfig] = useState("{}");
  if (user?.system_role !== "ADMIN")
    return <p>Administrator access is required.</p>;
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
  return (
    <>
      <Heading
        eyebrow="DEVELOPMENT ADMINISTRATION"
        title="Provider registry & system."
        description="Manage demo configuration. This role does not grant access to other families’ documents."
      />
      <ErrorBox error={error || q.error || metrics.error} />
      <div className="stats-grid">
        {Object.entries(q.data?.events || {}).map(([key, value]) => (
          <div className="stat-card" key={key}>
            <span>Events {key}</span>
            <strong>{String(value)}</strong>
          </div>
        ))}
      </div>
      <section className="panel compact-panel">
        <h2>{editing ? "Edit demo provider" : "Add demo provider"}</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            run(async () => {
              await api(`/admin/providers${editing ? "/" + editing : ""}`, {
                method: editing ? "PUT" : "POST",
                body: JSON.stringify(form),
              });
              setEditing("");
            });
          }}
        >
          <div className="form-grid">
            {(["name", "category", "website"] as const).map((key) => (
              <Field key={key} label={label(key)}>
                <input
                  required
                  value={form[key]}
                  onChange={(e) => setForm({ ...form, [key]: e.target.value })}
                />
              </Field>
            ))}
            <Field label="Adapter mode">
              <select
                value={form.adapterType}
                onChange={(e) =>
                  setForm({ ...form, adapterType: e.target.value })
                }
              >
                {[
                  "SIMULATED",
                  "MANUAL",
                  "EMAIL",
                  "FORM",
                  "EXTERNAL_PORTAL",
                  "API",
                ].map((x) => (
                  <option key={x}>{x}</option>
                ))}
              </select>
            </Field>
            <Field label="Estimated response days">
              <input
                required
                min={1}
                max={365}
                type="number"
                value={form.estimatedDays}
                onChange={(e) =>
                  setForm({ ...form, estimatedDays: Number(e.target.value) })
                }
              />
            </Field>
            <Field label="Notes">
              <input
                value={form.notes}
                onChange={(e) => setForm({ ...form, notes: e.target.value })}
              />
            </Field>
          </div>
          <fieldset>
            <legend>Document requirements for new requests</legend>
            <div className="checkbox-grid">
              {categories.map((c) => (
                <label className="checkbox-row" key={c}>
                  <input
                    type="checkbox"
                    checked={form.requirements.includes(c)}
                    onChange={(e) =>
                      setForm({
                        ...form,
                        requirements: e.target.checked
                          ? [...form.requirements, c]
                          : form.requirements.filter((x) => x !== c),
                      })
                    }
                  />
                  {label(c)}
                </label>
              ))}
            </div>
          </fieldset>
          <Button disabled={busy}>Save demo provider</Button>
        </form>
      </section>
      <section className="panel">
        <div className="panel-heading">
          <h2>Registry</h2>
        </div>
        {q.data?.providers.map((p: any) => (
          <div className="list-row" key={p.id}>
            <strong>{p.name}</strong>
            <span>
              {p.category} · {p.adapter_type}
            </span>
            <Button
              variant="ghost"
              onClick={() => {
                setEditing(p.id);
                setForm({
                  name: p.name,
                  category: p.category,
                  website: p.website,
                  adapterType: p.adapter_type,
                  estimatedDays: p.estimated_days,
                  notes: p.notes,
                  requirements: p.requirements?.split(",") || [],
                });
                window.scrollTo({ top: 0, behavior: "smooth" });
              }}
            >
              Edit
            </Button>
          </div>
        ))}
      </section>
      <section className="panel compact-panel">
        <h2>Operational metrics</h2>
        <div className="metric-grid">
          {Object.entries(metrics.data || {}).map(([k, v]) => (
            <div key={k}>
              <span>{label(k)}</span>
              <strong>{v === null ? "Not yet measured" : String(v)}</strong>
            </div>
          ))}
        </div>
      </section>
      <section className="panel compact-panel">
        <h2>Failed event delivery</h2>
        {q.data?.failedJobs.length ? (
          q.data.failedJobs.map((job: any) => (
            <div className="list-row" key={job.id}>
              <span>
                {job.topic} · {job.attempts} attempts
              </span>
              <Button
                disabled={busy}
                onClick={() => run(() => post(`/admin/events/${job.id}/retry`))}
              >
                Retry
              </Button>
            </div>
          ))
        ) : (
          <p>No exhausted outbox jobs.</p>
        )}
      </section>
      <section className="panel compact-panel">
        <h2>Configuration drafts</h2>
        <p>
          Store version-ready translation, category, and workflow template
          drafts. Runtime behavior uses the reviewed source catalogs and state
          machine; these drafts require a code review and release to activate.
        </p>
        <Field label="Configuration key">
          <select
            value={configKey}
            onChange={(e) => setConfigKey(e.target.value)}
          >
            {[
              "translations.en",
              "translations.hi",
              "categories",
              "workflow-template",
            ].map((k) => (
              <option key={k}>{k}</option>
            ))}
          </select>
        </Field>
        <Field label="JSON configuration">
          <textarea
            rows={5}
            value={config}
            onChange={(e) => setConfig(e.target.value)}
          />
        </Field>
        <Button
          disabled={busy}
          onClick={() =>
            run(() =>
              api(`/admin/configuration/${configKey}`, {
                method: "PUT",
                body: JSON.stringify(JSON.parse(config)),
              }),
            )
          }
        >
          Save configuration draft
        </Button>
      </section>
    </>
  );
}
