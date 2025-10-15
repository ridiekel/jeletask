package io.github.ridiekel.jeletask.end2end;

import io.github.ridiekel.jeletask.MockingTeletaskTestSupport;
import org.junit.jupiter.api.Test;

@SuppressWarnings("resource")
class End2EndConditionTest extends MockingTeletaskTestSupport {
    @Test
    void condition() {
        teletask().condition(4).turnOff();
        ha().web().condition(4)
                .shouldHaveStateTextOff()
                .shouldHaveBinarySensorIcon()
                .shouldHaveIconStateOff();

        teletask().condition(4).turnOn();
        ha().web().condition(4)
                .shouldHaveStateTextOn()
                .shouldHaveBinarySensorIcon()
                .shouldHaveIconStateOn();
    }
}
