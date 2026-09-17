package in.onenotify.workflow;

import in.onenotify.consent.ConsentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class WorkflowController {
  private final WorkflowService service;
  private final ConsentService consents;
  private final DemoProviderService demo;

  public WorkflowController(
      WorkflowService service, ConsentService consents, DemoProviderService demo) {
    this.service = service;
    this.consents = consents;
    this.demo = demo;
  }

  public record Submit(@NotNull UUID consentId) {}

  public record Action(String action) {}

  @PostMapping("/provider-cases/{id}/prepare")
  Object prepare(@PathVariable UUID id) {
    return service.prepare(id);
  }

  @PostMapping("/provider-cases/{id}/submit")
  Object submit(
      @PathVariable UUID id,
      @Valid @RequestBody Submit r,
      @RequestHeader("Idempotency-Key") String key) {
    return service.submit(id, r.consentId(), key);
  }

  @PostMapping("/provider-cases/{id}/action")
  Object action(@PathVariable UUID id, @RequestBody Action r) {
    return service.action(id, r.action());
  }

  @PostMapping("/provider-cases/{id}/simulate-response")
  Object simulate(@PathVariable UUID id) {
    return demo.advance(id);
  }

  @PostMapping("/consents")
  Object consent(@Valid @RequestBody ConsentService.Approval r) {
    return consents.approve(r);
  }

  @DeleteMapping("/consents/{id}")
  Object revoke(@PathVariable UUID id) {
    consents.revoke(id);
    return Map.of("ok", true);
  }
}
