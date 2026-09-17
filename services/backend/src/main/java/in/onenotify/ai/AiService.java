package in.onenotify.ai;

import java.util.*;

public interface AiService {
  Map<String, Object> classifyDocument(String filename, String text);

  Map<String, Object> extractMetadata(String text);

  Map<String, Object> extractRequirements(String instructions);

  Map<String, Object> classifyEmail(String text);

  String explain(String text);

  String draft(String type, String person, String provider);
}
