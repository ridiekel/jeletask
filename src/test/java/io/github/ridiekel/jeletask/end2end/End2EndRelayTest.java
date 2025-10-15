package io.github.ridiekel.jeletask.end2end;

import io.github.ridiekel.jeletask.MockingTeletaskTestSupport;
import org.junit.jupiter.api.Test;

@SuppressWarnings("resource")
class End2EndRelayTest extends MockingTeletaskTestSupport {
    @Test
    void relay() {
        ha().web().relay(10).toggle();
        mqtt().expect().relay(10).lastStateMessage().toHave().state().off();
        ha().web().relay(10).toggle();
        mqtt().expect().relay(10).lastStateMessage().toHave().state().on();

        teletask().relay(10).turnOff();
        ha().web().relay(10)
                .shouldNotBeChecked()
                .shouldHaveIconStateOff()
                .shouldHaveLightIcon();

        teletask().relay(10).turnOn();
        ha().web().relay(10)
                .shouldBeChecked()
                .shouldHaveLightIcon()
                .shouldHaveIconStateOn();
    }

    @Test
    void relaySwitch() {
        ha().web().relay(11).toggle();
        mqtt().expect().relay(11).lastStateMessage().toHave().state().off();
        ha().web().relay(11).toggle();
        mqtt().expect().relay(11).lastStateMessage().toHave().state().on();

        teletask().relay(11).turnOff();
        ha().web().relay(11)
                .shouldNotBeChecked()
                .shouldHaveSwitchIcon()
                .shouldHaveIconStateOff();

        teletask().relay(11).turnOn();
        ha().web().relay(11)
                .shouldBeChecked()
                .shouldHaveSwitchIcon()
                .shouldHaveIconStateOn();
    }

    @Test
    void relayScene() {
        teletask().relay(12).turnOff();
        mqtt().expect().relay(12).lastStateMessage().toHave().state().off();
        ha().web().relayScene(12).click();
        mqtt().expect().relay(12).lastStateMessage().toHave().state().on();
    }
}
