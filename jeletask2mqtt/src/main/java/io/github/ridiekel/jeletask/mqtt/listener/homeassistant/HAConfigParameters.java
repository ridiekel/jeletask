package io.github.ridiekel.jeletask.mqtt.listener.homeassistant;

import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;

public class HAConfigParameters {
    private final CentralUnit centralUnit;
    private final ComponentSpec componentSpec;
    private final String baseTopic;
    private final String type;
    private final String identifier;

    public HAConfigParameters(CentralUnit centralUnit, ComponentSpec componentSpec, String baseTopic, String type, String identifier) {
        this.centralUnit = centralUnit;
        this.componentSpec = componentSpec;
        this.baseTopic = baseTopic;
        this.type = type;
        this.identifier = identifier;
    }

    public CentralUnit getCentralUnit() {
        return centralUnit;
    }

    public ComponentSpec getComponentSpec() {
        return componentSpec;
    }

    public String getBaseTopic() {
        return baseTopic;
    }

    public String getType() {
        return type;
    }

    public String getIdentifier() {
        return identifier;
    }
}
