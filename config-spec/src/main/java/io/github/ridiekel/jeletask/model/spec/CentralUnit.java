package io.github.ridiekel.jeletask.model.spec;

import java.util.List;

public interface CentralUnit {
    String getHost();

    int getPort();

    ComponentSpec getComponent(Function function, int number);

    List<? extends ComponentSpec> getComponents(Function function);

    List<? extends ComponentSpec> getAllComponents();

    CentralUnitType getCentralUnitType();

    List<? extends RoomSpec> getRooms();
}
