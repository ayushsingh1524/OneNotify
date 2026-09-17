package in.onenotify.integrations;

import in.onenotify.common.Db;
import java.util.concurrent.TimeUnit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EventDispatcher {
  private final Db db;
  private final KafkaTemplate<String, String> kafka;

  public EventDispatcher(Db db, KafkaTemplate<String, String> kafka) {
    this.db = db;
    this.kafka = kafka;
  }

  @Scheduled(fixedDelay = 3000)
  @Transactional
  public void publish() {
    for (var event :
        db.list(
            "select * from outbox_events where published_at is null and failed_at is null order by created_at limit 30 for update skip locked")) {
      try {
        kafka
            .send(
                event.get("topic").toString(),
                event.get("aggregate_id").toString(),
                event.get("payload").toString())
            .get(5, TimeUnit.SECONDS);
        db.update("update outbox_events set published_at=now() where id=?", event.get("id"));
      } catch (Exception e) {
        if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        db.update(
            "update outbox_events set attempts=attempts+1,failed_at=case when attempts>=9 then now() else null end where id=?",
            event.get("id"));
        org.slf4j.LoggerFactory.getLogger(getClass())
            .warn(
                "Event delivery retry eventId={} type={}",
                event.get("id"),
                e.getClass().getSimpleName());
      }
    }
  }
}
