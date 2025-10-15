package io.github.ridiekel.jeletask.end2end;

import io.github.ridiekel.jeletask.MockingTeletaskTestSupport;
import org.junit.jupiter.api.Test;

@SuppressWarnings("resource")
class End2EndGeneralMoodTest extends MockingTeletaskTestSupport {
    @Test
    void generalmood() {
        ha().web().generalmood(60).toggle();
        mqtt().expect().generalmood(60).lastStateMessage().toHave().state().off();
        ha().web().generalmood(60).toggle();
        mqtt().expect().generalmood(60).lastStateMessage().toHave().state().on();

        teletask().generalmood(60).turnOff();
        ha().web().generalmood(60)
                .shouldNotBeChecked()
                .shouldHaveSwitchIcon()
                .shouldHaveIconStateOff();

        teletask().generalmood(60).turnOn();
        ha().web().generalmood(60)
                .shouldBeChecked()
                .shouldHaveSwitchIcon()
                .shouldHaveIconStateOn();
    }

    @Test
    void generalmoodLight() {
        ha().web().generalmood(61).toggle();
        mqtt().expect().generalmood(61).lastStateMessage().toHave().state().off();
        ha().web().generalmood(61).toggle();
        mqtt().expect().generalmood(61).lastStateMessage().toHave().state().on();

        teletask().generalmood(61).turnOff();
        ha().web().generalmood(61)
                .shouldNotBeChecked()
                .shouldHaveLightIcon()
                .shouldHaveIconStateOff();

        teletask().generalmood(61).turnOn();
        ha().web().generalmood(61)
                .shouldBeChecked()
                .shouldHaveLightIcon()
                .shouldHaveIconStateOn();
    }

    @Test
    void generalmoodScene() {
        teletask().generalmood(62).turnOff();
        mqtt().expect().generalmood(62).lastStateMessage().toHave().state().off();
        ha().web().generalmoodScene(62).click();
        mqtt().expect().generalmood(62).lastStateMessage().toHave().state().on();
    }
}
