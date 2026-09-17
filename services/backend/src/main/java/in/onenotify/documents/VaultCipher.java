package in.onenotify.documents;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VaultCipher {
  private final SecretKeySpec key;

  public VaultCipher(@Value("${app.vault-key}") String raw) {
    byte[] decoded = Base64.getDecoder().decode(raw);
    if (decoded.length != 32) throw new IllegalStateException("Vault key must be 32 bytes");
    key = new SecretKeySpec(decoded, "AES");
  }

  public byte[] encrypt(byte[] plain, String context) {
    try {
      byte[] iv = new byte[12];
      new SecureRandom().nextBytes(iv);
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
      c.updateAAD(context.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      byte[] result = c.doFinal(plain);
      return ByteBuffer.allocate(iv.length + result.length).put(iv).put(result).array();
    } catch (Exception e) {
      throw new IllegalStateException("Encryption failed", e);
    }
  }

  public byte[] decrypt(byte[] encrypted, String context) {
    try {
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(
          Cipher.DECRYPT_MODE,
          key,
          new GCMParameterSpec(128, Arrays.copyOfRange(encrypted, 0, 12)));
      c.updateAAD(context.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return c.doFinal(Arrays.copyOfRange(encrypted, 12, encrypted.length));
    } catch (Exception e) {
      throw new IllegalStateException("Document integrity check failed", e);
    }
  }
}
