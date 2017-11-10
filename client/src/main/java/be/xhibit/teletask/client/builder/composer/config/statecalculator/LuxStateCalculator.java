package be.xhibit.teletask.client.builder.composer.config.statecalculator;

import be.xhibit.teletask.client.builder.composer.config.NumberConverter;
import be.xhibit.teletask.model.spec.ComponentSpec;

public class LuxStateCalculator extends SimpleStateCalculator {
    public LuxStateCalculator(NumberConverter numberConverter) {
        super(numberConverter);
    }

    @Override
    public String convertGet(ComponentSpec component, byte[] value) {
        long longValue = this.getNumberConverter().convert(value).longValue();
        double exponent = longValue / 40d;
        double powered = Math.pow(10, exponent);
        double luxValue = powered - 1;
        return String.valueOf(Math.round(luxValue));
    }

    public static void main(String[] args) {
        String s = new LuxStateCalculator(NumberConverter.UNSIGNED_BYTE).convertGet(null, new byte[]{(byte) 0x80});
        System.out.println("s = " + s);
    }

    @Override
    public byte[] convertSet(ComponentSpec component, String value) {
        Long longValue = Long.valueOf(value);
        long inBetween = longValue + 1;
        double log10 = Math.log10(inBetween);
        double convertedValue = log10 * 40;
        return this.getNumberConverter().convert(Math.round(convertedValue));
    }

    @Override
    public String getDefaultState(ComponentSpec component) {
        return "3547";
    }
}