package io.jeletask.model.nbt;

import io.jeletask.model.spec.Function;

public class GeneralMood extends ComponentSupport {
    public GeneralMood(int id, Room room, String type, String description) {
        super(id, room, type, description);
    }

    @Override
    public Function getFunction() {
        return Function.GENMOOD;
    }
}
