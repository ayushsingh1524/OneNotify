package in.onenotify.integrations;

import in.onenotify.common.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class Events {
  private final Db db;
  private final Json json;

  public Events(Db db, Json json) {
    this.db = db;
    this.json = json;
  }

  public void emit(String topic, UUID aggregate, UUID caseId, Map<String, ?> data) {
    UUID id = UUID.randomUUID();
    var body = new LinkedHashMap<String, Object>(data);
    body.put("eventId", id);
    body.put("version", 1);
    body.put("aggregateId", aggregate);
    body.put("caseId", caseId);
    db.update(
        "insert into outbox_events(id,aggregate_id,case_id,topic,payload) values(?,?,?,?,?::jsonb)",
        id,
        aggregate,
        caseId,
        topic,
        json.write(body));
  }
}
