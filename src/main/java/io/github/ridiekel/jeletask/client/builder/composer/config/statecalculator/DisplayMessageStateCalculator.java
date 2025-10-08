package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.DisplayMessageState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;

public class DisplayMessageStateCalculator extends StateCalculatorSupport<DisplayMessageState> {
    @Override
    protected Class<DisplayMessageState> getStateType() {
        return DisplayMessageState.class;
    }

    @Override
    public byte[] toCommand(DisplayMessageState state) {
        return Bytes.concat(
                toMessageTypeBytes(state),
                toIsAsciiBytes(state),
                toMessageLineBytes(state, DisplayMessageState::getMessageLine1),
                toMessageLineBytes(state, DisplayMessageState::getMessageLine2),
                toNumberOfBeepsBytes(state)
        );
    }

    @Override
    public DisplayMessageState fromEvent(ComponentSpec component, byte[] dataBytes) {
        long number = NumberConverter.UNSIGNED_BYTE.convert(dataBytes).longValue();
        return new DisplayMessageState(number == -1 ? 255 : number);
    }

    private static byte[] toMessageLineBytes(DisplayMessageState state, Function<DisplayMessageState, String> extractor) {
        int length = state.isAscii() ? 16 : 8;
        return String.format("%-" + length + "s",
                Optional.ofNullable(extractor.apply(state))
                        .map(m -> StringUtils.left(m, length))
                        .orElseGet(() -> StringUtils.repeat(' ', length))
        ).getBytes(state.isAscii() ? StandardCharsets.US_ASCII : StandardCharsets.UTF_16BE);
    }

    private static byte[] toIsAsciiBytes(DisplayMessageState state) {
        return new byte[]{(byte) (state.isAscii() ? 1 : 0)};
    }

    private static byte[] toNumberOfBeepsBytes(DisplayMessageState state) {
        return Optional.ofNullable(state.getMessageBeeps())
                .map(b -> new byte[]{b.byteValue()})
                .orElseGet(() -> new byte[]{(byte) 1});
    }

    private static byte[] toMessageTypeBytes(DisplayMessageState state) {
        // msgType: 0x01 = message, 0x00 = alarm
        return new byte[]{(byte) (state.isMessage() ? 1 : 0)};
    }
}
