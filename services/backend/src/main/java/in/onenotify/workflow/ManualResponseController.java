package in.onenotify.workflow;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/provider-cases/{id}/record-response")
public class ManualResponseController {
  private final ManualResponseService service;

  public ManualResponseController(ManualResponseService service) {
    this.service = service;
  }

  @PostMapping
  Object record(@PathVariable UUID id, @Valid @RequestBody ManualResponseService.Response r) {
    return service.record(id, r);
  }
}
