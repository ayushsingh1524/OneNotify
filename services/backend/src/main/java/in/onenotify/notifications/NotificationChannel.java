package in.onenotify.notifications;

import java.util.UUID;

public interface NotificationChannel {
  enum Mode {
    IN_APP,
    EMAIL,
    SMS,
    WHATSAPP
  }

  void deliver(UUID user, String title, String body);
}
