package in.onenotify.export;

import in.onenotify.audit.Audit;
import in.onenotify.auth.Access;
import in.onenotify.common.*;
import in.onenotify.documents.DocumentService;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportService {
  private final Db db;
  private final Access access;
  private final PdfRenderer pdf;
  private final DocumentService documents;
  private final Audit audit;

  public ExportService(
      Db db, Access access, PdfRenderer pdf, DocumentService documents, Audit audit) {
    this.db = db;
    this.access = access;
    this.pdf = pdf;
    this.documents = documents;
    this.audit = audit;
  }

  @Transactional
  public byte[] summary(UUID caseId) {
    access.read(caseId);
    var person =
        db.one(
            "select full_name,date_of_death,city,state from deceased_profiles where case_id=?",
            caseId);
    var lines = new ArrayList<String>();
    lines.add("Case reference: " + caseId);
    lines.add("Family member: " + person.get("full_name"));
    lines.add("Date of death: " + person.get("date_of_death"));
    lines.add("Location: " + person.get("city") + ", " + person.get("state"));
    lines.add("");
    lines.add("Participating family members");
    db.list(
            "select u.name,m.role from case_members m join users u on u.id=m.user_id where m.case_id=? and m.status='ACTIVE'",
            caseId)
        .forEach(r -> lines.add(r.get("name") + " - " + r.get("role")));
    lines.add("");
    lines.add("Organization requests (DEMO rules are not official procedures)");
    db.list(
            "select p.name,pc.action,w.state from provider_cases pc join providers p on p.id=pc.provider_id join workflow_instances w on w.id=pc.id where pc.case_id=?",
            caseId)
        .forEach(r -> lines.add(r.get("name") + " | " + r.get("action") + " | " + r.get("state")));
    lines.add("Discovered relationships (ownership requires confirmation)");
    db.list(
            "select p.name,d.status from discovered_accounts d join providers p on p.id=d.provider_id where d.case_id=?",
            caseId)
        .forEach(r -> lines.add(r.get("name") + " | " + r.get("status")));
    lines.add("");
    lines.add("Document manifest");
    db.list("select filename,category,status from documents where case_id=?", caseId)
        .forEach(
            r ->
                lines.add(r.get("filename") + " | " + r.get("category") + " | " + r.get("status")));
    lines.add("");
    lines.add("Submission approvals");
    db.list(
            "select id,provider_case_id,created_at from consents where case_id=? and consumed_at is not null",
            caseId)
        .forEach(r -> lines.add(r.get("created_at") + " | consent reference " + r.get("id")));
    lines.add("");
    lines.add("Case timeline / audit references");
    db.list(
            "select id,action,created_at from audit_events where case_id=? order by created_at",
            caseId)
        .forEach(
            r -> lines.add(r.get("created_at") + " | " + r.get("action") + " | " + r.get("id")));
    audit.record(access.actor(), caseId, null, "CASE_EXPORTED", "CASE", caseId, Map.of(), Map.of());
    return pdf.render("OneNotify Case Summary", lines);
  }

  @Transactional
  public byte[] packageZip(UUID id) {
    UUID caseId = access.providerCase(id, false);
    var pc =
        db.one(
            "select p.name,p.notes,d.full_name,pc.action from provider_cases pc join providers p on p.id=pc.provider_id join deceased_profiles d on d.case_id=pc.case_id where pc.id=?",
            id);
    var lines = new ArrayList<String>();
    lines.add("DRAFT - review before sending. No information has been transmitted.");
    lines.add("Case reference: " + caseId);
    lines.add("Organization request: " + id);
    lines.add("To: " + pc.get("name"));
    lines.add("Re: " + pc.get("full_name") + " / " + pc.get("action"));
    lines.add("Please confirm your current bereavement procedure and required forms.");
    lines.add("This package is prepared by the family, not a legal representative.");
    lines.add("Official provider forms must be obtained directly from the provider.");
    lines.add(pc.get("notes").toString());
    lines.add("");
    lines.add("Document manifest");
    var docs =
        db.list(
            "select distinct on(d.category) d.id,d.filename,d.category,d.checksum from documents d join workflow_requirements r on r.category=d.category where d.case_id=? and r.provider_case_id=? and d.status='AVAILABLE' and (d.expires_at is null or d.expires_at>now()) order by d.category,d.uploaded_at desc",
            caseId,
            id);
    docs.forEach(
        d ->
            lines.add(
                d.get("filename") + " | " + d.get("category") + " | SHA256 " + d.get("checksum")));
    try (var out = new ByteArrayOutputStream();
        var zip = new ZipOutputStream(out)) {
      zip.putNextEntry(new ZipEntry("cover-letter-and-manifest.pdf"));
      zip.write(pdf.render("Provider Request Package", lines));
      zip.closeEntry();
      for (var d : docs) {
        var content = documents.authorizedContent((UUID) d.get("id"));
        zip.putNextEntry(new ZipEntry("documents/" + d.get("id") + "-" + content.name()));
        zip.write(content.bytes());
        zip.closeEntry();
      }
      zip.finish();
      audit.record(
          access.actor(),
          caseId,
          id,
          "PACKAGE_EXPORTED",
          "PROVIDER_CASE",
          id,
          Map.of(),
          Map.of("documentCount", docs.size()));
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Package generation failed", e);
    }
  }
}
