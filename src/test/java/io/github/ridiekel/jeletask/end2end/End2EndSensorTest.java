package io.github.ridiekel.jeletask.end2end;

import io.github.ridiekel.jeletask.MockingTeletaskTestSupport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

@SuppressWarnings("resource")
class End2EndSensorTest extends MockingTeletaskTestSupport {
    @Test
    void lightSensor() {
        //Not all values can be converted correctly due to the math that is being done in teletask server. 3548 and 794 should work to convert to bytes and back.
        teletask().lightSensor(30).update(new BigDecimal("794"));
        mqtt().expect().sensor(30).lastStateMessage().toHave().state().value("794");
        ha().web().sensor(30)
                .shouldHaveState("794")
                .shouldHaveSensorIcon();
    }

    @Test
    void temperatureSensor() {
        teletask().temperatureSensor(31).update(new BigDecimal("25.6"));
        mqtt().expect().sensor(31).lastStateMessage().toHave().state().value("25.6");

        ha().web().sensor(31)
                .shouldHaveState("25.6")
                .shouldHaveSensorIcon();
    }

    @Test
    void humiditySensor() {
        teletask().humiditySensor(36).update(new BigDecimal("73"));
        mqtt().expect().sensor(36).lastStateMessage().toHave().state().value("73");

        ha().web().sensor(36)
                .shouldHaveState("73")
                .shouldHaveSensorIcon();
    }
}
