package in.onenotify.integrations;

import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class AdapterRegistry {
  private final Map<ProviderAdapter.Mode, ProviderAdapter> adapters =
      new EnumMap<>(ProviderAdapter.Mode.class);

  public AdapterRegistry() {
    for (var mode : ProviderAdapter.Mode.values())
      adapters.put(mode, new ManualProviderAdapter(mode));
    adapters.put(ProviderAdapter.Mode.SIMULATED, new MockApiProviderAdapter());
    adapters.put(ProviderAdapter.Mode.EMAIL, new EmailProviderAdapter());
    adapters.put(ProviderAdapter.Mode.EXTERNAL_PORTAL, new ExternalPortalAdapter());
  }

  public ProviderAdapter get(String mode) {
    return adapters.get(ProviderAdapter.Mode.valueOf(mode));
  }
}
