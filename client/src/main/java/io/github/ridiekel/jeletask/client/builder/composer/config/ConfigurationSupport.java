package io.github.ridiekel.jeletask.client.builder.composer.config;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.StateCalculator;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public abstract class ConfigurationSupport<T, C extends Configurable<T>, K> {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationSupport.class);

    private Map<K, C> configByKey;
    private Map<T, C> configByObject;
    private final Iterable<C> config;

    public ConfigurationSupport(Iterable<C> config) {
        this.config = config;
    }

    public static ComponentState getState(MessageHandler messageHandler, CentralUnit config, Function function, int number, byte[] payload, int startIndex) {
        ComponentSpec component = config.getComponent(function, number);

        StateCalculator stateCalculator = messageHandler.getFunctionConfig(function).getStateCalculator(component);

        byte[] dataBytes = ArrayUtils.subarray(payload, startIndex, payload.length);

        ComponentState state = stateCalculator.toComponentState(dataBytes);

        LOG.trace("Parsed state {} to {}", Bytes.bytesToHex(dataBytes), state);

        if (state == null) {
            throw new IllegalStateException("Got state '" + Bytes.bytesToHex(dataBytes) + "' ("+stateCalculator.getNumberConverter().convert(dataBytes)+") for " + function + ":" + number + ", which resolved to <null> using '" + stateCalculator.getClass().getSimpleName() + "'");
        }

        return state;
    }

    public T getConfigObject(K key) {
        C configObject = this.getConfigByKey().get(key);

        if (configObject == null) {
            throw new IllegalStateException("Configuration " + this.getClass().getSimpleName() + " not found for key " + key);
        }

        return configObject.getObject();
    }

    public C getConfigurable(T configObject) {
        C state = this.getConfigByObject().get(configObject);
        if (state == null) {
            throw new IllegalStateException("Configuration " + this.getClass().getSimpleName() + " not found for configObject " + configObject);
        }
        return state;
    }

    private Map<K, C> getConfigByKey() {
        if (this.configByKey == null) {
            this.configByKey = this.createConfigByKeyMap(this.getConfig());
        }
        return this.configByKey;
    }

    private Map<T, C> getConfigByObject() {
        if (this.configByObject == null) {
            this.configByObject = this.createConfigByObjectMap(this.getConfig());
        }
        return this.configByObject;
    }

    private Map<T, C> createConfigByObjectMap(Iterable<C> map) {
        Map<T, C> configMap = new HashMap<>();
        for (C value : map) {
            configMap.put(value.getObject(), value);
        }
        return configMap;
    }

    private Map<K, C> createConfigByKeyMap(Iterable<C> map) {
        Map<K, C> configMap = new HashMap<>();
        for (C value : map) {
            configMap.put(this.getKey(value), value);
        }
        return configMap;
    }

    protected abstract K getKey(C configurable);

    private Iterable<C> getConfig() {
        return this.config;
    }

    public boolean knows(T command) {
        return this.getConfigByObject().containsKey(command);
    }
}
