package io.jeletask.model.nbt;

import io.jeletask.model.spec.Function;

public class Motor extends ComponentSupport {
    public Motor(int id, Room room, String type, String description) {
        super(id, room, type, description);
    }

    @Override
    public Function getFunction() {
        return Function.MOTOR;
    }
}
