package in.onenotify;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers(disabledWithoutDocker = true)
class PostgresSchemaTest {
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @Test
  void migrationsAndHistoryImmutability() throws Exception {
    try (var connection =
            DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var statement = connection.createStatement()) {
      statement.execute(
          "create role onenotify_runtime login password 'test-only-runtime-password' nosuperuser nocreatedb nocreaterole");
    }
    Flyway.configure()
        .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
        .load()
        .migrate();
    try (Connection c =
            DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement s = c.createStatement()) {
      s.execute(
          "insert into audit_events(id,action,entity_type) values('00000000-0000-0000-0000-000000000001','TEST','TEST')");
      assertThrows(
          SQLException.class, () -> s.execute("update audit_events set action='REWRITTEN'"));
      assertThrows(SQLException.class, () -> s.execute("delete from audit_events"));
      var r =
          s.executeQuery(
              "select count(*) from information_schema.tables where table_schema='public'");
      r.next();
      assertTrue(r.getInt(1) >= 28);
    }
    try (var connection =
            DriverManager.getConnection(
                postgres.getJdbcUrl(), "onenotify_runtime", "test-only-runtime-password");
        var statement = connection.createStatement()) {
      assertDoesNotThrow(() -> statement.executeQuery("select id from users"));
      assertDoesNotThrow(
          () ->
              statement.execute(
                  "insert into audit_events(id,action,entity_type) values(gen_random_uuid(),'RUNTIME_TEST','TEST')"));
      assertThrows(SQLException.class, () -> statement.execute("delete from audit_events"));
      assertThrows(SQLException.class, () -> statement.execute("truncate audit_events cascade"));
      assertThrows(
          SQLException.class,
          () -> statement.execute("alter table users add column insecure text"));
      assertThrows(
          SQLException.class, () -> statement.execute("delete from flyway_schema_history"));
    }
  }
}
