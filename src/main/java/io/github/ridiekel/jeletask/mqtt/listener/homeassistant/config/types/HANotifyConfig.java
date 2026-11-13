package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAReadWriteConfig;

/**
 * <a href="https://www.home-assistant.io/integrations/notify.mqtt/">MQTT Notify Discovery</a>
 * <p>
 * A display message: {@code notify.send_message} shows the text on the displays. A display has no state, so there is
 * no state topic. The template splits the text over the two lines of 16 ASCII characters a display has; the message
 * type and the beeps come from the config of the display.
 */
public class HANotifyConfig extends HAReadWriteConfig<HANotifyConfig> {
    // tojson escapes quotes and the like in the text, so the payload stays valid JSON
    static final String COMMAND_TEMPLATE = "{{ {\"ascii\": true, \"message_line1\": value[:16], \"message_line2\": value[16:32]} | tojson }}";

    public HANotifyConfig(HAConfigParameters parameters) {
        super(parameters);

        this.getConfig().remove("state_topic");
        this.put("command_template", COMMAND_TEMPLATE);
    }
}
