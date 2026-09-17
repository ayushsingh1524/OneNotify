# Assistance without dependence

`AiService` defines document classification, metadata extraction, requirement extraction, email classification, explanation and draft generation. `MockAiService` is deterministic and local: it recognizes simple keywords and returns review-required suggestions. No paid model or external AI service receives files.

PDFBox can read embedded text from the first twenty PDF pages. It does not OCR image scans. The metadata response explicitly reports `ocrAvailable=false`. Organization name matches create **POSSIBLE** relationships, never ownership assertions. The family confirms or ignores suggestions. Sample email ingestion is a separate interface ready for a future consented Gmail adapter; no mailbox is connected.

Missing-document detection uses normalized category comparisons and file availability/expiry, not an LLM. Drafts cover notification, closure, transfer, missing-document responses, follow-ups, and escalation; they are editable and never automatically sent. Provider-instruction/email suggestions never directly move the workflow.

AI must never decide heirship, inheritance eligibility, ownership, entitlement, document legal validity or provider approval. Future providers must preserve this boundary, clearly disclose data processing, minimize submitted text, defend against instructions embedded in documents, return structured reviewable results, and support regional languages.

Browser voice input is optional. The browser/vendor may process speech outside this application. The UI explains this before the user starts. Mixed Hindi/English LIC input can be converted to a suggested insurance discovery intent; it never establishes ownership.
