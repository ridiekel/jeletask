package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import ch.obermuhlner.math.big.BigDecimalMath;
import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.LuxState;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class LuxStateCalculator extends StateCalculatorSupport<LuxState> {

    public static final NumberConverter NUMBER_CONVERTER = NumberConverter.UNSIGNED_SHORT;
    public static final BigDecimal FORTY = new BigDecimal("40");

    private static BigDecimal byteToLux(long asLong) {
        BigDecimal exponent = BigDecimal.valueOf(asLong).divide(FORTY, 50, RoundingMode.HALF_UP);
        BigDecimal luxValue = BigDecimalMath.pow(BigDecimal.TEN, exponent, new MathContext(50, RoundingMode.HALF_UP));
        return luxValue.setScale(0, RoundingMode.HALF_UP);
    }

    private static long luxToByte(BigDecimal value) {
        BigDecimal inBetween = value.add(BigDecimal.ONE);
        BigDecimal log10 = BigDecimalMath.log10(inBetween, new MathContext(50, RoundingMode.HALF_UP));
        BigDecimal convertedValue = log10.multiply(FORTY);
        return convertedValue.longValue();
    }

    @Override
    public LuxState fromEvent(ComponentSpec component, byte[] dataBytes) {
        return new LuxState(byteToLux(NUMBER_CONVERTER.convert(dataBytes).longValue()));
    }

    @Override
    public byte[] toCommand(LuxState value) {
        return NUMBER_CONVERTER.convert(luxToByte(value.getState()));
    }

    @Override
    protected Class<LuxState> getStateType() {
        return LuxState.class;
    }
}
