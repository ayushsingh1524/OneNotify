package in.onenotify.auth;

import in.onenotify.audit.Audit;
import in.onenotify.common.*;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  public record Register(
      @NotBlank @Size(max = 120) String name,
      @Email @NotBlank @Size(max = 254) String email,
      @NotNull @Size(min = 12, max = 72) String password) {}

  public record Login(@Email @NotBlank String email, @NotBlank @Size(max = 72) String password) {}

  public record Session(String accessToken, String refreshToken, Map<String, Object> user) {}

  private final Db db;
  private final PasswordEncoder passwords;
  private final TokenService tokens;
  private final Audit audit;
  private final EmailVerification verification;
  private final boolean demo;

  public AuthService(
      Db db,
      PasswordEncoder passwords,
      TokenService tokens,
      Audit audit,
      EmailVerification verification,
      @org.springframework.beans.factory.annotation.Value("${app.demo-enabled}") boolean demo) {
    this.db = db;
    this.passwords = passwords;
    this.tokens = tokens;
    this.audit = audit;
    this.verification = verification;
    this.demo = demo;
  }

  @Transactional
  public Session register(Register input) {
    UUID id = UUID.randomUUID();
    String email = input.email().strip().toLowerCase(Locale.ROOT);
    db.update(
        "insert into users(id,email,name,password_hash,email_verified) values(?,?,?,?,?)",
        id,
        email,
        input.name().strip(),
        passwords.encode(input.password()),
        demo);
    if (demo)
      db.update(
          "update case_members set user_id=?,status='ACTIVE' where invited_email=? and status='INVITED'",
          id,
          email);
    audit.record(id, null, null, "USER_REGISTERED", "USER", id, Map.of(), Map.of());
    if (!demo) {
      verification.send(id, email);
      return new Session("", "", Map.of("verificationRequired", true));
    }
    return session(id);
  }

  @Transactional
  public Session login(Login input) {
    var users =
        db.list(
            "select * from users where email=?", input.email().strip().toLowerCase(Locale.ROOT));
    String hash =
        users.isEmpty()
            ? "$2a$12$ZmWGK.K8C.YJXuRY.2w2u.BSGYMS44pbBg/QCCUaPJqpFKehRWlkE."
            : users.getFirst().get("password_hash").toString();
    boolean valid = passwords.matches(input.password(), hash);
    if (users.isEmpty() || !valid)
      throw new ApiException(401, "INVALID_CREDENTIALS", "Email or password is incorrect.");
    if (!Boolean.TRUE.equals(users.getFirst().get("email_verified")))
      throw new ApiException(403, "EMAIL_UNVERIFIED", "Verify your email before signing in.");
    return session((UUID) users.getFirst().get("id"));
  }

  @Transactional
  public Session refresh(String raw) {
    if (raw == null) throw new ApiException(401, "SESSION_EXPIRED", "Please sign in again.");
    var rows =
        db.list(
            "select * from refresh_tokens where token_hash=? and not revoked and expires_at>now() for update",
            hash(raw));
    if (rows.isEmpty()) throw new ApiException(401, "SESSION_EXPIRED", "Please sign in again.");
    db.update("update refresh_tokens set revoked=true where id=?", rows.getFirst().get("id"));
    return session((UUID) rows.getFirst().get("user_id"));
  }

  public void logout(String raw) {
    if (raw != null)
      db.update("update refresh_tokens set revoked=true where token_hash=?", hash(raw));
  }

  private Session session(UUID id) {
    String refresh = random();
    UUID sessionId = UUID.randomUUID();
    db.update(
        "insert into refresh_tokens(id,user_id,token_hash,expires_at) values(?,?,?,now()+interval '7 days')",
        sessionId,
        id,
        hash(refresh));
    return new Session(
        tokens.issue(id, sessionId),
        refresh,
        db.one("select id,name,email,system_role from users where id=?", id));
  }

  public static String random() {
    byte[] b = new byte[32];
    new SecureRandom().nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
