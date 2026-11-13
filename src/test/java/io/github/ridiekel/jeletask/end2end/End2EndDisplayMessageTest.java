package io.github.ridiekel.jeletask.end2end;

import io.github.ridiekel.jeletask.MockingTeletaskTestSupport;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandlerFactory;
import io.github.ridiekel.jeletask.client.spec.CentralUnitType;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.mockserver.TeletaskMockServer;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("resource")
class End2EndDisplayMessageTest extends MockingTeletaskTestSupport {
    private static final MessageHandler HANDLER = MessageHandlerFactory.getMessageHandler(CentralUnitType.MICROS_PLUS);
    // Display 60 in test-config.json: bus 1 address 13 and bus 1 address 5
    private static final String SET_TOPIC = "test_prefix_teletask2mqtt/MAN_TEST_localhost/displaymessage/60/set";
    private static final String HELLO = "{\"message_type\":\"message\",\"ascii\":true,\"message_line1\":\"Hello\",\"message_line2\":\"World\",\"message_beeps\":2}";

    @Autowired
    private TeletaskMockServer teletaskServer;

    @BeforeEach
    void forgetEarlierDisplayMessages() {
        teletaskServer.clearReceivedDisplayMessages();
    }

    /**
     * TT15132-v3.8 §6.6: the buses, then the addresses, then message type, ascii, both lines padded to 16 and the beeps.
     */
    private static String helloOnBothDisplays() {
        byte[] payload = Bytes.concat(
                new byte[]{1, 1},
                new byte[]{13, 5},
                new byte[]{1},
                new byte[]{1},
                String.format("%-16s", "Hello").getBytes(StandardCharsets.US_ASCII),
                String.format("%-16s", "World").getBytes(StandardCharsets.US_ASCII),
                new byte[]{2}
        );
        return Bytes.bytesToHex(HANDLER.compose(Command.DISPLAYMESSAGE, payload));
    }

    private List<String> displayMessagesAtTheCentralUnit(int atLeast) {
        Awaitility.await("" + atLeast + " display message(s) at the central unit")
                .atMost(Duration.ofSeconds(10))
                .until(() -> teletaskServer.receivedDisplayMessages().size() >= atLeast);
        return teletaskServer.receivedDisplayMessages().stream().map(Bytes::bytesToHex).toList();
    }

    @Test
    void aMessageOverMqttIsShownOnTheDisplays() {
        mqtt().publish(SET_TOPIC, HELLO);

        assertThat(displayMessagesAtTheCentralUnit(1)).containsExactly(helloOnBothDisplays());
    }

    @Test
    void theSameMessageTwiceIsShownTwice() {
        // A display has no state to compare with: a doorbell rings twice, so the second message must not be dropped
        mqtt().publish(SET_TOPIC, HELLO);
        displayMessagesAtTheCentralUnit(1);
        mqtt().publish(SET_TOPIC, HELLO);

        assertThat(displayMessagesAtTheCentralUnit(2)).containsExactly(helloOnBothDisplays(), helloOnBothDisplays());
    }
}
