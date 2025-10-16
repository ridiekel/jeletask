package io.github.ridiekel.jeletask.end2end;

import io.github.ridiekel.jeletask.MockingTeletaskTestSupport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

@SuppressWarnings("resource")
class End2EndSensorTest extends MockingTeletaskTestSupport {
    @Test
    void lightSensor() {
        //Not all values can be converted correctly due to the math that is being done in teletask server. 3548 and 794 should work to convert to bytes and back.
        String number = "794";
        teletask().lightSensor(30).update(new BigDecimal(number));
        mqtt().expect().sensor(30).lastStateMessage().toHave().state().value(number);
        ha().web().sensor(30)
                .shouldHaveState(number)
                .shouldHaveSensorIcon();
    }

    @Test
    void temperatureSensor() {
        String number = "25.6";
        teletask().temperatureSensor(31).update(new BigDecimal(number));
        mqtt().expect().sensor(31).lastStateMessage().toHave().state().value(number);

        ha().web().sensor(31)
                .shouldHaveState(number)
                .shouldHaveSensorIcon();
    }

    @Test
    void humiditySensor() {
        String number = "73";
        teletask().humiditySensor(36).update(new BigDecimal(number));
        mqtt().expect().sensor(36).lastStateMessage().toHave().state().value(number);

        ha().web().sensor(36)
                .shouldHaveState(number)
                .shouldHaveSensorIcon();
    }

    @Test
    void gasSensor() {
        String number = "21.02";
        teletask().gasSensor(321).update(new BigDecimal(number));
        mqtt().expect().sensor(321).lastStateMessage().toHave().state().value(number);

        ha().web().sensor(321)
                .shouldHaveState(number)
                .shouldHaveSensorIcon();
    }
}
