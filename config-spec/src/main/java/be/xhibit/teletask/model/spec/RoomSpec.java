package be.xhibit.teletask.model.spec;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

@JsonSerialize(as=RoomSpec.class)
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