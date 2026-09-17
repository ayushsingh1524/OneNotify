package in.onenotify.documents;

import io.minio.*;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MinioStorage implements ObjectStorage {
  private final MinioClient client;
  private final String bucket = "onenotify-vault";

  public MinioStorage(
      @Value("${app.minio-endpoint}") String endpoint,
      @Value("${app.minio-user}") String user,
      @Value("${app.minio-password}") String password) {
    client = MinioClient.builder().endpoint(endpoint).credentials(user, password).build();
  }

  public synchronized void put(String key, byte[] bytes) {
    try {
      if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))
        client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
      client.putObject(
          PutObjectArgs.builder().bucket(bucket).object(key).stream(
                  new ByteArrayInputStream(bytes), bytes.length, -1)
              .contentType("application/octet-stream")
              .build());
    } catch (Exception e) {
      throw new IllegalStateException("Encrypted storage unavailable", e);
    }
  }

  public byte[] get(String key) {
    try (var response =
        client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
      return response.readAllBytes();
    } catch (Exception e) {
      throw new IllegalStateException("Encrypted storage unavailable", e);
    }
  }

  public String signedCiphertextUrl(String key) {
    try {
      return client.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .bucket(bucket)
              .object(key)
              .method(Method.GET)
              .expiry(60)
              .build());
    } catch (Exception e) {
      throw new IllegalStateException("Storage unavailable", e);
    }
  }

  public void delete(String key) {
    try {
      client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
    } catch (Exception e) {
      throw new IllegalStateException("Storage unavailable", e);
    }
  }
}
