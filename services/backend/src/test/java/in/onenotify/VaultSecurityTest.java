package in.onenotify;

import static org.junit.jupiter.api.Assertions.*;

import in.onenotify.common.ApiException;
import in.onenotify.documents.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class VaultSecurityTest {
  private final VaultCipher cipher =
      new VaultCipher(Base64.getEncoder().encodeToString(new byte[32]));

  @Test
  void encryptsWithFreshNonceAndChecksContext() {
    byte[] original = "sensitive sample".getBytes();
    byte[] a = cipher.encrypt(original, "case/document");
    byte[] b = cipher.encrypt(original, "case/document");
    assertFalse(Arrays.equals(a, b));
    assertArrayEquals(original, cipher.decrypt(a, "case/document"));
    assertThrows(IllegalStateException.class, () -> cipher.decrypt(a, "other-case/document"));
    a[15] ^= 1;
    assertThrows(IllegalStateException.class, () -> cipher.decrypt(a, "case/document"));
  }

  @Test
  void doesNotAcceptHtmlDisguisedAsPdf() {
    assertThrows(
        ApiException.class, () -> DocumentService.detect("<html>not a PDF</html>".getBytes()));
    assertEquals("application/pdf", DocumentService.detect("%PDF-1.4 example".getBytes()));
  }

  @Test
  void productionFailsClosedWithoutScanner() {
    assertFalse(new DemoMalwareScanner(false).scan("sample".getBytes()).accepted());
    assertFalse(
        new DemoMalwareScanner(true)
            .scan("EICAR-STANDARD-ANTIVIRUS-TEST-FILE".getBytes())
            .accepted());
  }
}
