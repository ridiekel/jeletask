package io.jeletask.teletask.client.builder.composer.config.statecalculator;

import io.jeletask.teletask.client.builder.composer.config.NumberConverter;
import io.jeletask.teletask.model.spec.ComponentSpec;

public interface StateCalculator {
    String convertGet(ComponentSpec component, byte[] value);

    byte[] convertSet(ComponentSpec component, String value);

    NumberConverter getNumberConverter();

    boolean isValidState(String state);

    String getDefaultState(ComponentSpec component);
}
