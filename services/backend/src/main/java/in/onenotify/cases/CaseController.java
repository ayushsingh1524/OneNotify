package in.onenotify.cases;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cases")
public class CaseController {
  private final CaseService service;

  public CaseController(CaseService service) {
    this.service = service;
  }

  @GetMapping
  Object list() {
    return service.list();
  }

  @PostMapping
  Object create(@Valid @RequestBody CaseService.Create r) {
    return service.create(r);
  }

  @GetMapping("/{id}")
  Object detail(@PathVariable UUID id) {
    return service.detail(id);
  }

  @PutMapping("/{id}")
  Object update(@PathVariable UUID id, @Valid @RequestBody CaseService.Create r) {
    return service.update(id, r);
  }

  @GetMapping("/{id}/timeline")
  Object timeline(@PathVariable UUID id) {
    return service.timeline(id);
  }

  @PostMapping("/{id}/deletion-request")
  Object delete(@PathVariable UUID id) {
    return service.retention(id);
  }
}
