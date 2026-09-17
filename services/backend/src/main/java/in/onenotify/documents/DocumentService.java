package in.onenotify.documents;

import in.onenotify.audit.Audit;
import in.onenotify.auth.*;
import in.onenotify.common.*;
import in.onenotify.integrations.Events;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {
  public static final Set<String> CATEGORIES =
      Set.of(
          "DEATH_CERTIFICATE",
          "USER_IDENTITY",
          "DECEASED_IDENTITY",
          "NOMINEE_DOCUMENT",
          "LEGAL_HEIR_CERTIFICATE",
          "SUCCESSION_CERTIFICATE",
          "MARRIAGE_CERTIFICATE",
          "ACCOUNT_DOCUMENT",
          "POLICY_DOCUMENT",
          "BANK_STATEMENT",
          "INVESTMENT_STATEMENT",
          "UTILITY_BILL",
          "EMPLOYER_DOCUMENT",
          "CORRESPONDENCE",
          "OTHER");
  private final Db db;
  private final Access access;
  private final ObjectStorage storage;
  private final VaultCipher cipher;
  private final MalwareScanner scanner;
  private final Audit audit;
  private final Events events;
  private final StringRedisTemplate redis;
  private final Json json;

  public DocumentService(
      Db db,
      Access access,
      ObjectStorage storage,
      VaultCipher cipher,
      MalwareScanner scanner,
      Audit audit,
      Events events,
      StringRedisTemplate redis,
      Json json) {
    this.db = db;
    this.access = access;
    this.storage = storage;
    this.cipher = cipher;
    this.scanner = scanner;
    this.audit = audit;
    this.events = events;
    this.redis = redis;
    this.json = json;
  }

  public Object list(UUID caseId) {
    access.read(caseId);
    return db.list(
        "select id,case_id,filename,category,mime_type,checksum,uploaded_by,uploaded_at,version,is_original,status,size_bytes,expires_at,extracted_metadata::text as metadata from documents where case_id=? order by uploaded_at desc",
        caseId);
  }

  @Transactional
  public Object upload(
      UUID caseId, String category, MultipartFile file, java.time.LocalDate expiresOn) {
    if (expiresOn != null && expiresOn.isBefore(java.time.LocalDate.now()))
      throw new ApiException(
          400, "EXPIRED_DOCUMENT", "Choose a current document or leave expiry empty.");
    access.write(caseId);
    db.one("select id from bereavement_cases where id=? for update", caseId);
    if (!CATEGORIES.contains(category))
      throw new ApiException(400, "INVALID_CATEGORY", "Choose a document category.");
    if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024)
      throw new ApiException(400, "INVALID_SIZE", "Choose a file between 1 byte and 10 MB.");
    try {
      byte[] bytes = file.getBytes();
      String mime = detect(bytes);
      if (!mime.equals(file.getContentType()))
        throw new ApiException(
            400, "INVALID_FILE", "Only genuine PDF, PNG and JPEG files are accepted.");
      var scan = scanner.scan(bytes);
      if (!scan.accepted())
        throw new ApiException(
            422, "SCAN_REJECTED", "The file could not pass the configured safety check.");
      String checksum =
          HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
      UUID id = UUID.randomUUID();
      String key = caseId + "/" + id;
      String name =
          Objects.toString(file.getOriginalFilename(), "document")
              .replaceAll("[^\\p{L}\\p{N}._ -]", "_");
      if (name.length() > 200) name = name.substring(name.length() - 200);
      int version =
          ((Number)
                  db.one(
                          "select coalesce(max(version),0)+1 as version from documents where case_id=? and category=?",
                          caseId,
                          category)
                      .get("version"))
              .intValue();
      storage.put(key, cipher.encrypt(bytes, key));
      db.update(
          "insert into documents(id,case_id,filename,category,mime_type,checksum,storage_path,uploaded_by,version,size_bytes,extracted_metadata) values(?,?,?,?,?,?,?,?,?,?,?::jsonb)",
          id,
          caseId,
          name,
          category,
          mime,
          checksum,
          key,
          access.actor(),
          version,
          bytes.length,
          json.write(
              Map.of(
                  "scanner", scan.engine(), "classification", "USER_SELECTED", "ocr", "NOT_RUN")));
      if (expiresOn != null)
        db.update(
            "update documents set expires_at=? where id=?",
            java.sql.Timestamp.from(
                expiresOn
                    .plusDays(1)
                    .atStartOfDay(java.time.ZoneId.of("Asia/Kolkata"))
                    .toInstant()),
            id);
      db.update(
          "insert into document_versions(id,document_id,version,storage_path,checksum) values(?,?,?,?,?)",
          UUID.randomUUID(),
          id,
          version,
          key,
          checksum);
      audit.record(
          access.actor(),
          caseId,
          null,
          "DOCUMENT_UPLOADED",
          "DOCUMENT",
          id,
          Map.of(),
          Map.of("category", category, "version", version));
      events.emit(
          "document.uploaded", id, caseId, Map.of("title", "A document was added to the vault"));
      return Map.of("id", id, "checksum", checksum, "version", version);
    } catch (java.io.IOException | java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException("Upload failed", e);
    }
  }

  public static String detect(byte[] b) {
    if (b.length >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-')
      return "application/pdf";
    if (b.length >= 8
        && b[0] == (byte) 137
        && b[1] == 80
        && b[2] == 78
        && b[3] == 71
        && b[4] == 13
        && b[5] == 10
        && b[6] == 26
        && b[7] == 10) return "image/png";
    if (b.length >= 3 && b[0] == (byte) 255 && b[1] == (byte) 216 && b[2] == (byte) 255)
      return "image/jpeg";
    throw new ApiException(400, "INVALID_FILE", "Only PDF, PNG and JPEG files are supported.");
  }

  public Object ticket(UUID id) {
    var row = db.one("select case_id from documents where id=? and status='AVAILABLE'", id);
    access.read((UUID) row.get("case_id"));
    String token = AuthService.random();
    redis
        .opsForValue()
        .set("vault:" + AuthService.hash(token), access.actor() + ":" + id, Duration.ofSeconds(60));
    return Map.of("ticket", token, "expiresInSeconds", 60);
  }

  public record Content(byte[] bytes, String mime, String name) {}

  @Transactional
  public Content content(UUID id, String ticket) {
    String value = redis.opsForValue().getAndDelete("vault:" + AuthService.hash(ticket));
    if (!Objects.equals(value, access.actor() + ":" + id))
      throw new ApiException(403, "LINK_EXPIRED", "This link expired. Open the document again.");
    return authorizedContent(id);
  }

  public Content authorizedContent(UUID id) {
    var row = db.one("select * from documents where id=? and status='AVAILABLE'", id);
    UUID caseId = (UUID) row.get("case_id");
    access.read(caseId);
    String key = row.get("storage_path").toString();
    byte[] plain = cipher.decrypt(storage.get(key), key);
    db.update(
        "insert into document_access_logs(id,document_id,actor_id,action) values(?,?,?,'READ')",
        UUID.randomUUID(),
        id,
        access.actor());
    audit.record(
        access.actor(), caseId, null, "DOCUMENT_VIEWED", "DOCUMENT", id, Map.of(), Map.of());
    return new Content(plain, row.get("mime_type").toString(), row.get("filename").toString());
  }

  @Transactional
  public void deletion(UUID id) {
    var row = db.one("select case_id from documents where id=?", id);
    UUID caseId = (UUID) row.get("case_id");
    access.manage(caseId);
    db.update("update documents set status='DELETION_REQUESTED' where id=?", id);
    audit.record(
        access.actor(),
        caseId,
        null,
        "DOCUMENT_DELETION_REQUESTED",
        "DOCUMENT",
        id,
        Map.of(),
        Map.of());
  }
}
