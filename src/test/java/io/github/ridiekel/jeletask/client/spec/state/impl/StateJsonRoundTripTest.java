package io.github.ridiekel.jeletask.client.spec.state.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator.ValidOnOffToggle;
import io.github.ridiekel.jeletask.client.spec.state.State;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * States without a calculator of their own only get built through their no-argument constructor when Jackson reads them,
 * which is also what the native reflection hints keep them around for.
 */
class StateJsonRoundTripTest {
    @Test
    void aStringStateRoundTrips() throws JsonProcessingException {
        StringState version = new StringState("6.4.1");

        String json = version.toString();
        StringState read = State.OBJECT_MAPPER.readValue(json, StringState.class);

        assertThat(json).isEqualTo("{\"state\":\"6.4.1\"}");
        assertThat(read).isEqualTo(version).isNotSameAs(version);
    }

    @Test
    void aLogStateRoundTripsAndIsNoPlainOnOffState() throws JsonProcessingException {
        LogState on = new LogState(ValidOnOffToggle.ON);

        String json = on.toString();
        LogState read = State.OBJECT_MAPPER.readValue(json, LogState.class);

        assertThat(json).isEqualTo("{\"state\":\"ON\"}");
        assertThat(read).isEqualTo(on).isNotEqualTo(new OnOffState(ValidOnOffToggle.ON));
    }

    @Test
    void enumValuesAreReadCaseInsensitive() throws JsonProcessingException {
        assertThat(State.OBJECT_MAPPER.readValue("{\"state\":\"off\"}", LogState.class).getState()).isEqualTo(ValidOnOffToggle.OFF);
    }

    @Test
    void anEmptyObjectGivesAStateWithoutValue() throws JsonProcessingException {
        assertThat(State.OBJECT_MAPPER.readValue("{}", StringState.class).getState()).isNull();
        assertThat(State.OBJECT_MAPPER.readValue("{}", LogState.class).getState()).isNull();
    }
}
