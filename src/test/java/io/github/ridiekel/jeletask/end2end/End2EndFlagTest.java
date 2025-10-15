package io.github.ridiekel.jeletask.end2end;

import io.github.ridiekel.jeletask.MockingTeletaskTestSupport;
import org.junit.jupiter.api.Test;

@SuppressWarnings("resource")
class End2EndFlagTest extends MockingTeletaskTestSupport {
    @Test
    void flag() {
        ha().web().flag(50).toggle();
        mqtt().expect().flag(50).lastStateMessage().toHave().state().off();
        ha().web().flag(50).toggle();
        mqtt().expect().flag(50).lastStateMessage().toHave().state().on();

        teletask().flag(50).turnOff();
        ha().web().flag(50)
                .shouldNotBeChecked()
                .shouldHaveSwitchIcon()
                .shouldHaveIconStateOff();

        teletask().flag(50).turnOn();
        ha().web().flag(50)
                .shouldBeChecked()
                .shouldHaveIconStateOn()
                .shouldHaveSwitchIcon();
    }

    @Test
    void flagLight() {
        ha().web().flag(51).toggle();
        mqtt().expect().flag(51).lastStateMessage().toHave().state().off();
        ha().web().flag(51).toggle();
        mqtt().expect().flag(51).lastStateMessage().toHave().state().on();

        teletask().flag(51).turnOff();
        ha().web().flag(51)
                .shouldNotBeChecked()
                .shouldHaveLightIcon()
                .shouldHaveIconStateOff();

        teletask().flag(51).turnOn();
        ha().web().flag(51)
                .shouldBeChecked()
                .shouldHaveIconStateOn()
                .shouldHaveLightIcon();
    }

    @Test
    void flagScene() {
        teletask().flag(52).turnOff();
        mqtt().expect().flag(52).lastStateMessage().toHave().state().off();
        ha().web().flagScene(52).click();
        mqtt().expect().flag(52).lastStateMessage().toHave().state().on();
    }

}
