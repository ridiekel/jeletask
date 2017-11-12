package io.github.ridiekel.jeletask.model.nbt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.ridiekel.jeletask.model.spec.ComponentSpec;

public abstract class ComponentSupport implements ComponentSpec {
    protected final int id;
    protected final Room room;
    protected final String description;
    protected final String type;
    private String state;

    public ComponentSupport(int id, Room room, String type, String description) {
        this.id = id;
        this.room = room;
        this.description = description;
        this.type = type;
    }

    @Override
    public String getState() {
        return this.state;
    }

    @Override
    public void setState(String state) {
        this.state = state;
    }

    public int getId() {
        return this.id;
    }

    @Override
    public int getNumber() {
        return this.getId();
    }

    @JsonIgnore
    public Room getRoom() {
        return this.room;
    }

    @Override
    public String getRoomName() {
        return this.room.getName();
    }

    public int getRoomId() {
        return this.getRoom().getId();
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    public String getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "id=" + this.id +
                ", description='" + this.description + '\'' +
                '}';
    }
}
