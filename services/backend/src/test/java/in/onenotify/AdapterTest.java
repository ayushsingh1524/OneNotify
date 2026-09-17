package in.onenotify;

import static org.junit.jupiter.api.Assertions.*;

import in.onenotify.integrations.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AdapterTest {
  @Test
  void allNonSimulatedModesRequireManualAction() {
    var registry = new AdapterRegistry();
    var request =
        new ProviderAdapter.Request(
            UUID.randomUUID(), UUID.randomUUID(), List.of(UUID.randomUUID()), "NOTIFY_DEATH");
    for (var mode : ProviderAdapter.Mode.values()) {
      var result = registry.get(mode.name()).submit(request);
      if (mode == ProviderAdapter.Mode.SIMULATED) {
        assertTrue(result.simulated());
        assertEquals("SUBMITTED", result.status());
      } else {
        assertFalse(result.simulated());
        assertEquals("EXTERNAL_ACTION_REQUIRED", result.status());
      }
    }
  }
}
