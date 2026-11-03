package io.github.ridiekel.jeletask.mockserver;

import io.github.ridiekel.jeletask.client.builder.message.messages.impl.EventMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.GroupGetMessage;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.CentralUnitType;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.component.DisplayMessageSpec;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TeletaskMockServerDisplayMessageTest {
    @Test
    void groupGetOfDisplayMessagesAnswersWithADisplayMessageEvent() {
        ComponentSpec display = new DisplayMessageSpec();
        display.setNumber(1000);

        CentralUnit centralUnit = new CentralUnit();
        centralUnit.setType(CentralUnitType.MICROS_PLUS);
        centralUnit.setComponentsTypes(Map.of(Function.DISPLAYMESSAGE, List.of(display)));

        TeletaskMockServer server = new TeletaskMockServer(0, centralUnit);
        server.mock(server::mockDisplayMessage);

        GroupGetMessage groupGet = new GroupGetMessage(centralUnit, Function.DISPLAYMESSAGE, 1000);
        EventMessage event = server.getMocks().get(new MockServerCommand(groupGet).toString()).getFirst().get().create(centralUnit, groupGet);

        assertThat(event.getFunction()).isEqualTo(Function.DISPLAYMESSAGE);
        assertThat(event.getNumber()).isEqualTo(1000);
        // TT15132-v3.8 §6.5 and §5: Fnc is the fifth byte, FNC_MESSAGE = 54 = 36h, followed by the number 03 E8
        assertThat(Bytes.bytesToHex(event.getRawBytes())).startsWith("02 2B 10 01 36 03 E8 00 ");
    }
}
