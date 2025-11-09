package io.github.ridiekel.jeletask.end2end;

import com.codeborne.selenide.Selenide;
import io.github.ridiekel.jeletask.MockingTeletaskTestSupport;
import org.junit.jupiter.api.Test;

@SuppressWarnings("resource")
class End2EndInputTest extends MockingTeletaskTestSupport {
    @Test
    void inputShortPress() {
        teletask().input(10).close();
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
    void inputLongPressWithRelease() {
        teletask().input(10).close();
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
    void inputVeryLongPressWithoutRelease() {
        teletask().input(10).close();
        ha().web().input(10)
                .shouldHaveStateTextNotPressed()
                .shouldHaveSensorIcon()
                .shouldHaveIconStateNotPressed();
        Selenide.sleep(2000);

        mqtt().expect().input(10).lastStateMessage(1).toHave().state().longPress();
        mqtt().expect().input(10).lastStateMessage(0).toHave().state().notPressed();

        //LONG_PRESS is sent to HA, but at this time I cannot verify this, since it happens too fast. Maybe in the HA history page?

        ha().web().input(10)
                .shouldHaveStateTextNotPressed()
                .shouldHaveSensorIcon()
                .shouldHaveIconStateNotPressed();
    }
}
