package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config;

import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;

public class HAConfigParameters {
    private final CentralUnit centralUnit;
    private final ComponentSpec componentSpec;
    private final String componentTopic;
    private final String availabilityTopic;
    private final String identifier;

    public HAConfigParameters(CentralUnit centralUnit, ComponentSpec componentSpec, String baseTopic, String identifier) {
        this.centralUnit = centralUnit;
        this.componentSpec = componentSpec;
        this.componentTopic = componentTopic(baseTopic, componentSpec);
        this.identifier = identifier;
        this.availabilityTopic = availabilityTopic(baseTopic);
    }

    public CentralUnit getCentralUnit() {
        return centralUnit;
    }

    public ComponentSpec getComponentSpec() {
        return componentSpec;
    }

    public String getComponentTopic() {
        return componentTopic;
    }

    public String getIdentifier() {
        return identifier;
    }
    
    public String getAvailabilityTopic() {
        return availabilityTopic;
    }
    
    public static String componentTopic(String baseTopic, ComponentSpec c) {
        return baseTopic + "/" + c.getFunction().toString().toLowerCase() + "/" + c.getNumber();
    }

    public static String availabilityTopic(String baseTopic) {
        return baseTopic + "/state";
    }
}
