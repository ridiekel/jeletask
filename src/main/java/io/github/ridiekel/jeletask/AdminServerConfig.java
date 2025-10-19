package io.github.ridiekel.jeletask;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "sba.server", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableAdminServer
public class AdminServerConfig {
}
