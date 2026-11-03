package io.github.ridiekel.jeletask.client.builder.composer.microsplus;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator.ValidOnOffToggle;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.EventMessage;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.CentralUnitType;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.component.PulseCounterSpec;
import io.github.ridiekel.jeletask.client.spec.component.RelaySpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;
import io.github.ridiekel.jeletask.client.spec.state.impl.PulseCounterState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MicrosPlusMessageHandlerTest {
    private final MicrosPlusMessageHandler handler = new MicrosPlusMessageHandler();

    private static CentralUnit centralUnit() {
        ComponentSpec relay = new RelaySpec();
        relay.setNumber(3);

        ComponentSpec pulseCounter = new PulseCounterSpec();
        pulseCounter.setNumber(6);

        Map<Function, List<ComponentSpec>> components = new LinkedHashMap<>();
        components.put(Function.RELAY, List.of(relay));
        components.put(Function.SENSOR, List.of(pulseCounter));

        CentralUnit centralUnit = new CentralUnit();
        centralUnit.setType(CentralUnitType.MICROS_PLUS);
        centralUnit.setComponentsTypes(components);
        return centralUnit;
    }

    /**
     * TT15132-v3.8 §6.5: Parameter 5 is the ErrorState, the states start at Parameter 6 and stop before the checksum.
     */
    @Test
    void parseEventReadsTheStateAfterTheErrorState() {
        EventMessage event = handler.parseEvent(centralUnit(), Bytes.hexToBytes("02 09 10 01 01 00 03 00 FF 1F"));

        assertThat(event.getFunction()).isEqualTo(Function.RELAY);
        assertThat(event.getNumber()).isEqualTo(3);
        assertThat(event.getState()).isEqualTo(new OnOffState(ValidOnOffToggle.ON));
    }

    @Test
    void parseEventAndParseAgreeOnAReportWithSeveralStates() {
        CentralUnit centralUnit = centralUnit();
        byte[] frame = Bytes.hexToBytes("02 1C 10 01 14 00 06 00 06 3A 00 00 00 00 00 00 00 1A 00 00 FF 00 00 00 00 0F 42 40 33");

        PulseCounterState state = (PulseCounterState) handler.parseEvent(centralUnit, frame).getState();

        assertThat(state.getCurrent()).isEqualTo((short) 1594);
        assertThat(state.getTotal()).isEqualTo(1000f);
        assertThat(state).isEqualTo(((EventMessage) handler.parse(centralUnit, frame)).getState());
    }
}
