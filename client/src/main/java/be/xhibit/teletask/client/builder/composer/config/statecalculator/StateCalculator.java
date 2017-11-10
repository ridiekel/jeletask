package be.xhibit.teletask.client.builder.composer.config.statecalculator;

import be.xhibit.teletask.client.builder.composer.config.NumberConverter;
import be.xhibit.teletask.model.spec.ComponentSpec;

public interface StateCalculator {
    String convertGet(ComponentSpec component, byte[] value);

    byte[] convertSet(ComponentSpec component, String value);

    NumberConverter getNumberConverter();

    boolean isValidState(String state);

    String getDefaultState(ComponentSpec component);
}
