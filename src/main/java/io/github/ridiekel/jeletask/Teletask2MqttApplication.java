package io.github.ridiekel.jeletask;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.UUID;

@SpringBootApplication
@EnableConfigurationProperties({Teletask2MqttConfigurationProperties.class})
@EnableScheduling
@EnableAdminServer
public class Teletask2MqttApplication {
    private static final Logger LOG = LogManager.getLogger();

    static {
        if (System.getenv("SPRING_SECURITY_USER_NAME") == null) {
            String username = UUID.randomUUID().toString();
            LOG.info(() -> String.format("Web admin user not set! Please use the environment variable 'SPRING_SECURITY_USER_NAME' to set the username. Now using random username: %s", username));
            System.setProperty("spring.security.user.name", username);
        }
        if (System.getenv("SPRING_SECURITY_USER_PASSWORD") == null) {
            String password = UUID.randomUUID().toString();
            LOG.info(() -> String.format("Web admin password not set! Please use the environment variable 'SPRING_SECURITY_USER_PASSWORD' to set the password. Now using random password: %s", password));
            System.setProperty("spring.security.user.password", password);
        }
    }

    public static void main(String[] args) {
        handleDeprecations();

        ApplicationContext context = SpringApplication.run(Teletask2MqttApplication.class, args);

        Teletask2MqttConfigurationProperties configuration = context.getBean(Teletask2MqttConfigurationProperties.class);

        LOG.info(() -> String.format("Teletask2Mqtt %s started!", configuration.getCentral().getVersion()));
    }

    public static void handleDeprecations() {
        copyDeprecatedVariable("TELETASK_HOST");
        copyDeprecatedVariable("TELETASK_PORT");
        copyDeprecatedVariable("TELETASK_ID");
    }

    private static void copyDeprecatedVariable(String variable) {
        String value = System.getenv(variable);
        if (value != null) {
            String newVariable = variable.replaceAll("TELETASK", "TELETASK_CENTRAL");
            LOG.warn(() -> String.format("Environment variable '%s' is being used, please update your config to use '%s'. Support for '%s' will be removed in a later release.", variable, newVariable, variable));
            System.setProperty(newVariable.toLowerCase().replaceAll("_", "."), value);
        }
    }
}
