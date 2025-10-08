package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAReadWriteConfig;

/**
 * <a href="https://www.home-assistant.io/integrations/cover.mqtt/">MQTT Cover Discovery</a>
 */
public class HAMotorConfig extends HAReadWriteConfig<HAMotorConfig> {
    public HAMotorConfig(HAConfigParameters parameters) {
        super(parameters);

        this.put("set_position_topic", "~/set");

        this.put("optimistic", "false");

        this.put("payload_close", """
                { "state": "DOWN" }
                """);
        this.put("payload_open", """
                { "state": "UP" }
                """);
        this.put("payload_stop", """
                { "state": "STOP" }
                """);

        this.put("state_closed", "closed");
        this.put("state_open", "open");
        this.put("state_opening", "opening");
        this.put("state_closing", "closing");
        this.put("state_stopped", "stopped");

        this.putInt("position_open", 0);
        this.putInt("position_closed", 100);
        this.put("position_template", "{{ value_json.current_position }}");
        this.put("position_topic", "~/state");
        this.put("set_position_template", """
                {"state": "MOTOR_GO_TO_POSITION", "requested_position": {{ 100 - position }}}
                """);

        this.put("value_template", """
                {% if value_json.power == 'OFF' %}
                    {% if value_json.current_position == 100 %}
                        closed
                    {% elif value_json.current_position == 0 %}
                        open
                    {% else %}
                        stopped
                    {% endif %}
                {% else %}
                    {% if value_json.state == 'DOWN' %}
                        closing
                    {% else %}
                        opening
                    {% endif %}
                {% endif %}
                """);
    }
}
