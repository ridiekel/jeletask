package io.github.ridiekel.jeletask;

import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.mqtt.container.TestContainers;
import io.github.ridiekel.jeletask.server.TeletaskTestServer;
import org.springframework.boot.SpringApplication;
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
        return new TeletaskTestServer(centralUnit.getPort(), centralUnit);
    }

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "test");
        Teletask2MqttApplication.handleDeprecations();
        SpringApplication.run(Teletask2MqttTestApplication.class, args);
    }
}
