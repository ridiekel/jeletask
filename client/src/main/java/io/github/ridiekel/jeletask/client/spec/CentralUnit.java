package io.github.ridiekel.jeletask.client.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandlerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * POJO representation of the TDS config JSON file.
 */
//@JsonIgnoreProperties(ignoreUnknown = true)
public class CentralUnit {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(CentralUnit.class);

    private String host;
    private int port;
    private Map<Function, List<ComponentSpec>> componentsTypes = new LinkedHashMap<>();
    private List<ComponentSpec> allComponents;
    private CentralUnitType type;

    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }


    @JsonCreator(mode = JsonCreator.Mode.DISABLED)
    public Map<Function, List<ComponentSpec>> getComponentsTypes() {
        return this.componentsTypes;
    }

    @JsonCreator(mode = JsonCreator.Mode.DISABLED)
    public void setComponentsTypes(Map<Function, List<ComponentSpec>> componentsTypes) {
        this.componentsTypes = componentsTypes;
    }

    public CentralUnitType getCentralUnitType() {
        return type;
    }

    public CentralUnitType getType() {
        return type;
    }

    public void setType(CentralUnitType type) {
        this.type = type;
    }

    // ================================ HELPER METHODS

    public ComponentSpec getComponent(Function function, int number) {
        return this.componentsTypes.computeIfAbsent(function, k -> new ArrayList<>()).stream().filter(c -> c.getNumber() == number).peek(c -> c.setFunction(function)).findAny().orElseThrow(() -> {
            LOG.debug(
                    AnsiOutput.toString(AnsiColor.YELLOW, "[EVENT  ] - {}", AnsiColor.DEFAULT, " - {}"),
                    String.format("[%s] - [%s] - [%s]", StringUtils.rightPad(function.toString(), 10), StringUtils.leftPad(String.valueOf(number), 3), StringUtils.leftPad("", 40)),
                    AnsiOutput.toString(AnsiColor.BLUE, "Component not found in config json", AnsiColor.DEFAULT)
            );
            return new ComponentNotFoundInConfigException(function + "(" + number + ") Not Found!");
        });
    }

    @JsonIgnore
    public List<? extends ComponentSpec> getComponents(Function function) {
        return this.componentsTypes.getOrDefault(function, List.of());
    }

    @JsonIgnore
    public List<? extends ComponentSpec> getAllComponents() {
        if (this.allComponents == null) {
            this.allComponents = new ArrayList<>();
            for (Map.Entry<Function, List<ComponentSpec>> components : componentsTypes.entrySet()) {
                this.allComponents.addAll(components.getValue().stream().peek(v -> v.setFunction(components.getKey())).toList());
            }
        }
        return this.allComponents;
    }

    @JsonIgnore
    public MessageHandler getMessageHandler() {
        return MessageHandlerFactory.getMessageHandler(this.getCentralUnitType());
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

