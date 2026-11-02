package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import io.github.ridiekel.jeletask.client.TestCentralUnit;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.impl.DisplayMessageState;
import org.junit.jupiter.api.Test;

import static io.github.ridiekel.jeletask.client.TestCentralUnit.DISPLAY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Identity of a display message: TeletaskClientImpl and the mock server compare and log them, DisplayMessageTest has the frames.
 */
class DisplayMessageEqualityTest {
    private final TestCentralUnit unit = TestCentralUnit.on(0);

    private static DisplayMessageState text(String line1) {
        return DisplayMessageState.builder().messageType("message").ascii(true).messageLine1(line1).messageBeeps(1).build();
    }

    private DisplayMessage display(DisplayMessageState state) {
        return new DisplayMessage(this.unit, Function.DISPLAYMESSAGE, DISPLAY, state);
    }

    @Test
    void equalOnlyForTheSameDisplayAndText() {
        DisplayMessage hello = display(text("HELLO"));
        Object get = new GetMessage(this.unit, Function.DISPLAYMESSAGE, DISPLAY);

        assertThat(hello)
                .isEqualTo(hello)
                .isEqualTo(display(text("HELLO")))
                .hasSameHashCodeAs(display(text("HELLO")))
                .isNotEqualTo(display(text("BYE")))
                .isNotEqualTo(null)
                .isNotEqualTo(get);
    }

    @Test
    void theIdNamesTheDisplayNumber() {
        assertThat(display(text("HELLO")).getId()).isEqualTo("DISPLAYMESSAGE DISPLAYMESSAGE(5)");
    }

    @Test
    void toStringShowsFunctionNumberAndTheTextOfTheState() {
        assertThat(display(text("HELLO")).toString())
                .startsWith("DisplayMessage[function=DISPLAYMESSAGE,number=5,state={")
                .contains("\"message_line1\":\"HELLO\"")
                .endsWith("}]");
    }
}
