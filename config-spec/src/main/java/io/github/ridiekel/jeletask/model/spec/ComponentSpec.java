package io.github.ridiekel.jeletask.model.spec;

public interface ComponentSpec {
    String getState();

    void setState(String state);

    Function getFunction();

    int getNumber();

    String getRoomName();

    String getDescription();

    String getType();
}
