package in.onenotify;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.onenotify.common.*;
import in.onenotify.notifications.*;
import org.junit.jupiter.api.Test;

class NotificationConsumerTest {
  @Test
  void duplicateDeliveryDoesNotNotifyAgain() {
    Db db = mock(Db.class);
    NotificationChannel channel = mock(NotificationChannel.class);
    var consumer = new NotificationConsumer(db, new Json(new ObjectMapper()), channel);
    consumer.receive("{\"eventId\":\"00000000-0000-0000-0000-000000000001\",\"version\":1}");
    verifyNoInteractions(channel);
    verify(db, never()).list(anyString(), any());
  }

  @Test
  void unknownVersionGoesToRetryAndDlq() {
    var consumer =
        new NotificationConsumer(
            mock(Db.class), new Json(new ObjectMapper()), mock(NotificationChannel.class));
    assertThrows(IllegalArgumentException.class, () -> consumer.receive("{\"version\":2}"));
  }
}
