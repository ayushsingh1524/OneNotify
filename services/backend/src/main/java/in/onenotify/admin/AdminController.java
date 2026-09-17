package in.onenotify.admin;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
  private final AdminService service;

  public AdminController(AdminService service) {
    this.service = service;
  }

  @GetMapping
  Object overview() {
    return service.overview();
  }

  @GetMapping("/metrics")
  Object metrics() {
    return service.metrics();
  }

  @PostMapping("/providers")
  Object create(@Valid @RequestBody AdminService.Provider r) {
    return service.provider(null, r);
  }

  @PutMapping("/providers/{id}")
  Object update(@PathVariable UUID id, @Valid @RequestBody AdminService.Provider r) {
    return service.provider(id, r);
  }

  @PostMapping("/events/{id}/retry")
  Object retry(@PathVariable UUID id) {
    service.retry(id);
    return Map.of("ok", true);
  }

  @PutMapping("/configuration/{key}")
  Object config(@PathVariable String key, @RequestBody Map<String, Object> r) {
    return service.config(key, r);
  }
}
