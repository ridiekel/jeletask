package be.xhibit.teletask.client.builder.composer.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class Configurable<T> {
    private final int number;
    private final T object;

    public Configurable(int number, T object) {
        this.number = number;
        this.object = object;
    }

    public T getObject() {
        return this.object;
    }

    public int getNumber() {
        return this.number;
    }

    public byte[] getBytes() {
        return new byte[]{(byte) this.number};
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
