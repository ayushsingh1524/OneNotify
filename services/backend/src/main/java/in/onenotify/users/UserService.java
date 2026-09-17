package in.onenotify.users;

import in.onenotify.auth.Access;
import in.onenotify.common.Db;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final Db db;
  private final Access access;

  public UserService(Db db, Access access) {
    this.db = db;
    this.access = access;
  }

  public Map<String, Object> currentProfile() {
    return db.one("select id,name,email,system_role from users where id=?", access.actor());
  }
}
