package io.github.ridiekel.jeletask.client.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * Default constructor.
     */
    private CentralUnit() {
    }

    public CentralUnit(String host, int port) {
        this.host = host;
        this.port = port;
    }

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
        return this.componentsTypes.get(function);
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

    public static CentralUnit read(InputStream jsonData) throws IOException {

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        //convert json string to object
        CentralUnit clientConfig = objectMapper.readValue(jsonData, CentralUnit.class);
        LOG.debug("JSON Config loaded: TDS HOST: {}:{}", clientConfig.getHost(), clientConfig.getPort());

        LOG.debug("CentralUnit initialized.");

        return clientConfig;
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
}

