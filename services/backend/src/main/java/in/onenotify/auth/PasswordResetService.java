package in.onenotify.auth;

import in.onenotify.common.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
  private final Db db;
  private final JavaMailSender mail;
  private final PasswordEncoder passwords;
  private final String origin;
  private final String sender;

  public PasswordResetService(
      Db db,
      JavaMailSender mail,
      PasswordEncoder passwords,
      @Value("${APP_ORIGIN:http://localhost:3000}") String origin,
      @Value("${MAIL_FROM:support@onenotify.test}") String sender) {
    this.db = db;
    this.mail = mail;
    this.passwords = passwords;
    this.origin = origin;
    this.sender = sender;
  }

  @Transactional
  public void request(String email) {
    var users =
        db.list("select id from users where email=?", email.strip().toLowerCase(Locale.ROOT));
    if (users.isEmpty()) return;
    String token = AuthService.random();
    db.update(
        "insert into password_resets(id,user_id,token_hash,expires_at) values(?,?,?,now()+interval '30 minutes')",
        UUID.randomUUID(),
        users.getFirst().get("id"),
        AuthService.hash(token));
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(sender);
    message.setTo(email);
    message.setSubject("Reset your OneNotify password");
    message.setText(
        "Use this link within 30 minutes: " + origin + "/reset-password?token=" + token);
    mail.send(message);
  }

  @Transactional
  public void reset(String token, String password) {
    var rows =
        db.list(
            "select * from password_resets where token_hash=? and not used and expires_at>now() for update",
            AuthService.hash(token));
    if (rows.isEmpty())
      throw new ApiException(
          400, "INVALID_RESET", "This reset link has expired or was already used.");
    var row = rows.getFirst();
    db.update(
        "update users set password_hash=? where id=?",
        passwords.encode(password),
        row.get("user_id"));
    db.update("update password_resets set used=true where user_id=?", row.get("user_id"));
    db.update("update refresh_tokens set revoked=true where user_id=?", row.get("user_id"));
  }
}
