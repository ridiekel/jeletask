package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.LuxState;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class LuxStateCalculator extends StateCalculatorSupport<LuxState> {

    public static final NumberConverter NUMBER_CONVERTER = NumberConverter.UNSIGNED_SHORT;
    public static final BigDecimal FORTY = new BigDecimal("40");

    @Override
    public LuxState fromEvent(ComponentSpec component, byte[] dataBytes) {
        BigDecimal exponent = BigDecimal.valueOf(NUMBER_CONVERTER.convert(dataBytes).longValue())
                .divide(FORTY, 2, RoundingMode.HALF_UP);
        BigDecimal luxValue = BigDecimal.TEN.pow(exponent.intValue()).subtract(BigDecimal.ONE);
        return new LuxState(luxValue);
    }

    @Override
    protected Class<LuxState> getStateType() {
        return LuxState.class;
    }

    @Override
    public byte[] toCommand(LuxState value) {
        BigDecimal inBetween = value.getState().add(BigDecimal.ONE);
        BigDecimal convertedValue = BigDecimal.valueOf(Math.log10(inBetween.doubleValue())).multiply(FORTY);
        return NUMBER_CONVERTER.convert(convertedValue.longValue());
    }
}
