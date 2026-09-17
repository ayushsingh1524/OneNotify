package in.onenotify.audit;

import in.onenotify.common.*;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class Audit {
  private final Db db;
  private final Json json;

  public Audit(Db db, Json json) {
    this.db = db;
    this.json = json;
  }

  public void record(
      UUID actor,
      UUID caseId,
      UUID providerId,
      String action,
      String type,
      UUID entity,
      Object before,
      Object after) {
    db.update(
        "insert into audit_events(id,actor_id,case_id,provider_case_id,action,entity_type,entity_id,before_state,after_state,correlation_id) values(?,?,?,?,?,?,?,?::jsonb,?::jsonb,?)",
        UUID.randomUUID(),
        actor,
        caseId,
        providerId,
        action,
        type,
        entity,
        json.write(before),
        json.write(after),
        MDC.get("correlationId"));
  }
}
