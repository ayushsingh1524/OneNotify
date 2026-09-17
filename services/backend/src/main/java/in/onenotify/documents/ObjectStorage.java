package in.onenotify.documents;

public interface ObjectStorage {
  void put(String key, byte[] bytes);

  byte[] get(String key);

  String signedCiphertextUrl(String key);

  void delete(String key);
}
