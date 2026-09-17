package in.onenotify.consent;

import in.onenotify.audit.Audit;
import in.onenotify.auth.Access;
import in.onenotify.common.*;
import in.onenotify.providers.ProviderService;
import in.onenotify.workflow.*;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsentService {
  public record Approval(
      @NotNull UUID providerCaseId,
      @NotEmpty List<UUID> documentIds,
      @AssertTrue boolean approved) {}

  private final Db db;
  private final Access access;
  private final Audit audit;
  private final Json json;
  private final ProviderService providers;
  private final WorkflowTransitions transitions;

  public ConsentService(
      Db db,
      Access access,
      Audit audit,
      Json json,
      ProviderService providers,
      WorkflowTransitions transitions) {
    this.db = db;
    this.access = access;
    this.audit = audit;
    this.json = json;
    this.providers = providers;
    this.transitions = transitions;
  }

  @Transactional
  public Object approve(Approval r) {
    UUID caseId = access.providerCase(r.providerCaseId(), true);
    access.manage(caseId);
    var w = transitions.locked(r.providerCaseId());
    if (w.state != WorkflowState.USER_APPROVAL_REQUIRED)
      throw new ApiException(409, "NOT_READY", "Prepare the request before approving it.");
    var categories = new HashSet<String>();
    for (UUID id : r.documentIds()) {
      var d =
          db.one(
              "select category from documents where id=? and case_id=? and status='AVAILABLE' and (expires_at is null or expires_at>now())",
              id,
              caseId);
      categories.add(d.get("category").toString());
    }
    for (var req : providers.requirements(r.providerCaseId(), caseId))
      if (!categories.contains(req.get("category")))
        throw new ApiException(400, "DOCUMENT_MISSING", "Select every required document.");
    UUID id = UUID.randomUUID();
    db.update(
        "insert into consents(id,case_id,provider_case_id,actor_id,document_ids,purpose,shared_fields,expires_at) values(?,?,?,?,?::jsonb,?,?::jsonb,now()+interval '30 minutes')",
        id,
        caseId,
        r.providerCaseId(),
        access.actor(),
        json.write(r.documentIds()),
        "Provider-specific bereavement request",
        json.write(
            List.of("deceased full name", "date of death", "requester name", "requested action")));
    audit.record(
        access.actor(),
        caseId,
        r.providerCaseId(),
        "USER_APPROVED_SUBMISSION",
        "CONSENT",
        id,
        Map.of(),
        Map.of("documentCount", r.documentIds().size()));
    return Map.of("id", id, "expiresInSeconds", 1800);
  }

  public List<UUID> validate(UUID id, UUID pc, UUID caseId) {
    var c =
        db.one(
            "select * from consents where id=? and provider_case_id=? and actor_id=? and revoked_at is null and consumed_at is null and expires_at>now() for update",
            id,
            pc,
            access.actor());
    var docs = new ArrayList<UUID>();
    json.read(c.get("document_ids").toString()).forEach(n -> docs.add(UUID.fromString(n.asText())));
    for (var doc : docs)
      if (!db.exists(
          "select id from documents where id=? and case_id=? and status='AVAILABLE' and (expires_at is null or expires_at>now())",
          doc,
          caseId))
        throw new ApiException(
            409,
            "CONSENT_STALE",
            "A selected document is no longer available. Review your request again.");
    var categories = new HashSet<String>();
    for (var doc : docs)
      categories.add(
          db.one("select category from documents where id=?", doc).get("category").toString());
    for (var req : providers.requirements(pc, caseId))
      if (!categories.contains(req.get("category")))
        throw new ApiException(
            409, "CONSENT_STALE", "Requirements changed. Review your consent again.");
    return docs;
  }

  @Transactional
  public void revoke(UUID id) {
    var c = db.one("select case_id,provider_case_id from consents where id=?", id);
    UUID caseId = (UUID) c.get("case_id");
    access.manage(caseId);
    db.update("update consents set revoked_at=now() where id=? and consumed_at is null", id);
    audit.record(
        access.actor(),
        caseId,
        (UUID) c.get("provider_case_id"),
        "CONSENT_REVOKED",
        "CONSENT",
        id,
        Map.of(),
        Map.of());
  }
}
