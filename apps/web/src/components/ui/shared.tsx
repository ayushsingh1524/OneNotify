"use client";
import { label } from "@/lib/api";
import {
  Check,
  Clock3,
  Circle,
  AlertCircle,
  ArrowRight,
  Loader2,
} from "lucide-react";
import Link from "next/link";
export function Status({ state }: { state: string }) {
  const done = state === "COMPLETED";
  const waiting = [
    "SUBMITTED",
    "PROVIDER_ACKNOWLEDGED",
    "UNDER_REVIEW",
    "RESUBMITTED",
  ].includes(state);
  const neutral = ["DRAFT", "CANCELLED", "PAUSED"].includes(state);
  const Icon = done ? Check : waiting ? Clock3 : neutral ? Circle : AlertCircle;
  return (
    <span
      className={`status ${done ? "done" : waiting ? "waiting" : neutral ? "neutral" : "attention"}`}
    >
      <Icon size={13} />
      {state === "DRAFT" ? "Not started" : label(state)}
    </span>
  );
}
export function Loading() {
  return (
    <div className="empty" role="status">
      <Loader2 className="spin" />
      Loading your workspace…
    </div>
  );
}
export function ErrorBox({ error }: { error: unknown }) {
  return error ? (
    <div className="error" role="alert">
      {error instanceof Error ? error.message : String(error)}
    </div>
  ) : null;
}
export function Empty({
  title,
  body,
  href,
  action,
}: {
  title: string;
  body: string;
  href?: string;
  action?: string;
}) {
  return (
    <div className="empty">
      <div className="empty-mark">
        <Circle size={24} />
      </div>
      <h3>{title}</h3>
      <p>{body}</p>
      {href && (
        <Link className="text-link" href={href}>
          {action}
          <ArrowRight size={16} />
        </Link>
      )}
    </div>
  );
}
export function Heading({
  eyebrow,
  title,
  description,
  children,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  children?: React.ReactNode;
}) {
  return (
    <div className="page-heading">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      <div className="heading-actions">{children}</div>
    </div>
  );
}
export function Field({
  label: caption,
  children,
  hint,
}: {
  label: string;
  children: React.ReactNode;
  hint?: string;
}) {
  return (
    <label className="field">
      <span>{caption}</span>
      {children}
      {hint && <small>{hint}</small>}
    </label>
  );
}
