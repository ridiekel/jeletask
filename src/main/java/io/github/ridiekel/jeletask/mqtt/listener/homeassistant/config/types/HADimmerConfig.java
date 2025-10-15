package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAReadWriteConfig;

/**
 * <a href="https://www.home-assistant.io/integrations/light.mqtt/#json-schema">MQTT Light Discovery</a>
 */
public class HADimmerConfig extends HAReadWriteConfig<HADimmerConfig> {
    public HADimmerConfig(HAConfigParameters parameters) {
        super(parameters);
        this.put("schema", "json");
        this.putBoolean("brightness", true);
        this.putInt("brightness_scale", 100);
        this.putArray("supported_color_modes", "brightness");
    }
}
