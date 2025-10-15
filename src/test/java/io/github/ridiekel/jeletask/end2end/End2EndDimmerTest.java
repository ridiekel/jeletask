package io.github.ridiekel.jeletask.end2end;

import io.github.ridiekel.jeletask.MockingTeletaskTestSupport;
import org.junit.jupiter.api.Test;

@SuppressWarnings("resource")
class End2EndDimmerTest extends MockingTeletaskTestSupport {
    @Test
    void dimmer() {
        ha().web().dimmer(9).toggle();
        mqtt().expect().dimmer(9).lastStateMessage().toHave().state().dimmerOn();
        ha().web().dimmer(9).toggle();
        mqtt().expect().dimmer(9).lastStateMessage().toHave().state().dimmerOff();

        teletask().dimmer(9).turnOn();
        ha().web().dimmer(9)
                .shouldBeChecked()
                .shouldHaveLightIcon()
                .shouldHaveIconStateOn();

        teletask().dimmer(9).turnOff();
        ha().web().dimmer(9)
                .shouldNotBeChecked()
                .shouldHaveLightIcon()
                .shouldHaveIconStateOff();


        ha().web().dimmer(9).openDetails();
        teletask().dimmer(9).turnOn();
        teletask().dimmer(9).turnOff();

        ha().web().dimmer(9)
                .shouldHaveSliderState("0")
                .shouldHaveValidSliderRange()
                .shouldHaveHeaderStateText("Off");

        teletask().dimmer(9).turnOn();
        ha().web().dimmer(9)
                .shouldHaveHeaderStateText("100%")
                .shouldHaveSliderState("100");

        teletask().dimmer(9).turnOff();
        ha().web().dimmer(9)
                .shouldHaveSliderState("0")
                .shouldHaveHeaderStateText("Off");

        teletask().dimmer(9).brightness(66);
        ha().web().dimmer(9)
                .shouldHaveSliderState("66")
                .shouldHaveHeaderStateText("66%");

        ha().web().dimmer(9)
                .slideToValue45()
                .shouldHaveSliderState("45")
                .shouldHaveHeaderStateText("45%");
        mqtt().expect().dimmer(9).lastStateMessage().toHave().state().dimmerOn();
        mqtt().expect().dimmer(9).lastStateMessage().toHave().state().brightness(45);

        ha().web().dimmer(9).closeDetails();
    }
}
