package in.onenotify.integrations;

public final class MockApiProviderAdapter implements ProviderAdapter {
  public Mode mode() {
    return Mode.SIMULATED;
  }

  public Result submit(Request r) {
    validateRequest(r);
    return new Result(
        "DEMO-" + r.providerCaseId(),
        "SUBMITTED",
        "SIMULATED submission. No organization has been contacted.",
        true);
  }
}
