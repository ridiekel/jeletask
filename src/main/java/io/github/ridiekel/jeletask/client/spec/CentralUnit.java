package io.github.ridiekel.jeletask.client.spec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandlerFactory;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.SensorType;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.StringState;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * POJO representation of the TDS config JSON file.
 */
@Getter
@Setter
public class CentralUnit {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LogManager.getLogger();
    private static final int BRIDGE_NUMBER = -1;

    private String host;
    private String version;
    private int port;
    private Map<Function, List<ComponentSpec>> componentsTypes = new LinkedHashMap<>();
    private List<ComponentSpec> allComponents;
    private CentralUnitType type;
    private ComponentSpec bridge;
    private Map<String, ComponentSpec> deviceMap = new HashMap<>();

    public CentralUnitType getCentralUnitType() {
        return type;
    }

    public ComponentSpec getDevice(String device) {
        return this.deviceMap.computeIfAbsent(device, key -> {
            ComponentSpec deviceSpec = new ComponentSpec();
            deviceSpec.setState(new StringState(this.getVersion()));
            deviceSpec.setFunction(Function.SENSOR);
            deviceSpec.setNumber(Math.abs(key.hashCode()) * -1);
            deviceSpec.setType(SensorType.STRING.toString());
            deviceSpec.setDescription(key);
            return deviceSpec;
        });
    }

    public ComponentSpec getComponent(Function function, int number) {
        return getComponents(function).filter(c -> c.getNumber() == number).peek(c -> c.setFunction(function)).findAny().orElseThrow(() -> {
            LOG.debug(
                    AnsiOutput.toString(AnsiColor.YELLOW, "[EVENT  ] - {}", AnsiColor.DEFAULT, " - {}"),
                    String.format("[%s] - [%s] - [%s]", StringUtils.rightPad(function.toString(), 10), StringUtils.leftPad(String.valueOf(number), 3), StringUtils.leftPad("", 40)),
                    AnsiOutput.toString(AnsiColor.BLUE, "Component not found in config json", AnsiColor.DEFAULT)
            );
            return new ComponentNotFoundInConfigException(function + "(" + number + ") Not Found!");
        });
    }

    @JsonIgnore
    public Stream<? extends ComponentSpec> getComponents(Function function) {
        return this.getAllComponents().stream().filter(c -> c.getFunction() == function);
    }

    @JsonIgnore
    public List<? extends ComponentSpec> getAllComponents() {
        if (this.allComponents == null) {
            this.allComponents = new ArrayList<>();

            this.allComponents.add(getBridgeComponent());

            Map<String, ComponentSpec> devices = new HashMap<>();
            for (Map.Entry<Function, List<ComponentSpec>> components : componentsTypes.entrySet()) {
                List<ComponentSpec> componentsOfType = components.getValue().stream().peek(v -> v.setFunction(components.getKey())).toList();
                Set<ComponentSpec> deviceSpecs = componentsOfType.stream()
                        .filter(c -> c.getDevice() != null)
                        .map(c -> {
                            ComponentSpec device = this.getDevice(c.getDevice());
                            devices.put(c.getDevice(), device);
                            return device;
                        })
                        .collect(Collectors.toSet());
                this.allComponents.addAll(deviceSpecs);

                this.allComponents.addAll(componentsOfType);
            }

            devices.forEach((key, value) -> {
                ((StringState) value.getState()).setState(String.valueOf(this.allComponents.stream().filter(c -> Objects.equals(c.getDevice(), key)).count()));
            });

            this.componentsTypes = null; //We no longer need this, so we can free up some memory
        }
        return this.allComponents;
    }

    private ComponentSpec getBridgeComponent() {
        if (this.bridge == null) {
            this.bridge = new ComponentSpec();
            this.bridge.setState(new StringState(this.getVersion()));
            this.bridge.setFunction(Function.SENSOR);
            this.bridge.setNumber(BRIDGE_NUMBER);
            this.bridge.setType(SensorType.STRING.toString());
            this.bridge.setDescription("Teletask2MQTT Bridge");
        }
        return this.bridge;
    }

    @JsonIgnore
    public MessageHandler getMessageHandler() {
        return MessageHandlerFactory.getMessageHandler(this.getCentralUnitType());
    }

    public State<?> stateFromMessage(Function function, int number, String message) {
        return this.getMessageHandler().getFunctionConfig(function).getStateCalculator(this.getComponent(function, number)).stateFromMessage(message);
    }

    public ComponentSpec getBridge() {
        return this.getComponent(Function.SENSOR, BRIDGE_NUMBER);
    }

    public static final class ComponentNotFoundInConfigException extends RuntimeException {
        public ComponentNotFoundInConfigException(String message) {
            super(message);
        }
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, ToStringStyle.JSON_STYLE).setExcludeFieldNames("componentsTypes", "allComponents").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        CentralUnit that = (CentralUnit) o;

        return new EqualsBuilder().append(port, that.port).append(host, that.host).append(type, that.type).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(host).append(port).append(type).toHashCode();
    }
}

