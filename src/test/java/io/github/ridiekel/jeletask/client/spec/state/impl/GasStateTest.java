package io.github.ridiekel.jeletask.client.spec.state.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.ridiekel.jeletask.client.spec.state.State;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GasStateTest {
    private static GasState gas(String state, Number current, Number total) {
        GasState gas = new GasState(new BigDecimal(state));
        gas.setCurrent(current);
        gas.setTotal(total);
        return gas;
    }

    @Test
    void currentAndTotalAreKeptNextToTheState() {
        GasState gas = gas("21.02", 3, 1000L);

        assertThat(gas.getState()).isEqualByComparingTo("21.02");
        assertThat(gas.getCurrent()).isEqualTo(3);
        assertThat(gas.getTotal()).isEqualTo(1000L);
    }

    @Test
    void equalWhenStateCurrentAndTotalAreEqual() {
        GasState gas = gas("21.02", 3, 1000L);
        Object humidity = new HumidityState(new BigDecimal("21.02"));

        assertThat(gas)
                .isEqualTo(gas)
                .isEqualTo(gas("21.02", 3, 1000L))
                .hasSameHashCodeAs(gas("21.02", 3, 1000L))
                .isNotEqualTo(gas("21.03", 3, 1000L))
                .isNotEqualTo(gas("21.02", 4, 1000L))
                .isNotEqualTo(gas("21.02", 3, 1001L))
                .isNotEqualTo(null)
                .isNotEqualTo(humidity);
    }

    @Test
    void jsonLeavesOutWhatIsNotSet() {
        // The MQTT state payload: NON_NULL on State, so a plain gas reading only carries the state
        assertThat(new GasState(new BigDecimal("21.02"))).hasToString("{\"state\":21.02}");
        assertThat(gas("21.02", 3, 1000L)).hasToString("{\"state\":21.02,\"current\":3,\"total\":1000}");
    }

    @Test
    void jsonCanBeReadBack() throws JsonProcessingException {
        GasState gas = State.OBJECT_MAPPER.readValue("{\"state\":21.02}", GasState.class);

        assertThat(gas).isEqualTo(new GasState(new BigDecimal("21.02")));
    }
}
