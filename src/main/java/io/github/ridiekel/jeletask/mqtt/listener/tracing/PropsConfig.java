package io.github.ridiekel.jeletask.mqtt.listener.tracing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PropsConfig {
    @Bean("mqttTraceProperties")
    @ConfigurationProperties(prefix = "teletask.trace")
    public MqttTraceProperties mqttTraceProperties() {
        return new MqttTraceProperties();
    }
}
