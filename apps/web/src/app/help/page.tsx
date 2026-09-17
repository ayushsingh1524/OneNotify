import Link from "next/link";
import { Sprout, ArrowLeft } from "lucide-react";
export default function Page() {
  return (
    <div className="legal-page">
      <Link className="brand" href="/">
        <span className="brand-icon">
          <Sprout />
        </span>
        OneNotify.
      </Link>
      <main id="main">
        <Link className="back-link" href="/app">
          <ArrowLeft size={16} />
          Your workspace
        </Link>
        <h1>A little guidance, whenever you need it.</h1>
        <h2>Start with one small step</h2>
        <p>
          Create a case with the details you know. You can save the onboarding
          draft and return later. A death certificate can be added whenever you
          have it.
        </p>
        <h2>Build a shared checklist</h2>
        <p>
          Add known banks, insurers, investments, mobile providers,
          subscriptions, and utilities. Sample email discovery can suggest
          possible organizations; you must confirm them before they are added.
        </p>
        <h2>Prepare, review, then approve</h2>
        <p>
          Each organization has a document checklist. Upload the missing
          documents, prepare the request, then review the information and
          selected files. Only case owners and family administrators can
          authorize a submission.
        </p>
        <h2>Share the work</h2>
        <p>
          Invite a trusted family member, assign an organization, add tasks, and
          keep call notes together. View-only access is available.
        </p>
        <h2>Understand the demo</h2>
        <p>
          All provider rules are examples, not verified official procedures.
          Simulated responses do not come from real organizations. Manual
          adapters prepare packages but never send them. Check current
          procedures directly with each provider.
        </p>
        <h2>When you need specialist help</h2>
        <p>
          OneNotify does not determine inheritance rights, ownership, legal
          eligibility, or document validity. For those questions, contact the
          provider or an appropriately qualified professional.
        </p>
      </main>
    </div>
  );
}
