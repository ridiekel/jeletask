package io.github.ridiekel.jeletask.model.spec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

@JsonSerialize(as=CentralUnit.class)
public interface CentralUnit {
    String getHost();

    int getPort();

    ComponentSpec getComponent(Function function, int number);

    @JsonIgnore
    List<? extends ComponentSpec> getComponents(Function function);

    @JsonIgnore
    List<? extends ComponentSpec> getAllComponents();

    CentralUnitType getCentralUnitType();

    List<? extends RoomSpec> getRooms();
}
