package in.onenotify.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class DiscoveryController {
  private final DiscoveryService service;

  public DiscoveryController(DiscoveryService service) {
    this.service = service;
  }

  @GetMapping("/cases/{caseId}/discovery")
  Object list(@PathVariable UUID caseId) {
    return service.list(caseId);
  }

  @PostMapping("/cases/{caseId}/discovery/sample-email")
  Object sample(@PathVariable UUID caseId) {
    return service.sample(caseId);
  }

  public record Status(@NotBlank String status) {}

  @PutMapping("/cases/{caseId}/discovery/{id}")
  Object status(@PathVariable UUID caseId, @PathVariable UUID id, @Valid @RequestBody Status r) {
    return service.status(caseId, id, r.status());
  }

  @PostMapping("/documents/{id}/analyze")
  Object analyze(@PathVariable UUID id) {
    return service.analyze(id);
  }

  public record Assist(
      @NotBlank @Size(max = 60) String type, @NotNull @Size(max = 10000) String text) {}

  @PostMapping("/provider-cases/{id}/assist")
  Object assist(@PathVariable UUID id, @Valid @RequestBody Assist r) {
    return service.assist(id, r.type(), r.text());
  }
}
