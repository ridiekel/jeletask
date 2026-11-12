package io.github.ridiekel.jeletask.mqtt.listener;

/**
 * The broker refused, did not answer in time, or the connection is gone.
 */
public class MqttConnectionException extends Exception {
    public MqttConnectionException(String message) {
        super(message);
    }

    public MqttConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
