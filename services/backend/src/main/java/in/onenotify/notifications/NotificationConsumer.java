package in.onenotify.notifications;

import in.onenotify.common.*;
import java.util.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationConsumer {
  private final Db db;
  private final Json json;
  private final NotificationChannel channel;

  public NotificationConsumer(Db db, Json json, NotificationChannel channel) {
    this.db = db;
    this.json = json;
    this.channel = channel;
  }

  @KafkaListener(
      topics = {
        "case.created",
        "document.uploaded",
        "provider-case.created",
        "workflow.status-changed",
        "task.created",
        "provider-response.received",
        "deadline.approaching",
        "notification.requested"
      })
  @Transactional
  public void receive(String payload) {
    var event = json.read(payload);
    if (event.path("version").asInt() != 1)
      throw new IllegalArgumentException("Unsupported event version");
    UUID id = UUID.fromString(event.path("eventId").asText());
    if (db.update(
            "insert into consumed_events(consumer,event_id) values('notifications-v1',?) on conflict do nothing",
            id)
        == 0) return;
    UUID caseId = UUID.fromString(event.path("caseId").asText());
    String title = event.path("title").asText("Your case has an update");
    for (var member :
        db.list(
            "select user_id from case_members where case_id=? and status='ACTIVE' and user_id is not null",
            caseId)) {
      UUID user = (UUID) member.get("user_id");
      db.update(
          "insert into notifications(id,user_id,case_id,title,body) values(?,?,?,?,?)",
          UUID.randomUUID(),
          user,
          caseId,
          title,
          "Open your case to review the latest activity.");
      channel.deliver(user, title, "");
    }
    org.slf4j.LoggerFactory.getLogger(getClass())
        .info("Domain event consumed eventId={} version=1", id);
  }
}
