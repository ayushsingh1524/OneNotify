package in.onenotify.cases;

import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/onboarding")
public class OnboardingController {
  private final OnboardingService service;

  public OnboardingController(OnboardingService service) {
    this.service = service;
  }

  @GetMapping
  Object get() {
    return service.get();
  }

  @PutMapping
  Object save(@RequestBody Map<String, Object> data) {
    return service.save(data);
  }

  @DeleteMapping
  Object clear() {
    service.clear();
    return Map.of("ok", true);
  }

  public record Timing(double seconds) {}

  @PostMapping("/timing")
  Object duration(@RequestBody Timing timing) {
    service.duration(timing.seconds());
    return Map.of("ok", true);
  }
}
