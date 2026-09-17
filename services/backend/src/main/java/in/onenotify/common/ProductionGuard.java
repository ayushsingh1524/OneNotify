package in.onenotify.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProductionGuard {
  public ProductionGuard(
      @Value("${app.demo-enabled}") boolean demo,
      @Value("${app.jwt-secret}") String jwt,
      @Value("${app.vault-key}") String key,
      @Value("${app.secure-cookie}") boolean cookie,
      @Value("${app.scanner:demo}") String scanner,
      @Value("${APP_ORIGIN:http://localhost:3000}") String origin) {
    if (!demo
        && (jwt.startsWith("local-only")
            || key.equals("MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=")
            || !cookie
            || !"clamav".equals(scanner)
            || !origin.startsWith("https://")))
      throw new IllegalStateException(
          "Non-demo mode requires unique secrets, a unique vault key, HTTPS, secure cookies and a configured real scanner.");
  }
}
