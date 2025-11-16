package io.github.ridiekel.jeletask.end2end;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import io.github.ridiekel.jeletask.MockingTeletaskTestSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.Selenide.$$;

@SuppressWarnings("resource")
class End2EndMqttAddonConfigTest extends MockingTeletaskTestSupport {
    @Test
    void asOneDevice() {
        this.ha().openMqttSettings();

        String[] expectedDevices = {
                "Condition 1",
                "Dimmer 1",
                "Flags",
                "GAS Sensors",
                "Humidity Sensor",
                "Input 1",
                "Input 2",
                "Local Mood 1",
                "Local Scenery 1",
                "Local Shining 1",
                "Moods",
                "Motor 1",
                "Scenery Flag 1",
                "Scenery Relay 1",
                "Shining Relay 1",
                "Shining Sensor",
                "Teletask2MQTT Bridge",
                "Temperature Control",
                "Temperature Sensor",
                "Toggle Relay 1"
        };
        devices().shouldHave(CollectionCondition.size(expectedDevices.length)); //Needs to be here to wait for the list to be populated in FE
        Assertions.assertThat(devices().texts()).containsExactlyInAnyOrder(expectedDevices);

        this.mqtt().processor().removeConfig();

        this.config.getPublish().setAsOneDevice(true);

        this.mqtt().processor().publishConfig();
        this.mqtt().processor().refreshStates();
        this.mqtt().processor().publishConnectionStatus();

        this.ha().openMqttSettings();

        devices().shouldHave(CollectionCondition.size(1));

        Assertions.assertThat(devices().texts()).containsExactlyInAnyOrder("Teletask2MQTT Bridge");

        this.mqtt().processor().removeConfig();

        this.config.getPublish().setAsOneDevice(false);

        this.mqtt().processor().publishConfig();
        this.mqtt().processor().refreshStates();
        this.mqtt().processor().publishConnectionStatus();

        this.ha().openMqttSettings();

        devices().shouldHave(CollectionCondition.size(expectedDevices.length));

        Assertions.assertThat(devices().texts()).containsExactlyInAnyOrder(expectedDevices);
    }

    private static ElementsCollection devices() {
        return $$(Selectors.shadowDeepCss("ha-config-entry-device-row div[slot=headline]"));
    }
}
