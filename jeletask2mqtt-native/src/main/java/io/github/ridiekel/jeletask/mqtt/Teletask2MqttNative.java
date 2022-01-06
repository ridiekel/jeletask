package io.github.ridiekel.jeletask.mqtt;

import org.eclipse.paho.client.mqttv3.logging.JSR47Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.nativex.hint.ResourceHint;
import org.springframework.nativex.hint.TypeHint;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.LinkedHashMap;

@ResourceHint(patterns = {".*/mqttv3/.*properties$"}) //spring-native: Needed for paho mqtt client
@TypeHint(types = {
        LinkedHashMap.class, //spring-native: Needed for jackson deserializing of the config
        JSR47Logger.class //spring-native: Needed for paho mqtt client
})
@SpringBootApplication
@EnableConfigurationProperties(Teletask2MqttConfiguration.class)
@EnableScheduling
public class Teletask2MqttNative {
    private static final Logger LOG = LoggerFactory.getLogger(Teletask2MqttNative.class);

    public static void main(String[] args) throws InterruptedException {
        LOG.info("Starting in native mode");

        ApplicationContext context = SpringApplication.run(Teletask2MqttNative.class, args);

        Teletask2MqttConfiguration configuration = context.getBean(Teletask2MqttConfiguration.class);

        LOG.info(String.format("Teletask2Mqtt %s started!", configuration.getVersion()));

        Thread.currentThread().join();
    }
}
