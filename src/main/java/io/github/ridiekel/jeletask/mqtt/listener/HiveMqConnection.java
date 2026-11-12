package io.github.ridiekel.jeletask.mqtt.listener;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientTransportConfig;
import com.hivemq.client.mqtt.MqttClientTransportConfigBuilder;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.datatypes.MqttTopic;
import com.hivemq.client.mqtt.datatypes.MqttTopicFilter;
import com.hivemq.client.mqtt.lifecycle.MqttDisconnectSource;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAckReturnCode;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link MqttConnection} on the HiveMQ MQTT client, speaking MQTT 3.1.1.
 * <p>
 * All messages come in through one callback and are handed to the handler of the matching filter here. The client's
 * own per-subscription callbacks would add a second one on every subscribe to the same filter, and with it every
 * command would run twice after a resubscribe.
 */
public final class HiveMqConnection implements MqttConnection {
    private static final Logger LOG = LogManager.getLogger();

    // Handlers and listeners may block (a stop exits the application), so not on the client's own threads
    private final ExecutorService callbacks = Executors.newSingleThreadExecutor(Thread.ofPlatform().name("mqtt-callback").daemon().factory());
    private final Map<String, MessageHandler> handlers = new ConcurrentHashMap<>();
    private final AtomicBoolean connectedBefore = new AtomicBoolean();
    private final AtomicBoolean up = new AtomicBoolean();
    private Mqtt3AsyncClient client;
    private Duration timeToWait;

    @Override
    public void connect(MqttConnectionSettings settings, Listener listener) throws MqttConnectionException {
        this.timeToWait = settings.timeToWait();
        Mqtt3ClientBuilder builder = MqttClient.builder()
                .useMqttVersion3()
                .identifier(settings.clientId())
                .transportConfig(transport(settings))
                // One connection needs one thread; by default every reconnect could start another, up to one per core
                .executorConfig()
                .nettyThreads(1)
                .applyExecutorConfig()
                .automaticReconnect()
                .initialDelay(settings.reconnectDelay().toMillis(), TimeUnit.MILLISECONDS)
                .maxDelay(settings.maxReconnectDelay().toMillis(), TimeUnit.MILLISECONDS)
                .applyAutomaticReconnect()
                .addConnectedListener(_ -> {
                    this.up.set(true);
                    boolean reconnect = this.connectedBefore.getAndSet(true);
                    this.callbacks.execute(() -> listener.connected(reconnect));
                })
                .addDisconnectedListener(context -> {
                    if (!this.connectedBefore.get()) {
                        // The first connect fails with its cause, instead of retrying until the caller stops waiting
                        context.getReconnector().reconnect(false);
                        return;
                    }
                    // Also called for every reconnect attempt that fails: only the loss of a connection counts
                    if (this.up.getAndSet(false) && context.getSource() != MqttDisconnectSource.USER) {
                        this.callbacks.execute(() -> listener.connectionLost(context.getCause()));
                    }
                });
        this.client = builder.buildAsync();
        this.client.publishes(MqttGlobalPublishFilter.ALL, this::dispatch, this.callbacks);

        var connect = this.client.connectWith()
                .cleanSession(false)
                .keepAlive((int) settings.keepAlive().toSeconds())
                .willPublish()
                .topic(settings.willTopic())
                .payload(settings.willPayload().getBytes(StandardCharsets.UTF_8))
                .qos(MqttQos.AT_LEAST_ONCE)
                .retain(true)
                .applyWillPublish();
        if (settings.username() != null) {
            var auth = connect.simpleAuth().username(settings.username());
            connect = (settings.password() == null ? auth : auth.password(settings.password().getBytes(StandardCharsets.UTF_8))).applySimpleAuth();
        }

        try {
            this.await(connect.send(), "connect");
        } catch (MqttConnectionException e) {
            // Otherwise the automatic reconnect keeps trying behind the back of a caller that gave up
            this.close();
            throw e;
        }
    }

    /**
     * In one piece: an ssl config set on the client builder next to a transport config gets lost.
     */
    private static MqttClientTransportConfig transport(MqttConnectionSettings settings) {
        MqttClientTransportConfigBuilder transport = MqttClientTransportConfig.builder()
                .serverHost(settings.mqtt().getHost())
                .serverPort(Integer.parseInt(settings.mqtt().getPort().trim()))
                .socketConnectTimeout(settings.connectTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .mqttConnectTimeout(settings.connectTimeout().toMillis(), TimeUnit.MILLISECONDS);
        return MqttTls.sslConfig(settings.mqtt()).map(transport::sslConfig).orElse(transport).build();
    }

    @Override
    public boolean isConnected() {
        return this.client != null && this.client.getState().isConnected();
    }

    @Override
    public void subscribe(String topicFilter, MessageHandler handler) throws MqttConnectionException {
        // Before the SUBSCRIBE: the broker sends the retained messages right away, maybe before its answer arrives
        MessageHandler previous = this.handlers.put(topicFilter, handler);
        Mqtt3SubAck subAck;
        try {
            subAck = this.await(this.client.subscribeWith().topicFilter(topicFilter).qos(MqttQos.AT_MOST_ONCE).send(), "subscribe to " + topicFilter);
        } catch (MqttConnectionException e) {
            this.restore(topicFilter, handler, previous);
            throw e;
        }
        if (subAck.getReturnCodes().contains(Mqtt3SubAckReturnCode.FAILURE)) {
            this.restore(topicFilter, handler, previous);
            throw new MqttConnectionException("The broker refused the subscription to " + topicFilter);
        }
    }

    /**
     * A subscribe that failed leaves the filter as it was. The broker may still hold the earlier subscription of our
     * session and keep delivering, and those messages need the handler that was there.
     */
    private void restore(String topicFilter, MessageHandler failed, MessageHandler previous) {
        if (previous == null) {
            this.handlers.remove(topicFilter, failed);
        } else {
            this.handlers.replace(topicFilter, failed, previous);
        }
    }

    @Override
    public void unsubscribe(String... topicFilters) throws MqttConnectionException {
        for (String topicFilter : topicFilters) {
            this.handlers.remove(topicFilter);
            this.await(this.client.unsubscribeWith().topicFilter(topicFilter).send(), "unsubscribe from " + topicFilter);
        }
    }

    @Override
    public void publish(String topic, byte[] payload, boolean retained) throws MqttConnectionException {
        this.await(this.client.publishWith().topic(topic).payload(payload).qos(MqttQos.AT_MOST_ONCE).retain(retained).send(), "publish to " + topic);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.disconnect().orTimeout(this.timeToWait.toMillis(), TimeUnit.MILLISECONDS).exceptionally(_ -> null).join();
        }
        this.callbacks.shutdown();
    }

    private void dispatch(Mqtt3Publish publish) {
        MqttTopic topic = publish.getTopic();
        Message message = new Message(publish.getPayloadAsBytes(), publish.isRetain());
        AtomicBoolean handled = new AtomicBoolean();
        this.handlers.forEach((filter, handler) -> {
            if (MqttTopicFilter.of(filter).matches(topic)) {
                handled.set(true);
                try {
                    handler.arrived(topic.toString(), message);
                } catch (RuntimeException e) {
                    // One bad message must not end the stream of all the others
                    LOG.error("Handling MQTT topic '{}' failed: {}", topic, e.getMessage(), e);
                }
            }
        });
        if (!handled.get()) {
            // Expected for a moment around an unsubscribe; anything else is a subscription nobody listens to
            LOG.debug("No handler for MQTT topic '{}', the message is dropped", topic);
        }
    }

    private <T> T await(CompletableFuture<T> future, String what) throws MqttConnectionException {
        try {
            return future.get(this.timeToWait.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MqttConnectionException("Interrupted during " + what, e);
        } catch (ExecutionException e) {
            throw new MqttConnectionException("Could not " + what + ": " + e.getCause().getMessage(), e.getCause());
        } catch (TimeoutException e) {
            future.cancel(false);
            throw new MqttConnectionException("No answer of the broker within " + this.timeToWait.toMillis() + " ms to " + what, e);
        }
    }
}
