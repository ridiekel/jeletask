package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.apache.commons.lang3.StringUtils;

public class DisplayMessageStateCalculator extends SimpleStateCalculator {

    public DisplayMessageStateCalculator(NumberConverter numberConverter) {
        super(numberConverter);
    }

    @Override
    public byte[] toBytes(ComponentState state) {
        byte[] bytes_msg1 = null;
        byte[] bytes_msg2 = null;
        byte[] beeps = null;
        boolean is_message = true;  // Default = message
        String str_msg1 = null;
        String str_msg2 = null;

        // Alarm requested? Set is_message to false.
        if (state.getMessageType() != null) {
            if (state.getMessageType().equalsIgnoreCase("alarm"))
                is_message = false;
        }

        // msgType: 0x01 = message, 0x00 = alarm
        byte[] msgType = new byte[]{(byte)(is_message?1:0)};

        if (state.getMessageLine1() != null)
            str_msg1 = StringUtils.left(state.getMessageLine1(), 16);
        else
            str_msg1 = StringUtils.repeat(' ', 16);;

        if (state.getMessageLine2() != null)
            str_msg2 = StringUtils.left(state.getMessageLine2(), 16);
        else
            str_msg2 = StringUtils.repeat(' ', 16);;

        // Create our message byte arrays
        bytes_msg1 = String.format("%-16s", str_msg1).getBytes();
        bytes_msg2 = String.format("%-16s", str_msg2).getBytes();

        // Beeps
        if (state.getMessageBeeps() != null)
            // How many beeps?
            beeps = new byte[]{state.getMessageBeeps().byteValue()};
        else
            // Default: 1 beep
            beeps = new byte[]{(byte)1};

        boolean is_ascii = true; // true = ascii, false = unicode

        return Bytes.concat(msgType, new byte[]{(byte)(is_ascii?1:0)}, bytes_msg1, bytes_msg2, beeps);
    }

}
