package io.github.ridiekel.jeletask.end2end;

import com.codeborne.selenide.Selenide;
import io.github.ridiekel.jeletask.MockingTeletaskTestSupport;
import org.junit.jupiter.api.Test;

@SuppressWarnings("resource")
class End2EndMotorTest extends MockingTeletaskTestSupport {

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
}
