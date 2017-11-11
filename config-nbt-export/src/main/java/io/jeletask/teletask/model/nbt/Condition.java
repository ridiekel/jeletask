package io.jeletask.teletask.model.nbt;

import io.jeletask.teletask.model.spec.Function;

public class Condition extends ComponentSupport {

    public Condition(int id, Room room, String type, String description) {
        super(id, room, type, description);
    }

    @Override
    public Function getFunction() {
        return Function.COND;
    }
}
