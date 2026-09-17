package in.onenotify.search;

import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
  private final SearchService service;

  public SearchController(SearchService service) {
    this.service = service;
  }

  @GetMapping
  Object search(@RequestParam UUID caseId, @RequestParam String q) {
    return service.search(caseId, q);
  }
}
