package in.onenotify.tasks;

import in.onenotify.audit.Audit;
import in.onenotify.auth.Access;
import in.onenotify.common.*;
import in.onenotify.integrations.Events;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {
  public record Create(
      @NotBlank @Size(max = 200) String title,
      UUID assignedTo,
      Instant dueAt,
      UUID providerCaseId) {}

  private final Db db;
  private final Access access;
  private final Audit audit;
  private final Events events;

  public TaskService(Db db, Access access, Audit audit, Events events) {
    this.db = db;
    this.access = access;
    this.audit = audit;
    this.events = events;
  }

  public Object list(UUID caseId) {
    access.read(caseId);
    return db.list(
        "select t.*,u.name as assigned_name from tasks t left join users u on u.id=t.assigned_to where t.case_id=? order by t.status,t.due_at nulls last,t.created_at",
        caseId);
  }

  @Transactional
  public Object create(UUID caseId, Create r) {
    access.write(caseId);
    if (r.assignedTo() != null
        && !db.exists(
            "select id from case_members where case_id=? and user_id=? and status='ACTIVE' and role<>'VIEWER'",
            caseId,
            r.assignedTo()))
      throw new ApiException(400, "INVALID_ASSIGNEE", "Choose an active contributor.");
    if (r.providerCaseId() != null
        && !db.exists(
            "select id from provider_cases where id=? and case_id=?", r.providerCaseId(), caseId))
      throw new ApiException(
          400, "INVALID_PROVIDER_CASE", "Choose an organization from this case.");
    UUID id = UUID.randomUUID();
    db.update(
        "insert into tasks(id,case_id,provider_case_id,title,assigned_to,due_at) values(?,?,?,?,?,?)",
        id,
        caseId,
        r.providerCaseId(),
        r.title(),
        r.assignedTo(),
        r.dueAt() == null ? null : java.sql.Timestamp.from(r.dueAt()));
    audit.record(
        access.actor(), caseId, r.providerCaseId(), "TASK_CREATED", "TASK", id, Map.of(), Map.of());
    events.emit("task.created", id, caseId, Map.of("title", "A family task was added"));
    return Map.of("id", id);
  }

  @Transactional
  public void status(UUID id, boolean done) {
    var task = db.one("select case_id,provider_case_id from tasks where id=?", id);
    UUID caseId = (UUID) task.get("case_id");
    access.write(caseId);
    db.update("update tasks set status=? where id=?", done ? "COMPLETED" : "OPEN", id);
    audit.record(
        access.actor(),
        caseId,
        (UUID) task.get("provider_case_id"),
        done ? "TASK_COMPLETED" : "TASK_REOPENED",
        "TASK",
        id,
        Map.of(),
        Map.of());
  }
}
