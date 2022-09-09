package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAReadWriteConfig;

public class HAMotorConfig extends HAReadWriteConfig<HAMotorConfig> {
    public HAMotorConfig(HAConfigParameters parameters) {
        super(parameters);

//        this.putInt("brightness_scale", 100);
//        this.put("on_command_type","brightness");
//        this.put("brightness_command_topic", "~/set");

    }
}
