package in.onenotify;

import static org.junit.jupiter.api.Assertions.*;

import in.onenotify.auth.TokenService;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TokenServiceTest {
  @Test
  void bindsIdentityToRevocableSession() {
    var tokens = new TokenService("a".repeat(64));
    var user = UUID.randomUUID();
    var session = UUID.randomUUID();
    var verified = tokens.verify(tokens.issue(user, session));
    assertEquals(user, verified.user());
    assertEquals(session, verified.session());
  }

  @Test
  void rejectsWrongSigningKey() {
    var token = new TokenService("a".repeat(64)).issue(UUID.randomUUID(), UUID.randomUUID());
    assertThrows(Exception.class, () -> new TokenService("b".repeat(64)).verify(token));
  }
}
