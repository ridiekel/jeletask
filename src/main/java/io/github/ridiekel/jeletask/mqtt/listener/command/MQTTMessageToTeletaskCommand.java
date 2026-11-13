package io.github.ridiekel.jeletask.mqtt.listener.command;

import io.github.ridiekel.jeletask.client.TeletaskClient;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.mqtt.listener.MqttConnection;
import io.github.ridiekel.jeletask.mqtt.listener.MqttLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.ridiekel.jeletask.mqtt.listener.MqttLogger.*;

public class MQTTMessageToTeletaskCommand implements MqttConnection.MessageHandler {
    private static final Logger LOG = LogManager.getLogger();

    private final Pattern teletaskComponentPattern;
    private final TeletaskClient teletaskClient;

    public MQTTMessageToTeletaskCommand(TeletaskClient teletaskClient, String prefix, String teletaskIdentifier) {
        this.teletaskClient = teletaskClient;
        this.teletaskComponentPattern = Pattern.compile(prefix + "/" + teletaskIdentifier + "/(\\w*)/(\\d*)/set");
    }

    @Override
    public void arrived(String topic, MqttConnection.Message mqttMessage) {
        LOG.trace(() -> String.format("MQTT message arrived '%s': '%s'", topic, mqttMessage.toString()));
        String message = mqttMessage.toString();
        try {
            Matcher matcher = this.teletaskComponentPattern.matcher(topic);
            if (matcher.find()) {
                Function function = Function.valueOf(matcher.group(1).toUpperCase());
                int number = Integer.parseInt(matcher.group(2));

                CentralUnit centralUnit = this.teletaskClient.getCentralUnit();

                State<?> state = centralUnit.stateFromMessage(function, number, message);

                String componentLog = getLoggingStringForComponent(centralUnit.getComponent(function, number)).get();
                LOG.info(() -> String.format(WHAT_LOG_PATTERNS.get(MqttLogger.What.RECEIVE), getWhat(MqttLogger.What.RECEIVE), componentLog, payloadToLogWithColors(mqttMessage.toString())));

                teletaskClient.set(function, number, state,
                        (f, n, s) -> LOG.trace(() -> String.format("%s - MQTT topic '%s' changed state for: %s / %s -> %s", componentLog, topic, f, n, s)),
                        (f, n, s, e) -> LOG.warn(String.format("%s - MQTT topic '%s' could not change state for: %s / %s -> %s", componentLog, topic, f, n, s)));

            }
        } catch (Exception e) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(() -> String.format("MQTT topic '%s' could not change state to: %s", topic, message), e);
            } else {
                LOG.warn(String.format("MQTT topic '%s' could not change state to: %s -- %s", topic, message, e.getMessage()), e);
            }
        }
    }
}
