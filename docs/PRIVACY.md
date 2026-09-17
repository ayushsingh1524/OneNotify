# Privacy and retention design

OneNotify collects a deceased profile, the requesting user’s identity, family membership, selected document originals, organization requests, notes and audit metadata. Profile identity references accept only the last four characters. Do not enter passwords, PINs, CVVs or OTPs. Do not use real sensitive documents in the local demo.

Purpose: helping authorized families organize, prepare and track administrative requests. OneNotify does not determine lawful heirs, entitlement or legal representation. Real provider disclosure requires an explicit recipient/purpose/document approval and a supported authorized channel.

Access is scoped to current case membership. A system administrator can maintain the provider registry and inspect delivery health but cannot casually browse another family’s files. Revocation is checked for every subsequent access, including a previously issued vault ticket.

Original vault objects are encrypted. Profile and correspondence rows rely on infrastructure disk encryption and database access controls; they are not individually application-encrypted in this version. Production must document data residency, subprocessors, backup retention, key management, contact/grievance routes and lawful processing grounds after an appropriate legal review.

Document/case deletion requests are recorded. Documents requested for deletion are excluded from new packages. Physical erasure is deliberately not automatic: a production operator must review retention obligations, revoke outstanding consent, destroy eligible data and keys, handle backups, and preserve only justified minimal audit metadata. No statutory retention period is fabricated here.

Reports omit storage paths, network addresses, correlation metadata and secrets. They still contain family information and must be handled privately after download. User-initiated browser voice processing has a separate browser-provider privacy boundary.
