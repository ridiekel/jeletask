package io.jeletask.client.builder.composer.config.statecalculator;

import io.jeletask.client.builder.composer.config.NumberConverter;
import io.jeletask.model.spec.ComponentSpec;

public interface StateCalculator {
    String convertGet(ComponentSpec component, byte[] value);

    byte[] convertSet(ComponentSpec component, String value);

    NumberConverter getNumberConverter();

    boolean isValidState(String state);

    String getDefaultState(ComponentSpec component);
}
