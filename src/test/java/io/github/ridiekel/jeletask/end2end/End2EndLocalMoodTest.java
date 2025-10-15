package io.github.ridiekel.jeletask.end2end;

import io.github.ridiekel.jeletask.MockingTeletaskTestSupport;
import org.junit.jupiter.api.Test;

@SuppressWarnings("resource")
class End2EndLocalMoodTest extends MockingTeletaskTestSupport {
    @Test
    void localmood() {
        ha().web().localmood(80).toggle();
        mqtt().expect().localmood(80).lastStateMessage().toHave().state().off();
        ha().web().localmood(80).toggle();
        mqtt().expect().localmood(80).lastStateMessage().toHave().state().on();

        teletask().localmood(80).turnOff();
        ha().web().localmood(80)
                .shouldNotBeChecked()
                .shouldHaveSwitchIcon()
                .shouldHaveIconStateOff();

        teletask().localmood(80).turnOn();
        ha().web().localmood(80)
                .shouldBeChecked()
                .shouldHaveIconStateOn()
                .shouldHaveSwitchIcon();
    }

    @Test
    void localmoodLight() {
        ha().web().localmood(81).toggle();
        mqtt().expect().localmood(81).lastStateMessage().toHave().state().off();
        ha().web().localmood(81).toggle();
        mqtt().expect().localmood(81).lastStateMessage().toHave().state().on();

        teletask().localmood(81).turnOff();
        ha().web().localmood(81)
                .shouldNotBeChecked()
                .shouldHaveLightIcon()
                .shouldHaveIconStateOff();

        teletask().localmood(81).turnOn();
        ha().web().localmood(81)
                .shouldBeChecked()
                .shouldHaveIconStateOn()
                .shouldHaveLightIcon();
    }

    @Test
    void localmoodScene() {
        teletask().localmood(82).turnOff();
        mqtt().expect().localmood(82).lastStateMessage().toHave().state().off();
        ha().web().localmoodScene(82).click();
        mqtt().expect().localmood(82).lastStateMessage().toHave().state().on();
    }
}
