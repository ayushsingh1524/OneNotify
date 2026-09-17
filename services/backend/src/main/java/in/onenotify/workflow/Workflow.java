package in.onenotify.workflow;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_instances")
public class Workflow {
  @Id public UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public WorkflowState state;

  @Version public long version;

  @Column(name = "updated_at")
  public Instant updatedAt;

  @Column(name = "submitted_at")
  public Instant submittedAt;

  @Column(name = "demo_step")
  public int demoStep;

  protected Workflow() {}

  public Workflow(UUID id) {
    this.id = id;
    state = WorkflowState.DRAFT;
    updatedAt = Instant.now();
  }
}
