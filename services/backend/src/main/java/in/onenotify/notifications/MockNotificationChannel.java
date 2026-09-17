package in.onenotify.notifications;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MockNotificationChannel implements NotificationChannel {
  public void deliver(UUID user, String title, String body) {
    org.slf4j.LoggerFactory.getLogger(getClass())
        .info("Mock outbound notification queued userId={} (no external message sent)", user);
  }
}
