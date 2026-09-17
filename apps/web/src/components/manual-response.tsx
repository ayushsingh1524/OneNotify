"use client";
import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { post, categories, label } from "@/lib/api";
import { Field, ErrorBox } from "./ui/shared";
import { Button } from "./ui/button";
const outcomes: Record<string, string[]> = {
  EXTERNAL_ACTION_REQUIRED: ["SUBMITTED"],
  SUBMITTED: ["PROVIDER_ACKNOWLEDGED"],
  PROVIDER_ACKNOWLEDGED: ["UNDER_REVIEW", "ADDITIONAL_DOCUMENTS_REQUIRED"],
  UNDER_REVIEW: ["ADDITIONAL_DOCUMENTS_REQUIRED", "APPROVED", "REJECTED"],
  APPROVED: ["COMPLETED"],
  RESUBMITTED: ["UNDER_REVIEW"],
  ESCALATION_REQUIRED: ["UNDER_REVIEW"],
};
export function ManualResponse({ id, state }: { id: string; state: string }) {
  const client = useQueryClient();
  const [outcome, setOutcome] = useState("");
  const [note, setNote] = useState("");
  const [reference, setReference] = useState("");
  const [category, setCategory] = useState("NOMINEE_DOCUMENT");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>();
  const options = outcomes[state] || [];
  if (!options.length) return null;
  return (
    <section className="panel compact-panel">
      <h3>Record an external update</h3>
      <p>
        Only record what the provider actually confirmed. This creates a
        family-reported record; OneNotify does not verify provider decisions.
      </p>
      <form
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          setError(null);
          try {
            await post(`/provider-cases/${id}/record-response`, {
              outcome: options.includes(outcome) ? outcome : options[0],
              note,
              reference,
              requestedCategory: category,
            });
            await client.invalidateQueries();
            setOutcome("");
            setNote("");
            setReference("");
          } catch (e) {
            setError(e);
          } finally {
            setBusy(false);
          }
        }}
      >
        <Field label="Provider outcome">
          <select
            value={options.includes(outcome) ? outcome : options[0]}
            onChange={(e) => setOutcome(e.target.value)}
          >
            {options.map((o) => (
              <option key={o} value={o}>
                {label(o)}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Provider reference / receipt">
          <input
            required
            value={reference}
            maxLength={120}
            onChange={(e) => setReference(e.target.value)}
          />
        </Field>
        <Field label="What did the provider confirm?">
          <textarea
            required
            value={note}
            maxLength={10000}
            rows={3}
            onChange={(e) => setNote(e.target.value)}
          />
        </Field>
        {outcome === "ADDITIONAL_DOCUMENTS_REQUIRED" && (
          <Field label="Requested document">
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            >
              {categories.map((c) => (
                <option key={c} value={c}>
                  {label(c)}
                </option>
              ))}
            </select>
          </Field>
        )}
        <ErrorBox error={error} />
        <Button disabled={busy}>Record provider update</Button>
      </form>
    </section>
  );
}
