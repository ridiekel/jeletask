package io.github.ridiekel.jeletask.client.builder.message.messages;

import io.github.ridiekel.jeletask.client.TestCentralUnit;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.GetMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.GroupGetMessage;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.junit.jupiter.api.Test;

import static io.github.ridiekel.jeletask.client.TestCentralUnit.DIMMER_1;
import static io.github.ridiekel.jeletask.client.TestCentralUnit.RELAY_1;
import static io.github.ridiekel.jeletask.client.TestCentralUnit.RELAY_2;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Frames follow TT15132-v3.8 §6 (STX, Length without ChkSm, Command, Parameters, ChkSm as the sum of all bytes) with
 * Get (command 06h) and Group Get (command 09h): Central Unit, Fnc (§5: relay 01h, dimmer 02h) and 2 bytes per number.
 */
class GetMessageSupportTest {
    private final TestCentralUnit unit = TestCentralUnit.on(0);
    private final MessageHandler handler = this.unit.getMessageHandler();

    private GetMessageSupport get(Function function, int number) {
        return new GetMessage(this.unit, function, number);
    }

    private GetMessageSupport groupGet(Function function, int... numbers) {
        return new GroupGetMessage(this.unit, function, numbers);
    }

    private String frame(GetMessageSupport message) {
        return Bytes.bytesToHex(this.handler.compose(message.getCommand(), message.getPayload()));
    }

    @Test
    void getAsksForOneNumberOfOneFunction() {
        assertThat(frame(get(Function.RELAY, RELAY_1))).isEqualTo("02 07 06 01 01 00 01 12");
        assertThat(frame(get(Function.DIMMER, DIMMER_1))).isEqualTo("02 07 06 01 02 00 07 19");
    }

    @Test
    void groupGetAsksForSeveralNumbersInOneFrame() {
        assertThat(frame(groupGet(Function.RELAY, RELAY_1, RELAY_2))).isEqualTo("02 09 09 01 01 00 01 00 02 19");
    }

    @Test
    void framesParseBackIntoAnEqualMessage() {
        GetMessageSupport get = get(Function.RELAY, RELAY_1);
        GetMessageSupport groupGet = groupGet(Function.RELAY, RELAY_1, RELAY_2);

        assertThat(this.handler.parse(this.unit, Bytes.hexToBytes(frame(get)))).isEqualTo(get).hasSameHashCodeAs(get);
        assertThat(this.handler.parse(this.unit, Bytes.hexToBytes(frame(groupGet)))).isEqualTo(groupGet).hasSameHashCodeAs(groupGet);
    }

    @Test
    void equalOnlyForTheSameKindFunctionAndNumbers() {
        GetMessageSupport get = get(Function.RELAY, RELAY_1);
        Object id = "GET RELAY(1)";

        assertThat(get)
                .isEqualTo(get)
                .isEqualTo(get(Function.RELAY, RELAY_1))
                .isNotEqualTo(get(Function.RELAY, RELAY_2))
                .isNotEqualTo(get(Function.DIMMER, RELAY_1))
                // Same function and numbers, but a different command on the wire
                .isNotEqualTo(groupGet(Function.RELAY, RELAY_1))
                .isNotEqualTo(null)
                .isNotEqualTo(id);
        assertThat(groupGet(Function.RELAY, RELAY_1, RELAY_2))
                .isNotEqualTo(groupGet(Function.RELAY, RELAY_2, RELAY_1))
                .isNotEqualTo(groupGet(Function.RELAY, RELAY_1));
    }

    @Test
    void theIdNamesCommandFunctionAndNumbers() {
        // The id ends up in the AcknowledgeException message
        assertThat(get(Function.RELAY, RELAY_1).getId()).isEqualTo("GET RELAY(1)");
        assertThat(groupGet(Function.RELAY, RELAY_1, RELAY_2).getId()).isEqualTo("GROUPGET RELAY(1,2)");
    }

    @Test
    void groupGetIsItsOwnCommand() {
        assertThat(get(Function.RELAY, RELAY_1).getCommand()).isEqualTo(Command.GET);
        assertThat(groupGet(Function.RELAY, RELAY_1).getCommand()).isEqualTo(Command.GROUPGET);
    }

    @Test
    void toStringShowsFunctionAndNumbers() {
        // MockServerCommand keys its expectations on this text
        assertThat(get(Function.RELAY, RELAY_1)).hasToString("GetMessage[function=RELAY,numbers={1}]");
        assertThat(groupGet(Function.RELAY, RELAY_1, RELAY_2)).hasToString("GroupGetMessage[function=RELAY,numbers={1,2}]");
    }

    @Test
    void logLinesNameTheComponents() {
        MessageSupport get = get(Function.RELAY, RELAY_1);
        MessageSupport groupGet = groupGet(Function.RELAY, RELAY_1, RELAY_2);

        assertThat(get.toLogLine("SENT")).isEqualTo("[SENT      ] - [RELAY     ] - [  1] - [%40s] - [GET       ]".formatted("Component 1"));
        // Group get puts the command first and the message last
        assertThat(groupGet.toLogLine("SENT")).isEqualTo("[GROUPGET  ] - [RELAY     ] - [1, 2] - [%40s] - [SENT      ]".formatted("Component 1, Component 2"));
    }
}
