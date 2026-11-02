package io.github.ridiekel.jeletask.client.spec;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CentralUnitTest {
    @Test
    void deviceNumberIsTheNegatedAbsoluteHashCode() {
        assertThat(CentralUnit.deviceNumber("MICROS+")).isEqualTo(-Math.abs("MICROS+".hashCode()));
        assertThat(CentralUnit.deviceNumber("a")).isEqualTo(-97);
    }

    @Test
    void deviceNumberKeepsMinValueForAHashCodeOfMinValue() {
        // This number ends up in MQTT topics and HA unique ids, so it may not change for this edge case either
        String name = "polygenelubricants";
        assertThat(name.hashCode()).isEqualTo(Integer.MIN_VALUE);

        assertThat(CentralUnit.deviceNumber(name)).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void getDeviceUsesTheDeviceNumber() {
        CentralUnit centralUnit = new CentralUnit();

        ComponentSpec device = centralUnit.getDevice("a");

        assertThat(device.getNumber()).isEqualTo(-97);
        assertThat(device.getFunction()).isEqualTo(Function.SENSOR);
        assertThat(centralUnit.getDevice("a")).isSameAs(device);
    }
}
