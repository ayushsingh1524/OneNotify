package in.onenotify.providers;

import in.onenotify.audit.Audit;
import in.onenotify.auth.Access;
import in.onenotify.common.*;
import in.onenotify.integrations.Events;
import in.onenotify.workflow.*;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderService {
  public record Add(@NotNull UUID providerId, @NotBlank @Size(max = 60) String action) {}

  private final Db db;
  private final Access access;
  private final WorkflowRepository workflows;
  private final Audit audit;
  private final Events events;

  public ProviderService(
      Db db, Access access, WorkflowRepository workflows, Audit audit, Events events) {
    this.db = db;
    this.access = access;
    this.workflows = workflows;
    this.audit = audit;
    this.events = events;
  }

  public Object registry() {
    access.actor();
    return db.list("select * from providers order by name");
  }

  public Object list(UUID caseId) {
    access.read(caseId);
    return db.list(
        "select pc.*,p.name,p.category,p.adapter_type,p.demo,w.state,w.updated_at,u.name as assigned_name,(select min(d.due_at) from deadlines d where d.provider_case_id=pc.id and not d.resolved) as due_at from provider_cases pc join providers p on p.id=pc.provider_id join workflow_instances w on w.id=pc.id left join users u on u.id=pc.assigned_to where pc.case_id=? order by pc.created_at",
        caseId);
  }

  @Transactional
  public Object add(UUID caseId, Add r) {
    access.write(caseId);
    var p = db.one("select * from providers where id=?", r.providerId());
    if (!Arrays.asList(p.get("supported_actions").toString().split(",")).contains(r.action()))
      throw new ApiException(400, "UNSUPPORTED_ACTION", "Choose a supported action.");
    UUID id = UUID.randomUUID();
    db.update(
        "insert into provider_cases(id,case_id,provider_id,action) values(?,?,?,?)",
        id,
        caseId,
        r.providerId(),
        r.action());
    db.update("update bereavement_cases set status='ACTIVE' where id=?", caseId);
    workflows.saveAndFlush(new Workflow(id));
    db.update(
        "insert into workflow_requirements(id,provider_case_id,category,description) select gen_random_uuid(),?,category,description from provider_requirements where provider_id=?",
        id,
        r.providerId());
    audit.record(
        access.actor(),
        caseId,
        id,
        "PROVIDER_ADDED",
        "PROVIDER_CASE",
        id,
        Map.of(),
        Map.of("provider", p.get("name")));
    events.emit(
        "provider-case.created",
        id,
        caseId,
        Map.of("title", "Organization added to your checklist"));
    return detail(id);
  }

  public Map<String, Object> detail(UUID id) {
    UUID caseId = access.providerCase(id, false);
    var row =
        new LinkedHashMap<>(
            db.one(
                "select pc.*,p.name,p.category,p.adapter_type,p.demo,p.website,p.notes,p.estimated_days,p.additional_rules,w.state,w.updated_at,w.version,w.demo_step from provider_cases pc join providers p on p.id=pc.provider_id join workflow_instances w on w.id=pc.id where pc.id=?",
                id));
    row.put("requirements", requirements(id, caseId));
    row.put(
        "timeline",
        db.list(
            "select e.*,u.name as actor from workflow_events e left join users u on u.id=e.actor_id where provider_case_id=? order by created_at",
            id));
    row.put(
        "deadlines",
        db.list(
            "select * from deadlines where provider_case_id=? and not resolved order by due_at",
            id));
    row.put(
        "correspondence",
        db.list(
            "select c.*,u.name as author from correspondence c left join users u on u.id=c.author_id where provider_case_id=? order by created_at",
            id));
    return row;
  }

  public List<Map<String, Object>> requirements(UUID id, UUID caseId) {
    return db.list(
        "select r.category,r.description,exists(select 1 from documents d where d.case_id=? and d.category=r.category and d.status='AVAILABLE' and (d.expires_at is null or d.expires_at>now())) as available from workflow_requirements r where r.provider_case_id=? order by category",
        caseId,
        id);
  }

  @Transactional
  public void assign(UUID id, UUID user) {
    UUID caseId = access.providerCase(id, true);
    if (user != null
        && !db.exists(
            "select id from case_members where case_id=? and user_id=? and status='ACTIVE' and role<>'VIEWER'",
            caseId,
            user))
      throw new ApiException(
          400, "INVALID_ASSIGNEE", "Choose an active family member who can edit.");
    db.update("update provider_cases set assigned_to=? where id=?", user, id);
    audit.record(
        access.actor(), caseId, id, "TASK_ASSIGNED", "PROVIDER_CASE", id, Map.of(), Map.of());
    events.emit("task.created", id, caseId, Map.of("title", "An organization task was assigned"));
  }
}
