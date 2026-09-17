package in.onenotify.tasks;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class TaskController {
  private final TaskService service;

  public TaskController(TaskService service) {
    this.service = service;
  }

  @GetMapping("/cases/{caseId}/tasks")
  Object list(@PathVariable UUID caseId) {
    return service.list(caseId);
  }

  @PostMapping("/cases/{caseId}/tasks")
  Object create(@PathVariable UUID caseId, @Valid @RequestBody TaskService.Create r) {
    return service.create(caseId, r);
  }

  public record Status(boolean done) {}

  @PutMapping("/tasks/{id}")
  Object status(@PathVariable UUID id, @RequestBody Status r) {
    service.status(id, r.done());
    return Map.of("ok", true);
  }
}
