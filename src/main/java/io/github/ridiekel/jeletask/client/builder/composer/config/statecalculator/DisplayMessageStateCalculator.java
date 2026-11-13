package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.component.DisplayMessageSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.DisplayMessageState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;

public class DisplayMessageStateCalculator extends StateCalculatorSupport<DisplayMessageState> {
    private static final Logger LOG = LogManager.getLogger();
    // A line holds 16 bytes: 16 ASCII characters or 8 UTF-16 ones (TT15132 §3.6)
    public static final int ASCII_LINE_LENGTH = 16;
    public static final int UNICODE_LINE_LENGTH = 8;
    // Hyphen, en dash, em dash and slash: a line may break after them
    private static final String BREAK_AFTER = "-\u2013\u2014/";

    /**
     * How many characters fit on a line of a display.
     */
    public static int lineLength(boolean ascii) {
        return ascii ? ASCII_LINE_LENGTH : UNICODE_LINE_LENGTH;
    }

    @Override
    protected Class<DisplayMessageState> getStateType() {
        return DisplayMessageState.class;
    }

    @Override
    public byte[] toCommand(ComponentSpec component, DisplayMessageState message) {
        DisplayMessageState state = withDefaultsOf(component, message);
        return Bytes.concat(
                toMessageTypeBytes(state),
                toIsAsciiBytes(state),
                toMessageLineBytes(component, state, 1, DisplayMessageState::getMessageLine1),
                toMessageLineBytes(component, state, 2, DisplayMessageState::getMessageLine2),
                toNumberOfBeepsBytes(state)
        );
    }

    @Override
    public DisplayMessageState fromEvent(ComponentSpec component, byte[] dataBytes) {
        long number = NumberConverter.UNSIGNED_BYTE.convert(dataBytes).longValue();
        return new DisplayMessageState(number == -1 ? 255 : number);
    }

    /**
     * The message type, ascii and beeps a message leaves out come from the config of the display, if it has them.
     * A {@code message} is spread over both lines, see {@link #wrap(String, int)}.
     */
    private static DisplayMessageState withDefaultsOf(ComponentSpec component, DisplayMessageState message) {
        DisplayMessageSpec display = component instanceof DisplayMessageSpec d ? d : new DisplayMessageSpec();
        Boolean ascii = Optional.ofNullable(message.getAscii()).orElse(display.getAscii());
        String[] lines = lines(message, lineLength(Boolean.TRUE.equals(ascii)));
        return DisplayMessageState.builder()
                .messageLine1(lines[0])
                .messageLine2(lines[1])
                .ascii(ascii)
                .messageType(Optional.ofNullable(message.getMessageType()).orElse(display.getMessageType()))
                .messageBeeps(Optional.ofNullable(message.getMessageBeeps()).orElse(display.getMessageBeeps()))
                .build();
    }

    private static String[] lines(DisplayMessageState message, int length) {
        if (message.getMessage() == null) {
            return new String[]{message.getMessageLine1(), message.getMessageLine2()};
        }
        if (message.getMessageLine1() != null || message.getMessageLine2() != null) {
            throw new IllegalArgumentException("A display message has either a 'message' or 'message_line1' and 'message_line2', not both");
        }
        return wrap(message.getMessage(), length);
    }

    /**
     * Spreads a text over the two lines of a display: line 1 ends at the last place that still fits where a line may
     * break, the rest goes to line 2. A line may break at a space, which then disappears, and after a hyphen, a dash or
     * a slash, which stay on line 1. Not after a full stop or a comma: without a space after them they are usually
     * part of a number. A word longer than a line is split where the line ends. What does not fit on line 2 is left
     * for {@link #toMessageLineBytes} to cut off, with a warning.
     */
    static String[] wrap(String text, int length) {
        String trimmed = text.strip();
        if (trimmed.length() <= length) {
            return new String[]{trimmed, null};
        }
        int end = length;
        while (end > 0 && trimmed.charAt(end) != ' ' && BREAK_AFTER.indexOf(trimmed.charAt(end - 1)) < 0) {
            end--;
        }
        if (end == 0) {
            end = length;
        }
        return new String[]{trimmed.substring(0, end).stripTrailing(), trimmed.substring(end).strip()};
    }

    private static byte[] toMessageLineBytes(ComponentSpec component, DisplayMessageState state, int line, Function<DisplayMessageState, String> extractor) {
        boolean ascii = isAscii(state);
        int length = lineLength(ascii);
        String text = Optional.ofNullable(extractor.apply(state)).orElse("");
        if (text.length() > length) {
            LOG.warn("Display message{} line {} '{}' has {} characters, the display shows {}{}: cut to '{}'",
                    displayNumber(component), line, text, text.length(), length, ascii ? " with ascii" : " without ascii (16 with ascii)", StringUtils.left(text, length));
        }
        return StringUtils.rightPad(StringUtils.left(text, length), length)
                .getBytes(ascii ? StandardCharsets.US_ASCII : StandardCharsets.UTF_16BE);
    }

    private static String displayNumber(ComponentSpec component) {
        return component == null ? "" : " " + component.getNumber();
    }

    // Left out (and not set on the display either) means unicode, as it always did
    private static boolean isAscii(DisplayMessageState state) {
        return Boolean.TRUE.equals(state.getAscii());
    }

    private static byte[] toIsAsciiBytes(DisplayMessageState state) {
        return new byte[]{(byte) (isAscii(state) ? 1 : 0)};
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
