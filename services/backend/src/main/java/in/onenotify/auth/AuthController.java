package in.onenotify.auth;

import in.onenotify.common.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService service;
  private final in.onenotify.users.UserService users;
  private final boolean secure;
  private final PasswordResetService resets;
  private final EmailVerification verification;

  public AuthController(
      AuthService service,
      in.onenotify.users.UserService users,
      @Value("${app.secure-cookie}") boolean secure,
      PasswordResetService resets,
      EmailVerification verification) {
    this.service = service;
    this.users = users;
    this.secure = secure;
    this.resets = resets;
    this.verification = verification;
  }

  public record Verify(@NotBlank String token) {}

  @PostMapping("/verify-email")
  Object verify(@Valid @RequestBody Verify r) {
    verification.verify(r.token());
    return Map.of("ok", true);
  }

  @PostMapping("/register")
  ResponseEntity<?> register(@Valid @RequestBody AuthService.Register r) {
    return response(service.register(r));
  }

  @PostMapping("/login")
  ResponseEntity<?> login(@Valid @RequestBody AuthService.Login r) {
    return response(service.login(r));
  }

  @PostMapping("/refresh")
  ResponseEntity<?> refresh(@CookieValue(name = "on_refresh", required = false) String token) {
    return response(service.refresh(token));
  }

  @PostMapping("/logout")
  ResponseEntity<?> logout(@CookieValue(name = "on_refresh", required = false) String token) {
    service.logout(token);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie("", 0))
        .body(Map.of("ok", true));
  }

  @GetMapping("/me")
  Object me() {
    return users.currentProfile();
  }

  public record Email(@jakarta.validation.constraints.Email @NotBlank String email) {}

  public record Reset(@NotBlank String token, @NotNull @Size(min = 12, max = 72) String password) {}

  @PostMapping("/forgot-password")
  Object forgot(@Valid @RequestBody Email email) {
    resets.request(email.email());
    return Map.of(
        "message",
        "If that email is registered, a reset link will be sent. Local demo mail is available in Mailpit.");
  }

  @PostMapping("/reset-password")
  Object reset(@Valid @RequestBody Reset r) {
    resets.reset(r.token(), r.password());
    return Map.of("ok", true);
  }

  private ResponseEntity<?> response(AuthService.Session s) {
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie(s.refreshToken(), 604800))
        .body(Map.of("accessToken", s.accessToken(), "user", s.user()));
  }

  private String cookie(String value, long seconds) {
    return ResponseCookie.from("on_refresh", value)
        .httpOnly(true)
        .secure(secure)
        .sameSite("Strict")
        .path("/api/v1/auth")
        .maxAge(seconds)
        .build()
        .toString();
  }
}
