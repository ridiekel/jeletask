package io.github.ridiekel.jeletask.client.builder.composer;

import io.github.ridiekel.jeletask.client.builder.composer.microsplus.MicrosPlusMessageHandler;
import io.github.ridiekel.jeletask.client.spec.CentralUnitType;

import java.util.Map;

public class MessageHandlerFactory {
    private static final Map<CentralUnitType, MessageHandler> COMPOSERS = Map.of(
            CentralUnitType.MICROS_PLUS, new MicrosPlusMessageHandler(),
            CentralUnitType.PICOS, new MicrosPlusMessageHandler(),
            CentralUnitType.NANOS, new MicrosPlusMessageHandler()
    );

    private MessageHandlerFactory() {
    }

    public static MessageHandler getMessageHandler(CentralUnitType centralUnitType) {
        return COMPOSERS.get(centralUnitType);
    }
}
