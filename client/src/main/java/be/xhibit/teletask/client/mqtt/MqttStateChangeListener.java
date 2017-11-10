package be.xhibit.teletask.client.mqtt;

import be.xhibit.teletask.client.listener.StateChangeListener;
import be.xhibit.teletask.model.spec.ComponentSpec;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MqttStateChangeListener implements StateChangeListener {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MqttStateChangeListener.class);

    private final MqttPublisher publisher;

    public MqttStateChangeListener(MqttPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void receive(List<ComponentSpec> components) {
        for (ComponentSpec component : components) {
            try {
                this.publisher.publish(component);
            } catch (MqttException e) {
                LOG.error("Exception ({}) caught in event: {}", e.getClass().getName(), e.getMessage(), e);
            }
        }
    }

    @Override
    public void stop() {
        this.publisher.stop();
    }
}
