package in.onenotify.common;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class Db {
  private final JdbcTemplate jdbc;

  public Db(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> list(String sql, Object... args) {
    return jdbc.queryForList(sql, args);
  }

  public Map<String, Object> one(String sql, Object... args) {
    var rows = list(sql, args);
    if (rows.isEmpty()) throw new ApiException(404, "NOT_FOUND", "This item could not be found.");
    return rows.getFirst();
  }

  public int update(String sql, Object... args) {
    return jdbc.update(sql, args);
  }

  public boolean exists(String sql, Object... args) {
    return !list(sql, args).isEmpty();
  }
}
