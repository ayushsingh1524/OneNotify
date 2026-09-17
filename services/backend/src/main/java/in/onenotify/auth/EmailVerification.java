package in.onenotify.auth;

import in.onenotify.common.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerification {
  private final Db db;
  private final JavaMailSender mail;
  private final String origin;
  private final String sender;

  public EmailVerification(
      Db db,
      JavaMailSender mail,
      @Value("${APP_ORIGIN:http://localhost:3000}") String origin,
      @Value("${MAIL_FROM:support@onenotify.test}") String sender) {
    this.db = db;
    this.mail = mail;
    this.origin = origin;
    this.sender = sender;
  }

  public void send(UUID id, String email) {
    String token = AuthService.random();
    db.update(
        "insert into email_verifications(id,user_id,token_hash,expires_at) values(?,?,?,now()+interval '24 hours')",
        UUID.randomUUID(),
        id,
        AuthService.hash(token));
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(sender);
    message.setTo(email);
    message.setSubject("Verify your OneNotify email");
    message.setText(
        "Verify your email before signing in: " + origin + "/verify-email?token=" + token);
    mail.send(message);
  }

  @Transactional
  public void verify(String token) {
    var row =
        db.one(
            "select v.id,v.user_id,u.email from email_verifications v join users u on u.id=v.user_id where v.token_hash=? and not v.used and v.expires_at>now() for update of v",
            AuthService.hash(token));
    db.update("update users set email_verified=true where id=?", row.get("user_id"));
    db.update("update email_verifications set used=true where user_id=?", row.get("user_id"));
    db.update(
        "update case_members set user_id=?,status='ACTIVE' where invited_email=? and status='INVITED'",
        row.get("user_id"),
        row.get("email"));
  }
}
