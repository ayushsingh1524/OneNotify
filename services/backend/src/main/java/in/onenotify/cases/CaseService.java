package in.onenotify.cases;

import in.onenotify.audit.Audit;
import in.onenotify.auth.Access;
import in.onenotify.common.*;
import in.onenotify.integrations.Events;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseService {
  public record Create(
      @NotBlank @Size(max = 120) String fullName,
      @Past LocalDate dateOfBirth,
      @NotNull @PastOrPresent LocalDate dateOfDeath,
      @NotBlank @Size(max = 120) String city,
      @NotBlank @Size(max = 120) String state,
      @NotBlank @Size(max = 60) String relationship,
      @Pattern(regexp = "[A-Za-z0-9]{4}|^$") String panLastFour,
      @Pattern(regexp = "[0-9]{4}|^$") String aadhaarLastFour,
      @Size(max = 500) String mobileNumbers,
      @Size(max = 500) String emailAddresses,
      @Size(max = 2000) String knownServices) {}

  private final Db db;
  private final Access access;
  private final Audit audit;
  private final Events events;

  public CaseService(Db db, Access access, Audit audit, Events events) {
    this.db = db;
    this.access = access;
    this.audit = audit;
    this.events = events;
  }

  public Object list() {
    return db.list(
        "select c.*,d.full_name,d.city,d.relationship from bereavement_cases c join deceased_profiles d on d.case_id=c.id join case_members m on m.case_id=c.id where m.user_id=? and m.status='ACTIVE' order by c.created_at desc",
        access.actor());
  }

  @Transactional
  public Map<String, Object> create(Create r) {
    if (r.dateOfBirth() != null && r.dateOfBirth().isAfter(r.dateOfDeath()))
      throw new ApiException(400, "INVALID_DATES", "Date of birth must be before date of death.");
    UUID id = UUID.randomUUID(), actor = access.actor();
    db.update(
        "insert into bereavement_cases(id,title,created_by) values(?,?,?)",
        id,
        r.fullName() + "’s case",
        actor);
    db.update(
        "insert into deceased_profiles(case_id,full_name,date_of_birth,date_of_death,city,state,relationship,pan_last_four,aadhaar_last_four,mobile_numbers,email_addresses,known_services) values(?,?,?,?,?,?,?,?,?,?,?,?)",
        id,
        r.fullName(),
        r.dateOfBirth(),
        r.dateOfDeath(),
        r.city(),
        r.state(),
        r.relationship(),
        r.panLastFour(),
        r.aadhaarLastFour(),
        Objects.toString(r.mobileNumbers(), ""),
        Objects.toString(r.emailAddresses(), ""),
        Objects.toString(r.knownServices(), ""));
    db.update(
        "insert into case_members(id,case_id,user_id,role) values(?,?,?,'CASE_OWNER')",
        UUID.randomUUID(),
        id,
        actor);
    audit.record(actor, id, null, "CASE_CREATED", "CASE", id, Map.of(), Map.of("status", "ACTIVE"));
    events.emit("case.created", id, id, Map.of("title", "Your case is ready"));
    return detail(id);
  }

  public Map<String, Object> detail(UUID id) {
    access.read(id);
    var data =
        new LinkedHashMap<>(
            db.one(
                "select c.*,d.full_name,d.date_of_birth,d.date_of_death,d.city,d.state,d.relationship,d.pan_last_four,d.aadhaar_last_four,d.mobile_numbers,d.email_addresses,d.known_services from bereavement_cases c join deceased_profiles d on d.case_id=c.id where c.id=?",
                id));
    data.put("role", access.role(id));
    data.put(
        "summary",
        db.one(
            "select count(*) as total,count(*) filter(where w.state='COMPLETED') as completed,count(*) filter(where w.state in ('SUBMITTED','PROVIDER_ACKNOWLEDGED','UNDER_REVIEW','RESUBMITTED')) as waiting,count(*) filter(where w.state in ('DOCUMENTS_PENDING','ADDITIONAL_DOCUMENTS_REQUIRED','USER_APPROVAL_REQUIRED','READY_TO_SUBMIT','ESCALATION_REQUIRED','EXTERNAL_ACTION_REQUIRED','FAILED','REJECTED')) as attention,count(*) filter(where w.state='DRAFT') as not_started from provider_cases p join workflow_instances w on w.id=p.id where p.case_id=?",
            id));
    data.put(
        "deadlines",
        db.list(
            "select d.*,p.name,pc.id as provider_case_id from deadlines d join provider_cases pc on pc.id=d.provider_case_id join providers p on p.id=pc.provider_id where pc.case_id=? and not d.resolved order by d.due_at",
            id));
    return data;
  }

  public Object timeline(UUID id) {
    access.read(id);
    return db.list(
        "select a.id,a.action,a.entity_type,a.entity_id,a.provider_case_id,a.created_at,u.name as actor from audit_events a left join users u on u.id=a.actor_id where a.case_id=? order by a.created_at desc limit 500",
        id);
  }

  @Transactional
  public Object retention(UUID id) {
    access.owner(id);
    db.update("update bereavement_cases set retention_requested_at=now() where id=?", id);
    audit.record(
        access.actor(), id, null, "CASE_DELETION_REQUESTED", "CASE", id, Map.of(), Map.of());
    return Map.of(
        "message",
        "Deletion requested. An operator must review retention obligations before erasure.");
  }

  @Transactional
  public Object update(UUID id, Create r) {
    access.manage(id);
    if (r.dateOfBirth() != null && r.dateOfBirth().isAfter(r.dateOfDeath()))
      throw new ApiException(400, "INVALID_DATES", "Check the dates.");
    db.update(
        "update deceased_profiles set full_name=?,date_of_birth=?,date_of_death=?,city=?,state=?,relationship=?,pan_last_four=?,aadhaar_last_four=?,mobile_numbers=?,email_addresses=?,known_services=? where case_id=?",
        r.fullName(),
        r.dateOfBirth(),
        r.dateOfDeath(),
        r.city(),
        r.state(),
        r.relationship(),
        r.panLastFour(),
        r.aadhaarLastFour(),
        Objects.toString(r.mobileNumbers(), ""),
        Objects.toString(r.emailAddresses(), ""),
        Objects.toString(r.knownServices(), ""),
        id);
    db.update("update bereavement_cases set title=? where id=?", r.fullName() + "’s case", id);
    audit.record(
        access.actor(),
        id,
        null,
        "PROFILE_UPDATED",
        "CASE",
        id,
        Map.of(),
        Map.of("fields", "profile"));
    return detail(id);
  }
}
