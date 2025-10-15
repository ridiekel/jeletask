package io.github.ridiekel.jeletask.sba.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfig;
import io.github.ridiekel.jeletask.utilities.ResourceUtilities;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Endpoint(id = "centralunit")
@RequiredArgsConstructor
public class CentralUnitEndpoint {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Teletask2MqttConfigurationProperties configuration;
    private final CentralUnit centralUnit;

    @ReadOperation
    public String getCentralUnitInfo() {
        return enrich(ResourceUtilities.asString(configuration.getConfigFile()));
    }

    public String enrich(String inputJson) {
        try {
            ObjectNode root = (ObjectNode) MAPPER.readTree(inputJson);
            root.put("host", centralUnit.getHost());
            root.put("port", centralUnit.getPort());
            ObjectNode componentsTypes = (ObjectNode) root.get("componentsTypes");

            for (Map.Entry<String, JsonNode> entry : componentsTypes.properties()) {
                String function = entry.getKey();
                ArrayNode components = (ArrayNode) entry.getValue();

                for (JsonNode item : components) {
                    int number = item.path("number").asInt();
                    ComponentSpec component = this.centralUnit.getComponent(Function.valueOf(function.toUpperCase()), number);
                    if (item instanceof ObjectNode obj) {
                        obj.set("state", MAPPER.valueToTree(component.getState()));
                        ArrayNode configs = JsonNodeFactory.instance.arrayNode();
                        component.getHaPublishedConfig().stream().map(HAConfig::getConfig).forEach(configs::add);
                        obj.set("haPublishedConfig", configs);
                    }
                }
            }

            return MAPPER.writer().writeValueAsString(root);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

}
