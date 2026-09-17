package in.onenotify.auth;

import in.onenotify.common.*;
import java.util.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class Access {
  private final Db db;

  public Access(Db db) {
    this.db = db;
  }

  public UUID actor() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null || !a.isAuthenticated() || a.getName().equals("anonymousUser"))
      throw new ApiException(401, "AUTH_REQUIRED", "Please sign in.");
    return UUID.fromString(a.getName());
  }

  public String role(UUID caseId) {
    var row =
        db.list(
            "select role from case_members where case_id=? and user_id=? and status='ACTIVE'",
            caseId,
            actor());
    if (row.isEmpty()) throw new ApiException(404, "NOT_FOUND", "This case could not be found.");
    return row.getFirst().get("role").toString();
  }

  public void read(UUID caseId) {
    role(caseId);
  }

  public void write(UUID caseId) {
    if (role(caseId).equals("VIEWER"))
      throw new ApiException(403, "READ_ONLY", "You have view-only access.");
  }

  public void manage(UUID caseId) {
    if (!Set.of("CASE_OWNER", "FAMILY_ADMIN").contains(role(caseId)))
      throw new ApiException(
          403, "OWNER_REQUIRED", "A case owner or family administrator must do this.");
  }

  public void owner(UUID caseId) {
    if (!role(caseId).equals("CASE_OWNER"))
      throw new ApiException(403, "OWNER_REQUIRED", "The case owner must do this.");
  }

  public UUID providerCase(UUID id, boolean write) {
    var c = (UUID) db.one("select case_id from provider_cases where id=?", id).get("case_id");
    if (write) write(c);
    else read(c);
    return c;
  }

  public void admin() {
    if (!db.exists("select id from users where id=? and system_role='ADMIN'", actor()))
      throw new ApiException(403, "ADMIN_REQUIRED", "Administrator access required.");
  }
}
