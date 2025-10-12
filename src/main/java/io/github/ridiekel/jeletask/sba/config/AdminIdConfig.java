package io.github.ridiekel.jeletask.sba.config;

import de.codecentric.boot.admin.server.domain.values.InstanceId;
import de.codecentric.boot.admin.server.domain.values.Registration;
import de.codecentric.boot.admin.server.services.InstanceIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminIdConfig {
  @Bean
  public InstanceIdGenerator instanceIdGenerator() {
    return (Registration registration) -> {
      // Kies je beleid: uit metadata, anders uit name/host
      String id = registration.getMetadata().getOrDefault(
          "instanceId",
          registration.getName()  // of combineer met host/port voor uniciteit
      );
      return InstanceId.of(id);
    };
  }
}
