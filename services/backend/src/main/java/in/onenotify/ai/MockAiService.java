package in.onenotify.ai;

import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class MockAiService implements AiService {
  public Map<String, Object> classifyDocument(String filename, String text) {
    String s = (filename + " " + text).toLowerCase(Locale.ROOT);
    String category =
        s.contains("death")
            ? "DEATH_CERTIFICATE"
            : s.contains("nominee")
                ? "NOMINEE_DOCUMENT"
                : s.contains("policy")
                    ? "POLICY_DOCUMENT"
                    : s.contains("statement") ? "BANK_STATEMENT" : "OTHER";
    return Map.of(
        "suggestedCategory", category, "reviewRequired", true, "engine", "DETERMINISTIC_MOCK");
  }

  public Map<String, Object> extractMetadata(String text) {
    return Map.of(
        "ocrAvailable",
        false,
        "textExtracted",
        !text.isBlank(),
        "reviewRequired",
        true,
        "message",
        "Embedded PDF text only. Image OCR requires a production OCR adapter.");
  }

  public Map<String, Object> extractRequirements(String text) {
    var docs = new ArrayList<String>();
    String lower = text.toLowerCase(Locale.ROOT);
    if (lower.contains("nominee")) docs.add("NOMINEE_DOCUMENT");
    if (lower.contains("death certificate")) docs.add("DEATH_CERTIFICATE");
    return Map.of(
        "suggestedDocuments",
        docs,
        "deadline",
        "NOT_VERIFIED",
        "nextStep",
        "Verify these suggestions against the provider's instructions.",
        "reviewRequired",
        true);
  }

  public Map<String, Object> classifyEmail(String text) {
    return Map.of(
        "suggestedState",
        text.toLowerCase(Locale.ROOT).contains("submit")
            ? "ADDITIONAL_DOCUMENTS_REQUIRED"
            : "HUMAN_REVIEW_REQUIRED",
        "reviewRequired",
        true,
        "engine",
        "DETERMINISTIC_MOCK");
  }

  public String explain(String text) {
    return "Review the provider's message and confirm what they need before taking action. Suggested checklist: "
        + extractRequirements(text).get("suggestedDocuments");
  }

  public String draft(String type, String person, String provider) {
    return "DRAFT — review before sending\n\nTo "
        + provider
        + ",\n\nI am contacting you regarding "
        + person
        + " following their death. I would like to request "
        + type.toLowerCase(Locale.ROOT).replace('_', ' ')
        + ". Please confirm your current requirements and the appropriate process. I can provide the documents you require through your approved channels.\n\nPlease acknowledge this request and provide a reference number.\n\nSincerely,\n[Your name and relationship]";
  }
}
