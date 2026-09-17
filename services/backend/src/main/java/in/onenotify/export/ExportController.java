package in.onenotify.export;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ExportController {
  private final ExportService service;

  public ExportController(ExportService service) {
    this.service = service;
  }

  @GetMapping("/cases/{id}/export")
  ResponseEntity<byte[]> summary(@PathVariable UUID id) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header("Content-Disposition", "attachment; filename=onenotify-summary.pdf")
        .body(service.summary(id));
  }

  @GetMapping("/provider-cases/{id}/package")
  ResponseEntity<byte[]> pack(@PathVariable UUID id) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/zip"))
        .header("Content-Disposition", "attachment; filename=provider-package.zip")
        .body(service.packageZip(id));
  }
}
