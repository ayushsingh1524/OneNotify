package in.onenotify.integrations;

import java.util.*;

public interface ProviderAdapter {
  enum Mode {
    API,
    EMAIL,
    FORM,
    MANUAL,
    EXTERNAL_PORTAL,
    SIMULATED
  }

  record Request(UUID providerCaseId, UUID consentId, List<UUID> documentIds, String action) {}

  record Result(String reference, String status, String instruction, boolean simulated) {}

  Mode mode();

  default void validateRequest(Request request) {
    if (request.consentId() == null) throw new IllegalArgumentException("Consent required");
  }

  default Map<String, Object> prepareSubmission(Request request) {
    return Map.of(
        "reference",
        request.providerCaseId(),
        "documents",
        request.documentIds(),
        "action",
        request.action());
  }

  Result submit(Request request);

  default Result checkStatus(String reference) {
    return new Result(
        reference, "EXTERNAL_ACTION_REQUIRED", "Check directly with the provider.", false);
  }

  default String parseResponse(String body) {
    return "HUMAN_REVIEW_REQUIRED";
  }

  default void cancel(String reference) {}
}
