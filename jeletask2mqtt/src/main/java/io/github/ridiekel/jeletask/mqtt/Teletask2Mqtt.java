package io.github.ridiekel.jeletask.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
@EnableConfigurationProperties(Teletask2MqttConfiguration.class)
public class Teletask2Mqtt {
    private static final Logger LOG = LoggerFactory.getLogger(Teletask2Mqtt.class);

    public static void main(String[] args) throws InterruptedException {
        ApplicationContext context = SpringApplication.run(Teletask2Mqtt.class, args);

        Teletask2MqttConfiguration configuration = context.getBean(Teletask2MqttConfiguration.class);

        LOG.info(String.format("Teletask2Mqtt %s started!", configuration.getVersion()));

        Thread.currentThread().join();
    }
}
