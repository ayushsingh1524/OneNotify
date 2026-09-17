package in.onenotify;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import in.onenotify.auth.Access;
import in.onenotify.common.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthorizationTest {
  Db db = mock(Db.class);
  UUID actor = UUID.randomUUID(), caseId = UUID.randomUUID();
  Access access = new Access(db);

  @BeforeEach
  void authenticate() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(actor.toString(), null, List.of()));
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void missingMembershipDoesNotLeakCase() {
    when(db.list(anyString(), eq(caseId), eq(actor))).thenReturn(List.of());
    var e = assertThrows(ApiException.class, () -> access.read(caseId));
    assertEquals(404, e.status);
  }

  @Test
  void viewerCannotWriteOrApprove() {
    when(db.list(anyString(), eq(caseId), eq(actor))).thenReturn(List.of(Map.of("role", "VIEWER")));
    access.read(caseId);
    assertEquals(403, assertThrows(ApiException.class, () -> access.write(caseId)).status);
    assertThrows(ApiException.class, () -> access.manage(caseId));
  }

  @Test
  void contributorCannotGrantAccess() {
    when(db.list(anyString(), eq(caseId), eq(actor)))
        .thenReturn(List.of(Map.of("role", "CONTRIBUTOR")));
    access.write(caseId);
    assertThrows(ApiException.class, () -> access.manage(caseId));
  }
}
