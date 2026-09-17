package in.onenotify.integrations;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SampleEmailImport implements EmailImport {
  public List<Email> sampleMessages() {
    return List.of(
        new Email(
            "sample@example.test",
            "DEMO LIC premium reminder",
            "LIC policy premium reminder; ownership unconfirmed"),
        new Email(
            "sample@example.test",
            "DEMO JioFiber bill",
            "JioFiber monthly bill; ownership unconfirmed"),
        new Email(
            "sample@example.test",
            "DEMO HDFC Bank statement",
            "HDFC Bank statement notification; ownership unconfirmed"));
  }
}
