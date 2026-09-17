package in.onenotify.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestFilter extends OncePerRequestFilter {
  private final in.onenotify.common.Db db;
  private final TokenService tokens;
  private final StringRedisTemplate redis;

  public RequestFilter(TokenService tokens, StringRedisTemplate redis, in.onenotify.common.Db db) {
    this.db = db;
    this.tokens = tokens;
    this.redis = redis;
  }

  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String correlation = UUID.randomUUID().toString();
    MDC.put("correlationId", correlation);
    res.setHeader("X-Correlation-ID", correlation);
    res.setHeader("Cache-Control", "no-store");
    try {
      if (!Set.of("GET", "HEAD", "OPTIONS").contains(req.getMethod())
          && "cross-site".equals(req.getHeader("Sec-Fetch-Site"))) {
        reject(res, 403, "CROSS_SITE_REQUEST", correlation);
        return;
      }
      String header = req.getHeader("Authorization");
      if (header != null && header.startsWith("Bearer ")) {
        try {
          var identity = tokens.verify(header.substring(7));
          var id = identity.user();
          var user =
              db.one(
                  "select u.system_role from users u join refresh_tokens s on s.user_id=u.id where u.id=? and s.id=? and not s.revoked and s.expires_at>now()",
                  id,
                  identity.session());
          SecurityContextHolder.getContext()
              .setAuthentication(
                  new UsernamePasswordAuthenticationToken(
                      id.toString(),
                      null,
                      List.of(
                          new org.springframework.security.core.authority.SimpleGrantedAuthority(
                              "ROLE_" + user.get("system_role")))));
        } catch (Exception e) {
          reject(res, 401, "TOKEN_EXPIRED", correlation);
          return;
        }
      }
      if (req.getRequestURI().startsWith("/api/")) {
        String identity =
            SecurityContextHolder.getContext().getAuthentication() == null
                ? req.getRemoteAddr()
                : SecurityContextHolder.getContext().getAuthentication().getName();
        boolean auth = req.getRequestURI().startsWith("/api/v1/auth");
        String key =
            "rate:"
                + (auth ? "auth:" : "api:")
                + identity
                + ":"
                + (System.currentTimeMillis() / 60000);
        try {
          Long count = redis.opsForValue().increment(key);
          if (count != null && count == 1) redis.expire(key, Duration.ofSeconds(65));
          if (count != null && count > (auth ? 30 : 240)) {
            res.setHeader("Retry-After", "60");
            reject(res, 429, "RATE_LIMITED", correlation);
            return;
          }
        } catch (org.springframework.data.redis.RedisConnectionFailureException e) {
          reject(res, 503, "SERVICE_UNAVAILABLE", correlation);
          return;
        }
      }
      chain.doFilter(req, res);
    } finally {
      MDC.clear();
      SecurityContextHolder.clearContext();
    }
  }

  private void reject(HttpServletResponse res, int status, String code, String id)
      throws IOException {
    res.setStatus(status);
    res.setContentType("application/json");
    res.getWriter()
        .write(
            "{\"status\":"
                + status
                + ",\"code\":\""
                + code
                + "\",\"message\":\"Please sign in again or retry shortly.\",\"correlationId\":\""
                + id
                + "\"}");
  }
}
