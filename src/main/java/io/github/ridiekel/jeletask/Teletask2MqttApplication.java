package io.github.ridiekel.jeletask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({Teletask2MqttConfigurationProperties.class})
@EnableScheduling
@ImportRuntimeHints(NativeRuntimeHints.class)
public class Teletask2MqttApplication {
    private static final Logger LOG = LogManager.getLogger();

    static void main(String[] args) {
        handleDeprecations();

        ApplicationContext context = SpringApplication.run(Teletask2MqttApplication.class, args);

        Teletask2MqttConfigurationProperties configuration = context.getBean(Teletask2MqttConfigurationProperties.class);

        LOG.info(() -> String.format("Teletask2Mqtt %s started!", configuration.getCentral().getVersion()));
    }

    public static void handleDeprecations() {
        /*
        WARNING: sun.misc.Unsafe::allocateMemory has been called by io.netty.util.internal.PlatformDependent0$2 (netty-common-4.1.127.Final.jar)
         */
        System.setProperty("io.netty.noUnsafe", "true");

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
