package in.onenotify.workflow;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select w from Workflow w where w.id=:id")
  Optional<Workflow> lock(@Param("id") UUID id);
}
