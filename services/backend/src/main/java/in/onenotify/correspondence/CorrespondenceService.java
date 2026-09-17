package in.onenotify.correspondence;

import in.onenotify.audit.Audit;
import in.onenotify.auth.Access;
import in.onenotify.common.*;
import in.onenotify.integrations.Events;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorrespondenceService {
  public record Create(
      UUID providerCaseId,
      @NotNull @Pattern(regexp = "NOTE|CALL_NOTE|EMAIL|LETTER") String kind,
      @NotBlank @Size(max = 200) String subject,
      @NotBlank @Size(max = 10000) String body,
      @Size(max = 120) String reference,
      List<UUID> attachments,
      List<UUID> mentions) {}

  private final Db db;
  private final Access access;
  private final Audit audit;
  private final Events events;

  public CorrespondenceService(Db db, Access access, Audit audit, Events events) {
    this.db = db;
    this.access = access;
    this.audit = audit;
    this.events = events;
  }

  public Object list(UUID caseId) {
    access.read(caseId);
    return db.list(
        "select c.*,u.name as author,p.name as provider_name from correspondence c left join users u on u.id=c.author_id left join provider_cases pc on pc.id=c.provider_case_id left join providers p on p.id=pc.provider_id where c.case_id=? order by c.created_at desc",
        caseId);
  }

  @Transactional
  public Object create(UUID caseId, Create r) {
    access.write(caseId);
    if (r.providerCaseId() != null && !caseId.equals(access.providerCase(r.providerCaseId(), true)))
      throw new ApiException(
          400, "INVALID_PROVIDER_CASE", "Choose an organization from this case.");
    if (r.attachments() != null)
      for (UUID doc : r.attachments())
        if (!db.exists(
            "select id from documents where id=? and case_id=? and status='AVAILABLE'",
            doc,
            caseId))
          throw new ApiException(400, "INVALID_ATTACHMENT", "Choose a document in this case.");
    if (r.mentions() != null)
      for (UUID user : r.mentions())
        if (!db.exists(
            "select id from case_members where user_id=? and case_id=? and status='ACTIVE'",
            user,
            caseId))
          throw new ApiException(400, "INVALID_MENTION", "Choose an active family member.");
    UUID id = UUID.randomUUID();
    db.update(
        "insert into correspondence(id,case_id,provider_case_id,author_id,kind,subject,body,reference) values(?,?,?,?,?,?,?,?)",
        id,
        caseId,
        r.providerCaseId(),
        access.actor(),
        r.kind(),
        r.subject(),
        r.body(),
        r.reference());
    if (r.attachments() != null)
      for (UUID doc : new HashSet<>(r.attachments()))
        db.update(
            "insert into correspondence_attachments(correspondence_id,document_id) values(?,?)",
            id,
            doc);
    if (r.mentions() != null)
      for (UUID user : new HashSet<>(r.mentions()))
        db.update(
            "insert into notifications(id,user_id,case_id,title,body) values(?,?,?,'You were mentioned in a case note','Open correspondence to review the note.')",
            UUID.randomUUID(),
            user,
            caseId);
    audit.record(
        access.actor(),
        caseId,
        r.providerCaseId(),
        "CORRESPONDENCE_ADDED",
        "CORRESPONDENCE",
        id,
        Map.of(),
        Map.of("kind", r.kind()));
    return Map.of("id", id);
  }
}
