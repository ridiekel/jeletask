package io.github.ridiekel.jeletask.client.builder;

public enum SendResult {
    SUCCESS(1),
    FAILED(0);

    private final int code;

    SendResult(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
