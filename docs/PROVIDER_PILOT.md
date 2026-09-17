# First provider pilot: SBI nominated bank deposit

Status: **proposed manual pilot; no API access, partnership, outreach or real submission**. Researched 17 September 2026. The running demo registry remains explicitly DEMO.

## Narrow first scope

Use an undisputed bank-deposit case with a registered nominee as the proposed first scenario. Exclude lockers, government savings schemes, competing claims and cases requiring legal interpretation from this pilot. A provider representative must confirm applicability before any real use.

SBI's official page describes branch and online deceased-claim submission and lists an application form, death certificate and KYC documents for deposits with nomination. It states deposit claims can be lodged at a chosen SBI branch, while lockers have a branch-specific route. The page identifies revised guidance effective 16 December 2025 and was last updated 5 January 2026. Treat the current official page/forms and provider confirmation as controlling, rather than freezing requirements from this draft. [SBI deceased settlement guidance](https://sbi.bank.in/en/web/personal-banking/information-services/deceased-settlement)

## Pilot workflow

1. Rehearse with fabricated names and sample files. Do not transmit them to SBI.
2. Ask a designated branch/customer-service contact to confirm the exact current form, eligible claimant category, submission channel, acknowledgment evidence and escalation route.
3. Record source URL, retrieval date, form version, reviewer, review date and next-review date in the provider record. Keep status `REVIEW_REQUIRED` until the review is complete.
4. Configure a manual adapter. Never present a prepared package as already submitted.
5. The claimant reviews the exact package and consent. They submit through the verified provider channel themselves.
6. Record acknowledgment/reference and original correspondence as **family-reported**. Only a verified provider response can be called provider-authenticated.
7. Track requests for more information, require fresh consent for new files, and close only with completion evidence.

The existing manual lifecycle implements the preparation/recorded-response mechanics. It does not verify claimant legal authority, authenticate provider receipts, or guarantee acceptance.

## Draft message for a provider contact — not sent

Subject: Request to confirm a manually assisted deceased-deposit claim workflow

Hello,

We are evaluating OneNotify, a tool that helps a family organize documents and track a bereavement-related request. We are not asking to access customer accounts or submit customer data in this initial discussion.

Could the appropriate team confirm the current process for an undisputed deposit claim with a registered nominee: official forms, required documents, accepted submission channel, acknowledgment format and escalation route? We would also like to know whether a formal partner/API onboarding route exists and what approval it requires.

Our proposed initial workflow keeps submission with the claimant and labels subsequent updates as family-reported. We will not describe OneNotify as affiliated with SBI or transmit documents without appropriate authorization.

Thank you.

## Acceptance evidence

- A named reviewer confirms the source and requirements for the exact scenario.
- Synthetic rehearsal completes without misleading submission labels.
- Claimant authorization, exact-document consent and package contents are reviewable.
- Submission receipt and any subsequent decisions are traceable to original correspondence.
- No credentials, OTPs or account-control permissions are collected by OneNotify.
- Any live API development waits for written onboarding terms, sandbox credentials, endpoint documentation and webhook verification requirements.

If SBI is not accessible for an initial conversation, choose a provider for which you already have a legitimate contact. Do not substitute an unofficial API or scrape an authenticated portal.
