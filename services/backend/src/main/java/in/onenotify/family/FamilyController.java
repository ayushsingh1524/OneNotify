package in.onenotify.family;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cases/{caseId}/members")
public class FamilyController {
  private final FamilyService service;

  public FamilyController(FamilyService service) {
    this.service = service;
  }

  @GetMapping
  Object list(@PathVariable UUID caseId) {
    return service.list(caseId);
  }

  @PostMapping
  Object invite(@PathVariable UUID caseId, @Valid @RequestBody FamilyService.Invite r) {
    return service.invite(caseId, r);
  }

  @DeleteMapping("/{id}")
  Object revoke(@PathVariable UUID caseId, @PathVariable UUID id) {
    service.revoke(caseId, id);
    return Map.of("ok", true);
  }
}
