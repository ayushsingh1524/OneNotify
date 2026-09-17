package in.onenotify.documents;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class DocumentController {
  private final DocumentService service;

  public DocumentController(DocumentService service) {
    this.service = service;
  }

  @GetMapping("/cases/{caseId}/documents")
  Object list(@PathVariable UUID caseId) {
    return service.list(caseId);
  }

  @PostMapping(value = "/cases/{caseId}/documents", consumes = "multipart/form-data")
  Object upload(
      @PathVariable UUID caseId,
      @RequestParam String category,
      @RequestPart MultipartFile file,
      @RequestParam(required = false) java.time.LocalDate expiresOn) {
    return service.upload(caseId, category, file, expiresOn);
  }

  @PostMapping("/documents/{id}/access")
  Object access(@PathVariable UUID id) {
    return service.ticket(id);
  }

  @GetMapping("/documents/{id}/content")
  ResponseEntity<byte[]> content(@PathVariable UUID id, @RequestParam String ticket) {
    var c = service.content(id, ticket);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(c.mime()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(c.name(), java.nio.charset.StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(c.bytes());
  }

  @DeleteMapping("/documents/{id}")
  Object delete(@PathVariable UUID id) {
    service.deletion(id);
    return Map.of("ok", true);
  }
}
