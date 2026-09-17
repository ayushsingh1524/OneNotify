package in.onenotify.ai;

import in.onenotify.audit.Audit;
import in.onenotify.auth.Access;
import in.onenotify.common.*;
import in.onenotify.documents.DocumentService;
import in.onenotify.integrations.EmailImport;
import in.onenotify.providers.ProviderService;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscoveryService {
  private final Db db;
  private final Access access;
  private final Audit audit;
  private final AiService ai;
  private final Json json;
  private final EmailImport emails;
  private final DocumentService documents;
  private final ProviderService providers;

  public DiscoveryService(
      Db db,
      Access access,
      Audit audit,
      AiService ai,
      Json json,
      EmailImport emails,
      DocumentService documents,
      ProviderService providers) {
    this.db = db;
    this.access = access;
    this.audit = audit;
    this.ai = ai;
    this.json = json;
    this.emails = emails;
    this.documents = documents;
    this.providers = providers;
  }

  public Object list(UUID caseId) {
    access.read(caseId);
    return db.list(
        "select d.*,p.name,p.category from discovered_accounts d join providers p on p.id=d.provider_id where case_id=? order by created_at desc",
        caseId);
  }

  @Transactional
  public Object sample(UUID caseId) {
    access.write(caseId);
    for (var email : emails.sampleMessages())
      discover(caseId, email.subject() + " " + email.text(), "SAMPLE_EMAIL");
    audit.record(
        access.actor(),
        caseId,
        null,
        "SAMPLE_EMAILS_ANALYZED",
        "CASE",
        caseId,
        Map.of(),
        Map.of("demo", true));
    return list(caseId);
  }

  private void discover(UUID caseId, String text, String source) {
    String lower = text.toLowerCase(Locale.ROOT);
    for (var p : db.list("select id,name from providers"))
      if (lower.contains(p.get("name").toString().toLowerCase(Locale.ROOT)))
        db.update(
            "insert into discovered_accounts(id,case_id,provider_id,source,hint) values(?,?,?,?,?) on conflict(case_id,provider_id) do nothing",
            UUID.randomUUID(),
            caseId,
            p.get("id"),
            source,
            "Name matched. This does not establish account ownership. Confirm with the family.");
  }

  @Transactional
  public Object analyze(UUID id) {
    var row = db.one("select case_id,filename from documents where id=?", id);
    UUID caseId = (UUID) row.get("case_id");
    access.write(caseId);
    var content = documents.authorizedContent(id);
    String text = "";
    if (content.mime().equals("application/pdf")) {
      try (var pdf = org.apache.pdfbox.Loader.loadPDF(content.bytes())) {
        var stripper = new org.apache.pdfbox.text.PDFTextStripper();
        stripper.setEndPage(Math.min(pdf.getNumberOfPages(), 20));
        text = stripper.getText(pdf);
      } catch (Exception e) {
        text = "";
      }
    }
    var output = new LinkedHashMap<String, Object>();
    output.put("classification", ai.classifyDocument(row.get("filename").toString(), text));
    output.put("metadata", ai.extractMetadata(text));
    discover(caseId, row.get("filename") + " " + text, "DOCUMENT_TEXT");
    db.update(
        "insert into ai_analysis(id,case_id,document_id,type,output) values(?,?,?,'DOCUMENT',?::jsonb)",
        UUID.randomUUID(),
        caseId,
        id,
        json.write(output));
    audit.record(
        access.actor(),
        caseId,
        null,
        "DOCUMENT_ANALYZED",
        "DOCUMENT",
        id,
        Map.of(),
        Map.of("engine", "DETERMINISTIC_MOCK"));
    return output;
  }

  @Transactional
  public Object status(UUID caseId, UUID id, String status) {
    access.write(caseId);
    if (!Set.of("CONFIRMED", "IGNORED").contains(status))
      throw new ApiException(400, "INVALID_STATUS", "Choose confirm or ignore.");
    var d =
        db.one(
            "select provider_id,status from discovered_accounts where id=? and case_id=? for update",
            id,
            caseId);
    if (status.equals("CONFIRMED")
        && !db.exists(
            "select id from provider_cases where case_id=? and provider_id=?",
            caseId,
            d.get("provider_id")))
      providers.add(caseId, new ProviderService.Add((UUID) d.get("provider_id"), "NOTIFY_DEATH"));
    db.update("update discovered_accounts set status=? where id=?", status, id);
    audit.record(
        access.actor(), caseId, null, "DISCOVERY_" + status, "DISCOVERY", id, Map.of(), Map.of());
    return list(caseId);
  }

  public Object assist(UUID pc, String type, String text) {
    UUID caseId = access.providerCase(pc, false);
    var row =
        db.one(
            "select d.full_name,p.name from provider_cases pc join providers p on p.id=pc.provider_id join deceased_profiles d on d.case_id=pc.case_id where pc.id=?",
            pc);
    return Map.of(
        "draft",
        ai.draft(type, row.get("full_name").toString(), row.get("name").toString()),
        "requirements",
        ai.extractRequirements(text),
        "explanation",
        ai.explain(text),
        "classification",
        ai.classifyEmail(text),
        "disclaimer",
        "Deterministic demo assistance. Review all suggestions. No legal eligibility or ownership decisions.");
  }
}
