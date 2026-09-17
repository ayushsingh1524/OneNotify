package in.onenotify.cases;

import in.onenotify.auth.Access;
import in.onenotify.common.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class OnboardingService {
  private final Db db;
  private final Access access;
  private final Json json;

  public OnboardingService(Db db, Access access, Json json) {
    this.db = db;
    this.access = access;
    this.json = json;
  }

  public Object get() {
    var rows = db.list("select data from onboarding_drafts where user_id=?", access.actor());
    return rows.isEmpty() ? Map.of() : json.read(rows.getFirst().get("data").toString());
  }

  public Object save(Map<String, Object> data) {
    String value = json.write(data);
    if (value.length() > 6000)
      throw new ApiException(400, "DRAFT_TOO_LARGE", "Please keep the draft concise.");
    db.update(
        "insert into onboarding_drafts(user_id,data) values(?,?::jsonb) on conflict(user_id) do update set data=excluded.data,updated_at=now()",
        access.actor(),
        value);
    return Map.of("ok", true);
  }

  public void clear() {
    db.update("delete from onboarding_drafts where user_id=?", access.actor());
  }

  public void duration(double seconds) {
    if (!Double.isFinite(seconds) || seconds < 0 || seconds > 86400)
      throw new ApiException(400, "INVALID_METRIC", "Invalid duration.");
    db.update(
        "insert into product_metrics(id,user_id,name,value) values(?,?,'case_creation_seconds',?)",
        UUID.randomUUID(),
        access.actor(),
        seconds);
  }
}
