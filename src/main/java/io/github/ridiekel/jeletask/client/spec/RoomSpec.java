package io.github.ridiekel.jeletask.client.spec;

import java.util.List;

public interface RoomSpec {
    int getId();

    String getName();

    List<? extends ComponentSpec> getRelays();

    List<? extends ComponentSpec> getLocalMoods();

    List<? extends ComponentSpec> getGeneralMoods();

    List<? extends ComponentSpec> getMotors();

    List<? extends ComponentSpec> getDimmers();

    List<? extends ComponentSpec> getConditions();

    List<? extends ComponentSpec> getSensors();
}
