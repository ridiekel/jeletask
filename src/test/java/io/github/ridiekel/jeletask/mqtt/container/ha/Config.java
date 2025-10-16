package io.github.ridiekel.jeletask.mqtt.container.ha;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Config extends HAObject {
    private String version;
}
