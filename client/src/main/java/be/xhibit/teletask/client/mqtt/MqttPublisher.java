package be.xhibit.teletask.client.mqtt;

import be.xhibit.teletask.model.spec.ComponentSpec;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(MqttPublisher.class);
    private final String host;
    private final String port;

    private MqttClient client;

    public MqttPublisher(String host, String port) {
        this.host = host;
        this.port = port;
        this.createClient();
    }

    private IMqttClient getClient() {
        if (this.client == null) {
            this.createClient();
        } else if (!this.client.isConnected()) {
            try {
                this.client.connect(this.getConnectOptions());
            } catch (MqttException e) {
                LOG.error("Exception ({}) caught in getClient: {}", e.getClass().getName(), e.getMessage(), e);
            }
        }

        return this.client;
    }

    private void createClient() {
        String broker = String.format("tcp://%s:%s", this.host, this.port);
        String clientId = "TeletaskClient";
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            this.client = new MqttClient(broker, clientId, persistence);
            this.connect(this.client);
        } catch (MqttException me) {
            LOG.error("Exception ({}) caught in getClient: {}", me.getClass().getName(), me.getMessage(), me);
        }
    }

    private void connect(IMqttClient mqttClient) {
        MqttConnectOptions connOpts = this.getConnectOptions();
        LOG.debug("Connecting to broker");
        try {
            mqttClient.connect(connOpts);
        } catch (MqttException e) {
            LOG.error("Exception ({}) caught in connect: {}", e.getClass().getName(), e.getMessage(), e);
        }
        LOG.debug("Mqtt Client Connected");
    }

    private MqttConnectOptions getConnectOptions() {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        return connOpts;
    }

    public void publish(ComponentSpec component) throws MqttException {
        MqttMessage message = new MqttMessage(component.getState().getBytes());
        message.setQos(2);
        String topic = this.toTopic(component);
        this.getClient().publish(topic, message);
        LOG.debug("Message '{}' published to topic '{}'", message.toString(), topic);
    }

    private String toTopic(ComponentSpec component) {
        return String.format("/teletask-api/%s/%s", component.getFunction().toString().toLowerCase(), component.getNumber());
    }

    public void stop() {
        if (this.client != null && this.client.isConnected()) {
            try {
                this.client.disconnect();
            } catch (MqttException e) {
                try {
                    this.client.disconnectForcibly();
                } catch (MqttException e1) {
                    LOG.error("Exception ({}) caught in stop: {}", e1.getClass().getName(), e1.getMessage(), e1);
                }
            }
        }
    }
}
