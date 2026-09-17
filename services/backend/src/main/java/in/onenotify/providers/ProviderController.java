package in.onenotify.providers;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ProviderController {
  private final ProviderService service;

  public ProviderController(ProviderService service) {
    this.service = service;
  }

  @GetMapping("/providers")
  Object registry() {
    return service.registry();
  }

  @GetMapping("/cases/{caseId}/providers")
  Object list(@PathVariable UUID caseId) {
    return service.list(caseId);
  }

  @PostMapping("/cases/{caseId}/providers")
  Object add(@PathVariable UUID caseId, @Valid @RequestBody ProviderService.Add r) {
    return service.add(caseId, r);
  }

  @GetMapping("/provider-cases/{id}")
  Object detail(@PathVariable UUID id) {
    return service.detail(id);
  }

  public record Assign(UUID userId) {}

  @PutMapping("/provider-cases/{id}/assignee")
  Object assign(@PathVariable UUID id, @RequestBody Assign r) {
    service.assign(id, r.userId());
    return Map.of("ok", true);
  }
}
