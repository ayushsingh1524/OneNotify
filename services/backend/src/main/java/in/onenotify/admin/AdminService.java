package in.onenotify.admin;

import in.onenotify.audit.Audit;
import in.onenotify.auth.Access;
import in.onenotify.common.*;
import in.onenotify.documents.DocumentService;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
  public record Provider(
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Size(max = 60) String category,
      @Pattern(regexp = "https://.*") @NotNull String website,
      @Pattern(regexp = "API|EMAIL|FORM|MANUAL|EXTERNAL_PORTAL|SIMULATED") @NotNull
          String adapterType,
      @Min(1) @Max(365) int estimatedDays,
      @NotEmpty List<String> requirements,
      @Size(max = 2000) String notes) {}

  private final Db db;
  private final Access access;
  private final Audit audit;
  private final Json json;

  public AdminService(Db db, Access access, Audit audit, Json json) {
    this.db = db;
    this.access = access;
    this.audit = audit;
    this.json = json;
  }

  public Object overview() {
    access.admin();
    return Map.of(
        "providers",
        db.list(
            "select p.*,(select string_agg(r.category,',') from provider_requirements r where r.provider_id=p.id) as requirements from providers p order by name"),
        "failedJobs",
        db.list(
            "select id,topic,attempts,created_at,failed_at from outbox_events where failed_at is not null order by created_at desc limit 100"),
        "events",
        db.one(
            "select count(*) as total,count(*) filter(where published_at is not null) as published,count(*) filter(where published_at is null) as pending from outbox_events"),
        "configuration",
        db.list("select key,value::text as value from admin_configuration"));
  }

  @Transactional
  public Object provider(UUID id, Provider r) {
    access.admin();
    if (!DocumentService.CATEGORIES.containsAll(r.requirements()))
      throw new ApiException(400, "INVALID_CATEGORY", "Choose supported document categories.");
    if (id == null) {
      id = UUID.randomUUID();
      db.update(
          "insert into providers(id,name,category,website,adapter_type,estimated_days,supported_actions,notes) values(?,?,?,?,?,?,'NOTIFY_DEATH,REQUEST_INFORMATION,CLOSE_ACCOUNT,CLAIM_FUNDS,CANCEL_SUBSCRIPTION,TRANSFER_UTILITY',?)",
          id,
          r.name(),
          r.category(),
          r.website(),
          r.adapterType(),
          r.estimatedDays(),
          "DEMO: " + Objects.toString(r.notes(), ""));
    } else {
      db.update(
          "update providers set name=?,category=?,website=?,adapter_type=?,estimated_days=?,notes=?,demo=true,last_verified_at=null where id=?",
          r.name(),
          r.category(),
          r.website(),
          r.adapterType(),
          r.estimatedDays(),
          "DEMO: " + Objects.toString(r.notes(), ""),
          id);
      db.update("delete from provider_requirements where provider_id=?", id);
    }
    for (String category : new HashSet<>(r.requirements()))
      db.update(
          "insert into provider_requirements(id,provider_id,category,description) values(?,?,?,?)",
          UUID.randomUUID(),
          id,
          category,
          "DEMO requirement. Verify with provider.");
    audit.record(
        access.actor(),
        null,
        null,
        "REGISTRY_UPDATED",
        "PROVIDER",
        id,
        Map.of(),
        Map.of("demo", true));
    return Map.of("id", id);
  }

  @Transactional
  public void retry(UUID id) {
    access.admin();
    db.update(
        "update outbox_events set failed_at=null,attempts=0 where id=? and published_at is null",
        id);
    audit.record(
        access.actor(), null, null, "EVENT_RETRY_REQUESTED", "EVENT", id, Map.of(), Map.of());
  }

  @Transactional
  public Object config(String key, Map<String, Object> value) {
    access.admin();
    if (!Set.of("categories", "translations.en", "translations.hi", "workflow-template")
        .contains(key))
      throw new ApiException(400, "INVALID_CONFIG", "Unsupported configuration key.");
    db.update(
        "insert into admin_configuration(key,value) values(?,?::jsonb) on conflict(key) do update set value=excluded.value,updated_at=now()",
        key,
        json.write(value));
    audit.record(
        access.actor(), null, null, "CONFIG_UPDATED", "CONFIG", null, Map.of(), Map.of("key", key));
    return Map.of("ok", true);
  }

  public Object metrics() {
    access.admin();
    return db.one(
        "select (select avg(value) from product_metrics where name='case_creation_seconds') as average_case_creation_seconds,(select count(*) from bereavement_cases) as cases,(select count(*) from workflow_instances where state='COMPLETED') as providers_resolved,(select count(*) from workflow_instances where state in ('DOCUMENTS_PENDING','ADDITIONAL_DOCUMENTS_REQUIRED','USER_APPROVAL_REQUIRED')) as action_backlog,(select count(*) from deadlines where due_at<now() and not resolved) as overdue,(select avg(extract(epoch from(w.submitted_at-c.created_at))) from workflow_instances w join provider_cases pc on pc.id=w.id join bereavement_cases c on c.id=pc.case_id) as seconds_to_first_submission,(select avg(extract(epoch from(w.updated_at-p.created_at))) from workflow_instances w join provider_cases p on p.id=w.id where w.state='COMPLETED') as average_workflow_seconds,(select count(*)::numeric/nullif((select count(*) from bereavement_cases),0) from workflow_instances where state not in ('COMPLETED','CANCELLED')) as average_unresolved,(select coalesce(sum(jsonb_array_length(document_ids)),0) from consents where consumed_at is not null)-(select count(distinct elem) from consents c cross join lateral jsonb_array_elements_text(c.document_ids) elem where consumed_at is not null) as repeated_uploads_avoided");
  }
}
