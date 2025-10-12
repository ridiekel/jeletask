package io.github.ridiekel.jeletask.mqtt.listener.tracing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqttMessageTraceConfig {
    @Bean("mqttTraceProperties")
    @ConfigurationProperties(prefix = "teletask.trace")
    public MqttMessageTraceProperties mqttTraceProperties() {
        return new MqttMessageTraceProperties();
    }
}
