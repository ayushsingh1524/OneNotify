-- Role is provisioned by staging init only; local demo remains unchanged.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='onenotify_runtime') THEN
    GRANT USAGE ON SCHEMA public TO onenotify_runtime;
    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO onenotify_runtime;
    GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO onenotify_runtime;
    REVOKE ALL ON flyway_schema_history FROM onenotify_runtime;
    REVOKE UPDATE, DELETE, TRUNCATE ON audit_events, workflow_events FROM onenotify_runtime;
    REVOKE CREATE ON SCHEMA public FROM PUBLIC;
  END IF;
END $$;
