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
        <h1>Your information belongs in your control.</h1>
        <h2>Collect only what is needed</h2>
        <p>
          OneNotify stores case details, family permissions, documents,
          correspondence, and a history of sensitive actions. Full Aadhaar and
          PAN numbers are not requested in profile forms. Never provide banking
          passwords, UPI PINs, CVVs, OTPs, or email passwords.
        </p>
        <h2>Document protection</h2>
        <p>
          Original files are encrypted with AES-256-GCM before they reach object
          storage. Downloads require current case membership and a short-lived,
          single-use access ticket. Document reads are recorded. This local demo
          uses a placeholder malware scanner and is intended for sample files.
        </p>
        <h2>Sharing and consent</h2>
        <p>
          Submission approval records the recipient, purpose, shared fields,
          selected document IDs, and expiration. No live provider integrations
          are configured. Downloaded packages are under your control; share them
          only through a provider’s approved channel.
        </p>
        <h2>Your choices</h2>
        <p>
          You can revoke family access, request document deletion, and request a
          case deletion review. These requests are recorded for an operator to
          review retention obligations; they are not immediate erasure. Audit
          history cannot be silently rewritten.
        </p>
        <h2>Optional assistance</h2>
        <p>
          AI assistance is a local deterministic mock. Embedded PDF text can be
          read for organization name matching. Image OCR and Gmail connection
          are not configured. Optional browser voice input may use your browser
          vendor’s speech processing service.
        </p>
        <h2>Before production use</h2>
        <p>
          This repository is a local demonstration. A production operator must
          define retention and erasure policy, configure malware scanning and
          managed encryption keys, obtain verified provider rules, review
          applicable legal obligations, and publish contact and grievance
          information.
        </p>
      </main>
    </div>
  );
}
