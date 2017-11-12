package io.github.ridiekel.jeletask.client.builder.composer;

import io.github.ridiekel.jeletask.client.builder.composer.v2_8.MicrosMessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.v3_1.MicrosPlusMessageHandler;
import io.github.ridiekel.jeletask.model.spec.CentralUnitType;

import java.util.Map;

public class MessageHandlerFactory {
    private static final Map<CentralUnitType, MessageHandler> COMPOSERS = Map.of(
            CentralUnitType.MICROS, new MicrosMessageHandler(),
            CentralUnitType.MICROS_PLUS, new MicrosPlusMessageHandler()
    );

    private MessageHandlerFactory() {
    }

    public static MessageHandler getMessageHandler(CentralUnitType centralUnitType) {
        return COMPOSERS.get(centralUnitType);
    }
}
