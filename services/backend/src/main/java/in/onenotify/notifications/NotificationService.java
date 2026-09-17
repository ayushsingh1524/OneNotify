package in.onenotify.notifications;

import in.onenotify.auth.Access;
import in.onenotify.common.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
  private final Db db;
  private final Access access;

  public NotificationService(Db db, Access access) {
    this.db = db;
    this.access = access;
  }

  public Object list() {
    return db.list(
        "select n.* from notifications n where n.user_id=? and (n.case_id is null or exists(select 1 from case_members m where m.case_id=n.case_id and m.user_id=? and m.status='ACTIVE')) order by n.created_at desc limit 100",
        access.actor(),
        access.actor());
  }

  public void read(UUID id) {
    db.update(
        "update notifications set read_at=now() where id=? and user_id=?", id, access.actor());
  }
}
