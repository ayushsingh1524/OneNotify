package in.onenotify.family;

import in.onenotify.audit.Audit;
import in.onenotify.auth.Access;
import in.onenotify.common.*;
import in.onenotify.integrations.Events;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FamilyService {
  public record Invite(
      @Email @NotBlank @Size(max = 254) String email,
      @Pattern(regexp = "FAMILY_ADMIN|CONTRIBUTOR|VIEWER|PROFESSIONAL_ADVISOR") @NotNull
          String role) {}

  private final Db db;
  private final Access access;
  private final Audit audit;
  private final Events events;

  public FamilyService(Db db, Access access, Audit audit, Events events) {
    this.db = db;
    this.access = access;
    this.audit = audit;
    this.events = events;
  }

  public Object list(UUID id) {
    access.read(id);
    return db.list(
        "select m.id,m.user_id,m.role,m.status,coalesce(u.name,m.invited_email) as name,coalesce(u.email,m.invited_email) as email from case_members m left join users u on u.id=m.user_id where m.case_id=? order by m.created_at",
        id);
  }

  @Transactional
  public Object invite(UUID id, Invite r) {
    access.manage(id);
    String email = r.email().strip().toLowerCase(Locale.ROOT);
    var users = db.list("select id from users where email=? and email_verified", email);
    UUID user = users.isEmpty() ? null : (UUID) users.getFirst().get("id");
    UUID member = UUID.randomUUID();
    db.update(
        "insert into case_members(id,case_id,user_id,invited_email,role,status) values(?,?,?,?,?,?)",
        member,
        id,
        user,
        email,
        r.role(),
        user == null ? "INVITED" : "ACTIVE");
    audit.record(
        access.actor(),
        id,
        null,
        "MEMBER_INVITED",
        "MEMBER",
        member,
        Map.of(),
        Map.of("role", r.role()));
    events.emit(
        "notification.requested", member, id, Map.of("title", "A family member was invited"));
    return list(id);
  }

  @Transactional
  public void revoke(UUID id, UUID member) {
    access.manage(id);
    var m = db.one("select role from case_members where id=? and case_id=?", member, id);
    if (m.get("role").equals("CASE_OWNER"))
      throw new ApiException(400, "OWNER_PROTECTED", "The case owner cannot be removed.");
    db.update("update case_members set status='REVOKED' where id=? and case_id=?", member, id);
    audit.record(access.actor(), id, null, "MEMBER_REVOKED", "MEMBER", member, Map.of(), Map.of());
  }
}
