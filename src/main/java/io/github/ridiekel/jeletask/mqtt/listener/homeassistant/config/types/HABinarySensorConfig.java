package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.types;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAReadOnlyConfig;

/**
 * <a href="https://www.home-assistant.io/integrations/binary_sensor.mqtt/">MQTT Binary Sensor Discovery</a>
 */
public class HABinarySensorConfig extends HAReadOnlyConfig<HABinarySensorConfig> {
    public HABinarySensorConfig(HAConfigParameters parameters) {
        super(parameters);

        this.put("payload_on", OnOffToggleStateCalculator.ValidOnOffToggle.ON.toString());
        this.put("payload_off", OnOffToggleStateCalculator.ValidOnOffToggle.OFF.toString());
        this.put("value_template", "{{ value_json.state }}");
    }
}
