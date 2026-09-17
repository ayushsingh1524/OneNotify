package in.onenotify.integrations;

import java.util.List;

public interface EmailImport {
  record Email(String sender, String subject, String text) {}

  List<Email> sampleMessages();
}
