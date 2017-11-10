package be.xhibit.teletask.client.builder.composer.config.statecalculator;

import be.xhibit.teletask.client.builder.composer.config.NumberConverter;
import be.xhibit.teletask.model.spec.ComponentSpec;

public class TemperatureStateCalculator extends SimpleStateCalculator {
    private final double divide;
    private final int subtract;

    public TemperatureStateCalculator(NumberConverter numberConverter, int divide, int subtract) {
        super(numberConverter);
        this.divide = divide;
        this.subtract = subtract;
    }

    @Override
    public String convertGet(ComponentSpec component, byte[] value) {
        long base = this.getNumberConverter().convert(value).longValue();
        double divided = base / this.divide;
        double subtracted = divided - this.subtract;
        return String.valueOf(subtracted);
    }

    @Override
    public byte[] convertSet(ComponentSpec component, String value) {
        Long base = Long.valueOf(value);
        long added = base + this.subtract;
        double multiplied = added * this.divide;
        return this.getNumberConverter().convert(Math.round(multiplied));
    }

    @Override
    public String getDefaultState(ComponentSpec component) {
        return "18";
    }
}