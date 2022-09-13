package io.github.ridiekel.jeletask.mqtt.listener.homeassistant;

public class HARelayConfig extends HAReadWriteConfig<HARelayConfig> {
    public HARelayConfig(HAConfigParameters parameters) {
        super(parameters);
        switch ( parameters.getComponentSpec().getType().toLowerCase() ) {
            case "light":
                this.put("state_value_template", "{{ value_json.state }}");
                break;
            case "switch":
                this.put("value_template", "{{ value_json.state }}");
                break;

        }
    }
}
