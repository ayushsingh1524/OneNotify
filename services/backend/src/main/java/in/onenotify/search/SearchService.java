package in.onenotify.search;

import in.onenotify.auth.Access;
import in.onenotify.common.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class SearchService {
  private final Db db;
  private final Access access;

  public SearchService(Db db, Access access) {
    this.db = db;
    this.access = access;
  }

  public Object search(UUID caseId, String query) {
    access.read(caseId);
    if (query.length() < 2 || query.length() > 120) return List.of();
    String q = "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    var out = new ArrayList<Map<String, Object>>();
    out.addAll(
        db.list(
            "select pc.id,p.name as title,'organizations' as type from provider_cases pc join providers p on p.id=pc.provider_id where pc.case_id=? and p.name ilike ? limit 20",
            caseId,
            q));
    out.addAll(
        db.list(
            "select id,filename as title,'documents' as type from documents where case_id=? and filename ilike ? limit 20",
            caseId,
            q));
    out.addAll(
        db.list(
            "select id,title,'tasks' as type from tasks where case_id=? and title ilike ? limit 20",
            caseId,
            q));
    out.addAll(
        db.list(
            "select id,subject as title,'correspondence' as type from correspondence where case_id=? and (subject ilike ? or body ilike ?) limit 20",
            caseId,
            q,
            q));
    out.addAll(
        db.list(
            "select m.id,u.name as title,'family' as type from case_members m join users u on u.id=m.user_id where m.case_id=? and m.status='ACTIVE' and u.name ilike ? limit 20",
            caseId,
            q));
    out.addAll(
        db.list(
            "select id,action as title,'timeline' as type from audit_events where case_id=? and action ilike ? limit 20",
            caseId,
            q));
    return out;
  }
}
