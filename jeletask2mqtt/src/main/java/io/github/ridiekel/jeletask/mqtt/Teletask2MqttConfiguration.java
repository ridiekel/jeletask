package io.github.ridiekel.jeletask.mqtt;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"io.github.ridiekel.jeletask.client"})
public class Teletask2MqttConfiguration {
}
