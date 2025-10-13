package io.github.ridiekel.jeletask;

import com.codeborne.selenide.Selenide;
import org.junit.jupiter.api.Test;

@SuppressWarnings("resource")
class TeletaskServiceTest extends MockingTeletaskTestSupport {

    @Test
    void motor() {
        ha().web().motor(7).openDetails();


        teletask().motor(7).down();
        Selenide.sleep(1000);
        mqtt().expect().motor(7).lastStateMessage().toHave().state().motorDown();

        ha().web().motor(7)
                .shouldHaveValidSliderRange()
                .shouldHaveHeaderStateText("Closing");

        Selenide.sleep(4000);

        ha().web().motor(7)
                .shouldHaveSliderState("0")
                .shouldHaveHeaderStateText("Closed");

        ha().web().motor(7).slideToValue44();
        mqtt().expect().motor(7).lastStateMessage().toHave().state().motorUp();
        Selenide.sleep(1000);

        ha().web().motor(7).shouldHaveHeaderStateText("Opening");
        ha().web().motor(7)
                .shouldHaveSliderState("44")
                .shouldHaveHeaderStateText("Open");

        teletask().motor(7).up();
        Selenide.sleep(1000);
        mqtt().expect().motor(7).lastStateMessage().toHave().state().motorUp();

        ha().web().motor(7).shouldHaveHeaderStateText("Opening");

        Selenide.sleep(4000);

        ha().web().motor(7)
                .shouldHaveSliderState("100")
                .shouldHaveHeaderStateText("Open");

        ha().web().motor(7).closeDetails();
    }

    @Test
    void inputShortPress() {
        teletask().input(10).close();
        mqtt().expect().input(10).lastStateMessage().toHave().state().notPressed();
        ha().web().input(10)
                .shouldHaveStateTextNotPressed()
                .shouldHaveSensorIcon()
                .shouldHaveIconStateNotPressed();
        Selenide.sleep(750);

        teletask().input(10).open();

        Selenide.sleep(500);

        mqtt().expect().input(10).lastStateMessage(1).toHave().state().shortPress();
        mqtt().expect().input(10).lastStateMessage(0).toHave().state().notPressed();

        //SHORT_PRESS is sent to HA, but at this time I cannot verify this, since it happens too fast. Maybe in the HA history page?

        ha().web().input(10)
                .shouldHaveStateTextNotPressed()
                .shouldHaveSensorIcon()
                .shouldHaveIconStateNotPressed();
    }

    @Test
    void inputWithoutTiming() {
        teletask().input(11).close();
        mqtt().expect().input(11).lastStateMessage().toHave().state().inputClosed();
        ha().web().input(11)
                .shouldHaveStateTextClosed()
                .shouldHaveSensorIcon()
                .shouldHaveIconStateClosed();
        Selenide.sleep(2000);

        teletask().input(11).open();

        Selenide.sleep(500);

        mqtt().expect().input(11).lastStateMessage(0).toHave().state().inputOpen();

        ha().web().input(11)
                .shouldHaveStateTextOpen()
                .shouldHaveSensorIcon()
                .shouldHaveIconStateOpen();
    }

    @Test
    void inputLongPress() {
        teletask().input(10).close();
        mqtt().expect().input(10).lastStateMessage().toHave().state().notPressed();
        ha().web().input(10)
                .shouldHaveStateTextNotPressed()
                .shouldHaveSensorIcon()
                .shouldHaveIconStateNotPressed();
        Selenide.sleep(2000);

        teletask().input(10).open();

        Selenide.sleep(500);

        mqtt().expect().input(10).lastStateMessage(1).toHave().state().longPress();
        mqtt().expect().input(10).lastStateMessage(0).toHave().state().notPressed();

        //LONG_PRESS is sent to HA, but at this time I cannot verify this, since it happens too fast. Maybe in the HA history page?

        ha().web().input(10)
                .shouldHaveStateTextNotPressed()
                .shouldHaveSensorIcon()
                .shouldHaveIconStateNotPressed();
    }

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

    @Test
    void flag() {
        ha().web().flag(5).toggle();
        mqtt().expect().flag(5).lastStateMessage().toHave().state().off();
        ha().web().flag(5).toggle();
        mqtt().expect().flag(5).lastStateMessage().toHave().state().on();

        teletask().flag(5).turnOff();
        ha().web().flag(5)
                .shouldNotBeChecked()
                .shouldHaveSwitchIcon()
                .shouldHaveIconStateOff();

        teletask().flag(5).turnOn();
        ha().web().flag(5)
                .shouldBeChecked()
                .shouldHaveSwitchIcon()
                .shouldHaveIconStateOn();
    }

    @Test
    void generalmood() {
        ha().web().generalmood(6).toggle();
        mqtt().expect().generalmood(6).lastStateMessage().toHave().state().off();
        ha().web().generalmood(6).toggle();
        mqtt().expect().generalmood(6).lastStateMessage().toHave().state().on();

        teletask().generalmood(6).turnOff();
        ha().web().generalmood(6)
                .shouldNotBeChecked()
                .shouldHaveSwitchIcon()
                .shouldHaveIconStateOff();

        teletask().generalmood(6).turnOn();
        ha().web().generalmood(6)
                .shouldBeChecked()
                .shouldHaveSwitchIcon()
                .shouldHaveIconStateOn();
    }

    @Test
    void localmood() {
        ha().web().localmood(8).toggle();
        mqtt().expect().localmood(8).lastStateMessage().toHave().state().off();
        ha().web().localmood(8).toggle();
        mqtt().expect().localmood(8).lastStateMessage().toHave().state().on();

        teletask().localmood(8).turnOff();
        ha().web().localmood(8)
                .shouldNotBeChecked()
                .shouldHaveSwitchIcon()
                .shouldHaveIconStateOff();

        teletask().localmood(8).turnOn();
        ha().web().localmood(8)
                .shouldBeChecked()
                .shouldHaveIconStateOn()
                .shouldHaveSwitchIcon();
    }

    @Test
    void light() {
        ha().web().relay(1).toggle();
        mqtt().expect().relay(1).lastStateMessage().toHave().state().off();
        ha().web().relay(1).toggle();
        mqtt().expect().relay(1).lastStateMessage().toHave().state().on();

        teletask().relay(1).turnOff();
        ha().web().relay(1)
                .shouldNotBeChecked()
                .shouldHaveIconStateOff()
                .shouldHaveLightIcon();

        teletask().relay(1).turnOn();
        ha().web().relay(1)
                .shouldBeChecked()
                .shouldHaveLightIcon()
                .shouldHaveIconStateOn();
    }

    @Test
    void switchRelay() {
        ha().web().relay(2).toggle();
        mqtt().expect().relay(2).lastStateMessage().toHave().state().off();
        ha().web().relay(2).toggle();
        mqtt().expect().relay(2).lastStateMessage().toHave().state().on();

        teletask().relay(2).turnOff();
        ha().web().relay(2)
                .shouldNotBeChecked()
                .shouldHaveSwitchIcon()
                .shouldHaveIconStateOff();

        teletask().relay(2).turnOn();
        ha().web().relay(2)
                .shouldBeChecked()
                .shouldHaveSwitchIcon()
                .shouldHaveIconStateOn();
    }

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
