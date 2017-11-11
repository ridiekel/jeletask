package io.jeletask.teletask.client.builder.composer;

public enum LogState {
    ON(255),
    OFF(0);

    private final int byteValue;

    LogState(int byteValue) {
        this.byteValue = byteValue;
    }

    public int getByteValue() {
        return this.byteValue;
    }
}
