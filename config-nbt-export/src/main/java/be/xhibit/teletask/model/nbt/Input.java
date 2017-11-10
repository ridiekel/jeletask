package be.xhibit.teletask.model.nbt;

import be.xhibit.teletask.model.spec.ComponentSpec;

public class Input {
    private final String id;
    private final String name;
    private final ComponentSpec shortAction;
    private final ComponentSpec longAction;

    public Input(String id, String name, ComponentSpec shortAction, ComponentSpec longAction) {
        this.id = id;
        this.name = name;
        this.shortAction = shortAction;
        this.longAction = longAction;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public ComponentSpec getShortAction() {
        return this.shortAction;
    }

    public ComponentSpec getLongAction() {
        return this.longAction;
    }
}
