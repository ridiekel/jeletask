package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import io.github.ridiekel.jeletask.client.TestCentralUnit;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator.ValidOnOffToggle;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.junit.jupiter.api.Test;

import static io.github.ridiekel.jeletask.client.TestCentralUnit.RELAY_1;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Identity of an event: EventMessageTest has the parsing. The frame is an Event Report of TT15132-v3.8 §6.5 (command 10h).
 */
class EventMessageEqualityTest {
    private static final OnOffState ON = new OnOffState(ValidOnOffToggle.ON);

    private final TestCentralUnit unit = TestCentralUnit.on(0);

    private EventMessage relay1On() {
        return (EventMessage) this.unit.getMessageHandler().parse(this.unit, this.unit.event(Function.RELAY, RELAY_1, ON));
    }

    @Test
    void theFrameItCameFromIsAnEventReport() {
        assertThat(Bytes.bytesToHex(relay1On().getRawBytes())).isEqualTo("02 09 10 01 01 00 01 00 FF 1D");
    }

    @Test
    void equalToTheSameEventOnly() {
        EventMessage event = relay1On();
        // Same function and number, but a command instead of a report
        Object set = new SetMessage(this.unit, Function.RELAY, RELAY_1, ON);

        assertThat(event)
                .isEqualTo(event)
                .isEqualTo(relay1On())
                .hasSameHashCodeAs(relay1On())
                .isNotEqualTo(null)
                .isNotEqualTo(set);
    }

    @Test
    void theIdNamesFunctionAndNumber() {
        assertThat(relay1On().getId()).isEqualTo("EVENT RELAY(1)");
    }

    @Test
    void toStringShowsFunctionNumberAndStateJson() {
        assertThat(relay1On()).hasToString("EventMessage[function=RELAY,number=1,state={\"state\":\"ON\"}]");
    }
}
