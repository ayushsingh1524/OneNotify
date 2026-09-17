package in.onenotify.notifications;

import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
  private final NotificationService service;

  public NotificationController(NotificationService service) {
    this.service = service;
  }

  @GetMapping
  Object list() {
    return service.list();
  }

  @PutMapping("/{id}/read")
  Object read(@PathVariable UUID id) {
    service.read(id);
    return Map.of("ok", true);
  }
}
