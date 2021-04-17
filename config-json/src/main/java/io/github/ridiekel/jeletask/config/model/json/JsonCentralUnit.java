package io.github.ridiekel.jeletask.config.model.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.ridiekel.jeletask.model.spec.CentralUnit;
import io.github.ridiekel.jeletask.model.spec.CentralUnitType;
import io.github.ridiekel.jeletask.model.spec.ComponentSpec;
import io.github.ridiekel.jeletask.model.spec.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * POJO representation of the TDS config JSON file.
 */
//@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(as = CentralUnit.class)
public class JsonCentralUnit implements CentralUnit {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(JsonCentralUnit.class);

    private String host;
    private int port;
    private Map<Function, List<TDSComponent>> componentsTypes;
    private List<TDSComponent> allComponents;
    private CentralUnitType type;

    /**
     * Default constructor.
     */
    private JsonCentralUnit() {
    }

    public JsonCentralUnit(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }


    public Map<Function, List<TDSComponent>> getComponentsTypes() {
        return this.componentsTypes;
    }

    public void setComponentsTypes(Map<Function, List<TDSComponent>> componentsTypes) {
        this.componentsTypes = componentsTypes;
    }

    @Override
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

    @Override
    public TDSComponent getComponent(Function function, int number) {
        return this.componentsTypes.computeIfAbsent(function, k -> new ArrayList<>()).stream().filter(c -> c.getNumber() == number).peek(c -> c.setFunction(function)).findAny().orElseThrow(() -> new ComponentNotFoundInConfigException(function + "(" + number + ") Not Found!"));
    }

    @Override
    @JsonIgnore
    public List<? extends ComponentSpec> getComponents(Function function) {
        return this.componentsTypes.get(function);
    }

    @Override
    @JsonIgnore
    public List<? extends ComponentSpec> getAllComponents() {
        if (this.allComponents == null) {
            this.allComponents = new ArrayList<>();
            for (Map.Entry<Function, List<TDSComponent>> components : componentsTypes.entrySet()) {
                this.allComponents.addAll(components.getValue().stream().peek(v->v.setFunction(components.getKey())).collect(Collectors.toList()));
            }
        }
        return this.allComponents;
    }

    public static JsonCentralUnit read(InputStream jsonData) throws IOException {

        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        //convert json string to object
        JsonCentralUnit clientConfig = objectMapper.readValue(jsonData, JsonCentralUnit.class);
        LOG.debug("JSON Config loaded: TDS HOST: {}:{}", clientConfig.getHost(), clientConfig.getPort());

        LOG.debug("JsonCentralUnit initialized.");

        return clientConfig;
    }
}
