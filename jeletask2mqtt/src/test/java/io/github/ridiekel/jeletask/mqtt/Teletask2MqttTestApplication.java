package io.github.ridiekel.jeletask.mqtt;

import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.mqtt.container.TestContainers;
import io.github.ridiekel.jeletask.server.TeletaskTestServer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(Teletask2MqttConfigurationProperties.class)
@EnableScheduling
public class Teletask2MqttTestApplication {
    @Bean
    public TeletaskTestServer teletaskTestServer(CentralUnit centralUnit, TestContainers containers) {
        return new TeletaskTestServer(1234, centralUnit);
    }


}
