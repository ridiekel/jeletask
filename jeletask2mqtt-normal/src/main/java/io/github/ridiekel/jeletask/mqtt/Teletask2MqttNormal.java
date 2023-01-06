package io.github.ridiekel.jeletask.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(Teletask2MqttConfigurationProperties.class)
@EnableScheduling
public class Teletask2MqttNormal {
    private static final Logger LOG = LoggerFactory.getLogger(Teletask2MqttNormal.class);

    public static void main(String[] args) throws InterruptedException {
        LOG.info("Starting in normal mode");

        ApplicationContext context = SpringApplication.run(Teletask2MqttNormal.class, args);

        Teletask2MqttConfigurationProperties configuration = context.getBean(Teletask2MqttConfigurationProperties.class);

        LOG.info(String.format("Teletask2Mqtt %s started!", configuration.getCentral().getVersion()));

        Thread.currentThread().join();
    }
}
