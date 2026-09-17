package in.onenotify.correspondence;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cases/{caseId}/correspondence")
public class CorrespondenceController {
  private final CorrespondenceService service;

  public CorrespondenceController(CorrespondenceService service) {
    this.service = service;
  }

  @GetMapping
  Object list(@PathVariable UUID caseId) {
    return service.list(caseId);
  }

  @PostMapping
  Object create(@PathVariable UUID caseId, @Valid @RequestBody CorrespondenceService.Create r) {
    return service.create(caseId, r);
  }
}
