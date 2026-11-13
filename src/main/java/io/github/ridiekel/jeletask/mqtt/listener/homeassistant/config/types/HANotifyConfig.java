package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.types;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.DisplayMessageStateCalculator;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.component.DisplayMessageSpec;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAReadWriteConfig;

/**
 * <a href="https://www.home-assistant.io/integrations/notify.mqtt/">MQTT Notify Discovery</a>
 * <p>
 * A display message: {@code notify.send_message} shows the text on the displays. A display has no state, so there is
 * no state topic. The template puts the text on line 1 and the rest on line 2, split where a line of this display
 * ends: 16 characters with ascii, 8 without. Whatever does not fit on line 2 reaches the bridge, which logs that it
 * cuts it off. The message type, ascii and the beeps come from the config of the display.
 */
public class HANotifyConfig extends HAReadWriteConfig<HANotifyConfig> {
    public HANotifyConfig(HAConfigParameters parameters) {
        super(parameters);

        this.getConfig().remove("state_topic");
        this.put("command_template", commandTemplate(parameters.getComponentSpec()));
    }

    // tojson escapes quotes and the like in the text, so the payload stays valid JSON
    static String commandTemplate(ComponentSpec component) {
        boolean ascii = component instanceof DisplayMessageSpec display && Boolean.TRUE.equals(display.getAscii());
        int length = DisplayMessageStateCalculator.lineLength(ascii);
        return "{{ {\"message_line1\": value[:" + length + "], \"message_line2\": value[" + length + ":]} | tojson }}";
    }
}
