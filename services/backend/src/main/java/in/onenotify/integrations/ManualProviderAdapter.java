package in.onenotify.integrations;

public class ManualProviderAdapter implements ProviderAdapter {
  private final Mode mode;

  public ManualProviderAdapter(Mode mode) {
    this.mode = mode;
  }

  public Mode mode() {
    return mode;
  }

  public Result submit(Request r) {
    validateRequest(r);
    return new Result(
        "MANUAL-" + r.providerCaseId(),
        "EXTERNAL_ACTION_REQUIRED",
        "Download your package and contact the provider. No information has been sent.",
        false);
  }
}
