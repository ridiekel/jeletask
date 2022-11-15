package io.github.ridiekel.jeletask.mqtt.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LongPressInputCaptor {
    private static final Logger LOG = LoggerFactory.getLogger(LongPressInputCaptor.class);
    private final Map<ComponentSpec, Captor> runningMotors = new ConcurrentHashMap<>();
    private final MqttProcessor processor;

    public LongPressInputCaptor(MqttProcessor processor) {
        this.processor = processor;
    }

    public void update(ComponentSpec component) {
        if (Objects.equals(component.getState().getState(), "OPEN")) {
            open(component);
        } else {
            close(component);
        }
    }

    public void open(ComponentSpec component) {
        getCaptor(component).open();
    }

    public void close(ComponentSpec component) {
        Captor captor = this.getCaptor(component);
        if (captor.getOpenTime() != null) {
            captor.close();
            this.publish(component, captor);
        } else {
            this.reset(component, captor);
        }
    }

    public void publish(ComponentSpec input, Captor captor) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Sending action for input: {}\n{}", input.getNumber(), captor.prettyString());
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Sending action for input: {}\n{}", input.getNumber(), captor.getState());
        }
        this.processor.publishState(input, captor);
        this.reset(input, captor);
    }

    private void reset(ComponentSpec input, Captor captor) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            LOG.trace(e.getMessage(), e);
        }
        captor.reset();
        this.processor.publishState(input, captor);
    }

    private Captor getCaptor(ComponentSpec component) {
        return this.runningMotors.computeIfAbsent(component, c -> new Captor(component.getLong_press_duration_millis()));
    }

    private static class Captor extends ComponentState {

        private Long openTime;
        private Long closeTime;
        private final long longPressConfigInMillis;

        public Captor(long longPressConfigInMillis) {
            this.longPressConfigInMillis = longPressConfigInMillis;
        }

        public void open() {
            this.openTime = System.currentTimeMillis();
        }

        public void close() {
            this.closeTime = System.currentTimeMillis();
        }

        public Long getCloseTime() {
            return closeTime;
        }

        public Long getOpenTime() {
            return openTime;
        }

        public Long getLongPressConfigInMillis() {
            return longPressConfigInMillis;
        }

        @Override
        public String getState() {
            return this.getOpenTime() == null ? "NOT_PRESSED" : this.getPressDurationMillis() >= this.getLongPressConfigInMillis() ? "LONG_PRESS" : "SHORT_PRESS";
        }

        public Long getPressDurationMillis() {
            return this.getOpenTime() == null ? -1 : this.getCloseTime() - this.getOpenTime();
        }

        @Override
        public String toString() {
            try {
                return ComponentState.OBJECT_MAPPER.writeValueAsString(this);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        public String prettyString() {
            try {
                return ComponentState.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this).lines().map(l -> "           " + l).collect(Collectors.joining("\n"));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        public void reset() {
            this.openTime = null;
        }
    }
}
