package in.onenotify.common;

import in.onenotify.audit.Audit;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoSeed implements ApplicationRunner {
  private final Db db;
  private final PasswordEncoder passwords;
  private final boolean enabled;
  private final String email;
  private final String password;
  private final Audit audit;

  public DemoSeed(
      Db db,
      PasswordEncoder passwords,
      @Value("${app.demo-enabled}") boolean enabled,
      @Value("${app.demo-email}") String email,
      @Value("${app.demo-password}") String password,
      Audit audit) {
    this.db = db;
    this.passwords = passwords;
    this.enabled = enabled;
    this.email = email;
    this.password = password;
    this.audit = audit;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!enabled) return;
    db.update(
        "update users set email_verified=true where email in (?,?)", email, "mother@example.test");
    if (db.exists("select id from users where email=?", email)) return;
    UUID user = UUID.randomUUID();
    db.update(
        "insert into users(id,email,name,password_hash,email_verified,system_role) values(?,?,?,?,true,'ADMIN')",
        user,
        email,
        "Ayush Sharma",
        passwords.encode(password));
    UUID mother = UUID.randomUUID();
    db.update(
        "insert into users(id,email,name,password_hash,email_verified) values(?,'mother@example.test','Meera Sharma',?,true)",
        mother,
        passwords.encode(password));
    UUID caseId = UUID.randomUUID();
    db.update(
        "insert into bereavement_cases(id,title,created_by) values(?,'Rajesh Sharma’s case',?)",
        caseId,
        user);
    db.update(
        "insert into deceased_profiles(case_id,full_name,date_of_birth,date_of_death,city,state,relationship,known_services) values(?,'Rajesh Sharma','1961-04-12',current_date-14,'Pune','Maharashtra','Child','SBI, LIC, Airtel')",
        caseId);
    db.update(
        "insert into case_members(id,case_id,user_id,role) values(?,?,?,'CASE_OWNER'),(?,?,?,'CONTRIBUTOR')",
        UUID.randomUUID(),
        caseId,
        user,
        UUID.randomUUID(),
        caseId,
        mother);
    String[][] providers = {
      {"SBI", "Banking", "https://sbi.co.in"},
      {"HDFC Bank", "Banking", "https://www.hdfcbank.com"},
      {"LIC", "Insurance", "https://licindia.in"},
      {"Airtel", "Telecom", "https://www.airtel.in"},
      {"JioFiber", "Broadband", "https://www.jio.com"},
      {"Electricity provider DEMO", "Utilities", "https://example.com"},
      {"Mutual fund DEMO", "Investments", "https://example.com"},
      {"Demat provider DEMO", "Investments", "https://example.com"},
      {"Stream subscription DEMO", "Subscriptions", "https://example.com"},
      {"Employer DEMO", "Employment", "https://example.com"},
      {"Pension provider DEMO", "Pension", "https://example.com"},
      {"LPG provider DEMO", "Utilities", "https://example.com"}
    };
    for (int i = 0; i < providers.length; i++) {
      UUID p = UUID.randomUUID();
      var data = providers[i];
      db.update(
          "insert into providers(id,name,category,website,supported_actions,adapter_type) values(?,?,?,?,'NOTIFY_DEATH,REQUEST_INFORMATION,CLOSE_ACCOUNT,CLAIM_FUNDS,CANCEL_SUBSCRIPTION,TRANSFER_UTILITY',?)",
          p,
          data[0],
          data[1],
          data[2],
          i < 9 ? "SIMULATED" : "MANUAL");
      for (String category : List.of("DEATH_CERTIFICATE", "USER_IDENTITY"))
        db.update(
            "insert into provider_requirements(id,provider_id,category,description) values(?,?,?,?)",
            UUID.randomUUID(),
            p,
            category,
            "DEMO checklist only. Confirm current requirements with the provider.");
      db.update(
          "insert into provider_adapters(id,provider_id,mode,enabled) values(?,?,?,true)",
          UUID.randomUUID(),
          p,
          i < 9 ? "SIMULATED" : "MANUAL");
      if (i < 4) {
        UUID pc = UUID.randomUUID();
        db.update(
            "insert into provider_cases(id,case_id,provider_id,action,assigned_to) values(?,?,?,'NOTIFY_DEATH',?)",
            pc,
            caseId,
            p,
            i == 2 ? mother : user);
        db.update("insert into workflow_instances(id,state) values(?,'DRAFT')", pc);
        db.update(
            "insert into workflow_requirements(id,provider_case_id,category,description) select gen_random_uuid(),?,category,description from provider_requirements where provider_id=?",
            pc,
            p);
        audit.record(
            user,
            caseId,
            pc,
            "PROVIDER_ADDED",
            "PROVIDER_CASE",
            pc,
            Map.of(),
            Map.of("demo", true));
      }
    }
    db.update(
        "insert into tasks(id,case_id,title,assigned_to,due_at) values(?,?,'Add the death certificate when you are ready',?,now()+interval '3 days')",
        UUID.randomUUID(),
        caseId,
        user);
    db.update(
        "insert into tasks(id,case_id,title,assigned_to,due_at) values(?,?,'Check for policy documents at home',?,now()+interval '5 days')",
        UUID.randomUUID(),
        caseId,
        mother);
    audit.record(
        user, caseId, null, "CASE_CREATED", "CASE", caseId, Map.of(), Map.of("demo", true));
    db.update(
        "insert into notifications(id,user_id,case_id,title,body) values(?,?,?,'Your family workspace is ready','This is a demo case. Add a sample document to explore the workflow.')",
        UUID.randomUUID(),
        user,
        caseId);
  }
}
