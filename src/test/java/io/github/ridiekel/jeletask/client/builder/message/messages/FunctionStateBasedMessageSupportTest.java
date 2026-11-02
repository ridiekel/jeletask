package io.github.ridiekel.jeletask.client.builder.message.messages;

import io.github.ridiekel.jeletask.client.TestCentralUnit;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator.ValidOnOffToggle;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.LogMessage;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.impl.LogState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LogMessage has no equals of its own, so it shows what FunctionStateBasedMessageSupport adds: the state.
 * Frames follow TT15132-v3.8 §6 with Log (command 03h, no Central Unit parameter): Fnc and State.
 */
class FunctionStateBasedMessageSupportTest {
    private final TestCentralUnit unit = TestCentralUnit.on(0);

    private LogMessage log(Function function, ValidOnOffToggle state) {
        return new LogMessage(this.unit, function, state == null ? null : new LogState(state));
    }

    @Test
    void keepsTheStateItWasCreatedWith() {
        assertThat(log(Function.RELAY, ValidOnOffToggle.ON).getState()).isEqualTo(new LogState(ValidOnOffToggle.ON));
        assertThat(log(Function.RELAY, null).getState()).isNull();
    }

    @Test
    void equalOnlyForTheSameFunctionAndState() {
        LogMessage relayOn = log(Function.RELAY, ValidOnOffToggle.ON);
        Object stateOnly = new LogState(ValidOnOffToggle.ON);

        assertThat(relayOn)
                .isEqualTo(relayOn)
                .isEqualTo(log(Function.RELAY, ValidOnOffToggle.ON))
                .hasSameHashCodeAs(log(Function.RELAY, ValidOnOffToggle.ON))
                .isNotEqualTo(log(Function.RELAY, ValidOnOffToggle.OFF))
                .isNotEqualTo(log(Function.DIMMER, ValidOnOffToggle.ON))
                .isNotEqualTo(log(Function.RELAY, null))
                .isNotEqualTo(null)
                .isNotEqualTo(stateOnly);
        assertThat(log(Function.RELAY, null)).isEqualTo(log(Function.RELAY, null)).hasSameHashCodeAs(log(Function.RELAY, null));
    }

    @Test
    void aLogFrameParsesBackIntoAnEqualMessage() {
        MessageSupport relayOff = log(Function.RELAY, ValidOnOffToggle.OFF);
        byte[] frame = this.unit.getMessageHandler().compose(relayOff.getCommand(), relayOff.getPayload());

        assertThat(Bytes.bytesToHex(frame)).isEqualTo("02 05 03 01 00 0B");
        assertThat(this.unit.getMessageHandler().parse(this.unit, frame)).isEqualTo(relayOff);
    }
}
