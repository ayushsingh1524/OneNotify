package in.onenotify.integrations;

import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {
  @Bean
  KafkaAdmin.NewTopics topics() {
    return new KafkaAdmin.NewTopics(
        java.util.stream.Stream.of(
                "case.created",
                "document.uploaded",
                "provider-case.created",
                "workflow.status-changed",
                "task.created",
                "provider-response.received",
                "deadline.approaching",
                "notification.requested",
                "audit.event")
            .flatMap(n -> java.util.stream.Stream.of(n, n + ".DLT"))
            .map(n -> TopicBuilder.name(n).partitions(1).replicas(1).build())
            .toArray(org.apache.kafka.clients.admin.NewTopic[]::new));
  }

  @Bean
  CommonErrorHandler kafkaErrors(KafkaTemplate<String, String> kafka) {
    return new DefaultErrorHandler(
        new DeadLetterPublishingRecoverer(kafka), new FixedBackOff(2000, 3));
  }
}
